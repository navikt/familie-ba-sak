package no.nav.familie.ba.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.datagenerator.defaultFagsak
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagBrevmottakerDb
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.integrasjoner.journalføring.UtgåendeJournalføringService
import no.nav.familie.ba.sak.integrasjoner.organisasjon.OrganisasjonService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerRepository
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerService
import no.nav.familie.ba.sak.kjerne.brev.mottaker.Bruker
import no.nav.familie.ba.sak.kjerne.brev.mottaker.FullmektigEllerVerge
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.institusjon.Institusjon
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.dto.JournalførManueltBrevDTO
import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class DokumentServiceTest {
    private val vilkårsvurderingService = mockk<VilkårsvurderingService>(relaxed = true)
    private val vilkårsvurderingForNyBehandlingService = mockk<VilkårsvurderingForNyBehandlingService>(relaxed = true)
    private val utgåendeJournalføringService = mockk<UtgåendeJournalføringService>(relaxed = true)
    private val taskRepository = mockk<TaskRepositoryWrapper>(relaxed = true)
    private val fagsakRepository = mockk<FagsakRepository>(relaxed = true)
    private val organisasjonService = mockk<OrganisasjonService>(relaxed = true)
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>(relaxed = true)
    private val brevmottakerRepository = mockk<BrevmottakerRepository>()
    private val brevmottakerService =
        spyk<BrevmottakerService>(
            BrevmottakerService(
                brevmottakerRepository = brevmottakerRepository,
                loggService = mockk(),
                validerBrevmottakerService = mockk(),
            ),
        )

    private val dokumentService: DokumentService =
        spyk(
            DokumentService(
                taskRepository = taskRepository,
                vilkårsvurderingService = vilkårsvurderingService,
                vilkårsvurderingForNyBehandlingService = vilkårsvurderingForNyBehandlingService,
                rolleConfig = mockk(relaxed = true),
                settPåVentService = mockk(relaxed = true),
                utgåendeJournalføringService = utgåendeJournalføringService,
                fagsakRepository = fagsakRepository,
                organisasjonService = organisasjonService,
                behandlingHentOgPersisterService = behandlingHentOgPersisterService,
                dokumentGenereringService = mockk(relaxed = true),
                brevmottakerService = brevmottakerService,
                validerBrevmottakerService = mockk(relaxed = true),
            ),
        )

    @Test
    fun `sendManueltBrev skal journalføre med brukerIdType ORGNR når fagsakType er INSTITUSJON`() {
        val avsenderMottaker = slot<AvsenderMottaker>()
        val behandling =
            lagBehandling(
                Fagsak(
                    type = FagsakType.INSTITUSJON,
                    aktør = randomAktør(),
                    institusjon = Institusjon(orgNummer = "123456789", tssEksternId = "xxx"),
                ),
            )

        every { fagsakRepository.finnFagsak(any()) } returns behandling.fagsak

        val taskSlot = slot<Task>()
        every { taskRepository.save(capture(taskSlot)) } returns mockk()

        val orgNummer = behandling.fagsak.institusjon!!.orgNummer
        every { organisasjonService.hentOrganisasjon(any()) } returns
            Organisasjon(
                organisasjonsnummer = orgNummer,
                navn = "Testinstitusjon",
            )
        every { brevmottakerService.hentBrevmottakere(behandling.id) } returns emptyList()

        runCatching {
            dokumentService.sendManueltBrev(
                ManueltBrevRequest(
                    brevmal = Brevmal.INNHENTE_OPPLYSNINGER,
                    enhet = Enhet("enhet", "enhetNavn"),
                ),
                behandling = behandling,
                fagsakId = behandling.fagsak.id,
            )
        }

        val capturedTask = taskSlot.captured

        assertThat(capturedTask.metadata["mottakerType"]).isEqualTo(Institusjon::class.simpleName)

        val journalførManueltBrevDTO = objectMapper.readValue(capturedTask.payload, JournalførManueltBrevDTO::class.java)
        assertThat(journalførManueltBrevDTO.mottaker.avsenderMottaker).isNotNull
        assertThat(journalførManueltBrevDTO.mottaker.avsenderMottaker!!.idType).isEqualTo(AvsenderMottakerIdType.ORGNR)
        assertThat(journalførManueltBrevDTO.mottaker.avsenderMottaker.id).isEqualTo(orgNummer)
        assertThat(journalførManueltBrevDTO.mottaker.avsenderMottaker.navn).isEqualTo("Testinstitusjon")
    }

    @Test
    fun `sendManueltBrev skal journalføre uten eksplisitt AvsenderMottaker når mottaker er bruker`() {
        val avsenderMottaker = slot<AvsenderMottaker>()
        val behandling = lagBehandling()

        every { fagsakRepository.finnFagsak(any()) } returns behandling.fagsak

        every {
            utgåendeJournalføringService.journalførDokument(
                fnr = any(),
                fagsakId = any(),
                journalførendeEnhet = any(),
                brev = any(),
                førsteside = any(),
                eksternReferanseId = any(),
                avsenderMottaker = capture(avsenderMottaker),
            )
        } returns "mockJournalpostId"

        every { brevmottakerService.hentBrevmottakere(behandling.id) } returns emptyList()

        runCatching {
            dokumentService.sendManueltBrev(
                ManueltBrevRequest(
                    brevmal = Brevmal.INNHENTE_OPPLYSNINGER,
                    enhet = Enhet("enhet", "enhetNavn"),
                ),
                behandling = behandling,
                fagsakId = behandling.fagsak.id,
            )
        }

        assert(!avsenderMottaker.isCaptured) { "AvsenderMottaker trenger ikke være satt når mottaker er bruker" }
    }

    @Test
    fun `sendManueltBrev skal legge til opplysningspliktvilkåret når gjeldende og forrige vilkårsvurdering mangler`() {
        val brevSomFørerTilOpplysningsplikt = Brevmal.entries.filter { it.førerTilOpplysningsplikt() }

        brevSomFørerTilOpplysningsplikt.forEach { brevmal ->
            val behandling = lagBehandling()
            val vilkårsvurdering = lagVilkårsvurdering(lagPerson().aktør, behandling, Resultat.IKKE_VURDERT)
            val personResultat = vilkårsvurdering.personResultater.find { it.erSøkersResultater() }!!

            // Scenario uten eksisterende vilkårsvurdering
            every { vilkårsvurderingService.hentAktivForBehandling(any()) } returns null
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns null
            every {
                vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
                    any(),
                    any(),
                    null,
                )
            } returns vilkårsvurdering

            every { brevmottakerService.hentBrevmottakere(behandling.id) } returns emptyList()

            sendBrev(brevmal, behandling)

            assertThat(personResultat.andreVurderinger).extracting("type").containsExactly(AnnenVurderingType.OPPLYSNINGSPLIKT)
            verify(exactly = 1) {
                behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)
            }
            verify(exactly = 1) {
                vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(behandling, any(), null)
            }
        }
    }

    @Test
    fun `sendManueltBrev skal legge til opplysningspliktvilkåret når gjeldende vilkårsvurdering mangler, men forrige finnes`() {
        val brevSomFørerTilOpplysningsplikt = Brevmal.entries.filter { it.førerTilOpplysningsplikt() }

        brevSomFørerTilOpplysningsplikt.forEach { brevmal ->
            val behandling = lagBehandling()
            val forrigeVedtatteBehandling = lagBehandling()
            val vilkårsvurdering = lagVilkårsvurdering(lagPerson().aktør, behandling, Resultat.IKKE_VURDERT)
            val personResultat = vilkårsvurdering.personResultater.find { it.erSøkersResultater() }!!

            // Scenario uten eksisterende vilkårsvurdering
            every { vilkårsvurderingService.hentAktivForBehandling(any()) } returns null
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns forrigeVedtatteBehandling
            every {
                vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
                    any(),
                    any(),
                    forrigeVedtatteBehandling,
                )
            } returns vilkårsvurdering

            every { brevmottakerService.hentBrevmottakere(behandling.id) } returns emptyList()

            sendBrev(brevmal, behandling)

            assertThat(personResultat.andreVurderinger).extracting("type").containsExactly(AnnenVurderingType.OPPLYSNINGSPLIKT)
            verify(exactly = 1) {
                behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)
            }
            verify(exactly = 1) {
                vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
                    behandling,
                    any(),
                    forrigeVedtatteBehandling,
                )
            }
        }
    }

    @Test
    fun `sendManueltBrev skal legge til opplysningspliktvilkåret når vilkårsvurderingen finnes`() {
        val brevSomFørerTilOpplysningsplikt = Brevmal.entries.filter { it.førerTilOpplysningsplikt() }

        brevSomFørerTilOpplysningsplikt.forEach { brevmal ->
            val behandling = lagBehandling()
            val vilkårsvurdering = lagVilkårsvurdering(lagPerson().aktør, behandling, Resultat.IKKE_VURDERT)
            val personResultat = vilkårsvurdering.personResultater.find { it.erSøkersResultater() }!!

            // Scenario med eksisterende vilkårsvurdering
            every { vilkårsvurderingService.hentAktivForBehandling(any()) } returns vilkårsvurdering
            every { brevmottakerService.hentBrevmottakere(behandling.id) } returns emptyList()

            sendBrev(brevmal, behandling)

            assertThat(personResultat.andreVurderinger).extracting("type").containsExactly(AnnenVurderingType.OPPLYSNINGSPLIKT)
            verify(exactly = 0) {
                vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(behandling, any(), null)
            }
        }
    }

    @Test
    fun `sendManueltBrev skal sende manuelt brev til FULLMEKTIG og bruker som har FULLMEKTIG manuelt brev mottaker`() {
        val behandling = lagBehandling()
        val manueltBrevRequest = ManueltBrevRequest(brevmal = Brevmal.SVARTIDSBREV)

        every { brevmottakerService.hentBrevmottakere(behandling.id) } returns
            listOf(
                lagBrevmottakerDb(behandlingId = behandling.id, navn = "Fullmektig navn"),
            )

        val lagredeTasker = mutableListOf<Task>()

        every { taskRepository.save(capture(lagredeTasker)) } returns mockk()
        every { fagsakRepository.finnFagsak(behandling.fagsak.id) } returns behandling.fagsak

        dokumentService.sendManueltBrev(manueltBrevRequest, behandling, behandling.fagsak.id)

        verify(exactly = 2) {
            taskRepository.save(
                any(),
            )
        }

        val mottakerTyper = lagredeTasker.map { it.metadata["mottakerType"] }
        assertThat(mottakerTyper).containsExactlyInAnyOrder(Bruker::class.simpleName, FullmektigEllerVerge::class.simpleName)
    }

    @Test
    fun `sendManueltBrev skal sende informasjonsbrev manuelt på fagsak`() {
        val fagsak = defaultFagsak()
        val manueltBrevRequest = ManueltBrevRequest(brevmal = Brevmal.INFORMASJONSBREV_KAN_SØKE)

        val taskSlot = slot<Task>()

        every { taskRepository.save(capture(taskSlot)) } returns mockk()
        every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

        dokumentService.sendManueltBrev(manueltBrevRequest, null, fagsak.id)

        verify(exactly = 1) {
            taskRepository.save(
                any(),
            )
        }

        val capturedTask = taskSlot.captured
        assertThat(capturedTask.type).isEqualTo("journalførManueltBrev")
        assertThat(capturedTask.metadata["fagsakId"]).isEqualTo(fagsak.id.toString())
        assertThat(capturedTask.metadata["behandlingId"]).isEqualTo("null")
        assertThat(capturedTask.payload).contains(Brevmal.INFORMASJONSBREV_KAN_SØKE.name)
    }

    @Test
    fun `sendManueltBrev skal feile hvis den manuelle brevmottakeren er ugyldig`() {
        // Arrange
        val behandling = lagBehandling()
        val manueltBrevRequest = ManueltBrevRequest(brevmal = Brevmal.SVARTIDSBREV)
        val avsenderMottakere = mutableListOf<AvsenderMottaker>()

        every { brevmottakerService.hentBrevmottakere(behandling.id) } returns
            listOf(
                lagBrevmottakerDb(behandlingId = behandling.id, landkode = "SE"),
                lagBrevmottakerDb(behandlingId = behandling.id, landkode = "NO"),
            )
        every {
            utgåendeJournalføringService.journalførDokument(
                fnr = any(),
                fagsakId = any(),
                journalførendeEnhet = any(),
                brev = any(),
                førsteside = any(),
                eksternReferanseId = any(),
                avsenderMottaker = capture(avsenderMottakere),
            )
        } returns "mockJournalPostId" andThen "mockJournalPostId1"

        every { taskRepository.save(any()) } returns mockk()
        every { fagsakRepository.finnFagsak(behandling.fagsak.id) } returns behandling.fagsak

        // Act & assert
        val exception =
            assertThrows<FunksjonellFeil> {
                dokumentService.sendManueltBrev(manueltBrevRequest, behandling, behandling.fagsak.id)
            }

        assertThat(exception.message).isEqualTo("Det finnes ugyldige brevmottakere i utsending av manuelt brev")

        verify(exactly = 0) {
            utgåendeJournalføringService.journalførDokument(
                fnr = any(),
                fagsakId = any(),
                journalførendeEnhet = any(),
                brev = any(),
                førsteside = any(),
                eksternReferanseId = any(),
                avsenderMottaker = any(),
            )
        }
        verify(exactly = 0) { taskRepository.save(any()) }
    }

    @Nested
    inner class HentBrevForVedtak {
        @BeforeEach
        fun beforeEach() {
            mockkObject(SikkerhetContext)
        }

        @AfterEach
        fun afterEach() {
            unmockkObject(SikkerhetContext)
        }

        @ParameterizedTest
        @EnumSource(value = BehandlerRolle::class, names = ["FORVALTER", "VEILEDER"])
        fun `Skal kaste Funksjonell feil dersom vedtaksbrev ikke finnes og man er VEILEDER eller FORVALTER`(rolle: BehandlerRolle) {
            // Arrange
            every { SikkerhetContext.hentHøyesteRolletilgangForInnloggetBruker(any()) } returns rolle

            val vedtakUtenStønadBrev = lagVedtak(stønadBrevPdF = null)

            // Act && Assert
            val feilmelding =
                assertThrows<FunksjonellFeil> {
                    dokumentService.hentBrevForVedtak(vedtakUtenStønadBrev)
                }.frontendFeilmelding

            assertThat(feilmelding).isEqualTo("Det finnes ikke noe vedtaksbrev.")
        }

        @ParameterizedTest
        @EnumSource(value = BehandlerRolle::class, names = ["SAKSBEHANDLER", "BESLUTTER"])
        fun `Skal kaste Feil dersom vedtaksbrev ikke finnes og man er SAKSBEHANDLER eller BESLUTTER`(rolle: BehandlerRolle) {
            // Arrange
            every { SikkerhetContext.hentHøyesteRolletilgangForInnloggetBruker(any()) } returns rolle

            val vedtakUtenStønadBrev = lagVedtak(stønadBrevPdF = null)

            // Act && Assert
            val feilmelding =
                assertThrows<FunksjonellFeil> {
                    dokumentService.hentBrevForVedtak(vedtakUtenStønadBrev)
                }.message

            assertThat(feilmelding).isEqualTo("Klarte ikke finne vedtaksbrev for vedtak med id ${vedtakUtenStønadBrev.id}. Innlogget bruker har rolle: $rolle")
        }

        @Test
        fun `Skal returnere bytearray med pdf innhold dersom det finnes på vedtak`() {
            // Arrange
            every { SikkerhetContext.hentHøyesteRolletilgangForInnloggetBruker(any()) } returns BehandlerRolle.BESLUTTER

            val byteArray = ByteArray(0)
            val vedtakMedStønadBrev = lagVedtak(stønadBrevPdF = byteArray)

            // Act
            val vedtaksbrevPdf = dokumentService.hentBrevForVedtak(vedtakMedStønadBrev)

            // Assert
            assertThat(vedtaksbrevPdf.data).isEqualTo(byteArray)
        }
    }

    private fun sendBrev(
        brevmal: Brevmal,
        behandling: Behandling,
    ) {
        dokumentService.sendManueltBrev(
            ManueltBrevRequest(
                brevmal = brevmal,
                enhet = Enhet("enhet", "enhetNavn"),
            ),
            behandling = behandling,
            fagsakId = behandling.fagsak.id,
        )
    }
}
