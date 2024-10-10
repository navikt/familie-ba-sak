package no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.oppgave.lagArbeidsfordelingPåBehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class ArbeidsfordelingPåBehandlingRepositoryTest(
    @Autowired private val arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository,
) : AbstractSpringIntegrationTest() {
    @Nested
    inner class HentArbeidsfordelingPåBehandlingTest {
        @Test
        fun `skal hente arbeidsfordeling på behandling`() {
            // Arrange
            val behandlingId = 1L

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                )

            arbeidsfordelingPåBehandlingRepository.save(arbeidsfordelingPåBehandling)

            // Act
            val lagretArbeidsfordelingPåBehandling =
                arbeidsfordelingPåBehandlingRepository.hentArbeidsfordelingPåBehandling(behandlingId)

            // Assert
            assertThat(lagretArbeidsfordelingPåBehandling).isEqualTo(arbeidsfordelingPåBehandling)
        }

        @Test
        fun `skal kaste feil hvis arbeidsfordeling på behandling returnerer null`() {
            // Act & assert
            val exception =
                assertThrows<Feil> {
                    arbeidsfordelingPåBehandlingRepository.hentArbeidsfordelingPåBehandling(1L)
                }
            assertThat(exception.message).isEqualTo("Finner ikke tilknyttet arbeidsfordelingsenhet på behandling 1")
        }
    }
}
