package no.nav.familie.ba.sak.statistikk.saksstatistikk

import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagEksternBehandlingRelasjon
import no.nav.familie.ba.sak.kjerne.behandling.domene.EksternBehandlingRelasjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class RelatertBehandlingTest {
    @Nested
    inner class FraBarnetrygdbehandling {
        @Test
        fun `skal opprette fra barnetrygdbehandling`() {
            // Arrange
            val behandling = lagBehandling()

            // Act
            val relatertBehandling = RelatertBehandling.fraBarnetrygdbehandling(behandling)

            // Assert
            assertThat(relatertBehandling.id).isEqualTo(behandling.id.toString())
            assertThat(relatertBehandling.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.BA)
        }
    }

    @Nested
    inner class FraEksternBehandlingRelasjon {
        @ParameterizedTest
        @EnumSource(value = EksternBehandlingRelasjon.Fagsystem::class)
        fun `skal opprette fra ekstern behandling relasjon`(
            fagsystem: EksternBehandlingRelasjon.Fagsystem,
        ) {
            // Arrange
            val eksternBehandlingRelasjon =
                lagEksternBehandlingRelasjon(
                    eksternBehandlingFagsystem = fagsystem,
                )

            // Act
            val relatertBehandling = RelatertBehandling.fraEksternBehandlingRelasjon(eksternBehandlingRelasjon)

            // Assert
            assertThat(relatertBehandling.id).isEqualTo(eksternBehandlingRelasjon.eksternBehandlingId)
            assertThat(relatertBehandling.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.valueOf(fagsystem.name))
        }
    }
}
