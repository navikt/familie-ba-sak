package no.nav.familie.ba.sak.kjerne.vedtak.forenklettilbakekrevingvedtak

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ForenkletTilbakekrevingVedtakServiceTest {
    private val forenkletTilbakekrevingVedtakRepository = mockk<ForenkletTilbakekrevingVedtakRepository>()
    private val loggService = mockk<LoggService>()

    private val forenkletTilbakekrevingVedtakService =
        ForenkletTilbakekrevingVedtakService(
            forenkletTilbakekrevingVedtakRepository,
            loggService,
        )

    @Nested
    inner class FinnForenkletTilbakekrevingVedtakTest {
        @Test
        fun `skal returnere null hvis det ikke finnes forenklet tilbakekreving vedtak for behandling`() {
            // Arrange
            val behandling = lagBehandling(id = 1)

            every { forenkletTilbakekrevingVedtakRepository.finnForenkletTilbakekrevingVedtakForBehandling(behandling.id) } returns null

            // Act
            val forenkletTilbakekrevingVedtak = forenkletTilbakekrevingVedtakService.finnForenkletTilbakekrevingVedtak(behandling.id)

            // Assert
            assertThat(forenkletTilbakekrevingVedtak).isNull()
        }

        @Test
        fun `skal returner forenklet tilbakekreving vedtak hvis det finnes for behandling`() {
            // Arrange
            val behandling = lagBehandling(id = 1)
            val eksisterendeForenkletTilbakekrevingVedtak = ForenkletTilbakekrevingVedtak(behandlingId = behandling.id, samtykke = null, fritekst = "fritekst")

            every { forenkletTilbakekrevingVedtakRepository.finnForenkletTilbakekrevingVedtakForBehandling(behandling.id) } returns eksisterendeForenkletTilbakekrevingVedtak

            // Act
            val forenkletTilbakekrevingVedtak = forenkletTilbakekrevingVedtakService.finnForenkletTilbakekrevingVedtak(behandling.id)

            // Assert
            assertThat(forenkletTilbakekrevingVedtak).isEqualTo(eksisterendeForenkletTilbakekrevingVedtak)
        }
    }

    @Nested
    inner class OpprettForenkletTilbakekrevingVedtakTest {
        @Test
        fun `Skal returnere eksisterende forenklet tilbakekreving vedtak dersom det allerede finnes`() {
            // Arrange
            val behandling = lagBehandling(id = 1)
            val eksisterendeForenkletTilbakekrevingVedtak = ForenkletTilbakekrevingVedtak(behandlingId = behandling.id, samtykke = null, fritekst = "fritekst")

            every { forenkletTilbakekrevingVedtakRepository.finnForenkletTilbakekrevingVedtakForBehandling(behandling.id) } returns eksisterendeForenkletTilbakekrevingVedtak

            // Act
            val forenkletTilbakekrevingVedtak = forenkletTilbakekrevingVedtakService.opprettForenkletTilbakekrevingVedtak(behandling.id)

            // Assert
            assertThat(forenkletTilbakekrevingVedtak).isEqualTo(eksisterendeForenkletTilbakekrevingVedtak)
            verify(exactly = 0) { forenkletTilbakekrevingVedtakRepository.save(any()) }
        }

        @Test
        fun `skal opprette forenklet tilbakekreving vedtak hvis det ikke finnes for behandling`() {
            // Arrange
            val behandling = lagBehandling(id = 1)

            every { forenkletTilbakekrevingVedtakRepository.finnForenkletTilbakekrevingVedtakForBehandling(behandling.id) } returns null
            every { forenkletTilbakekrevingVedtakRepository.save(any()) } returnsArgument (0)
            every { loggService.loggForenkletTilbakekrevingVedtakOpprettet(behandling.id) } returns mockk()

            // Act
            val forenkletTilbakekrevingVedtak = forenkletTilbakekrevingVedtakService.opprettForenkletTilbakekrevingVedtak(behandling.id)

            // Assert
            assertThat(forenkletTilbakekrevingVedtak.behandlingId).isEqualTo(behandling.id)
            verify(exactly = 1) { forenkletTilbakekrevingVedtakRepository.save(forenkletTilbakekrevingVedtak) }
            verify(exactly = 1) { loggService.loggForenkletTilbakekrevingVedtakOpprettet(behandling.id) }
        }
    }

    @Nested
    inner class OppdaterSamtykkePåForenkletTilbakekrevingVedtakTest {
        @Test
        fun `Skal oppdatere samtykke på eksisterende forenklet tilbakekreving vedtak og opprette logg på dette`() {
            // Arrange
            val behandling = lagBehandling(id = 1)
            val eksisterendeForenkletTilbakekrevingVedtak = ForenkletTilbakekrevingVedtak(behandlingId = behandling.id, samtykke = null, fritekst = "fritekst")

            every { forenkletTilbakekrevingVedtakRepository.finnForenkletTilbakekrevingVedtakForBehandling(behandling.id) } returns eksisterendeForenkletTilbakekrevingVedtak
            every { loggService.loggForenkletTilbakekrevingVedtakOppdatertSamtykke(behandling.id) } returns mockk()
            every { forenkletTilbakekrevingVedtakRepository.save(any()) } returnsArgument (0)

            // Act
            val forenkletTilbakekrevingVedtak = forenkletTilbakekrevingVedtakService.oppdaterSamtykkePåForenkletTilbakekrevingVedtak(behandling.id, true)

            // Assert
            assertThat(forenkletTilbakekrevingVedtak.samtykke).isEqualTo(true)
            verify(exactly = 1) { loggService.loggForenkletTilbakekrevingVedtakOppdatertSamtykke(behandling.id) }
            verify(exactly = 1) { forenkletTilbakekrevingVedtakRepository.save(any()) }
        }

        @Test
        fun `Skal kaste funksjonell feil hvis forenklet tilbakekreving vedtak ikke finnes for behandling`() {
            // Arrange
            val behandling = lagBehandling(id = 1)

            every { forenkletTilbakekrevingVedtakRepository.finnForenkletTilbakekrevingVedtakForBehandling(behandling.id) } returns null

            // Act && Assert
            val feilmelding =
                assertThrows<FunksjonellFeil> {
                    forenkletTilbakekrevingVedtakService.oppdaterSamtykkePåForenkletTilbakekrevingVedtak(behandling.id, true)
                }.melding

            assertThat(feilmelding).isEqualTo("Forenklet tilbakekreving vedtak finnes ikke for behandling 1. Oppdater fanen og prøv igjen.")
        }
    }

    @Nested
    inner class OppdaterFritekstPåForenkletTilbakekrevingVedtakTest {
        @Test
        fun `Skal oppdatere fritekst på eksisterende forenklet tilbakekreving vedtak og opprette logg på dette`() {
            // Arrange
            val behandling = lagBehandling(id = 1)
            val eksisterendeForenkletTilbakekrevingVedtak = ForenkletTilbakekrevingVedtak(behandlingId = behandling.id, samtykke = null, fritekst = "")

            every { forenkletTilbakekrevingVedtakRepository.finnForenkletTilbakekrevingVedtakForBehandling(behandling.id) } returns eksisterendeForenkletTilbakekrevingVedtak
            every { loggService.loggForenkletTilbakekrevingVedtakOppdatertFritekst(behandling.id) } returns mockk()
            every { forenkletTilbakekrevingVedtakRepository.save(any()) } returnsArgument (0)

            // Act
            val forenkletTilbakekrevingVedtak = forenkletTilbakekrevingVedtakService.oppdaterFritekstPåForenkletTilbakekrevingVedtak(behandling.id, "oppdatert med fritekst")

            // Assert
            assertThat(forenkletTilbakekrevingVedtak.fritekst).isEqualTo("oppdatert med fritekst")
            verify(exactly = 1) { loggService.loggForenkletTilbakekrevingVedtakOppdatertFritekst(behandling.id) }
            verify(exactly = 1) { forenkletTilbakekrevingVedtakRepository.save(any()) }
        }

        @Test
        fun `Skal kaste funksjonell feil hvis forenklet tilbakekreving vedtak ikke finnes for behandling`() {
            // Arrange
            val behandling = lagBehandling(id = 1)

            every { forenkletTilbakekrevingVedtakRepository.finnForenkletTilbakekrevingVedtakForBehandling(behandling.id) } returns null

            // Act && Assert
            val feilmelding =
                assertThrows<FunksjonellFeil> {
                    forenkletTilbakekrevingVedtakService.oppdaterFritekstPåForenkletTilbakekrevingVedtak(behandling.id, "finnes ikke")
                }.melding

            assertThat(feilmelding).isEqualTo("Forenklet tilbakekreving vedtak finnes ikke for behandling 1. Oppdater fanen og prøv igjen.")
        }
    }

    @Nested
    inner class SlettForenkletTilbakekrevingVedtakTest {
        @Test
        fun `Skal slette forenklet tilbakekreving vedtak hvis det finnes`() {
            // Arrange
            val behandling = lagBehandling(id = 1)
            val eksisterendeForenkletTilbakekrevingVedtak = ForenkletTilbakekrevingVedtak(behandlingId = behandling.id, samtykke = null, fritekst = "")

            every { forenkletTilbakekrevingVedtakRepository.finnForenkletTilbakekrevingVedtakForBehandling(behandling.id) } returns eksisterendeForenkletTilbakekrevingVedtak
            every { loggService.loggForenkletTilbakekrevingVedtakSlettet(behandling.id) } returns mockk()
            every { forenkletTilbakekrevingVedtakRepository.delete(eksisterendeForenkletTilbakekrevingVedtak) } just runs

            // Act
            forenkletTilbakekrevingVedtakService.slettForenkletTilbakekrevingVedtak(behandling.id)

            // Assert
            verify(exactly = 1) { loggService.loggForenkletTilbakekrevingVedtakSlettet(behandling.id) }
            verify(exactly = 1) { forenkletTilbakekrevingVedtakRepository.delete(eksisterendeForenkletTilbakekrevingVedtak) }
        }

        @Test
        fun `Dersom det ikke eksisterer noe forenklet tilbakekreving vedtak så skal det ikke gjøres noe`() {
            // Arrange
            val behandling = lagBehandling(id = 1)

            every { forenkletTilbakekrevingVedtakRepository.finnForenkletTilbakekrevingVedtakForBehandling(behandling.id) } returns null

            // Act
            forenkletTilbakekrevingVedtakService.slettForenkletTilbakekrevingVedtak(behandling.id)

            // Assert
            verify(exactly = 0) { loggService.loggForenkletTilbakekrevingVedtakSlettet(any()) }
            verify(exactly = 0) { forenkletTilbakekrevingVedtakRepository.delete(any()) }
        }
    }
}
