package no.nav.familie.ba.sak.kjerne.korrigertetterbetaling

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.hamcrest.CoreMatchers.`is` as Is

internal class KorrigertEtterbetalingServiceTest {
    private val korrigertEtterbetalingRepository = mockk<KorrigertEtterbetalingRepository>()
    private val loggService = mockk<LoggService>()
    private val korrigertEtterbetalingService = KorrigertEtterbetalingService(korrigertEtterbetalingRepository, loggService)

    @Test
    fun `finnAktivtKorrigeringPåBehandling skal hente aktivt korrigering fra repository hvis det finnes`() {
        // Arrange
        val behandling = lagBehandling()
        val korrigertEtterbetaling = lagKorrigertEtterbetaling(behandling)

        every { korrigertEtterbetalingRepository.finnAktivtKorrigeringPåBehandling(behandling.id) } returns korrigertEtterbetaling

        // Act
        val hentetKorrigertEtterbetaling =
            korrigertEtterbetalingService.finnAktivtKorrigeringPåBehandling(behandling.id)
                ?: fail("etterbetaling korrigering ikke hentet riktig")

        // Assert
        assertThat(hentetKorrigertEtterbetaling.behandling.id, Is(behandling.id))
        assertThat(hentetKorrigertEtterbetaling.aktiv, Is(true))

        verify(exactly = 1) { korrigertEtterbetalingRepository.finnAktivtKorrigeringPåBehandling(behandling.id) }
    }

    @Test
    fun `finnAlleKorrigeringerPåBehandling skal hente alle korrigering fra repository hvis de finnes`() {
        // Arrange
        val behandling = lagBehandling()
        val korrigertEtterbetaling = lagKorrigertEtterbetaling(behandling)

        every { korrigertEtterbetalingRepository.finnAlleKorrigeringerPåBehandling(behandling.id) } returns
            listOf(
                korrigertEtterbetaling,
                korrigertEtterbetaling,
            )

        // Act
        val hentetKorrigertEtterbetaling =
            korrigertEtterbetalingService.finnAlleKorrigeringerPåBehandling(behandling.id)

        // Assert
        assertThat(hentetKorrigertEtterbetaling.size, Is(2))

        verify(exactly = 1) { korrigertEtterbetalingRepository.finnAlleKorrigeringerPåBehandling(behandling.id) }
    }

    @Test
    fun `lagreKorrigertEtterbetaling skal lagre korrigering på behandling og logg på dette`() {
        // Arrange
        val behandling = lagBehandling()
        val korrigertEtterbetaling = lagKorrigertEtterbetaling(behandling)

        every { korrigertEtterbetalingRepository.finnAktivtKorrigeringPåBehandling(behandling.id) } returns null
        every { korrigertEtterbetalingRepository.save(korrigertEtterbetaling) } returns korrigertEtterbetaling
        every { loggService.opprettKorrigertEtterbetalingLogg(behandling, any()) } returns Unit

        // Act
        val lagretKorrigertEtterbetaling =
            korrigertEtterbetalingService.lagreKorrigertEtterbetaling(korrigertEtterbetaling)

        // Assert
        assertThat(lagretKorrigertEtterbetaling.behandling.id, Is(behandling.id))

        verify(exactly = 1) { korrigertEtterbetalingRepository.finnAktivtKorrigeringPåBehandling(behandling.id) }
        verify(exactly = 1) { korrigertEtterbetalingRepository.save(korrigertEtterbetaling) }
        verify(exactly = 1) {
            loggService.opprettKorrigertEtterbetalingLogg(
                behandling,
                korrigertEtterbetaling,
            )
        }
    }

    @Test
    fun `lagreKorrigertEtterbetaling skal sette og lagre forrige korrigering til inaktivt hvis det finnes tidligere korrigering`() {
        // Arrange
        val behandling = lagBehandling()
        val forrigeKorrigering = mockk<KorrigertEtterbetaling>(relaxed = true)
        val korrigertEtterbetaling = lagKorrigertEtterbetaling(behandling)

        every { korrigertEtterbetalingRepository.finnAktivtKorrigeringPåBehandling(any()) } returns forrigeKorrigering
        every { korrigertEtterbetalingRepository.saveAndFlush(forrigeKorrigering) } returns korrigertEtterbetaling
        every { korrigertEtterbetalingRepository.save(korrigertEtterbetaling) } returns korrigertEtterbetaling
        every { loggService.opprettKorrigertEtterbetalingLogg(any(), any()) } returns Unit

        // Act
        korrigertEtterbetalingService.lagreKorrigertEtterbetaling(korrigertEtterbetaling)

        // Assert
        verify(exactly = 1) { korrigertEtterbetalingRepository.finnAktivtKorrigeringPåBehandling(any()) }
        verify(exactly = 1) { forrigeKorrigering setProperty "aktiv" value false }
        verify(exactly = 1) { korrigertEtterbetalingRepository.saveAndFlush(forrigeKorrigering) }
        verify(exactly = 1) { korrigertEtterbetalingRepository.save(korrigertEtterbetaling) }
    }

    @Test
    fun `settKorrigeringPåBehandlingTilInaktiv skal sette korrigering til inaktivt hvis det finnes`() {
        // Arrange
        val behandling = lagBehandling()
        val korrigertEtterbetaling = mockk<KorrigertEtterbetaling>(relaxed = true)

        every { korrigertEtterbetalingRepository.finnAktivtKorrigeringPåBehandling(any()) } returns korrigertEtterbetaling
        every { loggService.opprettKorrigertEtterbetalingLogg(any(), any()) } returns Unit

        // Act
        korrigertEtterbetalingService.settKorrigeringPåBehandlingTilInaktiv(behandling)

        // Assert
        verify(exactly = 1) { korrigertEtterbetaling setProperty "aktiv" value false }
        verify(exactly = 1) {
            loggService.opprettKorrigertEtterbetalingLogg(
                any(),
                korrigertEtterbetaling,
            )
        }
    }
}

fun lagKorrigertEtterbetaling(
    behandling: Behandling,
    årsak: KorrigertEtterbetalingÅrsak = KorrigertEtterbetalingÅrsak.FEIL_TIDLIGERE_UTBETALT_BELØP,
    begrunnelse: String? = null,
    beløp: Int = 2000,
    aktiv: Boolean = true,
) = KorrigertEtterbetaling(
    behandling = behandling,
    årsak = årsak,
    begrunnelse = begrunnelse,
    aktiv = aktiv,
    beløp = beløp,
)
