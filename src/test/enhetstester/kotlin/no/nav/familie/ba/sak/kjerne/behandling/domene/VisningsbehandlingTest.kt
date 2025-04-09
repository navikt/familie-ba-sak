package no.nav.familie.ba.sak.kjerne.behandling.domene

import no.nav.familie.ba.sak.datagenerator.lagBehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class VisningsbehandlingTest {
    @Nested
    inner class OpprettFraBehandling {
        @Test
        fun `skal opprette visningsbehandling fra behandling`() {
            // Arrange
            val behandling = lagBehandling()
            val vedtaksdato = LocalDateTime.now()

            // Act
            val visningsbehandling = Visningsbehandling.opprettFraBehandling(behandling, vedtaksdato)

            // Assert
            assertThat(visningsbehandling.behandlingId).isEqualTo(behandling.id)
            assertThat(visningsbehandling.opprettetTidspunkt).isEqualTo(behandling.opprettetTidspunkt)
            assertThat(visningsbehandling.aktivertTidspunkt).isEqualTo(behandling.aktivertTidspunkt)
            assertThat(visningsbehandling.kategori).isEqualTo(behandling.kategori)
            assertThat(visningsbehandling.underkategori).isEqualTo(behandling.underkategori)
            assertThat(visningsbehandling.aktiv).isEqualTo(behandling.aktiv)
            assertThat(visningsbehandling.opprettetÅrsak).isEqualTo(behandling.opprettetÅrsak)
            assertThat(visningsbehandling.type).isEqualTo(behandling.type)
            assertThat(visningsbehandling.status).isEqualTo(behandling.status)
            assertThat(visningsbehandling.resultat).isEqualTo(behandling.resultat)
            assertThat(visningsbehandling.vedtaksdato).isEqualTo(vedtaksdato)
        }
    }
}
