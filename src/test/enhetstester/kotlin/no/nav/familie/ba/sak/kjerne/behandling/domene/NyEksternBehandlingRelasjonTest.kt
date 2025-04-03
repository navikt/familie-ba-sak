package no.nav.familie.ba.sak.kjerne.behandling.domene

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class NyEksternBehandlingRelasjonTest {
    @Nested
    inner class OpprettForKlagebehandling {
        @Test
        fun `skal opprette ekstern behandling relasjon for klagebehandling`() {
            // Arrange
            val klagebehandlingId = UUID.randomUUID()

            // Act
            val nyEksternBehandlingRelasjon = NyEksternBehandlingRelasjon.opprettForKlagebehandling(klagebehandlingId)

            // Assert
            assertThat(nyEksternBehandlingRelasjon.eksternBehandlingId).isEqualTo(klagebehandlingId.toString())
            assertThat(nyEksternBehandlingRelasjon.eksternBehandlingFagsystem).isEqualTo(EksternBehandlingRelasjon.Fagsystem.KLAGE)
        }
    }
}
