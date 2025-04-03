package no.nav.familie.ba.sak.kjerne.behandling.domene

import no.nav.familie.ba.sak.datagenerator.lagNyEksternBehandlingRelasjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class EksternBehandlingRelasjonTest {
    @Nested
    inner class OpprettFraNyEksternBehandlingRelasjon {
        @Test
        fun `skal opprette fra ny ekstern behandling relasjon`() {
            // Arrange
            val internBehandlingId = 0L

            val nyEksternBehandlingRelasjon =
                lagNyEksternBehandlingRelasjon(
                    eksternBehandlingId = UUID.randomUUID().toString(),
                    eksternBehandlingFagsystem = EksternBehandlingRelasjon.Fagsystem.KLAGE,
                )

            // Act
            val eksternBehandlingRelasjon =
                EksternBehandlingRelasjon.opprettFraNyEksternBehandlingRelasjon(
                    internBehandlingId = internBehandlingId,
                    nyEksternBehandlingRelasjon = nyEksternBehandlingRelasjon,
                )

            // Assert
            assertThat(eksternBehandlingRelasjon.id).isEqualTo(0L)
            assertThat(eksternBehandlingRelasjon.internBehandlingId).isEqualTo(internBehandlingId)
            assertThat(eksternBehandlingRelasjon.eksternBehandlingId).isEqualTo(nyEksternBehandlingRelasjon.eksternBehandlingId)
            assertThat(eksternBehandlingRelasjon.eksternBehandlingFagsystem).isEqualTo(nyEksternBehandlingRelasjon.eksternBehandlingFagsystem)
            assertThat(eksternBehandlingRelasjon.opprettetTidspunkt).isNotNull()
        }
    }
}
