package no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.domene.SatsendringEøsKjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.domene.SatsendringEøsKjøringRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth

class SatsendringEøsKjøringServiceTest {
    private val satsendringEøsKjøringRepository: SatsendringEøsKjøringRepository = mockk()
    private val satsendringEøsKjøringService = SatsendringEøsKjøringService(satsendringEøsKjøringRepository)

    @Test
    fun `hentSatsendringEøsKjøring returnerer kjøring når den finnes`() {
        val kjøring =
            SatsendringEøsKjøring(
                id = 1L,
                fagsakId = 10L,
                behandlingId = 42L,
                utbetalingsland = "SE",
                satsTidspunkt = YearMonth.of(2025, 9),
            )
        every { satsendringEøsKjøringRepository.findByBehandlingId(42L) } returns kjøring

        val resultat = satsendringEøsKjøringService.hentSatsendringEøsKjøring(42L)

        assertThat(resultat).isEqualTo(kjøring)
    }

    @Test
    fun `hentSatsendringEøsKjøring kaster Feil når ingen kjøring finnes for behandlingId`() {
        every { satsendringEøsKjøringRepository.findByBehandlingId(99L) } returns null

        val feil =
            assertThrows<Feil> {
                satsendringEøsKjøringService.hentSatsendringEøsKjøring(99L)
            }

        assertThat(feil.message).contains("99")
    }
}
