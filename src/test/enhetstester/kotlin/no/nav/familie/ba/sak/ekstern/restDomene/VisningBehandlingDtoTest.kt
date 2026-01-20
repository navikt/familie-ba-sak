package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.datagenerator.lagVisningsbehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class VisningBehandlingDtoTest {
    @Nested
    inner class OpprettFraVisningsbehandling {
        @Test
        fun `skal opprette rest visningsbehandling fra domene visningsbehandling`() {
            // Arrange
            val visningsbehandling = lagVisningsbehandling()

            // Act
            val visningsbehandlingDto = VisningBehandlingDto.opprettFraVisningsbehandling(visningsbehandling)

            // Assert
            assertThat(visningsbehandlingDto.behandlingId).isEqualTo(visningsbehandling.behandlingId)
            assertThat(visningsbehandlingDto.opprettetTidspunkt).isEqualTo(visningsbehandling.opprettetTidspunkt)
            assertThat(visningsbehandlingDto.aktivertTidspunkt).isEqualTo(visningsbehandling.aktivertTidspunkt)
            assertThat(visningsbehandlingDto.kategori).isEqualTo(visningsbehandling.kategori)
            assertThat(visningsbehandlingDto.underkategori).isEqualTo(visningsbehandling.underkategori.tilDto())
            assertThat(visningsbehandlingDto.aktiv).isEqualTo(visningsbehandling.aktiv)
            assertThat(visningsbehandlingDto.årsak).isEqualTo(visningsbehandling.opprettetÅrsak)
            assertThat(visningsbehandlingDto.type).isEqualTo(visningsbehandling.type)
            assertThat(visningsbehandlingDto.status).isEqualTo(visningsbehandling.status)
            assertThat(visningsbehandlingDto.resultat).isEqualTo(visningsbehandling.resultat)
            assertThat(visningsbehandlingDto.vedtaksdato).isEqualTo(visningsbehandling.vedtaksdato)
        }
    }
}
