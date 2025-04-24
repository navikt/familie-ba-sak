package no.nav.familie.ba.sak.kjerne.behandling

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.datagenerator.lagEksternBehandlingRelasjon
import no.nav.familie.ba.sak.kjerne.behandling.domene.EksternBehandlingRelasjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
            val lagretEksternBehandlingRelasjon =
                eksternBehandlingRelasjonService.lagreEksternBehandlingRelasjon(
                    eksternBehandlingRelasjon,
                )

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

            val lagretEksternBehandlingRelasjon =
                lagEksternBehandlingRelasjon(
                    id = 1L,
                    internBehandlingId = behandlingId,
                    eksternBehandlingId = UUID.randomUUID().toString(),
                    eksternBehandlingFagsystem = EksternBehandlingRelasjon.Fagsystem.KLAGE,
                    opprettetTidspunkt = nåtidspunkt.minusSeconds(1),
                )

            every {
                eksternBehandlingRelasjonRepository.findByInternBehandlingIdOgFagsystem(
                    internBehandlingId = behandlingId,
                    fagsystem = EksternBehandlingRelasjon.Fagsystem.KLAGE,
                )
            } returns lagretEksternBehandlingRelasjon

            // Act
            val eksternBehandlingRelasjon =
                eksternBehandlingRelasjonService.finnEksternBehandlingRelasjon(
                    behandlingId = behandlingId,
                    fagsystem = EksternBehandlingRelasjon.Fagsystem.KLAGE,
                )

            // Assert
            assertThat(eksternBehandlingRelasjon).isEqualTo(lagretEksternBehandlingRelasjon)
        }

        @Test
        fun `skal ikke finne ekstern behandling relasjon da den ikke finnes`() {
            // Arrange
            val behandlingId = 1L

            every {
                eksternBehandlingRelasjonRepository.findByInternBehandlingIdOgFagsystem(
                    internBehandlingId = behandlingId,
                    fagsystem = EksternBehandlingRelasjon.Fagsystem.KLAGE,
                )
            } returns null

            // Act
            val eksternBehandlingRelasjon =
                eksternBehandlingRelasjonService.finnEksternBehandlingRelasjon(
                    behandlingId = behandlingId,
                    fagsystem = EksternBehandlingRelasjon.Fagsystem.KLAGE,
                )

            // Assert
            assertThat(eksternBehandlingRelasjon).isNull()
        }
    }
}
