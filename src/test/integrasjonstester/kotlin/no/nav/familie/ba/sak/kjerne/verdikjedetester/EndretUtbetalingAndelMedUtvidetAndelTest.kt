package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.datagenerator.lagSøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.EndretUtbetalingAndelDto
import no.nav.familie.ba.sak.ekstern.restDomene.PersonResultatDto
import no.nav.familie.ba.sak.ekstern.restDomene.RegistrerSøknadDto
import no.nav.familie.ba.sak.ekstern.restDomene.UtvidetBehandlingDto
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.ScenarioDto
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.ScenarioPersonDto
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.stubScenario
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

class EndretUtbetalingAndelMedUtvidetAndelTest(
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) : AbstractVerdikjedetest() {
    @Test
    fun `Skal teste at endret utbetalingsandeler for ordinær og utvidet endrer utbetaling for søker og barn`() {
        val barnFødselsdato = LocalDate.of(2022, 10, 15)

        val scenario =
            ScenarioDto(
                søker = ScenarioPersonDto(fødselsdato = "1996-01-12", fornavn = "Mor", etternavn = "Søker"),
                barna =
                    listOf(
                        ScenarioPersonDto(
                            fødselsdato = barnFødselsdato.toString(),
                            fornavn = "Barn",
                            etternavn = "Barnesen",
                        ),
                    ),
            ).also { stubScenario(it) }

        val søkersIdent = scenario.søker.ident

        val fagsak = familieBaSakKlient().opprettFagsak(søkersIdent = søkersIdent)
        val utvidetBehandlingDto =
            familieBaSakKlient()
                .opprettBehandling(
                    søkersIdent = søkersIdent,
                    behandlingUnderkategori = BehandlingUnderkategori.UTVIDET,
                    fagsakId = fagsak.data!!.id,
                ).data!!

        val registrerSøknadDto =
            RegistrerSøknadDto(
                søknad =
                    lagSøknadDTO(
                        søkerIdent = scenario.søker.ident,
                        barnasIdenter = scenario.barna.map { it.ident },
                        underkategori = BehandlingUnderkategori.UTVIDET,
                    ),
                bekreftEndringerViaFrontend = false,
            )
        val utvidetBehandlingDtoEtterRegistrertSøknad: Ressurs<UtvidetBehandlingDto> =
            familieBaSakKlient().registrererSøknad(
                behandlingId = utvidetBehandlingDto.behandlingId,
                registrerSøknadDto = registrerSøknadDto,
            )

        utvidetBehandlingDtoEtterRegistrertSøknad.data!!.personResultater.forEach { personResultatDto ->
            personResultatDto.vilkårResultater.forEach {
                familieBaSakKlient().putVilkår(
                    behandlingId = utvidetBehandlingDtoEtterRegistrertSøknad.data?.behandlingId!!,
                    vilkårId = it.id,
                    personResultatDto =
                        PersonResultatDto(
                            personIdent = personResultatDto.personIdent,
                            vilkårResultater =
                                listOf(
                                    it.copy(
                                        resultat = Resultat.OPPFYLT,
                                        periodeFom = barnFødselsdato,
                                        utdypendeVilkårsvurderinger =
                                            listOfNotNull(
                                                if (it.vilkårType == Vilkår.BOR_MED_SØKER) UtdypendeVilkårsvurdering.DELT_BOSTED else null,
                                            ),
                                        begrunnelse = "Oppfylt",
                                    ),
                                ),
                        ),
                )
            }
        }

        val utvidetBehandlingDtoEtterBehandlingsresultat =
            familieBaSakKlient()
                .validerVilkårsvurdering(
                    behandlingId = utvidetBehandlingDtoEtterRegistrertSøknad.data?.behandlingId!!,
                ).data!!

        val endretFom = barnFødselsdato.nesteMåned()
        val endretTom = endretFom.plusMonths(2)

        val endretUtbetalingAndelDtoUtvidetBarnetrygd =
            EndretUtbetalingAndelDto(
                id = null,
                personIdenter = listOf(scenario.søker.ident),
                prosent = BigDecimal(0),
                fom = endretFom,
                tom = endretTom,
                årsak = Årsak.DELT_BOSTED,
                avtaletidspunktDeltBosted = LocalDate.now(),
                søknadstidspunkt = LocalDate.now(),
                begrunnelse = "begrunnelse",
                erTilknyttetAndeler = true,
            )

        familieBaSakKlient().leggTilEndretUtbetalingAndel(
            utvidetBehandlingDtoEtterBehandlingsresultat.behandlingId,
            endretUtbetalingAndelDtoUtvidetBarnetrygd,
        )

        val endretUtbetalingAndelDtoOrdinærBarnetrygd =
            EndretUtbetalingAndelDto(
                id = null,
                personIdenter = listOf(scenario.barna.first().ident),
                prosent = BigDecimal(0),
                fom = endretFom,
                tom = endretTom,
                årsak = Årsak.DELT_BOSTED,
                avtaletidspunktDeltBosted = LocalDate.now(),
                søknadstidspunkt = LocalDate.now(),
                begrunnelse = "begrunnelse",
                erTilknyttetAndeler = true,
            )

        familieBaSakKlient().leggTilEndretUtbetalingAndel(
            utvidetBehandlingDtoEtterBehandlingsresultat.behandlingId,
            endretUtbetalingAndelDtoOrdinærBarnetrygd,
        )

        familieBaSakKlient().behandlingsresultatStegOgGåVidereTilNesteSteg(
            behandlingId = utvidetBehandlingDtoEtterBehandlingsresultat.behandlingId,
        )

        val andelerTilkjentYtelseMedEndretPeriode =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = utvidetBehandlingDtoEtterBehandlingsresultat.behandlingId)

        val endredeAndelerTilkjentYtelse =
            andelerTilkjentYtelseMedEndretPeriode.filter { it.kalkulertUtbetalingsbeløp == 0 }

        Assertions.assertEquals(
            endredeAndelerTilkjentYtelse.single { it.aktør.aktivFødselsnummer() == scenario.barna.first().ident }.stønadFom,
            endretFom,
        )

        Assertions.assertEquals(
            endredeAndelerTilkjentYtelse.single { it.aktør.aktivFødselsnummer() == scenario.barna.first().ident }.stønadTom,
            endretTom,
        )

        Assertions.assertEquals(
            endredeAndelerTilkjentYtelse.single { it.aktør.aktivFødselsnummer() == scenario.søker.ident }.stønadFom,
            endretFom,
        )

        Assertions.assertEquals(
            endredeAndelerTilkjentYtelse.single { it.aktør.aktivFødselsnummer() == scenario.søker.ident }.stønadTom,
            endretTom,
        )
    }
}
