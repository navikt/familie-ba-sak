package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.randomBarnFødselsdato
import no.nav.familie.ba.sak.datagenerator.randomSøkerFødselsdato
import no.nav.familie.ba.sak.ekstern.restDomene.TilbakekrevingDto
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService.Companion.leggTilPersonInfo
import no.nav.familie.ba.sak.fake.FakeØkonomiKlient
import no.nav.familie.ba.sak.fake.FakeØkonomiKlient.Companion.leggTilSimuleringResultat
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.DbOppgave
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.OppgaveRepository
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.RestHenleggBehandlingInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjørbehandling.kjørStegprosessForFGB
import no.nav.familie.ba.sak.kjørbehandling.kjørStegprosessForRevurderingÅrligKontroll
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.simulering.BetalingType
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import no.nav.familie.kontrakter.felles.simulering.MottakerType
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import no.nav.familie.kontrakter.felles.simulering.SimulertPostering
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class StegServiceIntegrationTest(
    @Autowired
    private val stegService: StegService,
    @Autowired
    private val vedtakService: VedtakService,
    @Autowired
    private val behandlingService: BehandlingService,
    @Autowired
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired
    private val persongrunnlagService: PersongrunnlagService,
    @Autowired
    private val fagsakService: FagsakService,
    @Autowired
    private val vilkårsvurderingService: VilkårsvurderingService,
    @Autowired
    private val totrinnskontrollService: TotrinnskontrollService,
    @Autowired
    private val personidentService: PersonidentService,
    @Autowired
    private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired
    private val oppgaveRepository: OppgaveRepository,
    @Autowired
    private val brevmalService: BrevmalService,
    @Autowired
    private val fakeØkonomiKlient: FakeØkonomiKlient,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `Skal sette default-verdier på gift-vilkår for barn`() {
        val søkerFnr = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(35))
        val barnFnr1 = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(2))
        val barnFnr2 =
            leggTilPersonInfo(
                fødselsdato = LocalDate.now().minusYears(16),
                egendefinertMock =
                    PersonInfo(
                        fødselsdato = LocalDate.now().minusYears(16),
                        sivilstander =
                            listOf(
                                Sivilstand(type = SIVILSTANDTYPE.GIFT, gyldigFraOgMed = LocalDate.now().minusMonths(8)),
                            ),
                        kjønn = Kjønn.entries.random(),
                        navn = "navn",
                    ),
            )

        val behandling =
            kjørStegprosessForFGB(
                tilSteg = StegType.REGISTRERE_SØKNAD,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(barnFnr1, barnFnr2),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                brevmalService = brevmalService,
            )

        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)!!
        assertEquals(
            Resultat.OPPFYLT,
            vilkårsvurdering.personResultater
                .first { it.aktør.aktivFødselsnummer() == barnFnr1 }
                .vilkårResultater
                .single { it.vilkårType == Vilkår.GIFT_PARTNERSKAP }
                .resultat,
        )
        assertEquals(
            Resultat.IKKE_VURDERT,
            vilkårsvurdering.personResultater
                .first { it.aktør.aktivFødselsnummer() == barnFnr2 }
                .vilkårResultater
                .single { it.vilkårType == Vilkår.GIFT_PARTNERSKAP }
                .resultat,
        )
    }

    @Test
    fun `Skal kjøre gjennom alle steg med datageneratoren`() {
        val søkerFnr = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(35))
        val barnFnr = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(2))
        val behandling =
            kjørStegprosessForFGB(
                tilSteg = StegType.BEHANDLING_AVSLUTTET,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(barnFnr),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                brevmalService = brevmalService,
                vilkårInnvilgetFom = LocalDate.now().minusYears(1),
            )

        // Venter med å kjøre gjennom til avsluttet til brev er støttet for fortsatt innvilget.
        kjørStegprosessForRevurderingÅrligKontroll(
            tilSteg = StegType.SEND_TIL_BESLUTTER,
            søkerFnr = søkerFnr,
            barnasIdenter = listOf(barnFnr),
            vedtakService = vedtakService,
            stegService = stegService,
            fagsakId = behandling.fagsak.id,
            brevmalService = brevmalService,
            vedtaksperiodeService = vedtaksperiodeService,
        )
    }

    @Test
    fun `Skal feile når man prøver å håndtere feil steg`() {
        val (søkerFnr, _) = mockHentPersoninfoForIdenter()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandlingUtenId(fagsak))
        assertEquals(FØRSTE_STEG, behandling.steg)

        assertThrows<FunksjonellFeil> {
            stegService.håndterVilkårsvurdering(behandling)
        }
    }

    @Test
    fun `Skal feile når man prøver å endre en avsluttet behandling`() {
        val (søkerFnr, _) = mockHentPersoninfoForIdenter()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandlingUtenId(fagsak))
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling, aktiv = true)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)

        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BEHANDLING_AVSLUTTET))
        behandling.status = BehandlingStatus.AVSLUTTET
        val feil =
            assertThrows<FunksjonellFeil> {
                stegService.håndterSendTilBeslutter(behandling, "1234")
            }
        assertEquals(
            "Behandling med id ${behandling.id} er avsluttet og stegprosessen kan ikke gjenåpnes",
            feil.message,
        )
    }

    @Test
    fun `Skal feile når man prøver å noe annet enn å beslutte behandling når den er på dette steget`() {
        val (søkerFnr, _) = mockHentPersoninfoForIdenter()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandlingUtenId(fagsak))
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling, aktiv = true)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)

        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
        behandling.status = BehandlingStatus.FATTER_VEDTAK
        assertThrows<FunksjonellFeil> {
            stegService.håndterSendTilBeslutter(behandling, "1234")
        }
    }

    @Test
    fun `Skal feile når man prøver å kalle beslutning-steget med feil status på behandling`() {
        val (søkerFnr, _) = mockHentPersoninfoForIdenter()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandlingUtenId(fagsak))
        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
        behandling.status = BehandlingStatus.IVERKSETTER_VEDTAK
        assertThrows<FunksjonellFeil> {
            stegService.håndterBeslutningForVedtak(
                behandling,
                RestBeslutningPåVedtak(beslutning = Beslutning.GODKJENT, begrunnelse = null),
            )
        }
    }

    @Test
    fun `Underkjent beslutning setter steg tilbake til send til beslutter`() {
        val (søkerFnr, _) = mockHentPersoninfoForIdenter()
        val søkerAktørId = personidentService.hentAktør(søkerFnr)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandlingUtenId(fagsak))
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(
            lagVilkårsvurdering(
                søkerAktørId,
                behandling,
                Resultat.OPPFYLT,
            ),
        )
        behandling.endretAv = "1234"
        assertEquals(FØRSTE_STEG, behandling.steg)

        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(behandling = behandling)
        behandling.behandlingStegTilstand.forEach { it.behandlingStegStatus = BehandlingStegStatus.UTFØRT }
        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BESLUTTE_VEDTAK))
        behandling.status = BehandlingStatus.FATTER_VEDTAK
        stegService.håndterBeslutningForVedtak(
            behandling,
            RestBeslutningPåVedtak(beslutning = Beslutning.UNDERKJENT, begrunnelse = "Feil"),
        )

        val behandlingEtterPersongrunnlagSteg = behandlingHentOgPersisterService.hent(behandlingId = behandling.id)
        assertEquals(StegType.SEND_TIL_BESLUTTER, behandlingEtterPersongrunnlagSteg.steg)
    }

    @Test
    fun `Henlegge før behandling er sendt til beslutter`() {
        val søkerFnr = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(35))
        val barnFnr = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(2))

        val vilkårsvurdertBehandling = kjørGjennomStegInkludertVurderTilbakekreving(søkerFnr, listOf(barnFnr))

        val henlagtBehandling =
            stegService.håndterHenleggBehandling(
                vilkårsvurdertBehandling,
                RestHenleggBehandlingInfo(
                    årsak = HenleggÅrsak.FEILAKTIG_OPPRETTET,
                    begrunnelse = "",
                ),
            )
        assertTrue(
            henlagtBehandling.behandlingStegTilstand.firstOrNull {
                it.behandlingSteg == StegType.HENLEGG_BEHANDLING && it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            } != null,
        )
        assertTrue(
            henlagtBehandling.behandlingStegTilstand.firstOrNull {
                it.behandlingSteg == StegType.FERDIGSTILLE_BEHANDLING && it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            } != null,
        )

        assertEquals(StegType.BEHANDLING_AVSLUTTET, henlagtBehandling.steg)
    }

    @Test
    fun `Teknisk henleggelse med begrunnelse Satsendring skal beholde behandleSak-oppgaven åpen`() {
        val søkerFnr = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(35))
        val barnFnr = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(2))

        val behandling = kjørGjennomStegInkludertVurderTilbakekreving(søkerFnr, listOf(barnFnr))
        oppgaveRepository.saveAll(
            listOf(
                DbOppgave(behandling = behandling, type = Oppgavetype.Journalføring, gsakId = "1"),
                DbOppgave(behandling = behandling, type = Oppgavetype.BehandleSak, gsakId = "2"),
                DbOppgave(behandling = behandling, type = Oppgavetype.BehandleUnderkjentVedtak, gsakId = "3"),
            ),
        )
        val henlagtBehandling =
            stegService.håndterHenleggBehandling(
                behandling,
                RestHenleggBehandlingInfo(
                    årsak = HenleggÅrsak.TEKNISK_VEDLIKEHOLD,
                    begrunnelse = "Satsendring",
                ),
            )
        assertEquals(StegType.BEHANDLING_AVSLUTTET, henlagtBehandling.steg)
        assertTrue {
            oppgaveRepository
                .findByBehandlingAndIkkeFerdigstilt(henlagtBehandling)
                .filter { it.type == Oppgavetype.BehandleSak }
                .isNotEmpty()
        }
        assertTrue {
            oppgaveRepository
                .findByBehandlingAndIkkeFerdigstilt(henlagtBehandling)
                .filter { it.type == Oppgavetype.BehandleUnderkjentVedtak }
                .isNotEmpty()
        }
        assertTrue {
            oppgaveRepository
                .findByBehandlingAndIkkeFerdigstilt(henlagtBehandling)
                .filter { it.type == Oppgavetype.Journalføring }
                .isEmpty()
        }
    }

    @Test
    fun `Henlegge etter behandling er sendt til beslutter`() {
        val søkerFnr = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(35))
        val barnFnr = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(2))

        val vilkårsvurdertBehandling = kjørGjennomStegInkludertVurderTilbakekreving(søkerFnr, listOf(barnFnr))
        stegService.håndterSendTilBeslutter(vilkårsvurdertBehandling, "1234")

        val behandlingEtterSendTilBeslutter =
            behandlingHentOgPersisterService.hent(behandlingId = vilkårsvurdertBehandling.id)

        assertThrows<FunksjonellFeil> {
            stegService.håndterHenleggBehandling(
                behandlingEtterSendTilBeslutter,
                RestHenleggBehandlingInfo(
                    årsak = HenleggÅrsak.FEILAKTIG_OPPRETTET,
                    begrunnelse = "",
                ),
            )
        }
    }

    // I de fleste tilfeller vil det ikke være mulig å henlegge en behandling som har kommet forbi iverksett steget.
    // Disse vil bli stoppet i BehandlingStegController.
    @Test
    fun `Henlegge dersom behandling står på FERDIGSTILLE_BEHANDLING steget`() {
        val (søkerFnr, _) = mockHentPersoninfoForIdenter()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandlingUtenId(fagsak))

        behandling.behandlingStegTilstand.add(
            BehandlingStegTilstand(
                0,
                behandling,
                StegType.FERDIGSTILLE_BEHANDLING,
                BehandlingStegStatus.IKKE_UTFØRT,
            ),
        )

        val behandlingEtterHenleggelse =
            stegService.håndterHenleggBehandling(
                behandling,
                RestHenleggBehandlingInfo(årsak = HenleggÅrsak.FEILAKTIG_OPPRETTET, begrunnelse = ""),
            )

        assertThat(behandlingEtterHenleggelse.steg).isEqualTo(StegType.BEHANDLING_AVSLUTTET)
        assertThat(behandlingEtterHenleggelse.status).isEqualTo(BehandlingStatus.AVSLUTTET)
        assertThat(behandlingEtterHenleggelse.behandlingStegTilstand.any { it.behandlingSteg == StegType.FERDIGSTILLE_BEHANDLING && it.behandlingStegStatus == BehandlingStegStatus.UTFØRT })
    }

    @Test
    fun `skal kjøre gjennom steg for migreringsbehandling med årsak endre migreringsdato og avvik i simulering innenfor beløpsgrenser`() {
        val søkerFnr = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(30))
        val barnFnr = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(2))

        val barnasIdenter = listOf(barnFnr)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)

        val simulertPosteringMock =
            listOf(
                SimulertPostering(
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                    fom = LocalDate.parse("2019-09-01"),
                    tom = LocalDate.parse("2019-09-30"),
                    betalingType = BetalingType.DEBIT,
                    beløp = 1.toBigDecimal(),
                    posteringType = PosteringType.FEILUTBETALING,
                    forfallsdato = LocalDate.parse("2021-02-23"),
                    utenInntrekk = false,
                    erFeilkonto = null,
                ),
            )

        val simuleringMottakerMock =
            listOf(
                SimuleringMottaker(
                    simulertPostering = simulertPosteringMock,
                    mottakerType = MottakerType.BRUKER,
                    mottakerNummer = "12345678910",
                ),
            )

        leggTilSimuleringResultat(fagsak.id.toString(), DetaljertSimuleringResultat(simuleringMottakerMock))

        kjørStegprosessForFGB(
            tilSteg = StegType.BEHANDLING_AVSLUTTET,
            søkerFnr = søkerFnr,
            barnasIdenter = barnasIdenter,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            brevmalService = brevmalService,
            vilkårInnvilgetFom = LocalDate.now().minusMonths(4),
        )

        val nyMigreringsdato = LocalDate.now().minusMonths(6)
        val behandling =
            stegService.håndterNyBehandling(
                NyBehandling(
                    kategori = BehandlingKategori.NASJONAL,
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                    behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                    behandlingÅrsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
                    søkersIdent = søkerFnr,
                    barnasIdenter = barnasIdenter,
                    nyMigreringsdato = nyMigreringsdato,
                    fagsakId = fagsak.id,
                ),
            )
        assertEquals(StegType.VILKÅRSVURDERING, behandling.steg)
        assertTrue {
            behandling.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.REGISTRERE_PERSONGRUNNLAG &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        assertMigreringsdato(nyMigreringsdato, behandling)
        assertNotNull(vilkårsvurderingService.hentAktivForBehandling(behandling.id))

        val behandlingEtterVilkårsvurdering = stegService.håndterVilkårsvurdering(behandling)
        assertEquals(StegType.BEHANDLINGSRESULTAT, behandlingEtterVilkårsvurdering.steg)

        val behandlingEtterBehandlingsresultatSteg =
            stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurdering)
        assertEquals(StegType.VURDER_TILBAKEKREVING, behandlingEtterBehandlingsresultatSteg.steg)

        val behandlingEtterTilbakekrevingSteg =
            stegService.håndterVurderTilbakekreving(
                behandlingEtterBehandlingsresultatSteg,
                TilbakekrevingDto(
                    valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                    begrunnelse = "ignorer tilbakekreving",
                ),
            )
        assertEquals(StegType.SEND_TIL_BESLUTTER, behandlingEtterTilbakekrevingSteg.steg)

        val behandlingEtterBeslutterSteg =
            stegService.håndterSendTilBeslutter(
                behandlingEtterTilbakekrevingSteg,
                "1234",
            )
        assertEquals(StegType.FERDIGSTILLE_BEHANDLING, behandlingEtterBeslutterSteg.steg)
        assertTrue {
            behandlingEtterBeslutterSteg.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.SEND_TIL_BESLUTTER &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        assertTrue {
            behandlingEtterBeslutterSteg.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.BESLUTTE_VEDTAK &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandling.id)
        assertNotNull(totrinnskontroll)
        assertEquals(true, totrinnskontroll!!.godkjent)
        assertEquals(SikkerhetContext.hentSaksbehandlerNavn(), totrinnskontroll.saksbehandler)
        assertEquals(SikkerhetContext.hentSaksbehandler(), totrinnskontroll.saksbehandlerId)
        assertEquals(SikkerhetContext.SYSTEM_NAVN, totrinnskontroll.beslutter)
        assertEquals(SikkerhetContext.SYSTEM_FORKORTELSE, totrinnskontroll.beslutterId)
    }

    @Test
    fun `skal kjøre gjennom steg for migreringsbehandling med årsak endre migreringsdato og avvik i simulering utenefor beløpsgrenser`() {
        val søkerFnr = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(29))
        val barnFnr = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(2))
        val barnasIdenter = listOf(barnFnr)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)

        val simulertPosteringMock =
            listOf(
                SimulertPostering(
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                    fom = LocalDate.parse("2019-09-01"),
                    tom = LocalDate.parse("2019-09-30"),
                    betalingType = BetalingType.DEBIT,
                    beløp = 600.toBigDecimal(),
                    posteringType = PosteringType.FEILUTBETALING,
                    forfallsdato = LocalDate.parse("2021-02-23"),
                    utenInntrekk = false,
                    erFeilkonto = null,
                ),
            )

        val simuleringMottakerMock =
            listOf(
                SimuleringMottaker(
                    simulertPostering = simulertPosteringMock,
                    mottakerType = MottakerType.BRUKER,
                    mottakerNummer = "12345678910",
                ),
            )

        leggTilSimuleringResultat(fagsak.id.toString(), DetaljertSimuleringResultat(simuleringMottakerMock))

        kjørStegprosessForFGB(
            tilSteg = StegType.BEHANDLING_AVSLUTTET,
            søkerFnr = søkerFnr,
            barnasIdenter = barnasIdenter,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            brevmalService = brevmalService,
            vilkårInnvilgetFom = LocalDate.now().minusMonths(4),
        )

        val nyMigreringsdato = LocalDate.now().minusMonths(6)
        val behandling =
            stegService.håndterNyBehandling(
                NyBehandling(
                    kategori = BehandlingKategori.NASJONAL,
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                    behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                    behandlingÅrsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
                    søkersIdent = søkerFnr,
                    barnasIdenter = barnasIdenter,
                    nyMigreringsdato = nyMigreringsdato,
                    fagsakId = fagsak.id,
                ),
            )
        assertEquals(StegType.VILKÅRSVURDERING, behandling.steg)
        assertTrue {
            behandling.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.REGISTRERE_PERSONGRUNNLAG &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        assertMigreringsdato(nyMigreringsdato, behandling)
        assertNotNull(vilkårsvurderingService.hentAktivForBehandling(behandling.id))

        val behandlingEtterVilkårsvurdering = stegService.håndterVilkårsvurdering(behandling)
        assertEquals(StegType.BEHANDLINGSRESULTAT, behandlingEtterVilkårsvurdering.steg)

        val behandlingEtterBehandlingsresultatSteg =
            stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurdering)
        assertEquals(StegType.VURDER_TILBAKEKREVING, behandlingEtterBehandlingsresultatSteg.steg)

        val behandlingEtterTilbakekrevingSteg =
            stegService.håndterVurderTilbakekreving(
                behandlingEtterBehandlingsresultatSteg,
                TilbakekrevingDto(
                    valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                    begrunnelse = "ignorer tilbakekreving",
                ),
            )
        assertEquals(StegType.SEND_TIL_BESLUTTER, behandlingEtterTilbakekrevingSteg.steg)

        val behandlingEtterBeslutterSteg =
            stegService.håndterSendTilBeslutter(
                behandlingEtterTilbakekrevingSteg,
                "1234",
            )
        assertEquals(StegType.FERDIGSTILLE_BEHANDLING, behandlingEtterBeslutterSteg.steg)
        assertTrue {
            behandlingEtterBeslutterSteg.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.SEND_TIL_BESLUTTER &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        assertTrue {
            behandlingEtterBeslutterSteg.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.BESLUTTE_VEDTAK &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandling.id)
        assertNotNull(totrinnskontroll)
        assertEquals(true, totrinnskontroll!!.godkjent)
        assertEquals(SikkerhetContext.hentSaksbehandlerNavn(), totrinnskontroll.saksbehandler)
        assertEquals(SikkerhetContext.hentSaksbehandler(), totrinnskontroll.saksbehandlerId)
        assertEquals(SikkerhetContext.SYSTEM_NAVN, totrinnskontroll.beslutter)
        assertEquals(SikkerhetContext.SYSTEM_FORKORTELSE, totrinnskontroll.beslutterId)
    }

    @Test
    fun `skal kjøre gjennom steg for migreringsbehandling med årsak endre migreringsdato og avvik i simulering utenfor beløpsgrenser`() {
        val søkerFnr = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(18))
        val barnFnr = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(2))
        val barnasIdenter = listOf(barnFnr)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)

        val simulertPosteringMock =
            listOf(
                SimulertPostering(
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                    fom = LocalDate.parse("2019-09-01"),
                    tom = LocalDate.parse("2019-09-30"),
                    betalingType = BetalingType.DEBIT,
                    beløp = 500.toBigDecimal(),
                    posteringType = PosteringType.FEILUTBETALING,
                    forfallsdato = LocalDate.parse("2021-02-23"),
                    utenInntrekk = false,
                    erFeilkonto = null,
                ),
            )

        val simuleringMottakerMock =
            listOf(
                SimuleringMottaker(
                    simulertPostering = simulertPosteringMock,
                    mottakerType = MottakerType.BRUKER,
                    mottakerNummer = "12345678910",
                ),
            )

        leggTilSimuleringResultat(fagsak.id.toString(), DetaljertSimuleringResultat(simuleringMottakerMock))

        kjørStegprosessForFGB(
            tilSteg = StegType.BEHANDLING_AVSLUTTET,
            søkerFnr = søkerFnr,
            barnasIdenter = barnasIdenter,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            brevmalService = brevmalService,
            vilkårInnvilgetFom = LocalDate.now().minusMonths(4),
        )

        val nyMigreringsdato = LocalDate.now().minusMonths(6)
        val behandling =
            stegService.håndterNyBehandling(
                NyBehandling(
                    kategori = BehandlingKategori.NASJONAL,
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                    behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                    behandlingÅrsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
                    søkersIdent = søkerFnr,
                    barnasIdenter = barnasIdenter,
                    nyMigreringsdato = nyMigreringsdato,
                    fagsakId = fagsak.id,
                ),
            )
        assertEquals(StegType.VILKÅRSVURDERING, behandling.steg)
        assertTrue {
            behandling.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.REGISTRERE_PERSONGRUNNLAG &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        assertMigreringsdato(nyMigreringsdato, behandling)
        assertNotNull(vilkårsvurderingService.hentAktivForBehandling(behandling.id))

        val behandlingEtterVilkårsvurdering = stegService.håndterVilkårsvurdering(behandling)
        assertEquals(StegType.BEHANDLINGSRESULTAT, behandlingEtterVilkårsvurdering.steg)

        val behandlingEtterBehandlingsresultatSteg =
            stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurdering)
        assertEquals(StegType.VURDER_TILBAKEKREVING, behandlingEtterBehandlingsresultatSteg.steg)

        val behandlingEtterTilbakekrevingSteg =
            stegService.håndterVurderTilbakekreving(
                behandlingEtterBehandlingsresultatSteg,
                TilbakekrevingDto(
                    valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                    begrunnelse = "ignorer tilbakekreving",
                ),
            )

        assertEquals(StegType.SEND_TIL_BESLUTTER, behandlingEtterTilbakekrevingSteg.steg)

        val behandlingEtterSendTilBeslutterSteg =
            stegService.håndterSendTilBeslutter(
                behandlingEtterTilbakekrevingSteg,
                "1234",
            )

        assertEquals(StegType.FERDIGSTILLE_BEHANDLING, behandlingEtterSendTilBeslutterSteg.steg)

        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandling.id)
        assertNotNull(totrinnskontroll)
        assertEquals(true, totrinnskontroll!!.godkjent)
        assertEquals(SikkerhetContext.hentSaksbehandlerNavn(), totrinnskontroll.saksbehandler)
        assertEquals(SikkerhetContext.hentSaksbehandler(), totrinnskontroll.saksbehandlerId)
        assertEquals(SikkerhetContext.SYSTEM_NAVN, totrinnskontroll.beslutter)
        assertEquals(SikkerhetContext.SYSTEM_FORKORTELSE, totrinnskontroll.beslutterId)
    }

    @Test
    fun `skal kjøre gjennom steg for helmanuell migrering med avvik i simulering innenfor beløpsgrenser`() {
        val søkerFnr = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(35))
        val barnFnr = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(2))
        val barnasIdenter = listOf(barnFnr)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)

        val simulertPosteringMock =
            listOf(
                SimulertPostering(
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                    fom = LocalDate.parse("2019-09-01"),
                    tom = LocalDate.parse("2019-09-30"),
                    betalingType = BetalingType.DEBIT,
                    beløp = 1.toBigDecimal(),
                    posteringType = PosteringType.FEILUTBETALING,
                    forfallsdato = LocalDate.parse("2021-02-23"),
                    utenInntrekk = false,
                    erFeilkonto = null,
                ),
            )

        val simuleringMottakerMock =
            listOf(
                SimuleringMottaker(
                    simulertPostering = simulertPosteringMock,
                    mottakerType = MottakerType.BRUKER,
                    mottakerNummer = "12345678910",
                ),
            )

        leggTilSimuleringResultat(fagsak.id.toString(), DetaljertSimuleringResultat(simuleringMottakerMock))

        val migreringsdato = LocalDate.now().minusMonths(6)
        val behandling =
            stegService.håndterNyBehandling(
                NyBehandling(
                    kategori = BehandlingKategori.NASJONAL,
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                    behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                    behandlingÅrsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
                    søkersIdent = søkerFnr,
                    barnasIdenter = barnasIdenter,
                    nyMigreringsdato = migreringsdato,
                    fagsakId = fagsak.id,
                ),
            )
        assertEquals(StegType.VILKÅRSVURDERING, behandling.steg)
        assertTrue {
            behandling.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.REGISTRERE_PERSONGRUNNLAG &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        assertMigreringsdato(migreringsdato, behandling)
        assertNotNull(vilkårsvurderingService.hentAktivForBehandling(behandling.id))
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandling.id)!!
        val barnPersonResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == barnFnr }.apply {
                vilkårResultater
                    .first { it.vilkårType == Vilkår.BOR_MED_SØKER }
                    .apply { utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED) }
            }
        val søkerPersonResultat = vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == søkerFnr }
        vilkårsvurdering.personResultater = setOf(søkerPersonResultat, barnPersonResultat)
        vilkårsvurderingService.oppdater(vilkårsvurdering)

        val behandlingEtterVilkårsvurdering = stegService.håndterVilkårsvurdering(behandling)
        assertEquals(StegType.BEHANDLINGSRESULTAT, behandlingEtterVilkårsvurdering.steg)

        val behandlingEtterBehandlingsresultatSteg =
            stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurdering)
        assertEquals(StegType.VURDER_TILBAKEKREVING, behandlingEtterBehandlingsresultatSteg.steg)

        val behandlingEtterTilbakekrevingSteg =
            stegService.håndterVurderTilbakekreving(
                behandlingEtterBehandlingsresultatSteg,
                TilbakekrevingDto(
                    valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                    begrunnelse = "ignorer tilbakekreving",
                ),
            )
        assertEquals(StegType.SEND_TIL_BESLUTTER, behandlingEtterTilbakekrevingSteg.steg)

        val behandlingEtterBesultterSteg =
            stegService.håndterSendTilBeslutter(
                behandlingEtterTilbakekrevingSteg,
                "1234",
            )
        assertEquals(StegType.IVERKSETT_MOT_OPPDRAG, behandlingEtterBesultterSteg.steg)
        assertTrue {
            behandlingEtterBesultterSteg.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.SEND_TIL_BESLUTTER &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        assertTrue {
            behandlingEtterBesultterSteg.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.BESLUTTE_VEDTAK &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandling.id)
        assertNotNull(totrinnskontroll)
        assertEquals(true, totrinnskontroll!!.godkjent)
        assertEquals(SikkerhetContext.hentSaksbehandlerNavn(), totrinnskontroll.saksbehandler)
        assertEquals(SikkerhetContext.hentSaksbehandler(), totrinnskontroll.saksbehandlerId)
        assertEquals(SikkerhetContext.SYSTEM_NAVN, totrinnskontroll.beslutter)
        assertEquals(SikkerhetContext.SYSTEM_FORKORTELSE, totrinnskontroll.beslutterId)
    }

    @Test
    fun `skal kjøre gjennom steg for helmanuell migrering med avvik i simulering utenfor beløpsgrenser`() {
        val søkerFnr = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(35))
        val barnFnr = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(2))
        val barnasIdenter = listOf(barnFnr)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)

        val simulertPosteringMock =
            listOf(
                SimulertPostering(
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                    fom = LocalDate.parse("2019-09-01"),
                    tom = LocalDate.parse("2019-09-30"),
                    betalingType = BetalingType.DEBIT,
                    beløp = 300.toBigDecimal(),
                    posteringType = PosteringType.FEILUTBETALING,
                    forfallsdato = LocalDate.parse("2021-02-23"),
                    utenInntrekk = false,
                    erFeilkonto = null,
                ),
            )

        val simuleringMottakerMock =
            listOf(
                SimuleringMottaker(
                    simulertPostering = simulertPosteringMock,
                    mottakerType = MottakerType.BRUKER,
                    mottakerNummer = "12345678910",
                ),
            )

        leggTilSimuleringResultat(fagsak.id.toString(), DetaljertSimuleringResultat(simuleringMottakerMock))

        val migreringsdato = LocalDate.now().minusMonths(6)
        val behandling =
            stegService.håndterNyBehandling(
                NyBehandling(
                    kategori = BehandlingKategori.NASJONAL,
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                    behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                    behandlingÅrsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
                    søkersIdent = søkerFnr,
                    barnasIdenter = barnasIdenter,
                    nyMigreringsdato = migreringsdato,
                    fagsakId = fagsak.id,
                ),
            )
        assertEquals(StegType.VILKÅRSVURDERING, behandling.steg)
        assertTrue {
            behandling.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.REGISTRERE_PERSONGRUNNLAG &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        assertMigreringsdato(migreringsdato, behandling)
        assertNotNull(vilkårsvurderingService.hentAktivForBehandling(behandling.id))
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandling.id)!!
        val barnPersonResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == barnFnr }.apply {
                vilkårResultater
                    .first { it.vilkårType == Vilkår.BOR_MED_SØKER }
                    .apply { utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED) }
            }
        val søkerPersonResultat = vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == søkerFnr }
        vilkårsvurdering.personResultater = setOf(søkerPersonResultat, barnPersonResultat)
        vilkårsvurderingService.oppdater(vilkårsvurdering)

        val behandlingEtterVilkårsvurdering = stegService.håndterVilkårsvurdering(behandling)
        assertEquals(StegType.BEHANDLINGSRESULTAT, behandlingEtterVilkårsvurdering.steg)

        val behandlingEtterBehandlingsresultatSteg =
            stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurdering)
        assertEquals(StegType.VURDER_TILBAKEKREVING, behandlingEtterBehandlingsresultatSteg.steg)

        val behandlingEtterTilbakekrevingSteg =
            stegService.håndterVurderTilbakekreving(
                behandlingEtterBehandlingsresultatSteg,
                TilbakekrevingDto(
                    valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                    begrunnelse = "ignorer tilbakekreving",
                ),
            )
        assertEquals(StegType.SEND_TIL_BESLUTTER, behandlingEtterTilbakekrevingSteg.steg)

        val behandlingEtterSendTilBeslutterSteg =
            stegService.håndterSendTilBeslutter(
                behandlingEtterTilbakekrevingSteg,
                "1234",
            )

        assertEquals(StegType.BESLUTTE_VEDTAK, behandlingEtterSendTilBeslutterSteg.steg)

        val behandlingEtterBesluttVedtakSteg =
            stegService.håndterBeslutningForVedtak(
                behandlingEtterSendTilBeslutterSteg,
                RestBeslutningPåVedtak(
                    Beslutning.GODKJENT,
                    "godkjent manuelt",
                ),
            )

        assertEquals(StegType.IVERKSETT_MOT_OPPDRAG, behandlingEtterBesluttVedtakSteg.steg)

        assertTrue {
            behandlingEtterBesluttVedtakSteg.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.SEND_TIL_BESLUTTER &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }

        assertTrue {
            behandlingEtterBesluttVedtakSteg.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.BESLUTTE_VEDTAK &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandling.id)
        assertNotNull(totrinnskontroll)
        assertEquals(true, totrinnskontroll!!.godkjent)
        assertEquals(SikkerhetContext.hentSaksbehandlerNavn(), totrinnskontroll.saksbehandler)
        assertEquals(SikkerhetContext.hentSaksbehandler(), totrinnskontroll.saksbehandlerId)
        assertEquals(SikkerhetContext.SYSTEM_NAVN, totrinnskontroll.beslutter)
        assertEquals(SikkerhetContext.SYSTEM_FORKORTELSE, totrinnskontroll.beslutterId)
    }

    @Test
    fun `skal kjøre gjennom steg for helmanuell migrering med manuelle posteringer med avvik innenfor beløpsgrenser`() {
        val søkerFnr = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(35))
        val barnFnr = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(2))

        val barnasIdenter = listOf(barnFnr)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)

        val simulertPosteringMock =
            listOf(
                SimulertPostering(
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD_INFOTRYGD_MANUELT,
                    fom = LocalDate.parse("2019-09-01"),
                    tom = LocalDate.parse("2019-09-30"),
                    betalingType = BetalingType.DEBIT,
                    beløp = 1.toBigDecimal(),
                    posteringType = PosteringType.FEILUTBETALING,
                    forfallsdato = LocalDate.parse("2021-02-23"),
                    utenInntrekk = false,
                    erFeilkonto = null,
                ),
            )

        val simuleringMottakerMock =
            listOf(
                SimuleringMottaker(
                    simulertPostering = simulertPosteringMock,
                    mottakerType = MottakerType.BRUKER,
                    mottakerNummer = "12345678910",
                ),
            )

        leggTilSimuleringResultat(fagsak.id.toString(), DetaljertSimuleringResultat(simuleringMottakerMock))

        val migreringsdato = LocalDate.now().minusMonths(6)
        val behandling =
            stegService.håndterNyBehandling(
                NyBehandling(
                    kategori = BehandlingKategori.NASJONAL,
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                    behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                    behandlingÅrsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
                    søkersIdent = søkerFnr,
                    barnasIdenter = barnasIdenter,
                    nyMigreringsdato = migreringsdato,
                    fagsakId = fagsak.id,
                ),
            )
        assertEquals(StegType.VILKÅRSVURDERING, behandling.steg)
        assertTrue {
            behandling.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.REGISTRERE_PERSONGRUNNLAG &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        assertMigreringsdato(migreringsdato, behandling)
        assertNotNull(vilkårsvurderingService.hentAktivForBehandling(behandling.id))
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandling.id)!!
        val barnPersonResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == barnFnr }.apply {
                vilkårResultater
                    .first { it.vilkårType == Vilkår.BOR_MED_SØKER }
                    .apply { utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED) }
            }
        val søkerPersonResultat = vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == søkerFnr }
        vilkårsvurdering.personResultater = setOf(søkerPersonResultat, barnPersonResultat)
        vilkårsvurderingService.oppdater(vilkårsvurdering)

        val behandlingEtterVilkårsvurdering = stegService.håndterVilkårsvurdering(behandling)
        assertEquals(StegType.BEHANDLINGSRESULTAT, behandlingEtterVilkårsvurdering.steg)

        val behandlingEtterBehandlingsresultatSteg =
            stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurdering)
        assertEquals(StegType.VURDER_TILBAKEKREVING, behandlingEtterBehandlingsresultatSteg.steg)

        val behandlingEtterTilbakekrevingSteg =
            stegService.håndterVurderTilbakekreving(
                behandlingEtterBehandlingsresultatSteg,
                TilbakekrevingDto(
                    valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                    begrunnelse = "ignorer tilbakekreving",
                ),
            )
        assertEquals(StegType.SEND_TIL_BESLUTTER, behandlingEtterTilbakekrevingSteg.steg)

        val behandlingEtterSendTilBeslutterSteg =
            stegService.håndterSendTilBeslutter(
                behandlingEtterTilbakekrevingSteg,
                "1234",
            )

        assertEquals(StegType.BESLUTTE_VEDTAK, behandlingEtterSendTilBeslutterSteg.steg)

        // Må manuelt godkjenne vedtak
        val behandlingEtterBesluttVedtakSteg =
            stegService.håndterBeslutningForVedtak(
                behandlingEtterSendTilBeslutterSteg,
                RestBeslutningPåVedtak(
                    Beslutning.GODKJENT,
                    "godkjent manuelt",
                ),
            )

        assertEquals(StegType.IVERKSETT_MOT_OPPDRAG, behandlingEtterBesluttVedtakSteg.steg)

        assertTrue {
            behandlingEtterBesluttVedtakSteg.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.SEND_TIL_BESLUTTER &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }

        assertTrue {
            behandlingEtterBesluttVedtakSteg.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.BESLUTTE_VEDTAK &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandling.id)
        assertNotNull(totrinnskontroll)
        assertEquals(true, totrinnskontroll!!.godkjent)
        assertEquals(SikkerhetContext.hentSaksbehandlerNavn(), totrinnskontroll.saksbehandler)
        assertEquals(SikkerhetContext.hentSaksbehandler(), totrinnskontroll.saksbehandlerId)
        assertEquals(SikkerhetContext.SYSTEM_NAVN, totrinnskontroll.beslutter)
        assertEquals(SikkerhetContext.SYSTEM_FORKORTELSE, totrinnskontroll.beslutterId)
    }

    @Test
    fun `skal kjøre gjennom steg for endre migreringsdato behandling og automatisk godkjenne totrinnskontroll`() {
        val søkerFnr = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(37))
        val barnFnr = leggTilPersonInfo(fødselsdato = LocalDate.now().minusYears(2))
        val barnasIdenter = listOf(barnFnr)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)

        val simulertPosteringMock =
            listOf(
                SimulertPostering(
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD_INFOTRYGD_MANUELT,
                    fom = LocalDate.parse("2019-09-01"),
                    tom = LocalDate.parse("2019-09-30"),
                    betalingType = BetalingType.DEBIT,
                    beløp = 1.toBigDecimal(),
                    posteringType = PosteringType.FEILUTBETALING,
                    forfallsdato = LocalDate.parse("2021-02-23"),
                    utenInntrekk = false,
                    erFeilkonto = null,
                ),
            )

        val simuleringMottakerMock =
            listOf(
                SimuleringMottaker(
                    simulertPostering = simulertPosteringMock,
                    mottakerType = MottakerType.BRUKER,
                    mottakerNummer = "12345678910",
                ),
            )

        leggTilSimuleringResultat(fagsak.id.toString(), DetaljertSimuleringResultat(simuleringMottakerMock))

        kjørStegprosessForFGB(
            tilSteg = StegType.BEHANDLING_AVSLUTTET,
            søkerFnr = søkerFnr,
            barnasIdenter = barnasIdenter,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            brevmalService = brevmalService,
            vilkårInnvilgetFom = LocalDate.now().minusMonths(4),
        )

        val migreringsdato = LocalDate.now().minusMonths(6)
        val behandling =
            stegService.håndterNyBehandling(
                NyBehandling(
                    kategori = BehandlingKategori.NASJONAL,
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                    behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                    behandlingÅrsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
                    søkersIdent = søkerFnr,
                    barnasIdenter = barnasIdenter,
                    nyMigreringsdato = migreringsdato,
                    fagsakId = fagsak.id,
                ),
            )
        assertEquals(StegType.VILKÅRSVURDERING, behandling.steg)
        assertTrue {
            behandling.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.REGISTRERE_PERSONGRUNNLAG &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        assertMigreringsdato(migreringsdato, behandling)
        assertNotNull(vilkårsvurderingService.hentAktivForBehandling(behandling.id))
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandling.id)!!
        val barnPersonResultat = vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == barnFnr }
        val søkerPersonResultat = vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == søkerFnr }
        vilkårsvurdering.personResultater = setOf(søkerPersonResultat, barnPersonResultat)
        vilkårsvurderingService.oppdater(vilkårsvurdering)

        val behandlingEtterVilkårsvurdering = stegService.håndterVilkårsvurdering(behandling)
        assertEquals(StegType.BEHANDLINGSRESULTAT, behandlingEtterVilkårsvurdering.steg)

        val behandlingEtterBehandlingsresultatSteg =
            stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurdering)
        assertEquals(StegType.VURDER_TILBAKEKREVING, behandlingEtterBehandlingsresultatSteg.steg)

        val behandlingEtterTilbakekrevingSteg =
            stegService.håndterVurderTilbakekreving(
                behandlingEtterBehandlingsresultatSteg,
                TilbakekrevingDto(
                    valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                    begrunnelse = "ignorer tilbakekreving",
                ),
            )
        assertEquals(StegType.SEND_TIL_BESLUTTER, behandlingEtterTilbakekrevingSteg.steg)

        val behandlingEtterSendTilBeslutterSteg =
            stegService.håndterSendTilBeslutter(
                behandlingEtterTilbakekrevingSteg,
                "1234",
            )

        assertEquals(StegType.FERDIGSTILLE_BEHANDLING, behandlingEtterSendTilBeslutterSteg.steg)

        assertTrue {
            behandlingEtterSendTilBeslutterSteg.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.SEND_TIL_BESLUTTER &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }

        assertTrue {
            behandlingEtterSendTilBeslutterSteg.behandlingStegTilstand.any {
                it.behandlingSteg == StegType.BESLUTTE_VEDTAK &&
                    it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
            }
        }
        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandling.id)
        assertNotNull(totrinnskontroll)
        assertEquals(true, totrinnskontroll!!.godkjent)
        assertEquals(SikkerhetContext.hentSaksbehandlerNavn(), totrinnskontroll.saksbehandler)
        assertEquals(SikkerhetContext.hentSaksbehandler(), totrinnskontroll.saksbehandlerId)
        assertEquals(SikkerhetContext.SYSTEM_NAVN, totrinnskontroll.beslutter)
        assertEquals(SikkerhetContext.SYSTEM_FORKORTELSE, totrinnskontroll.beslutterId)
    }

    private fun kjørGjennomStegInkludertVurderTilbakekreving(
        søkerFnr: String,
        barnasIdenter: List<String>,
    ): Behandling =
        kjørStegprosessForFGB(
            tilSteg = StegType.VURDER_TILBAKEKREVING,
            søkerFnr = søkerFnr,
            barnasIdenter = barnasIdenter,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            brevmalService = brevmalService,
        )

    private fun assertMigreringsdato(
        migreringsdato: LocalDate,
        behandling: Behandling,
    ) {
        assertEquals(migreringsdato, behandlingService.hentMigreringsdatoIBehandling(behandling.id))
    }

    private fun mockHentPersoninfoForIdenter(): Pair<String, String> {
        val søkerFødselsdato = randomSøkerFødselsdato()
        val søkerFnr =
            leggTilPersonInfo(
                fødselsdato = søkerFødselsdato,
                egendefinertMock =
                    PersonInfo(
                        fødselsdato = søkerFødselsdato,
                        kjønn = Kjønn.KVINNE,
                        navn = "Mor Moresen",
                        sivilstander = listOf(Sivilstand(type = SIVILSTANDTYPE.GIFT, gyldigFraOgMed = LocalDate.now().minusMonths(8))),
                    ),
            )
        val barnFødselsdato = randomBarnFødselsdato()
        val barnFnr =
            leggTilPersonInfo(
                fødselsdato = barnFødselsdato,
                egendefinertMock = PersonInfo(fødselsdato = barnFødselsdato, kjønn = Kjønn.KVINNE, navn = "Barn Barnesen"),
            )
        return søkerFnr to barnFnr
    }
}
