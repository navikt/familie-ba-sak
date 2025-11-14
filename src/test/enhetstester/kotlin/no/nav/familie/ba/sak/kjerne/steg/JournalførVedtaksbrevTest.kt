package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagBrevmottakerDb
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.integrasjoner.journalføring.UtgåendeJournalføringService
import no.nav.familie.ba.sak.integrasjoner.organisasjon.OrganisasjonService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerService
import no.nav.familie.ba.sak.kjerne.brev.mottaker.Bruker
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.institusjon.Institusjon
import no.nav.familie.ba.sak.kjerne.steg.domene.JournalførVedtaksbrevDTO
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JournalførVedtaksbrevTest {
    private val mockVedtakService = mockk<VedtakService>()
    private val mockArbeidsfordelingService = mockk<ArbeidsfordelingService>()
    private val mockUtgåendeJournalføringService = mockk<UtgåendeJournalføringService>()
    private val mockTaskRepository = mockk<TaskRepositoryWrapper>()
    private val mockFagsakRepository = mockk<FagsakRepository>()
    private val mockOrganisasjonService = mockk<OrganisasjonService>()
    private val mockBrevmottakerService = mockk<BrevmottakerService>()
    private val mockBrevmalService = mockk<BrevmalService>()

    private val behandling = lagBehandling()
    private val vedtak = lagVedtak(behandling).apply { stønadBrevPdF = ByteArray(50) }
    private val arbeidsfordelingPåBehandling = ArbeidsfordelingPåBehandling(behandlingId = behandling.id, behandlendeEnhetNavn = "testNavn", behandlendeEnhetId = "testId")

    private val journalførVedtaksbrevTask =
        JournalførVedtaksbrev(
            vedtakService = mockVedtakService,
            arbeidsfordelingService = mockArbeidsfordelingService,
            utgåendeJournalføringService = mockUtgåendeJournalføringService,
            taskRepository = mockTaskRepository,
            fagsakRepository = mockFagsakRepository,
            organisasjonService = mockOrganisasjonService,
            brevmottakerService = mockBrevmottakerService,
            brevmalService = mockBrevmalService,
        )

    @BeforeEach
    fun setUp() {
        every { mockVedtakService.hent(vedtak.id) } returns vedtak
        every { mockArbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandling.id) } returns arbeidsfordelingPåBehandling
        every { mockFagsakRepository.finnFagsak(behandling.fagsak.id) } returns behandling.fagsak
        every { mockOrganisasjonService.hentOrganisasjon(any()) } returns Organisasjon("orgNummer", "orgNavn")
        every { mockUtgåendeJournalføringService.journalførDokument(any(), any(), any(), any(), any(), any(), any(), any()) } returnsMany listOf("1", "2", "3", "4", "5", "6", "7", "8")
        every { mockBrevmalService.hentBrevmal(behandling) } returns Brevmal.VEDTAK_FØRSTEGANGSVEDTAK
        every { mockTaskRepository.save(any()) } returnsArgument 0
    }

    @Test
    fun `Skal kaste feil dersom fagsak er null`() {
        // Arrange
        every { mockFagsakRepository.finnFagsak(behandling.fagsak.id) } returns null

        // Act & Assert
        val feilmelding =
            assertThrows<Feil> {
                journalførVedtaksbrevTask.utførStegOgAngiNeste(
                    behandling = behandling,
                    data = JournalførVedtaksbrevDTO(vedtakId = vedtak.id, task = mockk()),
                )
            }.message

        assertThat(feilmelding).isEqualTo("Journalfør vedtaksbrev feil: fagsak er null eller institusjon fagsak har ikke institusjonsinformasjon")
    }

    @Test
    fun `Skal kaste feil dersom fagsak er institusjon og det ikke er institusjonsinformasjon på fagsaken`() {
        // Arrange
        every { mockFagsakRepository.finnFagsak(behandling.fagsak.id) } returns lagFagsak(type = FagsakType.INSTITUSJON, institusjon = null)

        // Act & Assert
        val feilmelding =
            assertThrows<Feil> {
                journalførVedtaksbrevTask.utførStegOgAngiNeste(
                    behandling = behandling,
                    data = JournalførVedtaksbrevDTO(vedtakId = vedtak.id, task = mockk()),
                )
            }.message

        assertThat(feilmelding).isEqualTo("Journalfør vedtaksbrev feil: fagsak er null eller institusjon fagsak har ikke institusjonsinformasjon")
    }

    @Test
    fun `Skal journalføre med mottaker type ORGNR og lage task for å distribuere dokument til institusjon hvis fagsak er av type institusjon`() {
        // Arrange
        every { mockFagsakRepository.finnFagsak(behandling.fagsak.id) } returns lagFagsak(type = FagsakType.INSTITUSJON, institusjon = Institusjon(orgNummer = "testOrgNummer", tssEksternId = null))

        // Act
        journalførVedtaksbrevTask.utførStegOgAngiNeste(
            behandling = behandling,
            data = JournalførVedtaksbrevDTO(vedtakId = vedtak.id, task = mockk(relaxed = true)),
        )

        // Assert
        verify(exactly = 1) {
            mockUtgåendeJournalføringService.journalførDokument(
                fnr = any(),
                fagsakId = any(),
                journalførendeEnhet = any(),
                brev = any(),
                vedlegg = any(),
                førsteside = any(),
                avsenderMottaker =
                    AvsenderMottaker(
                        idType = AvsenderMottakerIdType.ORGNR,
                        id = "testOrgNummer",
                        navn = "orgNavn",
                    ),
                eksternReferanseId = any(),
            )
        }
        verify(exactly = 1) { mockTaskRepository.save(any()) }
    }

    @Test
    fun `Skal journalføre uten avsendermottaker hvis det ikke er noen brevmottakere og lage task for å distribuere dokument til person hvis fagsak er av type normal`() {
        // Arrange
        every { mockFagsakRepository.finnFagsak(behandling.fagsak.id) } returns lagFagsak(type = FagsakType.NORMAL)
        every { mockBrevmottakerService.hentBrevmottakere(behandling.id) } returns emptyList()

        // Act
        journalførVedtaksbrevTask.utførStegOgAngiNeste(
            behandling = behandling,
            data = JournalførVedtaksbrevDTO(vedtakId = vedtak.id, task = mockk(relaxed = true)),
        )

        // Assert
        verify(exactly = 1) {
            mockUtgåendeJournalføringService.journalførDokument(
                fnr = any(),
                fagsakId = any(),
                journalførendeEnhet = any(),
                brev = any(),
                vedlegg = any(),
                førsteside = any(),
                avsenderMottaker = null,
                eksternReferanseId = any(),
            )
        }
        verify(exactly = 1) { mockTaskRepository.save(any()) }
    }

    @Test
    fun `Skal journalføre med brevmottakere og lage task for å distribuere dokument til alle mottakere hvis fagsak er av type normal`() {
        // Arrange
        every { mockFagsakRepository.finnFagsak(behandling.fagsak.id) } returns lagFagsak(type = FagsakType.NORMAL)
        every { mockBrevmottakerService.hentBrevmottakere(behandling.id) } returns listOf(lagBrevmottakerDb(behandlingId = behandling.id, landkode = "NO"), lagBrevmottakerDb(behandlingId = behandling.id, landkode = "SE"))
        every { mockBrevmottakerService.lagMottakereFraBrevMottakere(any()) } returns listOf(Bruker, Bruker)

        // Act
        journalførVedtaksbrevTask.utførStegOgAngiNeste(
            behandling = behandling,
            data = JournalførVedtaksbrevDTO(vedtakId = vedtak.id, task = mockk(relaxed = true)),
        )

        // Assert
        verify(exactly = 2) {
            mockUtgåendeJournalføringService.journalførDokument(
                fnr = any(),
                fagsakId = any(),
                journalførendeEnhet = any(),
                brev = any(),
                vedlegg = any(),
                førsteside = any(),
                avsenderMottaker = null,
                eksternReferanseId = any(),
            )
        }
        verify(exactly = 2) { mockTaskRepository.save(any()) }
    }
}
