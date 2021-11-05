package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.RestEndretUtbetalingAndel
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
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
        val (scenario, restFagsakEtterBehandlingsresultat) = genererBehandlingsresultat()

        val behandlingEtterBehandlingsresultat =
            hentAktivBehandling(restFagsak = restFagsakEtterBehandlingsresultat.data!!)

        val andelerTilkjentYtelse =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandlingEtterBehandlingsresultat.behandlingId)

        val endretFom = andelerTilkjentYtelse.last().stønadFom
        val endretTom = andelerTilkjentYtelse.last().stønadTom.minusMonths(2)

        val restEndretUtbetalingAndelUtvidetBarnetrygd = RestEndretUtbetalingAndel(
            id = null,
            personIdent = scenario.søker.ident,
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
            behandlingEtterBehandlingsresultat.behandlingId,
            restEndretUtbetalingAndelUtvidetBarnetrygd
        )

        val restEndretUtbetalingAndelOrdinærBarnetrygd = RestEndretUtbetalingAndel(
            id = null,
            personIdent = scenario.barna.first().ident,
            prosent = BigDecimal(0),
            fom = endretFom,
            tom = endretTom,
            årsak = Årsak.DELT_BOSTED, avtaletidspunktDeltBosted = LocalDate.now(),
            søknadstidspunkt = LocalDate.now(),
            begrunnelse = "begrunnelse",
            erTilknyttetAndeler = true,
        )

        familieBaSakKlient().leggTilEndretUtbetalingAndel(
            behandlingEtterBehandlingsresultat.behandlingId,
            restEndretUtbetalingAndelOrdinærBarnetrygd
        )

        familieBaSakKlient().behandlingsresultatStegOgGåVidereTilNesteSteg(
            behandlingId = behandlingEtterBehandlingsresultat.behandlingId
        )

        val andelerTilkjentYtelseMedEndretPeriode =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandlingEtterBehandlingsresultat.behandlingId)

        val endretAndeleTilkjentYtelser =
            andelerTilkjentYtelseMedEndretPeriode.filter { it.kalkulertUtbetalingsbeløp === 0 }

        Assertions.assertEquals(
            endretAndeleTilkjentYtelser.single { it.personIdent == scenario.barna.first().ident }.stønadFom,
            endretFom
        )

        Assertions.assertEquals(
            endretAndeleTilkjentYtelser.single { it.personIdent == scenario.barna.first().ident }.stønadTom,
            endretTom
        )

        Assertions.assertEquals(
            endretAndeleTilkjentYtelser.single { it.personIdent == scenario.søker.ident }.stønadFom,
            endretFom
        )

        Assertions.assertEquals(
            endretAndeleTilkjentYtelser.single { it.personIdent == scenario.søker.ident }.stønadTom,
            endretTom
        )
    }

    private fun genererBehandlingsresultat(): Pair<RestScenario, Ressurs<RestFagsak>> {
        val barnFødselsdato = LocalDate.now().minusYears(3)

        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1996-01-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = barnFødselsdato.toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                        bostedsadresser = emptyList()
                    )
                )
            )
        )

        val søkersIdent = scenario.søker.ident!!

        familieBaSakKlient().opprettFagsak(søkersIdent = søkersIdent)
        val restFagsakMedBehandling = familieBaSakKlient().opprettBehandling(
            søkersIdent = søkersIdent,
            behandlingUnderkategori = BehandlingUnderkategori.UTVIDET
        )

        val aktivBehandling = hentAktivBehandling(restFagsak = restFagsakMedBehandling.data!!)
        val restRegistrerSøknad =
            RestRegistrerSøknad(
                søknad = lagSøknadDTO(
                    søkerIdent = scenario.søker.ident,
                    barnasIdenter = scenario.barna.map { it.ident!! },
                    underkategori = BehandlingUnderkategori.UTVIDET
                ),
                bekreftEndringerViaFrontend = false
            )
        val restFagsakEtterRegistrertSøknad: Ressurs<RestFagsak> =
            familieBaSakKlient().registrererSøknad(
                behandlingId = aktivBehandling.behandlingId,
                restRegistrerSøknad = restRegistrerSøknad
            )

        val aktivBehandlingEtterRegistrertSøknad = hentAktivBehandling(restFagsakEtterRegistrertSøknad.data!!)
        aktivBehandlingEtterRegistrertSøknad.personResultater.forEach { restPersonResultat ->
            restPersonResultat.vilkårResultater.filter { it.resultat == Resultat.IKKE_VURDERT }.forEach {
                familieBaSakKlient().putVilkår(
                    behandlingId = aktivBehandlingEtterRegistrertSøknad.behandlingId,
                    vilkårId = it.id,
                    restPersonResultat =
                    RestPersonResultat(
                        personIdent = restPersonResultat.personIdent,
                        vilkårResultater = listOf(
                            it.copy(
                                resultat = Resultat.OPPFYLT,
                                periodeFom = barnFødselsdato,
                                erDeltBosted = it.vilkårType == Vilkår.BOR_MED_SØKER
                            )
                        )
                    )
                )
            }
        }

        val restFagsakEtterBehandlingsresultat = familieBaSakKlient().validerVilkårsvurdering(
            behandlingId = aktivBehandlingEtterRegistrertSøknad.behandlingId
        )

        return Pair(scenario, restFagsakEtterBehandlingsresultat)
    }
}
