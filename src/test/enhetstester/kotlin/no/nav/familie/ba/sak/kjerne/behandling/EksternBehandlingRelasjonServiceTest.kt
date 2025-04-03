package no.nav.familie.ba.sak.kjerne.behandling

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagEksternBehandlingRelasjon
import no.nav.familie.ba.sak.kjerne.behandling.domene.EksternBehandlingRelasjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID

class EksternBehandlingRelasjonServiceTest {
    private val eksternBehandlingRelasjonRepository = mockk<EksternBehandlingRelasjonRepository>()
    private val eksternBehandlingRelasjonService =
        EksternBehandlingRelasjonService(
            eksternBehandlingRelasjonRepository = eksternBehandlingRelasjonRepository,
        )

    @Nested
    inner class LagreEksternBehandlingRelasjon {
        @Test
        fun `skal lagre ekstern behandling relasjon`() {
            // Arrange
            val eksternBehandlingRelasjon = lagEksternBehandlingRelasjon()

            every { eksternBehandlingRelasjonRepository.save(any()) } returnsArgument 0

            // Act
            val lagretEksternBehandlingRelasjon = eksternBehandlingRelasjonService.lagreEksternBehandlingRelasjon(eksternBehandlingRelasjon)

            // Assert
            verify { eksternBehandlingRelasjonRepository.save(eksternBehandlingRelasjon) }
            assertThat(lagretEksternBehandlingRelasjon).isEqualTo(eksternBehandlingRelasjon)
        }
    }

    @Nested
    inner class FinnEksternBehandlingRelasjon {
        @Test
        fun `skal finne ekstern behandling relasjon`() {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()
            val behandlingId = 1L

            val eksternBehandlingRelasjon1 =
                lagEksternBehandlingRelasjon(
                    id = 1L,
                    internBehandlingId = behandlingId,
                    eksternBehandlingId = UUID.randomUUID().toString(),
                    eksternBehandlingFagsystem = EksternBehandlingRelasjon.Fagsystem.KLAGE,
                    opprettetTidspunkt = nåtidspunkt.minusSeconds(1),
                )

            val eksternBehandlingRelasjon2 =
                lagEksternBehandlingRelasjon(
                    id = 2L,
                    internBehandlingId = behandlingId,
                    eksternBehandlingId = UUID.randomUUID().toString(),
                    eksternBehandlingFagsystem = EksternBehandlingRelasjon.Fagsystem.TILBAKEKREVING,
                    opprettetTidspunkt = nåtidspunkt,
                )

            every {
                eksternBehandlingRelasjonRepository.findAllByInternBehandlingId(behandlingId)
            } returns
                listOf(
                    eksternBehandlingRelasjon1,
                    eksternBehandlingRelasjon2,
                )

            // Act
            val eksternBehandlingRelasjon =
                eksternBehandlingRelasjonService.finnEksternBehandlingRelasjon(
                    behandlingId = behandlingId,
                    fagsystem = EksternBehandlingRelasjon.Fagsystem.KLAGE,
                )

            // Assert
            assertThat(eksternBehandlingRelasjon).isEqualTo(eksternBehandlingRelasjon1)
        }

        @Test
        fun `skal kaste exception om det finnes mer enn 1 ekstern behandling relasjon for et gitt fagsystem`() {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()
            val behandlingId = 1L

            val eksternBehandlingRelasjon1 =
                lagEksternBehandlingRelasjon(
                    id = 1L,
                    internBehandlingId = behandlingId,
                    eksternBehandlingId = UUID.randomUUID().toString(),
                    eksternBehandlingFagsystem = EksternBehandlingRelasjon.Fagsystem.KLAGE,
                    opprettetTidspunkt = nåtidspunkt.minusSeconds(1),
                )

            val eksternBehandlingRelasjon2 =
                lagEksternBehandlingRelasjon(
                    id = 2L,
                    internBehandlingId = behandlingId,
                    eksternBehandlingId = UUID.randomUUID().toString(),
                    eksternBehandlingFagsystem = EksternBehandlingRelasjon.Fagsystem.KLAGE,
                    opprettetTidspunkt = nåtidspunkt,
                )

            every {
                eksternBehandlingRelasjonRepository.findAllByInternBehandlingId(behandlingId)
            } returns
                listOf(
                    eksternBehandlingRelasjon1,
                    eksternBehandlingRelasjon2,
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    eksternBehandlingRelasjonService.finnEksternBehandlingRelasjon(
                        behandlingId = behandlingId,
                        fagsystem = EksternBehandlingRelasjon.Fagsystem.KLAGE,
                    )
                }
            assertThat(exception.message).isEqualTo("Forventet maks 1 ekstern behandling relasjon av type KLAGE for behandling $behandlingId")
        }
    }
}
