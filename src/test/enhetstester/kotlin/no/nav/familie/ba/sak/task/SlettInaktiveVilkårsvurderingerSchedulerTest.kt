package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.config.LeaderClientService
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.SlettInaktiveVilkårsvurderingerService
import org.junit.jupiter.api.Test

class SlettInaktiveVilkårsvurderingerSchedulerTest {
    private val slettInaktiveVilkårsvurderingerService = mockk<SlettInaktiveVilkårsvurderingerService>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val leaderClientService = mockk<LeaderClientService>()
    private val scheduler = SlettInaktiveVilkårsvurderingerScheduler(slettInaktiveVilkårsvurderingerService, featureToggleService, leaderClientService)

    @Test
    fun `skal ikke slette noe når podden ikke er leader`() {
        // Arrange
        every { leaderClientService.isLeader() } returns false

        // Act
        scheduler.slettInaktiveVilkårsvurderinger()

        // Assert
        verify(exactly = 0) { featureToggleService.isEnabled(any<FeatureToggle>()) }
        verify(exactly = 0) { slettInaktiveVilkårsvurderingerService.slettBatchMedInaktiveVilkårsvurderinger(any(), any()) }
    }

    @Test
    fun `skal ikke slette noe når feature toggle er avslått`() {
        // Arrange
        every { leaderClientService.isLeader() } returns true
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_SLETTE_INAKTIVE_VILKÅRSVURDERINGER) } returns false

        // Act
        scheduler.slettInaktiveVilkårsvurderinger()

        // Assert
        verify(exactly = 0) { slettInaktiveVilkårsvurderingerService.slettBatchMedInaktiveVilkårsvurderinger(any(), any()) }
    }

    @Test
    fun `skal slette inaktive vilkårsvurderinger i batcher fra siste slettede id til det ikke er flere igjen`() {
        // Arrange
        every { leaderClientService.isLeader() } returns true
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_SLETTE_INAKTIVE_VILKÅRSVURDERINGER) } returns true

        val førsteBatch = (1L..500L).toList()
        val andreBatch = (501L..800L).toList()
        every {
            slettInaktiveVilkårsvurderingerService.slettBatchMedInaktiveVilkårsvurderinger(any(), any())
        } returnsMany listOf(førsteBatch, andreBatch, emptyList())

        // Act
        scheduler.slettInaktiveVilkårsvurderinger()

        // Assert
        verify(exactly = 1) { slettInaktiveVilkårsvurderingerService.slettBatchMedInaktiveVilkårsvurderinger(etterId = 0L, batchStørrelse = 500) }
        verify(exactly = 1) { slettInaktiveVilkårsvurderingerService.slettBatchMedInaktiveVilkårsvurderinger(etterId = 500L, batchStørrelse = 500) }
        // Tredje kall (etter siste slettede id 800) returnerer tom liste -> løkka stopper og sletter ikke mer
        verify(exactly = 1) { slettInaktiveVilkårsvurderingerService.slettBatchMedInaktiveVilkårsvurderinger(etterId = 800L, batchStørrelse = 500) }
        verify(exactly = 3) { slettInaktiveVilkårsvurderingerService.slettBatchMedInaktiveVilkårsvurderinger(any(), any()) }
    }

    @Test
    fun `skal ikke slette mer enn maks antall batcher per kjøring`() {
        // Arrange
        every { leaderClientService.isLeader() } returns true
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_SLETTE_INAKTIVE_VILKÅRSVURDERINGER) } returns true
        // Returnerer alltid en full batch -> ville løpt uendelig uten maksgrense
        every { slettInaktiveVilkårsvurderingerService.slettBatchMedInaktiveVilkårsvurderinger(any(), any()) } returns (1L..500L).toList()

        // Act
        scheduler.slettInaktiveVilkårsvurderinger()

        // Assert
        verify(exactly = 100) { slettInaktiveVilkårsvurderingerService.slettBatchMedInaktiveVilkårsvurderinger(any(), any()) }
    }
}
