package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.datagenerator.lagVisningsbehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RestVisningBehandlingTest {
    @Nested
    inner class OpprettFraVisningsbehandling {
        @Test
        fun `skal opprette rest visningsbehandling fra domene visningsbehandling`() {
            // Arrange
            val visningsbehandling = lagVisningsbehandling()

            // Act
            val restVisningsbehandling = RestVisningBehandling.opprettFraVisningsbehandling(visningsbehandling)

            // Assert
            assertThat(restVisningsbehandling.behandlingId).isEqualTo(visningsbehandling.behandlingId)
            assertThat(restVisningsbehandling.opprettetTidspunkt).isEqualTo(visningsbehandling.opprettetTidspunkt)
            assertThat(restVisningsbehandling.aktivertTidspunkt).isEqualTo(visningsbehandling.aktivertTidspunkt)
            assertThat(restVisningsbehandling.kategori).isEqualTo(visningsbehandling.kategori)
            assertThat(restVisningsbehandling.underkategori).isEqualTo(visningsbehandling.underkategori.tilDto())
            assertThat(restVisningsbehandling.aktiv).isEqualTo(visningsbehandling.aktiv)
            assertThat(restVisningsbehandling.årsak).isEqualTo(visningsbehandling.opprettetÅrsak)
            assertThat(restVisningsbehandling.type).isEqualTo(visningsbehandling.type)
            assertThat(restVisningsbehandling.status).isEqualTo(visningsbehandling.status)
            assertThat(restVisningsbehandling.resultat).isEqualTo(visningsbehandling.resultat)
            assertThat(restVisningsbehandling.vedtaksdato).isEqualTo(visningsbehandling.vedtaksdato)
        }
    }
}
