package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.config.LeaderClientService
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.SlettInaktivePersonopplysningsgrunnlagService
import no.nav.familie.ba.sak.task.SlettInaktivePersonopplysningsgrunnlagScheduler.Companion.BATCH_STØRRELSE
import no.nav.familie.ba.sak.task.SlettInaktivePersonopplysningsgrunnlagScheduler.Companion.MAKS_ANTALL_BATCHER_PER_KJØRING
import org.junit.jupiter.api.Test

class SlettInaktivePersonopplysningsgrunnlagSchedulerTest {
    private val slettInaktivePersonopplysningsgrunnlagService = mockk<SlettInaktivePersonopplysningsgrunnlagService>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val leaderClientService = mockk<LeaderClientService>()
    private val scheduler = SlettInaktivePersonopplysningsgrunnlagScheduler(slettInaktivePersonopplysningsgrunnlagService, featureToggleService, leaderClientService)

    @Test
    fun `skal ikke slette noe når podden ikke er leader`() {
        // Arrange
        every { leaderClientService.isLeader() } returns false

        // Act
        scheduler.slettInaktivePersonopplysningsgrunnlag()

        // Assert
        verify(exactly = 0) { featureToggleService.isEnabled(any<FeatureToggle>()) }
        verify(exactly = 0) { slettInaktivePersonopplysningsgrunnlagService.slettBatchMedInaktiveGrunnlag(any(), any()) }
    }

    @Test
    fun `skal ikke slette noe når feature toggle er avslått`() {
        // Arrange
        every { leaderClientService.isLeader() } returns true
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_SLETTE_INAKTIVE_PERSONOPPLYSNINGSGRUNNLAG) } returns false

        // Act
        scheduler.slettInaktivePersonopplysningsgrunnlag()

        // Assert
        verify(exactly = 0) { slettInaktivePersonopplysningsgrunnlagService.slettBatchMedInaktiveGrunnlag(any(), any()) }
    }

    @Test
    fun `skal slette inaktive personopplysningsgrunnlag i batcher fra siste slettede id til det ikke er flere igjen`() {
        // Arrange
        every { leaderClientService.isLeader() } returns true
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_SLETTE_INAKTIVE_PERSONOPPLYSNINGSGRUNNLAG) } returns true
        every { slettInaktivePersonopplysningsgrunnlagService.tellInaktiveGrunnlagUtenAktivtGrunnlagPåSammeBehandling() } returns 0L

        val førsteBatch = (1L..100L).toList()
        val andreBatch = (101L..150L).toList()
        every {
            slettInaktivePersonopplysningsgrunnlagService.slettBatchMedInaktiveGrunnlag(any(), any())
        } returnsMany listOf(førsteBatch, andreBatch, emptyList())

        // Act
        scheduler.slettInaktivePersonopplysningsgrunnlag()

        // Assert
        verify(exactly = 1) { slettInaktivePersonopplysningsgrunnlagService.slettBatchMedInaktiveGrunnlag(etterId = 0L, batchStørrelse = BATCH_STØRRELSE) }
        verify(exactly = 1) { slettInaktivePersonopplysningsgrunnlagService.slettBatchMedInaktiveGrunnlag(etterId = 100L, batchStørrelse = BATCH_STØRRELSE) }
        // Tredje kall (etter siste slettede id 150) returnerer tom liste -> løkka stopper og sletter ikke mer
        verify(exactly = 1) { slettInaktivePersonopplysningsgrunnlagService.slettBatchMedInaktiveGrunnlag(etterId = 150L, batchStørrelse = BATCH_STØRRELSE) }
        verify(exactly = 3) { slettInaktivePersonopplysningsgrunnlagService.slettBatchMedInaktiveGrunnlag(any(), any()) }
    }

    @Test
    fun `skal ikke slette mer enn maks antall batcher per kjøring`() {
        // Arrange
        every { leaderClientService.isLeader() } returns true
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_SLETTE_INAKTIVE_PERSONOPPLYSNINGSGRUNNLAG) } returns true
        every { slettInaktivePersonopplysningsgrunnlagService.tellInaktiveGrunnlagUtenAktivtGrunnlagPåSammeBehandling() } returns 0L
        // Returnerer alltid en full batch -> ville løpt uendelig uten maksgrense
        every { slettInaktivePersonopplysningsgrunnlagService.slettBatchMedInaktiveGrunnlag(any(), any()) } returns (1L..200L).toList()

        // Act
        scheduler.slettInaktivePersonopplysningsgrunnlag()

        // Assert
        verify(exactly = MAKS_ANTALL_BATCHER_PER_KJØRING) { slettInaktivePersonopplysningsgrunnlagService.slettBatchMedInaktiveGrunnlag(any(), any()) }
    }

    @Test
    fun `skal telle inaktive grunnlag uten aktivt grunnlag på samme behandling selv om det ikke finnes noe å slette`() {
        // Arrange
        every { leaderClientService.isLeader() } returns true
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_SLETTE_INAKTIVE_PERSONOPPLYSNINGSGRUNNLAG) } returns true
        every { slettInaktivePersonopplysningsgrunnlagService.slettBatchMedInaktiveGrunnlag(any(), any()) } returns emptyList()
        every { slettInaktivePersonopplysningsgrunnlagService.tellInaktiveGrunnlagUtenAktivtGrunnlagPåSammeBehandling() } returns 3L

        // Act
        scheduler.slettInaktivePersonopplysningsgrunnlag()

        // Assert
        verify(exactly = 1) { slettInaktivePersonopplysningsgrunnlagService.tellInaktiveGrunnlagUtenAktivtGrunnlagPåSammeBehandling() }
    }
}
