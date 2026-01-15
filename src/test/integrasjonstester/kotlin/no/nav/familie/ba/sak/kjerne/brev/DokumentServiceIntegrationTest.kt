package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagBostedsadresse
import no.nav.familie.ba.sak.datagenerator.lagVegadresse
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.randomBarnFødselsdato
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.datagenerator.randomSøkerFødselsdato
import no.nav.familie.ba.sak.ekstern.restDomene.RestInstitusjon
import no.nav.familie.ba.sak.fake.FakeIntegrasjonKlient
import no.nav.familie.ba.sak.fake.FakePdlRestKlient.Companion.leggTilBostedsadresseIPDL
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService.Companion.leggTilPersonInfo
import no.nav.familie.ba.sak.fake.FakeTaskRepositoryWrapper
import no.nav.familie.ba.sak.fake.FakeØkonomiKlient.Companion.leggTilSimuleringResultat
import no.nav.familie.ba.sak.fake.tilPayload
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet
import no.nav.familie.ba.sak.kjerne.autovedtak.FinnmarkstilleggData
import no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg.AutovedtakFinnmarkstilleggService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.bostedsadresse.GrMatrikkeladresseBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.Personident
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjørbehandling.kjørStegprosessForFGB
import no.nav.familie.ba.sak.task.JournalførManueltBrevTask
import no.nav.familie.ba.sak.task.dto.JournalførManueltBrevDTO
import no.nav.familie.ba.sak.testfiler.Testfil.TEST_PDF
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class DokumentServiceIntegrationTest(
    @Autowired
    private val fagsakService: FagsakService,
    @Autowired
    private val behandlingService: BehandlingService,
    @Autowired
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired
    private val personidentService: PersonidentService,
    @Autowired
    private val vilkårsvurderingService: VilkårsvurderingService,
    @Autowired
    private val persongrunnlagService: PersongrunnlagService,
    @Autowired
    private val vedtakService: VedtakService,
    @Autowired
    private val dokumentService: DokumentService,
    @Autowired
    private val totrinnskontrollService: TotrinnskontrollService,
    @Autowired
    private val stegService: StegService,
    @Autowired
    private val brevService: BrevService,
    @Autowired
    private val fakeIntegrasjonKlient: FakeIntegrasjonKlient,
    @Autowired
    private val arbeidsfordelingService: ArbeidsfordelingService,
    @Autowired
    private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired
    private val dokumentGenereringService: DokumentGenereringService,
    @Autowired
    private val brevmalService: BrevmalService,
    @Autowired
    private val fakeTaskRepositoryWrapper: FakeTaskRepositoryWrapper,
    @Autowired
    private val autovedtakFinnmarkstilleggService: AutovedtakFinnmarkstilleggService,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `Hent vedtaksbrev`() {
        val barnFnr = leggTilPersonInfo(randomBarnFødselsdato())

        val behandlingEtterVilkårsvurderingSteg =
            kjørStegprosessForFGB(
                tilSteg = StegType.VURDER_TILBAKEKREVING,
                søkerFnr = randomFnr(),
                barnasIdenter = listOf(barnFnr),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                brevmalService = brevmalService,
            )

        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(
            behandlingEtterVilkårsvurderingSteg,
            "ansvarligSaksbehandler",
            "saksbehandlerId",
        )
        totrinnskontrollService.besluttTotrinnskontroll(
            behandlingEtterVilkårsvurderingSteg,
            "ansvarligBeslutter",
            "beslutterId",
            Beslutning.GODKJENT,
        )
        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandlingEtterVilkårsvurderingSteg.id)

        vedtakService.oppdaterVedtakMedStønadsbrev(vedtak!!)

        val pdfvedtaksbrevRess = dokumentService.hentBrevForVedtak(vedtak)
        assertEquals(Ressurs.Status.SUKSESS, pdfvedtaksbrevRess.status)
        assert(pdfvedtaksbrevRess.data!!.contentEquals(TEST_PDF))
    }

    @Test
    fun `Skal generere vedtaksbrev`() {
        val barnFnr = leggTilPersonInfo(randomBarnFødselsdato())

        val behandlingEtterVilkårsvurderingSteg =
            kjørStegprosessForFGB(
                tilSteg = StegType.VURDER_TILBAKEKREVING,
                søkerFnr = randomFnr(),
                barnasIdenter = listOf(barnFnr),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                brevmalService = brevmalService,
            )

        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(
            behandlingEtterVilkårsvurderingSteg,
            "ansvarligSaksbehandler",
            "saksbehandlerId",
        )
        totrinnskontrollService.besluttTotrinnskontroll(
            behandlingEtterVilkårsvurderingSteg,
            "ansvarligBeslutter",
            "beslutterId",
            Beslutning.GODKJENT,
        )

        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandlingEtterVilkårsvurderingSteg.id)
        vedtakService.oppdaterVedtakMedStønadsbrev(vedtak!!)

        val pdfvedtaksbrev = dokumentGenereringService.genererBrevForVedtak(vedtak)
        assert(pdfvedtaksbrev.contentEquals(TEST_PDF))
    }

    @Test
    fun `Skal verifisere at brev får riktig signatur ved alle steg i behandling`() {
        val mockSaksbehandler = "Mock Saksbehandler"
        val mockSaksbehandlerId = "mock.saksbehandler@nav.no"
        val mockBeslutter = "Mock Beslutter"
        val mockBeslutterId = "mock.beslutter@nav.no"

        val barnFnr = leggTilPersonInfo(randomBarnFødselsdato())
        val søkerFnr = leggTilPersonInfo(randomSøkerFødselsdato())

        val behandlingEtterVilkårsvurderingSteg =
            kjørStegprosessForFGB(
                tilSteg = StegType.VURDER_TILBAKEKREVING,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(barnFnr),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                brevmalService = brevmalService,
            )
        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandlingEtterVilkårsvurderingSteg.id)!!

        val vedtaksbrevFellesFelter = brevService.lagVedtaksbrevFellesfelter(vedtak)

        assertEquals("Nav familie- og pensjonsytelser Oslo 1", vedtaksbrevFellesFelter.enhet)
        assertEquals("System", vedtaksbrevFellesFelter.saksbehandler)
        assertEquals("Beslutter", vedtaksbrevFellesFelter.beslutter)

        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(
            behandlingEtterVilkårsvurderingSteg,
            mockSaksbehandler,
            mockSaksbehandlerId,
        )
        val behandlingEtterSendTilBeslutter =
            behandlingEtterVilkårsvurderingSteg.leggTilBehandlingStegTilstand(StegType.BESLUTTE_VEDTAK)
        behandlingHentOgPersisterService.lagreEllerOppdater(behandlingEtterSendTilBeslutter)

        val vedtakEtterSendTilBeslutter =
            vedtakService.hentAktivForBehandling(behandlingId = behandlingEtterSendTilBeslutter.id)!!

        val vedtaksbrevFellesFelterEtterSendTilBeslutter =
            brevService.lagVedtaksbrevFellesfelter(vedtakEtterSendTilBeslutter)

        assertEquals(mockSaksbehandler, vedtaksbrevFellesFelterEtterSendTilBeslutter.saksbehandler)
        assertEquals("System", vedtaksbrevFellesFelterEtterSendTilBeslutter.beslutter)

        totrinnskontrollService.besluttTotrinnskontroll(
            behandling = behandlingEtterSendTilBeslutter,
            beslutter = mockBeslutter,
            beslutterId = mockBeslutterId,
            beslutning = Beslutning.GODKJENT,
        )
        val behandlingEtterVedtakBesluttet =
            behandlingEtterVilkårsvurderingSteg.leggTilBehandlingStegTilstand(StegType.IVERKSETT_MOT_OPPDRAG)

        val vedtakEtterVedtakBesluttet =
            vedtakService.hentAktivForBehandling(behandlingId = behandlingEtterVedtakBesluttet.id)!!

        val vedtaksbrevFellesFelterEtterVedtakBesluttet =
            brevService.lagVedtaksbrevFellesfelter(vedtakEtterVedtakBesluttet)

        assertEquals(mockSaksbehandler, vedtaksbrevFellesFelterEtterVedtakBesluttet.saksbehandler)
        assertEquals(mockBeslutter, vedtaksbrevFellesFelterEtterVedtakBesluttet.beslutter)
    }

    @Test
    fun `Skal ekskludere navn på enhet i signatur i brev for automatisk behandling`() {
        val barnFnr = leggTilPersonInfo(randomBarnFødselsdato())
        val søkerFnr = leggTilPersonInfo(randomSøkerFødselsdato())

        val forrigeBehandling =
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
            )

        fakeIntegrasjonKlient.leggTilBehandlendeEnhet(
            ident = søkerFnr,
            enheter = listOf(BarnetrygdEnhet.MIDLERTIDIG_ENHET),
        )

        leggTilBostedsadresseIPDL(
            personIdenter = listOf(søkerFnr, barnFnr),
            bostedsadresse = lagBostedsadresse(vegadresse = lagVegadresse(kommunenummer = "5601")),
        )

        leggTilSimuleringResultat(
            fagsakId = forrigeBehandling.fagsak.id.toString(),
            simuleringResultat = DetaljertSimuleringResultat(simuleringMottaker = emptyList()),
        )

        autovedtakFinnmarkstilleggService.kjørBehandling(FinnmarkstilleggData(forrigeBehandling.fagsak.id))

        val automatiskBehandlingMedVedtaksbrev = behandlingHentOgPersisterService.hentBehandlinger(forrigeBehandling.fagsak.id).first { it.aktiv }

        assertEquals(automatiskBehandlingMedVedtaksbrev.opprettetÅrsak, BehandlingÅrsak.FINNMARKSTILLEGG)

        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = automatiskBehandlingMedVedtaksbrev.id)!!

        val vedtaksbrevFellesFelter = brevService.lagVedtaksbrevFellesfelter(vedtak)

        assertEquals("Nav familie- og pensjonsytelser", vedtaksbrevFellesFelter.enhet)
        assertEquals("System", vedtaksbrevFellesFelter.saksbehandler)
        assertEquals("System", vedtaksbrevFellesFelter.beslutter)
    }

    @Test
    fun `Skal verifisere at man ikke får generert brev etter at behandlingen er sendt fra beslutter`() {
        val barnFnr = leggTilPersonInfo(randomBarnFødselsdato())

        val behandlingEtterVedtakBesluttet =
            kjørStegprosessForFGB(
                tilSteg = StegType.BESLUTTE_VEDTAK,
                søkerFnr = randomFnr(),
                barnasIdenter = listOf(barnFnr),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                brevmalService = brevmalService,
            )

        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandlingEtterVedtakBesluttet.id)!!
        val feil =
            assertThrows<FunksjonellFeil> {
                dokumentGenereringService.genererBrevForVedtak(vedtak)
            }

        assert(
            feil.message!!.contains("Ikke tillatt å generere brev etter at behandlingen er sendt fra beslutter"),
        )
    }

    @Test
    fun `Test sending varsel om revurdering til institusjon`() {
        val fnr = "09121079074"
        val orgNummer = "998765432"

        val fagsak =
            fagsakService.hentEllerOpprettFagsakForPersonIdent(
                fødselsnummer = fnr,
                fagsakType = FagsakType.INSTITUSJON,
                institusjon = RestInstitusjon(orgNummer = orgNummer, tssEksternId = "8000000"),
            )
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandlingUtenId(fagsak))

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlagForInstitusjon(
                behandlingId = behandling.id,
                barnasIdenter = listOf(fnr),
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(
            lagVilkårsvurdering(
                behandling.fagsak.aktør,
                behandling,
                resultat = Resultat.IKKE_VURDERT,
            ),
        )

        val manueltBrevRequest =
            dokumentService
                .byggMottakerdataFraBehandling(
                    behandling,
                    ManueltBrevRequest(brevmal = Brevmal.VARSEL_OM_REVURDERING_INSTITUSJON),
                )
        dokumentService.sendManueltBrev(manueltBrevRequest, behandling, behandling.fagsak.id)

        val lagretJournalførManueltBrevTaskPayloadForBehandling = fakeTaskRepositoryWrapper.hentLagredeTaskerAvType(JournalførManueltBrevTask.TASK_STEP_TYPE).tilPayload<JournalførManueltBrevDTO>().single { it.behandlingId == behandling.id }

        assertThat(lagretJournalførManueltBrevTaskPayloadForBehandling).isNotNull
        val avsenderMottaker = lagretJournalførManueltBrevTaskPayloadForBehandling.mottaker.avsenderMottaker
        val lagretBrevReuest = lagretJournalførManueltBrevTaskPayloadForBehandling.manuellBrevRequest
        assertThat(avsenderMottaker?.id == orgNummer && avsenderMottaker.navn == "Testinstitusjon").isTrue
        assertThat(lagretBrevReuest.vedrørende?.navn).isEqualTo("institusjonsbarnets navn")
        assertThat(lagretBrevReuest.vedrørende?.fødselsnummer).isEqualTo(fnr)
        assertThat(lagretBrevReuest.brevmal).isEqualTo(manueltBrevRequest.brevmal)
    }

    @Test
    fun `Test sending innhent dokumentasjon til institusjon`() {
        val fnr = randomFnr()
        val orgNummer = "998765432"

        val fagsak =
            fagsakService.hentEllerOpprettFagsakForPersonIdent(
                fødselsnummer = fnr,
                fagsakType = FagsakType.INSTITUSJON,
                institusjon = RestInstitusjon(orgNummer = orgNummer, tssEksternId = "8000000"),
            )
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandlingUtenId(fagsak))

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlagForInstitusjon(
                behandlingId = behandling.id,
                barnasIdenter = listOf(fnr),
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(
            lagVilkårsvurdering(
                behandling.fagsak.aktør,
                behandling,
                resultat = Resultat.IKKE_VURDERT,
            ),
        )

        val manueltBrevRequest =
            dokumentService.byggMottakerdataFraBehandling(
                behandling,
                ManueltBrevRequest(brevmal = Brevmal.INNHENTE_OPPLYSNINGER_INSTITUSJON),
            )
        dokumentService.sendManueltBrev(manueltBrevRequest, behandling, behandling.fagsak.id)

        val lagretJournalførManueltBrevTaskPayloadForBehandling = fakeTaskRepositoryWrapper.hentLagredeTaskerAvType(JournalførManueltBrevTask.TASK_STEP_TYPE).tilPayload<JournalførManueltBrevDTO>().single { it.behandlingId == behandling.id }
        assertThat(lagretJournalførManueltBrevTaskPayloadForBehandling).isNotNull
        val avsenderMottaker = lagretJournalførManueltBrevTaskPayloadForBehandling.mottaker.avsenderMottaker
        val lagretBrevReuest = lagretJournalførManueltBrevTaskPayloadForBehandling.manuellBrevRequest
        assertThat(avsenderMottaker?.id == orgNummer && avsenderMottaker.navn == "Testinstitusjon").isTrue
        assertThat(lagretBrevReuest.vedrørende?.navn).isEqualTo("institusjonsbarnets navn")
        assertThat(lagretBrevReuest.vedrørende?.fødselsnummer).isEqualTo(fnr)
        assertThat(lagretBrevReuest.brevmal).isEqualTo(manueltBrevRequest.brevmal)
    }

    @Test
    fun `Test sending svartidsbrev til institusjon`() {
        val fnr = "10121079074"
        val orgNummer = "998765432"

        val fagsak =
            fagsakService.hentEllerOpprettFagsakForPersonIdent(
                fødselsnummer = fnr,
                fagsakType = FagsakType.INSTITUSJON,
                institusjon = RestInstitusjon(orgNummer = orgNummer, tssEksternId = "8000000"),
            )
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandlingUtenId(fagsak))

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlagForInstitusjon(
                behandlingId = behandling.id,
                barnasIdenter = listOf(fnr),
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(
            lagVilkårsvurdering(
                behandling.fagsak.aktør,
                behandling,
                resultat = Resultat.IKKE_VURDERT,
            ),
        )

        val manueltBrevRequest =
            dokumentService.byggMottakerdataFraBehandling(
                behandling,
                ManueltBrevRequest(brevmal = Brevmal.SVARTIDSBREV_INSTITUSJON),
            )
        dokumentService.sendManueltBrev(manueltBrevRequest, behandling, behandling.fagsak.id)

        val lagretJournalførManueltBrevTaskPayloadForBehandling = fakeTaskRepositoryWrapper.hentLagredeTaskerAvType(JournalførManueltBrevTask.TASK_STEP_TYPE).tilPayload<JournalførManueltBrevDTO>().single { it.behandlingId == behandling.id }
        assertThat(lagretJournalførManueltBrevTaskPayloadForBehandling).isNotNull
        val avsenderMottaker = lagretJournalførManueltBrevTaskPayloadForBehandling.mottaker.avsenderMottaker
        val lagretBrevReuest = lagretJournalførManueltBrevTaskPayloadForBehandling.manuellBrevRequest
        assertThat(avsenderMottaker?.id == orgNummer && avsenderMottaker.navn == "Testinstitusjon").isTrue
        assertThat(lagretBrevReuest.vedrørende?.navn).isEqualTo("institusjonsbarnets navn")
        assertThat(lagretBrevReuest.vedrørende?.fødselsnummer).isEqualTo(fnr)
        assertThat(lagretBrevReuest.brevmal).isEqualTo(manueltBrevRequest.brevmal)
    }

    @Test
    fun `Test sending forlenget svartidsbrev til institusjon`() {
        val fnr = randomFnr()
        val orgNummer = "998765432"

        val fagsak =
            fagsakService.hentEllerOpprettFagsakForPersonIdent(
                fødselsnummer = fnr,
                fagsakType = FagsakType.INSTITUSJON,
                institusjon = RestInstitusjon(orgNummer = orgNummer, tssEksternId = "8000000"),
            )
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandlingUtenId(fagsak))

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlagForInstitusjon(
                behandlingId = behandling.id,
                barnasIdenter = listOf(fnr),
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(
            lagVilkårsvurdering(
                behandling.fagsak.aktør,
                behandling,
                resultat = Resultat.IKKE_VURDERT,
            ),
        )

        val manueltBrevRequest =
            dokumentService.byggMottakerdataFraBehandling(
                behandling,
                ManueltBrevRequest(
                    brevmal = Brevmal.FORLENGET_SVARTIDSBREV_INSTITUSJON,
                    antallUkerSvarfrist = 3,
                ),
            )
        dokumentService.sendManueltBrev(manueltBrevRequest, behandling, behandling.fagsak.id)

        val lagretJournalførManueltBrevTaskPayloadForBehandling = fakeTaskRepositoryWrapper.hentLagredeTaskerAvType(JournalførManueltBrevTask.TASK_STEP_TYPE).tilPayload<JournalførManueltBrevDTO>().single { it.behandlingId == behandling.id }
        assertThat(lagretJournalførManueltBrevTaskPayloadForBehandling).isNotNull
        val avsenderMottaker = lagretJournalførManueltBrevTaskPayloadForBehandling.mottaker.avsenderMottaker
        val lagretBrevReuest = lagretJournalførManueltBrevTaskPayloadForBehandling.manuellBrevRequest
        assertThat(avsenderMottaker?.id == orgNummer && avsenderMottaker.navn == "Testinstitusjon").isTrue
        assertThat(lagretBrevReuest.vedrørende?.navn).isEqualTo("institusjonsbarnets navn")
        assertThat(lagretBrevReuest.vedrørende?.fødselsnummer).isEqualTo(fnr)
        assertThat(lagretBrevReuest.brevmal).isEqualTo(manueltBrevRequest.brevmal)
    }

    fun lagTestPersonopplysningGrunnlagForInstitusjon(
        behandlingId: Long,
        barnasIdenter: List<String>,
        barnasFødselsdatoer: List<LocalDate> = barnasIdenter.map { LocalDate.of(2019, 1, 1) },
        barnAktør: List<Aktør> =
            barnasIdenter.map { fødselsnummer ->
                lagAktør(fødselsnummer).also {
                    it.personidenter.add(
                        Personident(
                            fødselsnummer = fødselsnummer,
                            aktør = it,
                            aktiv = fødselsnummer == it.personidenter.first().fødselsnummer,
                        ),
                    )
                }
            },
    ): PersonopplysningGrunnlag {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandlingId)
        val bostedsadresse =
            GrMatrikkeladresseBostedsadresse(
                matrikkelId = null,
                bruksenhetsnummer = "H301",
                tilleggsnavn = "navn",
                postnummer = "0202",
                kommunenummer = "2231",
                poststed = "Oslo",
            )

        barnAktør.mapIndexed { index, aktør ->
            personopplysningGrunnlag.personer.add(
                Person(
                    aktør = aktør,
                    type = PersonType.BARN,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    fødselsdato = barnasFødselsdatoer[index],
                    navn = "institusjonsbarnets navn",
                    kjønn = Kjønn.MANN,
                ).also { barn ->
                    barn.statsborgerskap =
                        mutableListOf(
                            GrStatsborgerskap(
                                landkode = "NOR",
                                medlemskap = Medlemskap.NORDEN,
                                person = barn,
                            ),
                        )
                    barn.bostedsadresser = mutableListOf(bostedsadresse.apply { person = barn })
                    barn.sivilstander =
                        mutableListOf(
                            GrSivilstand(
                                type = SIVILSTANDTYPE.UGIFT,
                                person = barn,
                            ),
                        )
                },
            )
        }
        return personopplysningGrunnlag
    }
}
