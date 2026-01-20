package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.datagenerator.lagMockJournalføringDto
import no.nav.familie.ba.sak.datagenerator.lagSøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.BehandlingUnderkategoriDTO
import no.nav.familie.ba.sak.ekstern.restDomene.NavnOgIdent
import no.nav.familie.ba.sak.ekstern.restDomene.PersonResultatDto
import no.nav.familie.ba.sak.ekstern.restDomene.PutVedtaksperiodeMedStandardbegrunnelserDto
import no.nav.familie.ba.sak.ekstern.restDomene.RegistrerSøknadDto
import no.nav.familie.ba.sak.ekstern.restDomene.TilbakekrevingDto
import no.nav.familie.ba.sak.ekstern.restDomene.UtvidetBehandlingDto
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.SatsTidspunkt
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.BeslutningPåVedtakDto
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.ScenarioDto
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.ScenarioPersonDto
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
            ScenarioDto(
                søker = ScenarioPersonDto(fødselsdato = "1996-11-12", fornavn = "Mor", etternavn = "Søker"),
                barna =
                    listOf(
                        ScenarioPersonDto(
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
                    lagMockJournalføringDto(
                        bruker =
                            NavnOgIdent(
                                navn = scenario.søker.navn,
                                id = scenario.søker.ident,
                            ),
                    ),
            )

        assertEquals(Ressurs.Status.SUKSESS, fagsakId.status)

        val fagsakDtoEtterJournalføring = familieBaSakKlient().hentFagsak(fagsakId = fagsakId.data?.toLong()!!)
        generellAssertFagsak(
            fagsakDto = fagsakDtoEtterJournalføring,
            fagsakStatus = FagsakStatus.OPPRETTET,
            behandlingStegType = StegType.REGISTRERE_SØKNAD,
        )

        val aktivBehandling = hentAktivBehandling(fagsakDto = fagsakDtoEtterJournalføring.data!!)
        val registrerSøknadDto =
            RegistrerSøknadDto(
                søknad =
                    lagSøknadDTO(
                        søkerIdent = scenario.søker.ident,
                        barnasIdenter = scenario.barna.map { it.ident },
                    ),
                bekreftEndringerViaFrontend = false,
            )
        val utvidetBehandlingDto: Ressurs<UtvidetBehandlingDto> =
            familieBaSakKlient().registrererSøknad(
                behandlingId = aktivBehandling.behandlingId,
                registrerSøknadDto = registrerSøknadDto,
            )
        generellAssertUtvidetBehandlingDto(
            utvidetBehandlingDto = utvidetBehandlingDto,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.VILKÅRSVURDERING,
        )

        // Godkjenner alle vilkår på førstegangsbehandling.
        utvidetBehandlingDto.data!!.personResultater.forEach { personResultatDto ->
            personResultatDto.vilkårResultater.forEach {
                familieBaSakKlient().putVilkår(
                    behandlingId = utvidetBehandlingDto.data!!.behandlingId,
                    vilkårId = it.id,
                    personResultatDto =
                        PersonResultatDto(
                            personIdent = personResultatDto.personIdent,
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
            behandlingId = utvidetBehandlingDto.data!!.behandlingId,
        )

        val utvidetBehandlingDtoEtterBehandlingsresultat =
            familieBaSakKlient().behandlingsresultatStegOgGåVidereTilNesteSteg(
                behandlingId = utvidetBehandlingDto.data!!.behandlingId,
            )

        assertEquals(
            ordinærSatsNesteMånedTilTester().beløp,
            hentNåværendeEllerNesteMånedsUtbetaling(
                behandling = utvidetBehandlingDtoEtterBehandlingsresultat.data!!,
            ),
        )

        generellAssertUtvidetBehandlingDto(
            utvidetBehandlingDto = utvidetBehandlingDtoEtterBehandlingsresultat,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.VURDER_TILBAKEKREVING,
        )

        val utvidetBehandlingDtoEtterVurderTilbakekreving =
            familieBaSakKlient().lagreTilbakekrevingOgGåVidereTilNesteSteg(
                utvidetBehandlingDtoEtterBehandlingsresultat.data!!.behandlingId,
                TilbakekrevingDto(Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING, begrunnelse = "begrunnelse"),
            )
        generellAssertUtvidetBehandlingDto(
            utvidetBehandlingDto = utvidetBehandlingDtoEtterVurderTilbakekreving,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.SEND_TIL_BESLUTTER,
        )

        val vedtaksperioderMedBegrunnelser =
            vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelserDto(
                utvidetBehandlingDtoEtterVurderTilbakekreving.data!!.behandlingId,
            )

        val vedtaksperiode = vedtaksperioderMedBegrunnelser.sortedBy { it.fom }.first()
        familieBaSakKlient().oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = vedtaksperiode.id,
            putVedtaksperiodeMedStandardbegrunnelserDto =
                PutVedtaksperiodeMedStandardbegrunnelserDto(
                    standardbegrunnelser =
                        listOf(
                            Standardbegrunnelse.INNVILGET_BOR_HOS_SØKER.enumnavnTilString(),
                        ),
                ),
        )

        val utvidetBehandlingDtoEtterSendTilBeslutter =
            familieBaSakKlient().sendTilBeslutter(behandlingId = utvidetBehandlingDtoEtterVurderTilbakekreving.data!!.behandlingId)

        generellAssertUtvidetBehandlingDto(
            utvidetBehandlingDto = utvidetBehandlingDtoEtterSendTilBeslutter,
            behandlingStatus = BehandlingStatus.FATTER_VEDTAK,
            behandlingStegType = StegType.BESLUTTE_VEDTAK,
        )

        val utvidetBehandlingDtoEtterIverksetting =
            familieBaSakKlient().iverksettVedtak(
                behandlingId = utvidetBehandlingDtoEtterSendTilBeslutter.data!!.behandlingId,
                beslutningPåVedtakDto =
                    BeslutningPåVedtakDto(
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
        generellAssertUtvidetBehandlingDto(
            utvidetBehandlingDto = utvidetBehandlingDtoEtterIverksetting,
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
            ScenarioDto(
                søker = ScenarioPersonDto(fødselsdato = "1996-12-12", fornavn = "Mor", etternavn = "Søker"),
                barna =
                    listOf(
                        ScenarioPersonDto(
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
                    lagMockJournalføringDto(
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

        val fagsakDtoEtterJournalføring = familieBaSakKlient().hentFagsak(fagsakId = fagsakId.data?.toLong()!!)
        generellAssertFagsak(
            fagsakDto = fagsakDtoEtterJournalføring,
            fagsakStatus = FagsakStatus.OPPRETTET,
            behandlingStegType = StegType.REGISTRERE_SØKNAD,
        )

        val aktivBehandling = hentAktivBehandling(fagsakDto = fagsakDtoEtterJournalføring.data!!)

        assertEquals(BehandlingUnderkategoriDTO.UTVIDET, aktivBehandling.underkategori)

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
        val utvidetBehandlingDto: Ressurs<UtvidetBehandlingDto> =
            familieBaSakKlient().registrererSøknad(
                behandlingId = aktivBehandling.behandlingId,
                registrerSøknadDto = registrerSøknadDto,
            )
        generellAssertUtvidetBehandlingDto(
            utvidetBehandlingDto = utvidetBehandlingDto,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.VILKÅRSVURDERING,
        )

        // Godkjenner alle vilkår på førstegangsbehandling.
        assertEquals(
            3,
            utvidetBehandlingDto.data!!
                .personResultater
                .find { it.personIdent == scenario.søker.ident }
                ?.vilkårResultater
                ?.size,
        )

        utvidetBehandlingDto.data!!.personResultater.forEach { personResultatDto ->
            personResultatDto.vilkårResultater.forEach {
                familieBaSakKlient().putVilkår(
                    behandlingId = utvidetBehandlingDto.data!!.behandlingId,
                    vilkårId = it.id,
                    personResultatDto =
                        PersonResultatDto(
                            personIdent = personResultatDto.personIdent,
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
            behandlingId = utvidetBehandlingDto.data!!.behandlingId,
        )

        val utvidetBehandlingDtoEtterBehandlingsresultat =
            familieBaSakKlient().behandlingsresultatStegOgGåVidereTilNesteSteg(
                behandlingId = utvidetBehandlingDto.data!!.behandlingId,
            )

        assertEquals(
            ordinærSatsNesteMånedTilTester().beløp + sisteUtvidetSatsTilTester(),
            hentNåværendeEllerNesteMånedsUtbetaling(
                behandling = utvidetBehandlingDtoEtterBehandlingsresultat.data!!,
            ),
        )

        generellAssertUtvidetBehandlingDto(
            utvidetBehandlingDto = utvidetBehandlingDtoEtterBehandlingsresultat,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.VURDER_TILBAKEKREVING,
        )

        val utvidetBehandlingDtoEtterVurderTilbakekreving =
            familieBaSakKlient().lagreTilbakekrevingOgGåVidereTilNesteSteg(
                utvidetBehandlingDtoEtterBehandlingsresultat.data!!.behandlingId,
                TilbakekrevingDto(Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING, begrunnelse = "begrunnelse"),
            )
        generellAssertUtvidetBehandlingDto(
            utvidetBehandlingDto = utvidetBehandlingDtoEtterVurderTilbakekreving,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.SEND_TIL_BESLUTTER,
        )

        val vedtaksperioderMedBegrunnelser =
            vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelserDto(
                utvidetBehandlingDtoEtterVurderTilbakekreving.data!!.behandlingId,
            )

        val vedtaksperiode = vedtaksperioderMedBegrunnelser.sortedBy { it.fom }.first()

        familieBaSakKlient().oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = vedtaksperiode.id,
            putVedtaksperiodeMedStandardbegrunnelserDto =
                PutVedtaksperiodeMedStandardbegrunnelserDto(
                    standardbegrunnelser =
                        listOf(
                            Standardbegrunnelse.INNVILGET_BOR_HOS_SØKER.enumnavnTilString(),
                        ),
                ),
        )

        val utvidetBehandlingDtoEtterSendTilBeslutter =
            familieBaSakKlient().sendTilBeslutter(behandlingId = utvidetBehandlingDtoEtterVurderTilbakekreving.data!!.behandlingId)

        generellAssertUtvidetBehandlingDto(
            utvidetBehandlingDto = utvidetBehandlingDtoEtterSendTilBeslutter,
            behandlingStatus = BehandlingStatus.FATTER_VEDTAK,
            behandlingStegType = StegType.BESLUTTE_VEDTAK,
        )

        val utvidetBehandlingDtoEtterIverksetting =
            familieBaSakKlient().iverksettVedtak(
                behandlingId = utvidetBehandlingDtoEtterSendTilBeslutter.data!!.behandlingId,
                beslutningPåVedtakDto =
                    BeslutningPåVedtakDto(
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
        generellAssertUtvidetBehandlingDto(
            utvidetBehandlingDto = utvidetBehandlingDtoEtterIverksetting,
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
