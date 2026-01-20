package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.datagenerator.lagMockRestJournalføring
import no.nav.familie.ba.sak.datagenerator.lagSøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.BehandlingUnderkategoriDTO
import no.nav.familie.ba.sak.ekstern.restDomene.NavnOgIdent
import no.nav.familie.ba.sak.ekstern.restDomene.PersonResultatDto
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.TilbakekrevingDto
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.SatsTidspunkt
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.RestScenarioPerson
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.stubScenario
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.util.ordinærSatsNesteMånedTilTester
import no.nav.familie.ba.sak.util.sisteUtvidetSatsTilTester
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import java.time.LocalDate

class JournalførOgBehandleFørstegangssøknadNasjonalTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val brevmalService: BrevmalService,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
) : AbstractVerdikjedetest() {
    @BeforeEach
    fun førHverTest() {
        mockkObject(SatsTidspunkt)
        every { SatsTidspunkt.senesteSatsTidspunkt } returns LocalDate.of(2024, 9, 1)
    }

    @AfterEach
    fun etterHverTest() {
        unmockkObject(SatsTidspunkt)
    }

    @Test
    fun `Skal journalføre og behandle ordinær nasjonal sak`() {
        val fødselsdatoBarn = LocalDate.now().minusMonths(6)
        val scenario =
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1996-11-12", fornavn = "Mor", etternavn = "Søker"),
                barna =
                    listOf(
                        RestScenarioPerson(
                            fødselsdato = fødselsdatoBarn.toString(),
                            fornavn = "Barn",
                            etternavn = "Barnesen",
                        ),
                    ),
            ).also { stubScenario(it) }

        val fagsakId: Ressurs<String> =
            familieBaSakKlient().journalfør(
                journalpostId = "1234",
                oppgaveId = "5678",
                journalførendeEnhet = "4833",
                journalføringDto =
                    lagMockRestJournalføring(
                        bruker =
                            NavnOgIdent(
                                navn = scenario.søker.navn,
                                id = scenario.søker.ident,
                            ),
                    ),
            )

        assertEquals(Ressurs.Status.SUKSESS, fagsakId.status)

        val restFagsakEtterJournalføring = familieBaSakKlient().hentFagsak(fagsakId = fagsakId.data?.toLong()!!)
        generellAssertFagsak(
            restFagsak = restFagsakEtterJournalføring,
            fagsakStatus = FagsakStatus.OPPRETTET,
            behandlingStegType = StegType.REGISTRERE_SØKNAD,
        )

        val aktivBehandling = hentAktivBehandling(restFagsak = restFagsakEtterJournalføring.data!!)
        val restRegistrerSøknad =
            RestRegistrerSøknad(
                søknad =
                    lagSøknadDTO(
                        søkerIdent = scenario.søker.ident,
                        barnasIdenter = scenario.barna.map { it.ident },
                    ),
                bekreftEndringerViaFrontend = false,
            )
        val restUtvidetBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().registrererSøknad(
                behandlingId = aktivBehandling.behandlingId,
                restRegistrerSøknad = restRegistrerSøknad,
            )
        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandling,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.VILKÅRSVURDERING,
        )

        // Godkjenner alle vilkår på førstegangsbehandling.
        restUtvidetBehandling.data!!.personResultater.forEach { restPersonResultat ->
            restPersonResultat.vilkårResultater.forEach {
                familieBaSakKlient().putVilkår(
                    behandlingId = restUtvidetBehandling.data!!.behandlingId,
                    vilkårId = it.id,
                    personResultatDto =
                        PersonResultatDto(
                            personIdent = restPersonResultat.personIdent,
                            vilkårResultater =
                                listOf(
                                    it.copy(
                                        resultat = Resultat.OPPFYLT,
                                        periodeFom = if (it.vilkårType == Vilkår.UNDER_18_ÅR) fødselsdatoBarn else LocalDate.now().minusMonths(2),
                                        periodeTom = if (it.vilkårType == Vilkår.UNDER_18_ÅR) fødselsdatoBarn.plusYears(18) else null,
                                        begrunnelse = "Oppfylt",
                                    ),
                                ),
                        ),
                )
            }
        }

        familieBaSakKlient().validerVilkårsvurdering(
            behandlingId = restUtvidetBehandling.data!!.behandlingId,
        )

        val restUtvidetBehandlingEtterBehandlingsresultat =
            familieBaSakKlient().behandlingsresultatStegOgGåVidereTilNesteSteg(
                behandlingId = restUtvidetBehandling.data!!.behandlingId,
            )

        assertEquals(
            ordinærSatsNesteMånedTilTester().beløp,
            hentNåværendeEllerNesteMånedsUtbetaling(
                behandling = restUtvidetBehandlingEtterBehandlingsresultat.data!!,
            ),
        )

        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterBehandlingsresultat,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.VURDER_TILBAKEKREVING,
        )

        val restUtvidetBehandlingEtterVurderTilbakekreving =
            familieBaSakKlient().lagreTilbakekrevingOgGåVidereTilNesteSteg(
                restUtvidetBehandlingEtterBehandlingsresultat.data!!.behandlingId,
                TilbakekrevingDto(Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING, begrunnelse = "begrunnelse"),
            )
        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterVurderTilbakekreving,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.SEND_TIL_BESLUTTER,
        )

        val vedtaksperioderMedBegrunnelser =
            vedtaksperiodeService.hentRestUtvidetVedtaksperiodeMedBegrunnelser(
                restUtvidetBehandlingEtterVurderTilbakekreving.data!!.behandlingId,
            )

        val vedtaksperiode = vedtaksperioderMedBegrunnelser.sortedBy { it.fom }.first()
        familieBaSakKlient().oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = vedtaksperiode.id,
            restPutVedtaksperiodeMedStandardbegrunnelser =
                RestPutVedtaksperiodeMedStandardbegrunnelser(
                    standardbegrunnelser =
                        listOf(
                            Standardbegrunnelse.INNVILGET_BOR_HOS_SØKER.enumnavnTilString(),
                        ),
                ),
        )

        val restUtvidetBehandlingEtterSendTilBeslutter =
            familieBaSakKlient().sendTilBeslutter(behandlingId = restUtvidetBehandlingEtterVurderTilbakekreving.data!!.behandlingId)

        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterSendTilBeslutter,
            behandlingStatus = BehandlingStatus.FATTER_VEDTAK,
            behandlingStegType = StegType.BESLUTTE_VEDTAK,
        )

        val restUtvidetBehandlingEtterIverksetting =
            familieBaSakKlient().iverksettVedtak(
                behandlingId = restUtvidetBehandlingEtterSendTilBeslutter.data!!.behandlingId,
                restBeslutningPåVedtak =
                    RestBeslutningPåVedtak(
                        Beslutning.GODKJENT,
                    ),
                beslutterHeaders =
                    HttpHeaders().apply {
                        setBearerAuth(
                            token(
                                mapOf(
                                    "groups" to listOf("SAKSBEHANDLER", "BESLUTTER"),
                                    "azp" to "azp-test",
                                    "name" to "Mock McMockface Beslutter",
                                    "NAVident" to "Z0000",
                                ),
                            ),
                        )
                    },
            )
        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterIverksetting,
            behandlingStatus = BehandlingStatus.IVERKSETTER_VEDTAK,
            behandlingStegType = StegType.IVERKSETT_MOT_OPPDRAG,
        )

        håndterIverksettingAvBehandling(
            behandlingEtterVurdering = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId = fagsakId.data!!.toLong())!!,
            søkerFnr = scenario.søker.ident,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService,
            brevmalService = brevmalService,
        )
    }

    @Test
    fun `Skal journalføre og behandle utvidet nasjonal sak`() {
        System.setProperty(FeatureToggle.TEKNISK_ENDRING.navn, "true")
        val fødselsdatoBarn = LocalDate.now().minusMonths(6)

        val scenario =
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1996-12-12", fornavn = "Mor", etternavn = "Søker"),
                barna =
                    listOf(
                        RestScenarioPerson(
                            fødselsdato = fødselsdatoBarn.toString(),
                            fornavn = "Barn",
                            etternavn = "Barnesen",
                        ),
                    ),
            ).also { stubScenario(it) }

        val fagsakId: Ressurs<String> =
            familieBaSakKlient().journalfør(
                journalpostId = "1234",
                oppgaveId = "5678",
                journalførendeEnhet = "4833",
                journalføringDto =
                    lagMockRestJournalføring(
                        bruker =
                            NavnOgIdent(
                                navn = scenario.søker.navn,
                                id = scenario.søker.ident,
                            ),
                    ).copy(
                        journalpostTittel = "Søknad om utvidet barnetrygd",
                        underkategori = BehandlingUnderkategori.UTVIDET,
                    ),
            )

        assertEquals(Ressurs.Status.SUKSESS, fagsakId.status)

        val restFagsakEtterJournalføring = familieBaSakKlient().hentFagsak(fagsakId = fagsakId.data?.toLong()!!)
        generellAssertFagsak(
            restFagsak = restFagsakEtterJournalføring,
            fagsakStatus = FagsakStatus.OPPRETTET,
            behandlingStegType = StegType.REGISTRERE_SØKNAD,
        )

        val aktivBehandling = hentAktivBehandling(restFagsak = restFagsakEtterJournalføring.data!!)

        assertEquals(BehandlingUnderkategoriDTO.UTVIDET, aktivBehandling.underkategori)

        val restRegistrerSøknad =
            RestRegistrerSøknad(
                søknad =
                    lagSøknadDTO(
                        søkerIdent = scenario.søker.ident,
                        barnasIdenter = scenario.barna.map { it.ident },
                        underkategori = BehandlingUnderkategori.UTVIDET,
                    ),
                bekreftEndringerViaFrontend = false,
            )
        val restUtvidetBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().registrererSøknad(
                behandlingId = aktivBehandling.behandlingId,
                restRegistrerSøknad = restRegistrerSøknad,
            )
        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandling,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.VILKÅRSVURDERING,
        )

        // Godkjenner alle vilkår på førstegangsbehandling.
        assertEquals(
            3,
            restUtvidetBehandling.data!!
                .personResultater
                .find { it.personIdent == scenario.søker.ident }
                ?.vilkårResultater
                ?.size,
        )

        restUtvidetBehandling.data!!.personResultater.forEach { restPersonResultat ->
            restPersonResultat.vilkårResultater.forEach {
                familieBaSakKlient().putVilkår(
                    behandlingId = restUtvidetBehandling.data!!.behandlingId,
                    vilkårId = it.id,
                    personResultatDto =
                        PersonResultatDto(
                            personIdent = restPersonResultat.personIdent,
                            vilkårResultater =
                                listOf(
                                    it.copy(
                                        resultat = Resultat.OPPFYLT,
                                        periodeFom = if (it.vilkårType == Vilkår.UNDER_18_ÅR) fødselsdatoBarn else LocalDate.now().minusMonths(2),
                                        periodeTom = if (it.vilkårType == Vilkår.UNDER_18_ÅR) fødselsdatoBarn.plusYears(18) else null,
                                        begrunnelse = "Oppfylt",
                                    ),
                                ),
                        ),
                )
            }
        }

        familieBaSakKlient().validerVilkårsvurdering(
            behandlingId = restUtvidetBehandling.data!!.behandlingId,
        )

        val restUtvidetBehandlingEtterBehandlingsresultat =
            familieBaSakKlient().behandlingsresultatStegOgGåVidereTilNesteSteg(
                behandlingId = restUtvidetBehandling.data!!.behandlingId,
            )

        assertEquals(
            ordinærSatsNesteMånedTilTester().beløp + sisteUtvidetSatsTilTester(),
            hentNåværendeEllerNesteMånedsUtbetaling(
                behandling = restUtvidetBehandlingEtterBehandlingsresultat.data!!,
            ),
        )

        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterBehandlingsresultat,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.VURDER_TILBAKEKREVING,
        )

        val restUtvidetBehandlingEtterVurderTilbakekreving =
            familieBaSakKlient().lagreTilbakekrevingOgGåVidereTilNesteSteg(
                restUtvidetBehandlingEtterBehandlingsresultat.data!!.behandlingId,
                TilbakekrevingDto(Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING, begrunnelse = "begrunnelse"),
            )
        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterVurderTilbakekreving,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.SEND_TIL_BESLUTTER,
        )

        val vedtaksperioderMedBegrunnelser =
            vedtaksperiodeService.hentRestUtvidetVedtaksperiodeMedBegrunnelser(
                restUtvidetBehandlingEtterVurderTilbakekreving.data!!.behandlingId,
            )

        val vedtaksperiode = vedtaksperioderMedBegrunnelser.sortedBy { it.fom }.first()

        familieBaSakKlient().oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = vedtaksperiode.id,
            restPutVedtaksperiodeMedStandardbegrunnelser =
                RestPutVedtaksperiodeMedStandardbegrunnelser(
                    standardbegrunnelser =
                        listOf(
                            Standardbegrunnelse.INNVILGET_BOR_HOS_SØKER.enumnavnTilString(),
                        ),
                ),
        )

        val restUtvidetBehandlingEtterSendTilBeslutter =
            familieBaSakKlient().sendTilBeslutter(behandlingId = restUtvidetBehandlingEtterVurderTilbakekreving.data!!.behandlingId)

        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterSendTilBeslutter,
            behandlingStatus = BehandlingStatus.FATTER_VEDTAK,
            behandlingStegType = StegType.BESLUTTE_VEDTAK,
        )

        val restUtvidetBehandlingEtterIverksetting =
            familieBaSakKlient().iverksettVedtak(
                behandlingId = restUtvidetBehandlingEtterSendTilBeslutter.data!!.behandlingId,
                restBeslutningPåVedtak =
                    RestBeslutningPåVedtak(
                        Beslutning.GODKJENT,
                    ),
                beslutterHeaders =
                    HttpHeaders().apply {
                        setBearerAuth(
                            token(
                                mapOf(
                                    "groups" to listOf("SAKSBEHANDLER", "BESLUTTER"),
                                    "azp" to "azp-test",
                                    "name" to "Mock McMockface Beslutter",
                                    "NAVident" to "Z0000",
                                ),
                            ),
                        )
                    },
            )
        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterIverksetting,
            behandlingStatus = BehandlingStatus.IVERKSETTER_VEDTAK,
            behandlingStegType = StegType.IVERKSETT_MOT_OPPDRAG,
        )

        håndterIverksettingAvBehandling(
            behandlingEtterVurdering = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId = fagsakId.data!!.toLong())!!,
            søkerFnr = scenario.søker.ident,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService,
            brevmalService = brevmalService,
        )
    }
}
