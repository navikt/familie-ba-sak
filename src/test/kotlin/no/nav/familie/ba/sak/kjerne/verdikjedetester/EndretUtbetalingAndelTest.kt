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

class EndretUtbetalingAndelTest(
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) : AbstractVerdikjedetest() {

    @Test
    fun `Skal teste at endret utbetalingsandel overskriver eksisterende utbetalingsandel`() {
        val (scenario, restFagsakEtterBehandlingsresultat) = genererBehandlingsresultat()

        val behandlingEtterBehandlingsresultat =
            hentAktivBehandling(restFagsak = restFagsakEtterBehandlingsresultat.data!!)

        val andelerTilkjentYtelse =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandlingEtterBehandlingsresultat.behandlingId)

        val endretFom = andelerTilkjentYtelse.last().stønadFom
        val endretTom = andelerTilkjentYtelse.last().stønadTom.minusMonths(2)

        val restEndretUtbetalingAndel = RestEndretUtbetalingAndel(
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
            restEndretUtbetalingAndel
        )

        val andelerTilkjentYtelseMedEndretPeriode =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandlingEtterBehandlingsresultat.behandlingId)

        val endretAndeleTilkjentYtelse =
            andelerTilkjentYtelseMedEndretPeriode.single { it.kalkulertUtbetalingsbeløp === 0 }

        Assertions.assertEquals(
            endretAndeleTilkjentYtelse.stønadFom,
            endretFom
        )

        Assertions.assertEquals(
            endretAndeleTilkjentYtelse.stønadTom,
            endretTom
        )

        val førsteUtbetalingAndeleTilkjentYtelse =
            andelerTilkjentYtelseMedEndretPeriode.last { it.kalkulertUtbetalingsbeløp !== 0 }

        Assertions.assertEquals(
            førsteUtbetalingAndeleTilkjentYtelse.stønadFom,
            endretTom.plusMonths(1)
        )

        Assertions.assertEquals(
            førsteUtbetalingAndeleTilkjentYtelse.stønadTom,
            endretTom.plusMonths(2)
        )
    }

    @Test
    fun `Skal teste at fjernet endret utbetalingsandel oppretter tidligere eksisterende utbetalingsandel`() {
        val (scenario, restFagsakEtterBehandlingsresultat) = genererBehandlingsresultat()

        val behandling = hentAktivBehandling(restFagsak = restFagsakEtterBehandlingsresultat.data!!)

        val andelerTilkjentYtelse =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling.behandlingId)

        val endretFom = andelerTilkjentYtelse.last().stønadFom
        val endretTom = andelerTilkjentYtelse.last().stønadTom.minusMonths(2)

        val restEndretUtbetalingAndel = RestEndretUtbetalingAndel(
            id = null,
            personIdent = scenario.barna.first().ident,
            prosent = BigDecimal(0),
            fom = endretFom,
            tom = endretTom,
            årsak = Årsak.DELT_BOSTED,
            avtaletidspunktDeltBosted = LocalDate.now(),
            søknadstidspunkt = LocalDate.now(),
            begrunnelse = "begrunnelse",
            erTilknyttetAndeler = true,
        )

        val fagSakEtterEndretPeriode = familieBaSakKlient().leggTilEndretUtbetalingAndel(
            behandling.behandlingId,
            restEndretUtbetalingAndel
        )

        val endretUtbetalingAndelId =
            fagSakEtterEndretPeriode.data!!.behandlinger.first().endretUtbetalingAndeler.first().id

        familieBaSakKlient().fjernEndretUtbetalingAndel(behandling.behandlingId, endretUtbetalingAndelId!!)

        val andelerTilkjentYtelseEtterFjeringAvEndretUtbetaling =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling.behandlingId)

        Assertions.assertEquals(
            andelerTilkjentYtelseEtterFjeringAvEndretUtbetaling.last().stønadFom,
            endretFom
        )

        Assertions.assertEquals(
            andelerTilkjentYtelseEtterFjeringAvEndretUtbetaling.last().stønadTom,
            endretTom.plusMonths(2)
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
            behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR
        )

        val aktivBehandling = hentAktivBehandling(restFagsak = restFagsakMedBehandling.data!!)
        val restRegistrerSøknad =
            RestRegistrerSøknad(
                søknad = lagSøknadDTO(
                    søkerIdent = scenario.søker.ident,
                    barnasIdenter = scenario.barna.map { it.ident!! },
                    underkategori = BehandlingUnderkategori.ORDINÆR
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

        familieBaSakKlient().validerVilkårsvurdering(
            behandlingId = aktivBehandlingEtterRegistrertSøknad.behandlingId
        )

        val restFagsakEtterBehandlingsresultat =
            familieBaSakKlient().behandlingsresultatStegOgGåVidereTilNesteSteg(
                behandlingId = aktivBehandlingEtterRegistrertSøknad.behandlingId
            )
        return Pair(scenario, restFagsakEtterBehandlingsresultat)
    }
}
