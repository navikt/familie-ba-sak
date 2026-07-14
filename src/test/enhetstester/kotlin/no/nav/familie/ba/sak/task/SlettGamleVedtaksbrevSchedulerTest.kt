package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.leader.LeaderClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

class SlettGamleVedtaksbrevSchedulerTest {
    private val vedtakRepository = mockk<VedtakRepository>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val scheduler = SlettGamleVedtaksbrevScheduler(vedtakRepository, featureToggleService)

    @BeforeEach
    fun setUp() {
        mockkStatic(LeaderClient::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(LeaderClient::class)
    }

    @Test
    fun `skal ikke slette noe når podden ikke er leader`() {
        // Arrange
        every { LeaderClient.isLeader() } returns false

        // Act
        scheduler.slettGamleVedtaksbrev()

        // Assert
        verify(exactly = 0) { featureToggleService.isEnabled(any<FeatureToggle>()) }
        verify(exactly = 0) { vedtakRepository.finnVedtakIderMedStønadBrevPdf(any(), any(), any()) }
        verify(exactly = 0) { vedtakRepository.slettStønadBrevPdfForVedtak(any()) }
    }

    @Test
    fun `skal ikke slette noe når feature toggle er avslått`() {
        // Arrange
        every { LeaderClient.isLeader() } returns true
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_SLETTE_GAMLE_VEDTAKSBREV_FRA_DB) } returns false

        // Act
        scheduler.slettGamleVedtaksbrev()

        // Assert
        verify(exactly = 0) { vedtakRepository.finnVedtakIderMedStønadBrevPdf(any(), any(), any()) }
        verify(exactly = 0) { vedtakRepository.slettStønadBrevPdfForVedtak(any()) }
    }

    @Test
    fun `skal slette vedtaksbrev på avsluttede behandlinger i batcher til det ikke er flere igjen`() {
        // Arrange
        every { LeaderClient.isLeader() } returns true
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_SLETTE_GAMLE_VEDTAKSBREV_FRA_DB) } returns true

        val førsteBatch = (1L..500L).toList()
        val andreBatch = (501L..800L).toList()
        every {
            vedtakRepository.finnVedtakIderMedStønadBrevPdf(any(), any(), any())
        } returnsMany listOf(førsteBatch, andreBatch, emptyList())
        every { vedtakRepository.slettStønadBrevPdfForVedtak(any()) } answers { firstArg<List<Long>>().size }

        // Act
        scheduler.slettGamleVedtaksbrev()

        // Assert
        verify(exactly = 1) { vedtakRepository.slettStønadBrevPdfForVedtak(førsteBatch) }
        verify(exactly = 1) { vedtakRepository.slettStønadBrevPdfForVedtak(andreBatch) }
        // Tredje kall til finn returnerer tom liste -> løkka stopper og sletter ikke mer
        verify(exactly = 3) { vedtakRepository.finnVedtakIderMedStønadBrevPdf(any(), any(), any()) }
        verify(exactly = 2) { vedtakRepository.slettStønadBrevPdfForVedtak(any()) }
    }

    @Test
    fun `skal bruke vedtaksdato tre måneder tilbake i tid som grense`() {
        // Arrange
        every { LeaderClient.isLeader() } returns true
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_SLETTE_GAMLE_VEDTAKSBREV_FRA_DB) } returns true
        every { vedtakRepository.finnVedtakIderMedStønadBrevPdf(any(), any(), any()) } returns emptyList()

        val førKjøring = LocalDateTime.now().minusMonths(3)

        // Act
        scheduler.slettGamleVedtaksbrev()

        val etterKjøring = LocalDateTime.now().minusMonths(3)

        // Assert
        verify {
            vedtakRepository.finnVedtakIderMedStønadBrevPdf(
                status = BehandlingStatus.AVSLUTTET,
                vedtaksdatoFør =
                    match {
                        !it.isBefore(førKjøring) && !it.isAfter(etterKjøring)
                    },
                pageable = any(),
            )
        }
    }

    @Test
    fun `skal ikke slette mer enn maks antall batcher per kjøring`() {
        // Arrange
        every { LeaderClient.isLeader() } returns true
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_SLETTE_GAMLE_VEDTAKSBREV_FRA_DB) } returns true
        // Returnerer alltid en full batch -> ville løpt uendelig uten maksgrense
        every { vedtakRepository.finnVedtakIderMedStønadBrevPdf(any(), any(), any()) } returns (1L..500L).toList()
        every { vedtakRepository.slettStønadBrevPdfForVedtak(any()) } returns 500

        // Act
        scheduler.slettGamleVedtaksbrev()

        // Assert
        verify(exactly = 20) { vedtakRepository.slettStønadBrevPdfForVedtak(any()) }
    }
}
