package no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.domene.SatsendringEøsKjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.domene.SatsendringEøsKjøringRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.YearMonth

class SatsendringEøsKjøringServiceTest {
    private val satsendringEøsKjøringRepository: SatsendringEøsKjøringRepository = mockk()
    private val satsendringEøsKjøringService = SatsendringEøsKjøringService(satsendringEøsKjøringRepository)

    private val fagsakId = 10L
    private val utbetalingsland = "SE"
    private val satsTidspunkt = YearMonth.of(2025, 9)

    @Test
    fun `hentSatsendringEøsKjøring med behandlingId returnerer kjøring når den finnes`() {
        // Arrange
        val kjøring =
            SatsendringEøsKjøring(
                id = 1L,
                fagsakId = 10L,
                behandlingId = 42L,
                utbetalingsland = "SE",
                satsTidspunkt = YearMonth.of(2025, 9),
            )
        every { satsendringEøsKjøringRepository.findByBehandlingId(42L) } returns kjøring

        // Act
        val resultat = satsendringEøsKjøringService.hentSatsendringEøsKjøring(42L)

        // Assert
        assertThat(resultat).isEqualTo(kjøring)
    }

    @Test
    fun `hentSatsendringEøsKjøring kaster Feil når ingen kjøring finnes for behandlingId`() {
        // Arrange
        every { satsendringEøsKjøringRepository.findByBehandlingId(99L) } returns null

        // Act & Assert
        assertThatThrownBy { satsendringEøsKjøringService.hentSatsendringEøsKjøring(99L) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("99")
    }

    @Test
    fun `hentSatsendringEøsKjøring med fagsakId, land og tidspunkt returnerer kjøring når den finnes`() {
        // Arrange
        val kjøring = SatsendringEøsKjøring(fagsakId = fagsakId, utbetalingsland = utbetalingsland, satsTidspunkt = satsTidspunkt)
        every {
            satsendringEøsKjøringRepository.findByFagsakIdAndUtbetalingslandAndSatsTidspunkt(fagsakId, utbetalingsland, satsTidspunkt)
        } returns kjøring

        // Act
        val resultat = satsendringEøsKjøringService.hentSatsendringEøsKjøring(fagsakId, utbetalingsland, satsTidspunkt)

        // Assert
        assertThat(resultat).isEqualTo(kjøring)
    }

    @Test
    fun `hentSatsendringEøsKjøring med fagsakId, land og tidspunkt kaster Feil når ingen kjøring finnes`() {
        // Arrange
        every {
            satsendringEøsKjøringRepository.findByFagsakIdAndUtbetalingslandAndSatsTidspunkt(fagsakId, utbetalingsland, satsTidspunkt)
        } returns null

        // Act & Assert
        assertThatThrownBy { satsendringEøsKjøringService.hentSatsendringEøsKjøring(fagsakId, utbetalingsland, satsTidspunkt) }
            .isInstanceOf(Feil::class.java)
    }

    @Test
    fun `settBehandlingId setter behandlingId på kjøringen og lagrer den`() {
        // Arrange
        val kjøring = SatsendringEøsKjøring(fagsakId = fagsakId, utbetalingsland = utbetalingsland, satsTidspunkt = satsTidspunkt)
        every {
            satsendringEøsKjøringRepository.findByFagsakIdAndUtbetalingslandAndSatsTidspunkt(fagsakId, utbetalingsland, satsTidspunkt)
        } returns kjøring
        val kjøringSlot = slot<SatsendringEøsKjøring>()
        every { satsendringEøsKjøringRepository.save(capture(kjøringSlot)) } answers { firstArg() }

        // Act
        satsendringEøsKjøringService.settBehandlingId(fagsakId, utbetalingsland, satsTidspunkt, 123L)

        // Assert
        assertThat(kjøringSlot.captured.behandlingId).isEqualTo(123L)
    }

    @Test
    fun `settFerdigTidspunkt setter ferdigTidspunkt på kjøringen og lagrer den`() {
        // Arrange
        val kjøring = SatsendringEøsKjøring(fagsakId = fagsakId, utbetalingsland = utbetalingsland, satsTidspunkt = satsTidspunkt)
        every {
            satsendringEøsKjøringRepository.findByFagsakIdAndUtbetalingslandAndSatsTidspunkt(fagsakId, utbetalingsland, satsTidspunkt)
        } returns kjøring
        val kjøringSlot = slot<SatsendringEøsKjøring>()
        every { satsendringEøsKjøringRepository.save(capture(kjøringSlot)) } answers { firstArg() }

        // Act
        satsendringEøsKjøringService.settFerdigTidspunkt(fagsakId, utbetalingsland, satsTidspunkt)

        // Assert
        assertThat(kjøringSlot.captured.ferdigTidspunkt).isNotNull()
    }

    @Test
    fun `settFeiltype setter feiltype på kjøringen og lagrer den`() {
        // Arrange
        val kjøring = SatsendringEøsKjøring(fagsakId = fagsakId, utbetalingsland = utbetalingsland, satsTidspunkt = satsTidspunkt)
        every {
            satsendringEøsKjøringRepository.findByFagsakIdAndUtbetalingslandAndSatsTidspunkt(fagsakId, utbetalingsland, satsTidspunkt)
        } returns kjøring
        val kjøringSlot = slot<SatsendringEøsKjøring>()
        every { satsendringEøsKjøringRepository.save(capture(kjøringSlot)) } answers { firstArg() }

        // Act
        satsendringEøsKjøringService.settFeiltype(fagsakId, utbetalingsland, satsTidspunkt, "Må behandles manuelt")

        // Assert
        assertThat(kjøringSlot.captured.feiltype).isEqualTo("Må behandles manuelt")
        verify(exactly = 1) { satsendringEøsKjøringRepository.save(any()) }
    }
}
