package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.NavnOgIdent
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.SatsService.sisteUtvidetSatsTilTester
import no.nav.familie.ba.sak.kjerne.beregning.SatsService.tilleggOrdinærSatsTilTester
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import java.time.LocalDate

class JournalførOgBehandleFørstegangssøknadNasjonalTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingService: BehandlingService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val featureToggleService: FeatureToggleService
) : AbstractVerdikjedetest() {

    @Test
    fun `Skal journalføre og behandle ordinær nasjonal sak`() {
        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1996-11-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = LocalDate.now().minusMonths(6).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                        bostedsadresser = emptyList()
                    )
                )
            )
        )

        val fagsakId: Ressurs<String> = familieBaSakKlient().journalfør(
            journalpostId = "1234",
            oppgaveId = "5678",
            journalførendeEnhet = "4833",
            restJournalføring = lagMockRestJournalføring(
                bruker = NavnOgIdent(
                    navn = scenario.søker.navn,
                    id = scenario.søker.ident!!
                )
            )
        )

        assertEquals(Ressurs.Status.SUKSESS, fagsakId.status)

        val restFagsakEtterJournalføring = familieBaSakKlient().hentFagsak(fagsakId = fagsakId.data?.toLong()!!)
        generellAssertFagsak(
            restFagsak = restFagsakEtterJournalføring,
            fagsakStatus = FagsakStatus.OPPRETTET,
            behandlingStegType = StegType.REGISTRERE_SØKNAD
        )

        val aktivBehandling = hentAktivBehandling(restFagsak = restFagsakEtterJournalføring.data!!)
        val restRegistrerSøknad =
            RestRegistrerSøknad(
                søknad = lagSøknadDTO(
                    søkerIdent = scenario.søker.ident,
                    barnasIdenter = scenario.barna.map { it.ident!! }
                ),
                bekreftEndringerViaFrontend = false
            )
        val restUtvidetBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().registrererSøknad(
                behandlingId = aktivBehandling.behandlingId,
                restRegistrerSøknad = restRegistrerSøknad
            )
        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandling,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.VILKÅRSVURDERING
        )

        // Godkjenner alle vilkår på førstegangsbehandling.
        restUtvidetBehandling.data!!.personResultater.forEach { restPersonResultat ->
            restPersonResultat.vilkårResultater.filter { it.resultat == Resultat.IKKE_VURDERT }.forEach {

                familieBaSakKlient().putVilkår(
                    behandlingId = restUtvidetBehandling.data!!.behandlingId,
                    vilkårId = it.id,
                    restPersonResultat =
                    RestPersonResultat(
                        personIdent = restPersonResultat.personIdent,
                        vilkårResultater = listOf(
                            it.copy(
                                resultat = Resultat.OPPFYLT,
                                periodeFom = LocalDate.now().minusMonths(2)
                            )
                        )
                    )
                )
            }
        }

        familieBaSakKlient().validerVilkårsvurdering(
            behandlingId = restUtvidetBehandling.data!!.behandlingId
        )

        val restUtvidetBehandlingEtterBehandlingsresultat =
            familieBaSakKlient().behandlingsresultatStegOgGåVidereTilNesteSteg(
                behandlingId = restUtvidetBehandling.data!!.behandlingId
            )

        assertEquals(
            tilleggOrdinærSatsTilTester.beløp,
            hentNåværendeEllerNesteMånedsUtbetaling(
                behandling = restUtvidetBehandlingEtterBehandlingsresultat.data!!
            )
        )

        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterBehandlingsresultat,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.VURDER_TILBAKEKREVING
        )

        val restUtvidetBehandlingEtterVurderTilbakekreving =
            familieBaSakKlient().lagreTilbakekrevingOgGåVidereTilNesteSteg(
                restUtvidetBehandlingEtterBehandlingsresultat.data!!.behandlingId,
                RestTilbakekreving(Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING, begrunnelse = "begrunnelse")
            )
        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterVurderTilbakekreving,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.SEND_TIL_BESLUTTER
        )

        val vedtaksperiodeId =
            restUtvidetBehandlingEtterVurderTilbakekreving.data!!.vedtak!!.vedtaksperioderMedBegrunnelser.first()
        familieBaSakKlient().oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = vedtaksperiodeId.id,
            restPutVedtaksperiodeMedStandardbegrunnelser = RestPutVedtaksperiodeMedStandardbegrunnelser(
                standardbegrunnelser = listOf(
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_BOR_HOS_SØKER
                )
            )
        )

        val restUtvidetBehandlingEtterSendTilBeslutter =
            familieBaSakKlient().sendTilBeslutter(behandlingId = restUtvidetBehandlingEtterVurderTilbakekreving.data!!.behandlingId)

        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterSendTilBeslutter,
            behandlingStatus = BehandlingStatus.FATTER_VEDTAK,
            behandlingStegType = StegType.BESLUTTE_VEDTAK
        )

        val restUtvidetBehandlingEtterIverksetting =
            familieBaSakKlient().iverksettVedtak(
                behandlingId = restUtvidetBehandlingEtterSendTilBeslutter.data!!.behandlingId,
                restBeslutningPåVedtak = RestBeslutningPåVedtak(
                    Beslutning.GODKJENT
                ),
                beslutterHeaders = HttpHeaders().apply {
                    setBearerAuth(
                        token(
                            mapOf(
                                "groups" to listOf("SAKSBEHANDLER", "BESLUTTER"),
                                "azp" to "azp-test",
                                "name" to "Mock McMockface Beslutter",
                                "preferred_username" to "mock.mcmockface.beslutter@nav.no"
                            )
                        )
                    )
                }
            )
        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterIverksetting,
            behandlingStatus = BehandlingStatus.IVERKSETTER_VEDTAK,
            behandlingStegType = StegType.IVERKSETT_MOT_OPPDRAG
        )

        håndterIverksettingAvBehandling(
            behandlingEtterVurdering = behandlingService.hentAktivForFagsak(fagsakId = fagsakId.data!!.toLong())!!,
            søkerFnr = scenario.søker.ident,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService
        )
    }

    @Test
    fun `Skal journalføre og behandle utvidet nasjonal sak`() {
        every { featureToggleService.isEnabled(any()) } returns true

        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1996-12-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = LocalDate.now().minusMonths(6).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                        bostedsadresser = emptyList()
                    )
                )
            )
        )

        val fagsakId: Ressurs<String> = familieBaSakKlient().journalfør(
            journalpostId = "1234",
            oppgaveId = "5678",
            journalførendeEnhet = "4833",
            restJournalføring = lagMockRestJournalføring(
                bruker = NavnOgIdent(
                    navn = scenario.søker.navn,
                    id = scenario.søker.ident!!
                )
            ).copy(
                journalpostTittel = "Søknad om utvidet barnetrygd",
                underkategori = BehandlingUnderkategori.UTVIDET
            )
        )

        assertEquals(Ressurs.Status.SUKSESS, fagsakId.status)

        val restFagsakEtterJournalføring = familieBaSakKlient().hentFagsak(fagsakId = fagsakId.data?.toLong()!!)
        generellAssertFagsak(
            restFagsak = restFagsakEtterJournalføring,
            fagsakStatus = FagsakStatus.OPPRETTET,
            behandlingStegType = StegType.REGISTRERE_SØKNAD
        )

        val aktivBehandling = hentAktivBehandling(restFagsak = restFagsakEtterJournalføring.data!!)

        assertEquals(BehandlingUnderkategori.UTVIDET, aktivBehandling.underkategori)

        val restRegistrerSøknad =
            RestRegistrerSøknad(
                søknad = lagSøknadDTO(
                    søkerIdent = scenario.søker.ident,
                    barnasIdenter = scenario.barna.map { it.ident!! },
                    underkategori = BehandlingUnderkategori.UTVIDET
                ),
                bekreftEndringerViaFrontend = false
            )
        val restUtvidetBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().registrererSøknad(
                behandlingId = aktivBehandling.behandlingId,
                restRegistrerSøknad = restRegistrerSøknad
            )
        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandling,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.VILKÅRSVURDERING
        )

        // Godkjenner alle vilkår på førstegangsbehandling.
        assertEquals(
            3,
            restUtvidetBehandling.data!!.personResultater.find { it.personIdent == scenario.søker.ident }?.vilkårResultater?.size
        )

        restUtvidetBehandling.data!!.personResultater.forEach { restPersonResultat ->
            restPersonResultat.vilkårResultater.filter { it.resultat == Resultat.IKKE_VURDERT }.forEach {

                familieBaSakKlient().putVilkår(
                    behandlingId = restUtvidetBehandling.data!!.behandlingId,
                    vilkårId = it.id,
                    restPersonResultat =
                    RestPersonResultat(
                        personIdent = restPersonResultat.personIdent,
                        vilkårResultater = listOf(
                            it.copy(
                                resultat = Resultat.OPPFYLT,
                                periodeFom = LocalDate.now().minusMonths(2)
                            )
                        )
                    )
                )
            }
        }

        familieBaSakKlient().validerVilkårsvurdering(
            behandlingId = restUtvidetBehandling.data!!.behandlingId
        )

        val restUtvidetBehandlingEtterBehandlingsresultat =
            familieBaSakKlient().behandlingsresultatStegOgGåVidereTilNesteSteg(
                behandlingId = restUtvidetBehandling.data!!.behandlingId
            )

        assertEquals(
            tilleggOrdinærSatsTilTester.beløp + sisteUtvidetSatsTilTester.beløp,
            hentNåværendeEllerNesteMånedsUtbetaling(
                behandling = restUtvidetBehandlingEtterBehandlingsresultat.data!!
            )
        )

        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterBehandlingsresultat,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.VURDER_TILBAKEKREVING
        )

        val restUtvidetBehandlingEtterVurderTilbakekreving =
            familieBaSakKlient().lagreTilbakekrevingOgGåVidereTilNesteSteg(
                restUtvidetBehandlingEtterBehandlingsresultat.data!!.behandlingId,
                RestTilbakekreving(Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING, begrunnelse = "begrunnelse")
            )
        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterVurderTilbakekreving,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.SEND_TIL_BESLUTTER
        )

        val vedtaksperiodeId =
            restUtvidetBehandlingEtterVurderTilbakekreving.data!!.vedtak!!.vedtaksperioderMedBegrunnelser.first()

        familieBaSakKlient().oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = vedtaksperiodeId.id,
            restPutVedtaksperiodeMedStandardbegrunnelser = RestPutVedtaksperiodeMedStandardbegrunnelser(
                standardbegrunnelser = listOf(
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_BOR_HOS_SØKER
                )
            )
        )

        val restUtvidetBehandlingEtterSendTilBeslutter =
            familieBaSakKlient().sendTilBeslutter(behandlingId = restUtvidetBehandlingEtterVurderTilbakekreving.data!!.behandlingId)

        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterSendTilBeslutter,
            behandlingStatus = BehandlingStatus.FATTER_VEDTAK,
            behandlingStegType = StegType.BESLUTTE_VEDTAK
        )

        val restUtvidetBehandlingEtterIverksetting =
            familieBaSakKlient().iverksettVedtak(
                behandlingId = restUtvidetBehandlingEtterSendTilBeslutter.data!!.behandlingId,
                restBeslutningPåVedtak = RestBeslutningPåVedtak(
                    Beslutning.GODKJENT
                ),
                beslutterHeaders = HttpHeaders().apply {
                    setBearerAuth(
                        token(
                            mapOf(
                                "groups" to listOf("SAKSBEHANDLER", "BESLUTTER"),
                                "azp" to "azp-test",
                                "name" to "Mock McMockface Beslutter",
                                "preferred_username" to "mock.mcmockface.beslutter@nav.no"
                            )
                        )
                    )
                }
            )
        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterIverksetting,
            behandlingStatus = BehandlingStatus.IVERKSETTER_VEDTAK,
            behandlingStegType = StegType.IVERKSETT_MOT_OPPDRAG
        )

        håndterIverksettingAvBehandling(
            behandlingEtterVurdering = behandlingService.hentAktivForFagsak(fagsakId = fagsakId.data!!.toLong())!!,
            søkerFnr = scenario.søker.ident,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService
        )
    }
}
