package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagBrevmottakerDb
import no.nav.familie.ba.sak.integrasjoner.journalføring.UtgåendeJournalføringService
import no.nav.familie.ba.sak.integrasjoner.organisasjon.OrganisasjonService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerService
import no.nav.familie.ba.sak.kjerne.brev.mottaker.Bruker
import no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning.TilbakekrevingsvedtakMotregning
import no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning.TilbakekrevingsvedtakMotregningService
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JournalførTilbakekrevingsvedtakMotregningBrevTaskTest {
    private val mockArbeidsfordelingService = mockk<ArbeidsfordelingService>()
    private val mockUtgåendeJournalføringService = mockk<UtgåendeJournalføringService>()
    private val mockTaskRepository = mockk<TaskRepositoryWrapper>()
    private val mockOrganisasjonService = mockk<OrganisasjonService>()
    private val mockBrevmottakerService = mockk<BrevmottakerService>()
    private val mockTilbakekrevingsvedtakMotregningService = mockk<TilbakekrevingsvedtakMotregningService>()
    private val mockBehandlingService = mockk<BehandlingHentOgPersisterService>()

    private val behandling = lagBehandling()
    private val arbeidsfordelingPåBehandling = ArbeidsfordelingPåBehandling(behandlingId = behandling.id, behandlendeEnhetNavn = "testNavn", behandlendeEnhetId = "testId")

    private val journalførTilbakekrevingsvedtakMotregningBrevTask =
        JournalførTilbakekrevingsvedtakMotregningBrevTask(
            arbeidsfordelingService = mockArbeidsfordelingService,
            utgåendeJournalføringService = mockUtgåendeJournalføringService,
            taskRepository = mockTaskRepository,
            organisasjonService = mockOrganisasjonService,
            brevmottakerService = mockBrevmottakerService,
            tilbakekrevingsvedtakMotregningService = mockTilbakekrevingsvedtakMotregningService,
            behandlingService = mockBehandlingService,
        )

    @BeforeEach
    fun setUp() {
        every { mockBehandlingService.hent(behandling.id) } returns behandling
        every { mockArbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandling.id) } returns arbeidsfordelingPåBehandling
        every { mockOrganisasjonService.hentOrganisasjon(any()) } returns Organisasjon("orgNummer", "orgNavn")
        every { mockUtgåendeJournalføringService.journalførDokument(any(), any(), any(), any(), any(), any(), any(), any()) } returnsMany listOf("1", "2", "3", "4", "5", "6", "7", "8")
        every { mockTaskRepository.save(any()) } returnsArgument 0
    }

    @Test
    fun `Skal journalføre Tilbakekrevingsvedtak motregning til brevmottakere og lage task for å distribuere dokumentet til alle mottakere`() {
        // Arrange
        val tilbakekrevingsvedtakMotregning =
            TilbakekrevingsvedtakMotregning(
                id = 1,
                behandling = behandling,
                samtykke = true,
                vedtakPdf = ByteArray(0),
                heleBeløpetSkalKrevesTilbake = true,
            )

        every { mockTilbakekrevingsvedtakMotregningService.hentTilbakekrevingsvedtakMotregningEllerKastFunksjonellFeil(behandlingId = behandling.id) } returns tilbakekrevingsvedtakMotregning
        every { mockBrevmottakerService.hentBrevmottakere(behandling.id) } returns listOf(lagBrevmottakerDb(behandlingId = behandling.id, landkode = "NO"), lagBrevmottakerDb(behandlingId = behandling.id, landkode = "SE"))
        every { mockBrevmottakerService.lagMottakereFraBrevMottakere(any()) } returns listOf(Bruker, Bruker)
        every { mockTilbakekrevingsvedtakMotregningService.finnTilbakekrevingsvedtakMotregning(behandling.id) } returns tilbakekrevingsvedtakMotregning

        // Act
        journalførTilbakekrevingsvedtakMotregningBrevTask.doTask(
            JournalførTilbakekrevingsvedtakMotregningBrevTask.opprettTask(behandling.id),
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
