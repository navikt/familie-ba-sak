package no.nav.familie.ba.sak.kjerne.etterbetalingkorrigering

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(MockKExtension::class)
internal class EtterbetalingKorrigeringServiceTest {

    @MockK
    private lateinit var etterbetalingKorrigeringRepository: EtterbetalingKorrigeringRepository

    @MockK
    private lateinit var loggService: LoggService

    @InjectMockKs
    private lateinit var etterbetalingKorrigeringService: EtterbetalingKorrigeringService

    @Test
    fun `finnAktivtKorrigeringPåBehandling skal hente aktivt korrigering fra repository hvis det finnes`() {

        val behandling = lagBehandling()
        val etterbetalingKorrigering = lagEtterbetalingKorrigering(behandling)

        every { etterbetalingKorrigeringRepository.finnAktivtKorrigeringPåBehandling(behandling.id) } returns etterbetalingKorrigering

        val hentetEtterbetalingKorrigering =
            etterbetalingKorrigeringService.finnAktivtKorrigeringPåBehandling(behandling.id)
                ?: fail("etterbetaling korrigering ikke hentet riktig")

        assertThat(hentetEtterbetalingKorrigering.behandling.id, Is(behandling.id))
        assertThat(hentetEtterbetalingKorrigering.aktiv, Is(true))

        verify(exactly = 1) { etterbetalingKorrigeringRepository.finnAktivtKorrigeringPåBehandling(behandling.id) }
    }

    @Test
    fun `finnAlleKorrigeringerPåBehandling skal hente alle korrigering fra repository hvis de finnes`() {

        val behandling = lagBehandling()
        val etterbetalingKorrigering = lagEtterbetalingKorrigering(behandling)

        every { etterbetalingKorrigeringRepository.finnAlleKorrigeringerPåBehandling(behandling.id) } returns listOf(
            etterbetalingKorrigering, etterbetalingKorrigering
        )

        val hentetEtterbetalingKorrigering =
            etterbetalingKorrigeringService.finnAlleKorrigeringerPåBehandling(behandling.id)

        assertThat(hentetEtterbetalingKorrigering.size, Is(2))

        verify(exactly = 1) { etterbetalingKorrigeringRepository.finnAlleKorrigeringerPåBehandling(behandling.id) }
    }

    @Test
    fun `lagreEtterbetalingKorrigering skal lagre korrigering på behandling og logg på dette`() {

        val behandling = lagBehandling()
        val etterbetalingKorrigering = lagEtterbetalingKorrigering(behandling)

        every { etterbetalingKorrigeringRepository.finnAktivtKorrigeringPåBehandling(behandling.id) } returns null
        every { etterbetalingKorrigeringRepository.save(etterbetalingKorrigering) } returns etterbetalingKorrigering
        every { loggService.opprettEtterbetalingKorrigeringLogg(behandling, any()) } returns Unit

        val lagretEtterbetalingKorrigering =
            etterbetalingKorrigeringService.lagreEtterbetalingKorrigering(etterbetalingKorrigering)

        assertThat(lagretEtterbetalingKorrigering.behandling.id, Is(behandling.id))

        verify(exactly = 1) { etterbetalingKorrigeringRepository.finnAktivtKorrigeringPåBehandling(behandling.id) }
        verify(exactly = 1) { etterbetalingKorrigeringRepository.save(etterbetalingKorrigering) }
        verify(exactly = 1) {
            loggService.opprettEtterbetalingKorrigeringLogg(
                behandling,
                etterbetalingKorrigering
            )
        }
    }

    @Test
    fun `lagreEtterbetalingKorrigering skal sette og lagre forrige korrigering til inaktivt hvis det finnes tidligere korrigering`() {
        val behandling = lagBehandling()
        val forrigeKorrigering = mockk<EtterbetalingKorrigering>(relaxed = true)
        val etterbetalingKorrigering = lagEtterbetalingKorrigering(behandling)

        every { etterbetalingKorrigeringRepository.finnAktivtKorrigeringPåBehandling(any()) } returns forrigeKorrigering
        every { etterbetalingKorrigeringRepository.saveAndFlush(forrigeKorrigering) } returns etterbetalingKorrigering
        every { etterbetalingKorrigeringRepository.save(etterbetalingKorrigering) } returns etterbetalingKorrigering
        every { loggService.opprettEtterbetalingKorrigeringLogg(any(), any()) } returns Unit

        etterbetalingKorrigeringService.lagreEtterbetalingKorrigering(etterbetalingKorrigering)

        verify(exactly = 1) { etterbetalingKorrigeringRepository.finnAktivtKorrigeringPåBehandling(any()) }
        verify(exactly = 1) { forrigeKorrigering setProperty "aktiv" value false }
        verify(exactly = 1) { etterbetalingKorrigeringRepository.saveAndFlush(forrigeKorrigering) }
        verify(exactly = 1) { etterbetalingKorrigeringRepository.save(etterbetalingKorrigering) }
    }

    @Test
    fun `settKorrigeringPåBehandlingTilInaktiv skal sette korrigering til inaktivt hvis det finnes`() {
        val behandling = lagBehandling()
        val etterbetalingKorrigering = mockk<EtterbetalingKorrigering>(relaxed = true)

        every { etterbetalingKorrigeringRepository.finnAktivtKorrigeringPåBehandling(any()) } returns etterbetalingKorrigering
        every { loggService.opprettEtterbetalingKorrigeringLogg(any(), any()) } returns Unit

        etterbetalingKorrigeringService.settKorrigeringPåBehandlingTilInaktiv(behandling)

        verify(exactly = 1) { etterbetalingKorrigering setProperty "aktiv" value false }
        verify(exactly = 1) {
            loggService.opprettEtterbetalingKorrigeringLogg(
                any(),
                etterbetalingKorrigering
            )
        }
    }
}

fun lagEtterbetalingKorrigering(
    behandling: Behandling,
    årsak: EtterbetalingKorrigeringÅrsak = EtterbetalingKorrigeringÅrsak.FEIL_TIDLIGERE_UTBETALT_BELØP,
    begrunnelse: String? = null,
    beløp: Int = 2000,
    aktiv: Boolean = true
) =
    EtterbetalingKorrigering(
        behandling = behandling,
        årsak = årsak,
        begrunnelse = begrunnelse,
        aktiv = aktiv,
        beløp = beløp
    )
