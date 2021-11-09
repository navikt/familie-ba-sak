package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.ekstern.restDomene.RestEndretUtbetalingAndel
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.dokument.BrevService
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.flettefelt
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
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
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val brevService: BrevService,
) : AbstractVerdikjedetest() {

    @Test
    fun `Endret utbetalingsandeler for ordinær og utvidet endrer utbetaling for søker og barn og får kun én periode over satsendring`() {
        val (scenario, restBehandlingEtterVilkårsvurdering) = genererBehandlingsresultat()

        val andelerTilkjentYtelse =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = restBehandlingEtterVilkårsvurdering.behandlingId)

        val sorterteAndelerTilikjentYtelse = andelerTilkjentYtelse.sortedBy { it.stønadFom }
        val endretFomFørSatsendring = sorterteAndelerTilikjentYtelse[sorterteAndelerTilikjentYtelse.size - 2].stønadFom
        val endretTomEtterSatsendring = LocalDate.now().toYearMonth().minusMonths(1)

        val restEndretUtbetalingAndelUtvidetBarnetrygd = RestEndretUtbetalingAndel(
            id = null,
            personIdent = scenario.søker.ident,
            prosent = BigDecimal(0),
            fom = endretFomFørSatsendring,
            tom = endretTomEtterSatsendring,
            årsak = Årsak.DELT_BOSTED,
            avtaletidspunktDeltBosted = LocalDate.now(),
            søknadstidspunkt = LocalDate.now(),
            begrunnelse = "begrunnelse",
            erTilknyttetAndeler = true,
        )

        familieBaSakKlient().leggTilEndretUtbetalingAndel(
            restBehandlingEtterVilkårsvurdering.behandlingId,
            restEndretUtbetalingAndelUtvidetBarnetrygd
        )

        val restEndretUtbetalingAndelOrdinærBarnetrygd = RestEndretUtbetalingAndel(
            id = null,
            personIdent = scenario.barna.first().ident,
            prosent = BigDecimal(0),
            fom = endretFomFørSatsendring,
            tom = endretTomEtterSatsendring,
            årsak = Årsak.DELT_BOSTED, avtaletidspunktDeltBosted = LocalDate.now(),
            søknadstidspunkt = LocalDate.now(),
            begrunnelse = "begrunnelse",
            erTilknyttetAndeler = true,
        )

        familieBaSakKlient().leggTilEndretUtbetalingAndel(
            restBehandlingEtterVilkårsvurdering.behandlingId,
            restEndretUtbetalingAndelOrdinærBarnetrygd
        )

        val behandlingsresultatRessurs = familieBaSakKlient().behandlingsresultatStegOgGåVidereTilNesteSteg(
            behandlingId = restBehandlingEtterVilkårsvurdering.behandlingId
        )

        if (behandlingsresultatRessurs.status != Ressurs.Status.SUKSESS) {
            error("Klarte ikke å gjennomføre behandlingsresultatsteg.")
        }

        val andelerTilkjentYtelseMedEndretPeriode =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(
                behandlingId = restBehandlingEtterVilkårsvurdering.behandlingId
            )

        val endretAndeleTilkjentYtelser =
            andelerTilkjentYtelseMedEndretPeriode.filter { it.kalkulertUtbetalingsbeløp == 0 }

        val vedtak = vedtakService.hentAktivForBehandling(restBehandlingEtterVilkårsvurdering.behandlingId)!!
        val brevdata = brevService.lagVedtaksbrevFellesfelter(vedtak)

        Assertions.assertEquals(
            1,
            brevdata.perioder.size
        )

        Assertions.assertEquals(
            flettefelt(endretFomFørSatsendring.førsteDagIInneværendeMåned().tilDagMånedÅr()),
            brevdata.perioder.single().fom
        )

        Assertions.assertEquals(
            flettefelt("til ${endretTomEtterSatsendring.sisteDagIInneværendeMåned().tilDagMånedÅr()} "),
            brevdata.perioder.single().tom
        )

        Assertions.assertEquals(
            endretAndeleTilkjentYtelser.single { it.personIdent == scenario.barna.first().ident }.stønadFom,
            endretFomFørSatsendring
        )

        Assertions.assertEquals(
            endretAndeleTilkjentYtelser.single { it.personIdent == scenario.barna.first().ident }.stønadTom,
            endretTomEtterSatsendring
        )

        Assertions.assertEquals(
            endretAndeleTilkjentYtelser.single { it.personIdent == scenario.søker.ident }.stønadFom,
            endretFomFørSatsendring
        )

        Assertions.assertEquals(
            endretAndeleTilkjentYtelser.single { it.personIdent == scenario.søker.ident }.stønadTom,
            endretTomEtterSatsendring
        )
    }

    private fun genererBehandlingsresultat(): Pair<RestScenario, RestUtvidetBehandling> {
        val barnFødselsdato = LocalDate.now().minusYears(6)

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
        val restUtvidetBehandling = familieBaSakKlient().opprettBehandling(
            søkersIdent = søkersIdent,
            behandlingUnderkategori = BehandlingUnderkategori.UTVIDET
        ).data!!

        val restRegistrerSøknad =
            RestRegistrerSøknad(
                søknad = lagSøknadDTO(
                    søkerIdent = scenario.søker.ident,
                    barnasIdenter = scenario.barna.map { it.ident!! },
                    underkategori = BehandlingUnderkategori.UTVIDET
                ),
                bekreftEndringerViaFrontend = false
            )
        val restBehandlingEtterRegistrertSøknad: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().registrererSøknad(
                behandlingId = restUtvidetBehandling.behandlingId,
                restRegistrerSøknad = restRegistrerSøknad
            )

        restBehandlingEtterRegistrertSøknad.data!!.personResultater.forEach { restPersonResultat ->
            restPersonResultat.vilkårResultater.filter { it.resultat == Resultat.IKKE_VURDERT }.forEach {
                familieBaSakKlient().putVilkår(
                    behandlingId = restUtvidetBehandling.behandlingId,
                    vilkårId = it.id,
                    restPersonResultat =
                    RestPersonResultat(
                        personIdent = restPersonResultat.personIdent,
                        vilkårResultater = listOf(
                            it.copy(
                                resultat = Resultat.OPPFYLT,
                                periodeFom = barnFødselsdato.plusYears(3),
                                erDeltBosted = it.vilkårType == Vilkår.BOR_MED_SØKER
                            )
                        )
                    )
                )
            }
        }

        val restBehandlingEtterVilkårsvurdering = familieBaSakKlient().validerVilkårsvurdering(
            behandlingId = restUtvidetBehandling.behandlingId
        )

        return Pair(scenario, restBehandlingEtterVilkårsvurdering.data!!)
    }
}
