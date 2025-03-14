package no.nav.familie.ba.sak.kjerne.vedtak.forenklettilbakekrevingsvedtak

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.brev.DokumentGenereringService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ForenkletTilbakekrevingsvedtakServiceTest {
    private val forenkletTilbakekrevingsvedtakRepository = mockk<ForenkletTilbakekrevingsvedtakRepository>()
    private val behandlingService = mockk<BehandlingHentOgPersisterService>()
    private val dokumentGenereringService = mockk<DokumentGenereringService>()
    private val loggService = mockk<LoggService>()

    private val forenkletTilbakekrevingsvedtakService =
        ForenkletTilbakekrevingsvedtakService(
            forenkletTilbakekrevingsvedtakRepository,
            loggService,
            dokumentGenereringService,
            behandlingService,
        )

    @Nested
    inner class FinnForenkletTilbakekrevingsvedtakTest {
        @Test
        fun `skal returnere null hvis det ikke finnes forenklet tilbakekrevingsvedtak for behandling`() {
            // Arrange
            val behandling = lagBehandling(id = 1)

            every { forenkletTilbakekrevingsvedtakRepository.finnForenkletTilbakekrevingsvedtakForBehandling(behandling.id) } returns null

            // Act
            val forenkletTilbakekrevingsvedtak = forenkletTilbakekrevingsvedtakService.finnForenkletTilbakekrevingsvedtak(behandling.id)

            // Assert
            assertThat(forenkletTilbakekrevingsvedtak).isNull()
        }

        @Test
        fun `skal returner forenklet tilbakekrevingsvedtak hvis det finnes for behandling`() {
            // Arrange
            val behandling = lagBehandling(id = 1)
            val eksisterendeForenkletTilbakekrevingsvedtak = ForenkletTilbakekrevingsvedtak(behandling = behandling, samtykke = false, fritekst = "fritekst")

            every { forenkletTilbakekrevingsvedtakRepository.finnForenkletTilbakekrevingsvedtakForBehandling(behandling.id) } returns eksisterendeForenkletTilbakekrevingsvedtak

            // Act
            val forenkletTilbakekrevingsvedtak = forenkletTilbakekrevingsvedtakService.finnForenkletTilbakekrevingsvedtak(behandling.id)

            // Assert
            assertThat(forenkletTilbakekrevingsvedtak).isEqualTo(eksisterendeForenkletTilbakekrevingsvedtak)
        }
    }

    @Nested
    inner class OpprettForenkletTilbakekrevingsvedtakTest {
        @Test
        fun `Skal returnere eksisterende forenklet tilbakekrevingsvedtak dersom det allerede finnes`() {
            // Arrange
            val behandling = lagBehandling(id = 1)
            val eksisterendeForenkletTilbakekrevingsvedtak = ForenkletTilbakekrevingsvedtak(behandling = behandling, samtykke = false, fritekst = "fritekst")

            every { forenkletTilbakekrevingsvedtakRepository.finnForenkletTilbakekrevingsvedtakForBehandling(behandling.id) } returns eksisterendeForenkletTilbakekrevingsvedtak

            // Act
            val forenkletTilbakekrevingsvedtak = forenkletTilbakekrevingsvedtakService.opprettForenkletTilbakekrevingsvedtak(behandling.id)

            // Assert
            assertThat(forenkletTilbakekrevingsvedtak).isEqualTo(eksisterendeForenkletTilbakekrevingsvedtak)
            verify(exactly = 0) { forenkletTilbakekrevingsvedtakRepository.save(any()) }
        }

        @Test
        fun `skal opprette forenklet tilbakekrevingsvedtak hvis det ikke finnes for behandling`() {
            // Arrange
            val behandling = lagBehandling(id = 1)

            every { forenkletTilbakekrevingsvedtakRepository.finnForenkletTilbakekrevingsvedtakForBehandling(behandling.id) } returns null
            every { forenkletTilbakekrevingsvedtakRepository.save(any()) } returnsArgument (0)
            every { loggService.loggForenkletTilbakekrevingsvedtakOpprettet(behandling.id) } returns mockk()
            every { behandlingService.hent(behandlingId = behandling.id) } returns behandling

            // Act
            val forenkletTilbakekrevingsvedtak = forenkletTilbakekrevingsvedtakService.opprettForenkletTilbakekrevingsvedtak(behandling.id)

            // Assert
            assertThat(forenkletTilbakekrevingsvedtak.behandling.id).isEqualTo(behandling.id)
            assertThat(forenkletTilbakekrevingsvedtak.fritekst).isEqualTo("TEKST")
            assertThat(forenkletTilbakekrevingsvedtak.samtykke).isFalse()

            verify(exactly = 1) { forenkletTilbakekrevingsvedtakRepository.save(forenkletTilbakekrevingsvedtak) }
            verify(exactly = 1) { loggService.loggForenkletTilbakekrevingsvedtakOpprettet(behandling.id) }
        }
    }

    @Nested
    inner class OppdaterSamtykkePåForenkletTilbakekrevingsvedtakTest {
        @Test
        fun `Skal oppdatere samtykke på eksisterende forenklet tilbakekrevingsvedtak og opprette logg på dette`() {
            // Arrange
            val behandling = lagBehandling(id = 1)
            val eksisterendeForenkletTilbakekrevingsvedtak = ForenkletTilbakekrevingsvedtak(behandling = behandling, samtykke = false, fritekst = "fritekst")

            every { forenkletTilbakekrevingsvedtakRepository.finnForenkletTilbakekrevingsvedtakForBehandling(behandling.id) } returns eksisterendeForenkletTilbakekrevingsvedtak
            every { loggService.loggForenkletTilbakekrevingsvedtakOppdatertSamtykke(behandling.id) } returns mockk()
            every { forenkletTilbakekrevingsvedtakRepository.save(any()) } returnsArgument (0)

            // Act
            val forenkletTilbakekrevingsvedtak = forenkletTilbakekrevingsvedtakService.oppdaterSamtykkePåForenkletTilbakekrevingsvedtak(behandling.id, true)

            // Assert
            assertThat(forenkletTilbakekrevingsvedtak.samtykke).isEqualTo(true)
            verify(exactly = 1) { loggService.loggForenkletTilbakekrevingsvedtakOppdatertSamtykke(behandling.id) }
            verify(exactly = 1) { forenkletTilbakekrevingsvedtakRepository.save(any()) }
        }

        @Test
        fun `Skal kaste funksjonell feil hvis forenklet tilbakekrevingsvedtak ikke finnes for behandling`() {
            // Arrange
            val behandling = lagBehandling(id = 1)

            every { forenkletTilbakekrevingsvedtakRepository.finnForenkletTilbakekrevingsvedtakForBehandling(behandling.id) } returns null

            // Act && Assert
            val feilmelding =
                assertThrows<FunksjonellFeil> {
                    forenkletTilbakekrevingsvedtakService.oppdaterSamtykkePåForenkletTilbakekrevingsvedtak(behandling.id, true)
                }.melding

            assertThat(feilmelding).isEqualTo("Forenklet tilbakekrevingsvedtak finnes ikke for behandling 1. Oppdater fanen og prøv igjen.")
        }
    }

    @Nested
    inner class OppdaterFritekstPåForenkletTilbakekrevingsvedtakTest {
        @Test
        fun `Skal oppdatere fritekst på eksisterende forenklet tilbakekrevingsvedtak og opprette logg på dette`() {
            // Arrange
            val behandling = lagBehandling(id = 1)
            val eksisterendeForenkletTilbakekrevingsvedtak = ForenkletTilbakekrevingsvedtak(behandling = behandling, samtykke = false, fritekst = "")

            every { forenkletTilbakekrevingsvedtakRepository.finnForenkletTilbakekrevingsvedtakForBehandling(behandling.id) } returns eksisterendeForenkletTilbakekrevingsvedtak
            every { loggService.loggForenkletTilbakekrevingsvedtakOppdatertFritekst(behandling.id) } returns mockk()
            every { forenkletTilbakekrevingsvedtakRepository.save(any()) } returnsArgument (0)

            // Act
            val forenkletTilbakekrevingsvedtak = forenkletTilbakekrevingsvedtakService.oppdaterFritekstPåForenkletTilbakekrevingsvedtak(behandling.id, "oppdatert med fritekst")

            // Assert
            assertThat(forenkletTilbakekrevingsvedtak.fritekst).isEqualTo("oppdatert med fritekst")
            verify(exactly = 1) { loggService.loggForenkletTilbakekrevingsvedtakOppdatertFritekst(behandling.id) }
            verify(exactly = 1) { forenkletTilbakekrevingsvedtakRepository.save(any()) }
        }

        @Test
        fun `Skal kaste funksjonell feil hvis forenklet tilbakekrevingsvedtak ikke finnes for behandling`() {
            // Arrange
            val behandling = lagBehandling(id = 1)

            every { forenkletTilbakekrevingsvedtakRepository.finnForenkletTilbakekrevingsvedtakForBehandling(behandling.id) } returns null

            // Act && Assert
            val feilmelding =
                assertThrows<FunksjonellFeil> {
                    forenkletTilbakekrevingsvedtakService.oppdaterFritekstPåForenkletTilbakekrevingsvedtak(behandling.id, "finnes ikke")
                }.melding

            assertThat(feilmelding).isEqualTo("Forenklet tilbakekrevingsvedtak finnes ikke for behandling 1. Oppdater fanen og prøv igjen.")
        }
    }

    @Nested
    inner class SlettForenkletTilbakekrevingsvedtakTest {
        @Test
        fun `Skal slette forenklet tilbakekrevingsvedtak hvis det finnes`() {
            // Arrange
            val behandling = lagBehandling(id = 1)
            val eksisterendeForenkletTilbakekrevingsvedtak = ForenkletTilbakekrevingsvedtak(behandling = behandling, samtykke = false, fritekst = "")

            every { forenkletTilbakekrevingsvedtakRepository.finnForenkletTilbakekrevingsvedtakForBehandling(behandling.id) } returns eksisterendeForenkletTilbakekrevingsvedtak
            every { loggService.loggForenkletTilbakekrevingsvedtakSlettet(behandling.id) } returns mockk()
            every { forenkletTilbakekrevingsvedtakRepository.delete(eksisterendeForenkletTilbakekrevingsvedtak) } just runs

            // Act
            forenkletTilbakekrevingsvedtakService.slettForenkletTilbakekrevingsvedtak(behandling.id)

            // Assert
            verify(exactly = 1) { loggService.loggForenkletTilbakekrevingsvedtakSlettet(behandling.id) }
            verify(exactly = 1) { forenkletTilbakekrevingsvedtakRepository.delete(eksisterendeForenkletTilbakekrevingsvedtak) }
        }

        @Test
        fun `Dersom det ikke eksisterer noe forenklet tilbakekrevingsvedtak så skal det ikke gjøres noe`() {
            // Arrange
            val behandling = lagBehandling(id = 1)

            every { forenkletTilbakekrevingsvedtakRepository.finnForenkletTilbakekrevingsvedtakForBehandling(behandling.id) } returns null

            // Act
            forenkletTilbakekrevingsvedtakService.slettForenkletTilbakekrevingsvedtak(behandling.id)

            // Assert
            verify(exactly = 0) { loggService.loggForenkletTilbakekrevingsvedtakSlettet(any()) }
            verify(exactly = 0) { forenkletTilbakekrevingsvedtakRepository.delete(any()) }
        }
    }
}
