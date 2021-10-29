package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.integrasjoner.`ef-sak`.EfSakRestClient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.SatsService.sisteSmåbarnstilleggSatsTilTester
import no.nav.familie.ba.sak.kjerne.beregning.SatsService.sisteUtvidetSatsTilTester
import no.nav.familie.ba.sak.kjerne.beregning.SatsService.tilleggOrdinærSatsTilTester
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import java.time.LocalDate

class BehandleFørstegangssøknadSmåbarnstilleggTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingService: BehandlingService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val featureToggleService: FeatureToggleService,
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    @Autowired private val efSakRestClient: EfSakRestClient
) : AbstractVerdikjedetest() {

    @Test
    fun `Skal behandle utvidet nasjonal sak med småbarnstillegg`() {
        every { featureToggleService.isEnabled(any()) } returns true

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

        val periodeMedFullOvergangsstønadFom = barnFødselsdato.plusYears(1)
        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } returns PerioderOvergangsstønadResponse(
            perioder = listOf(
                PeriodeOvergangsstønad(
                    personIdent = søkersIdent,
                    fomDato = periodeMedFullOvergangsstønadFom,
                    tomDato = barnFødselsdato.plusYears(18),
                    datakilde = PeriodeOvergangsstønad.Datakilde.EF
                )
            )
        )

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
        generellAssertFagsak(
            restFagsak = restFagsakEtterRegistrertSøknad,
            fagsakStatus = FagsakStatus.OPPRETTET,
            behandlingStegType = StegType.VILKÅRSVURDERING
        )

        // Godkjenner alle vilkår på førstegangsbehandling.
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
                                periodeFom = barnFødselsdato
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
        val behandlingEtterBehandlingsresultat =
            hentAktivBehandling(restFagsak = restFagsakEtterBehandlingsresultat.data!!)

        assertEquals(
            tilleggOrdinærSatsTilTester.beløp + sisteUtvidetSatsTilTester.beløp + sisteSmåbarnstilleggSatsTilTester.beløp,
            hentNåværendeEllerNesteMånedsUtbetaling(
                behandling = behandlingEtterBehandlingsresultat
            )
        )

        val andelerTilkjentYtelse =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandlingEtterBehandlingsresultat.behandlingId)
        val utvidedeAndeler = andelerTilkjentYtelse.filter { it.type == YtelseType.UTVIDET_BARNETRYGD }
        val småbarnstilleggAndeler = andelerTilkjentYtelse.filter { it.type == YtelseType.SMÅBARNSTILLEGG }

        assertEquals(
            barnFødselsdato.plusMonths(1).toYearMonth(),
            utvidedeAndeler.minByOrNull { it.stønadFom }?.stønadFom
        )
        assertEquals(
            periodeMedFullOvergangsstønadFom.toYearMonth(),
            småbarnstilleggAndeler.minByOrNull { it.stønadFom }?.stønadFom
        )
        assertEquals(
            barnFødselsdato.plusYears(3).toYearMonth(),
            småbarnstilleggAndeler.maxByOrNull { it.stønadFom }?.stønadTom
        )

        generellAssertFagsak(
            restFagsak = restFagsakEtterBehandlingsresultat,
            fagsakStatus = FagsakStatus.OPPRETTET,
            behandlingStegType = StegType.VURDER_TILBAKEKREVING
        )

        val restFagsakEtterVurderTilbakekreving = familieBaSakKlient().lagreTilbakekrevingOgGåVidereTilNesteSteg(
            behandlingEtterBehandlingsresultat.behandlingId,
            RestTilbakekreving(Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING, begrunnelse = "begrunnelse")
        )
        generellAssertFagsak(
            restFagsak = restFagsakEtterVurderTilbakekreving,
            fagsakStatus = FagsakStatus.OPPRETTET,
            behandlingStegType = StegType.SEND_TIL_BESLUTTER
        )

        val vedtaksperiodeId =
            restFagsakEtterVurderTilbakekreving.data!!.behandlinger.single { it.aktiv }.vedtak!!.vedtaksperioderMedBegrunnelser.first()
        familieBaSakKlient().oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = vedtaksperiodeId.id,
            restPutVedtaksperiodeMedStandardbegrunnelser = RestPutVedtaksperiodeMedStandardbegrunnelser(
                standardbegrunnelser = listOf(
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_BOR_HOS_SØKER
                )
            )
        )

        val restFagsakEtterSendTilBeslutter =
            familieBaSakKlient().sendTilBeslutter(fagsakId = restFagsakEtterVurderTilbakekreving.data!!.id)

        generellAssertFagsak(
            restFagsak = restFagsakEtterSendTilBeslutter,
            fagsakStatus = FagsakStatus.OPPRETTET,
            behandlingStegType = StegType.BESLUTTE_VEDTAK
        )

        val restFagsakEtterIverksetting =
            familieBaSakKlient().iverksettVedtak(
                fagsakId = restFagsakEtterVurderTilbakekreving.data!!.id,
                restBeslutningPåVedtak = RestBeslutningPåVedtak(
                    Beslutning.GODKJENT
                ),
                beslutterHeaders = HttpHeaders().apply {
                    setBearerAuth(
                        token(
                            mapOf(
                                "groups" to listOf("SAKSBEHANDLER", "BESLUTTER"),
                                "azp" to "e2e-test",
                                "name" to "Mock McMockface Beslutter",
                                "preferred_username" to "mock.mcmockface.beslutter@nav.no"
                            )
                        )
                    )
                }
            )
        generellAssertFagsak(
            restFagsak = restFagsakEtterIverksetting,
            fagsakStatus = FagsakStatus.OPPRETTET,
            behandlingStegType = StegType.IVERKSETT_MOT_OPPDRAG
        )

        håndterIverksettingAvBehandling(
            behandlingEtterVurdering = behandlingService.hentAktivForFagsak(fagsakId = restFagsakEtterSendTilBeslutter.data!!.id)!!,
            søkerFnr = scenario.søker.ident,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService
        )
    }
}
