package no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning

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
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingTilSimuleringService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class TilbakekrevingsvedtakMotregningServiceTest {
    private val tilbakekrevingsvedtakMotregningRepository = mockk<TilbakekrevingsvedtakMotregningRepository>()
    private val behandlingService = mockk<BehandlingHentOgPersisterService>()
    private val dokumentGenereringService = mockk<DokumentGenereringService>()
    private val loggService = mockk<LoggService>()
    private val tilbakestillBehandlingTilSimuleringService = mockk<TilbakestillBehandlingTilSimuleringService>()

    private val tilbakekrevingsvedtakMotregningService =
        TilbakekrevingsvedtakMotregningService(
            tilbakekrevingsvedtakMotregningRepository,
            loggService,
            behandlingService,
            tilbakestillBehandlingTilSimuleringService,
        )

    private val tilbakekrevingsvedtakMotregningBrevService =
        TilbakekrevingsvedtakMotregningBrevService(
            tilbakekrevingsvedtakMotregningRepository,
            dokumentGenereringService,
        )

    @Nested
    inner class FinnTilbakekrevingsvedtakMotregningTest {
        @Test
        fun `skal returnere null hvis det ikke finnes Tilbakekrevingsvedtak motregning for behandling`() {
            // Arrange
            val behandling = lagBehandling(id = 1)

            every { tilbakekrevingsvedtakMotregningRepository.finnTilbakekrevingsvedtakMotregningForBehandling(behandling.id) } returns null

            // Act
            val tilbakekrevingsvedtakMotregning = tilbakekrevingsvedtakMotregningService.finnTilbakekrevingsvedtakMotregning(behandling.id)

            // Assert
            assertThat(tilbakekrevingsvedtakMotregning).isNull()
        }

        @Test
        fun `skal returner Tilbakekrevingsvedtak motregning hvis det finnes for behandling`() {
            // Arrange
            val behandling = lagBehandling(id = 1)
            val eksisterendeTilbakekrevingsvedtakMotregning =
                TilbakekrevingsvedtakMotregning(
                    behandling = behandling,
                    samtykke = false,
                    varselDato = LocalDate.now(),
                    heleBeløpetSkalKrevesTilbake = false,
                )

            every { tilbakekrevingsvedtakMotregningRepository.finnTilbakekrevingsvedtakMotregningForBehandling(behandling.id) } returns eksisterendeTilbakekrevingsvedtakMotregning

            // Act
            val tilbakekrevingsvedtakMotregning = tilbakekrevingsvedtakMotregningService.finnTilbakekrevingsvedtakMotregning(behandling.id)

            // Assert
            assertThat(tilbakekrevingsvedtakMotregning).isEqualTo(eksisterendeTilbakekrevingsvedtakMotregning)
        }
    }

    @Nested
    inner class OpprettTilbakekrevingsvedtakMotregningTest {
        @Test
        fun `Skal returnere eksisterende Tilbakekrevingsvedtak motregning dersom det allerede finnes`() {
            // Arrange
            val behandling = lagBehandling(id = 1)
            val eksisterendeTilbakekrevingsvedtakMotregning =
                TilbakekrevingsvedtakMotregning(
                    behandling = behandling,
                    samtykke = false,
                    varselDato = LocalDate.now(),
                    heleBeløpetSkalKrevesTilbake = false,
                )

            every { tilbakekrevingsvedtakMotregningRepository.finnTilbakekrevingsvedtakMotregningForBehandling(behandling.id) } returns eksisterendeTilbakekrevingsvedtakMotregning

            // Act
            val tilbakekrevingsvedtakMotregning = tilbakekrevingsvedtakMotregningService.opprettTilbakekrevingsvedtakMotregning(behandling.id)

            // Assert
            assertThat(tilbakekrevingsvedtakMotregning).isEqualTo(eksisterendeTilbakekrevingsvedtakMotregning)
            verify(exactly = 0) { tilbakekrevingsvedtakMotregningRepository.save(any()) }
        }

        @Test
        fun `skal opprette Tilbakekrevingsvedtak motregning hvis det ikke finnes for behandling`() {
            // Arrange
            val behandling = lagBehandling(id = 1)

            every { tilbakekrevingsvedtakMotregningRepository.finnTilbakekrevingsvedtakMotregningForBehandling(behandling.id) } returns null
            every { tilbakekrevingsvedtakMotregningRepository.save(any()) } returnsArgument (0)
            every { behandlingService.hent(behandlingId = behandling.id) } returns behandling

            // Act
            val tilbakekrevingsvedtakMotregning = tilbakekrevingsvedtakMotregningService.opprettTilbakekrevingsvedtakMotregning(behandling.id)

            // Assert
            assertThat(tilbakekrevingsvedtakMotregning.behandling.id).isEqualTo(behandling.id)
            assertThat(tilbakekrevingsvedtakMotregning.samtykke).isFalse()
            assertThat(tilbakekrevingsvedtakMotregning.årsakTilFeilutbetaling).isNull()
            assertThat(tilbakekrevingsvedtakMotregning.vurderingAvSkyld).isNull()
            assertThat(tilbakekrevingsvedtakMotregning.varselDato).isEqualTo(LocalDate.now())
            assertThat(tilbakekrevingsvedtakMotregning.vedtakPdf).isNull()

            verify(exactly = 1) { tilbakekrevingsvedtakMotregningRepository.save(tilbakekrevingsvedtakMotregning) }
            verify(exactly = 0) { loggService.loggUlovfestetMotregningBenyttet(behandling.id) }
        }
    }

    @Nested
    inner class OppdaterTilbakekrevingsvedtakMotregningTest {
        @Test
        fun `Skal oppdatere eksisterende Tilbakekrevingsvedtak motregning og opprette logg på dette`() {
            // Arrange
            val behandling = lagBehandling(id = 1)
            val eksisterendeTilbakekrevingsvedtakMotregning =
                TilbakekrevingsvedtakMotregning(
                    behandling = behandling,
                    samtykke = false,
                    varselDato = LocalDate.now(),
                    heleBeløpetSkalKrevesTilbake = false,
                )

            every { tilbakekrevingsvedtakMotregningRepository.finnTilbakekrevingsvedtakMotregningForBehandling(behandling.id) } returns eksisterendeTilbakekrevingsvedtakMotregning
            every { loggService.loggUlovfestetMotregningBenyttet(behandling.id) } returns mockk()
            every { tilbakekrevingsvedtakMotregningRepository.save(any()) } returnsArgument (0)
            every { tilbakestillBehandlingTilSimuleringService.tilbakestillBehandlingTilSimuering(any()) } returns mockk()

            // Act
            val tilbakekrevingsvedtakMotregning =
                tilbakekrevingsvedtakMotregningService.oppdaterTilbakekrevingsvedtakMotregning(
                    behandlingId = behandling.id,
                    samtykke = true,
                    årsakTilFeilutbetaling = "ny årsak",
                    vurderingAvSkyld = "ny vurdering",
                    varselDato = LocalDate.of(2025, 1, 1),
                    heleBeløpetSkalKrevesTilbake = true,
                )

            // Assert
            assertThat(tilbakekrevingsvedtakMotregning.samtykke).isEqualTo(true)
            assertThat(tilbakekrevingsvedtakMotregning.årsakTilFeilutbetaling).isEqualTo("ny årsak")
            assertThat(tilbakekrevingsvedtakMotregning.vurderingAvSkyld).isEqualTo("ny vurdering")
            assertThat(tilbakekrevingsvedtakMotregning.varselDato).isEqualTo(LocalDate.of(2025, 1, 1))
            assertThat(tilbakekrevingsvedtakMotregning.heleBeløpetSkalKrevesTilbake).isEqualTo(true)
            verify(exactly = 1) { loggService.loggUlovfestetMotregningBenyttet(behandling.id) }
            verify(exactly = 1) { tilbakekrevingsvedtakMotregningRepository.save(any()) }
        }

        @Test
        fun `Skal kun opprette logg dersom samtykke er true ved oppdatering av eksisterende Tilbakekrevingsvedtak motregning`() {
            // Arrange
            val behandling = lagBehandling(id = 1)
            val eksisterendeTilbakekrevingsvedtakMotregning =
                TilbakekrevingsvedtakMotregning(
                    behandling = behandling,
                    samtykke = false,
                    varselDato = LocalDate.now(),
                    heleBeløpetSkalKrevesTilbake = false,
                )

            every {
                tilbakekrevingsvedtakMotregningRepository.finnTilbakekrevingsvedtakMotregningForBehandling(
                    behandling.id,
                )
            } returns eksisterendeTilbakekrevingsvedtakMotregning
            every { loggService.loggUlovfestetMotregningBenyttet(behandling.id) } returns mockk()
            every { tilbakekrevingsvedtakMotregningRepository.save(any()) } returnsArgument (0)
            every { tilbakestillBehandlingTilSimuleringService.tilbakestillBehandlingTilSimuering(any()) } returns mockk()

            // Act
            tilbakekrevingsvedtakMotregningService.oppdaterTilbakekrevingsvedtakMotregning(
                behandlingId = behandling.id,
                heleBeløpetSkalKrevesTilbake = true,
            )

            verify(exactly = 0) { loggService.loggUlovfestetMotregningBenyttet(behandling.id) }

            tilbakekrevingsvedtakMotregningService.oppdaterTilbakekrevingsvedtakMotregning(
                behandlingId = behandling.id,
                varselDato = LocalDate.now().plusDays(1),
            )

            verify(exactly = 0) { loggService.loggUlovfestetMotregningBenyttet(behandling.id) }

            tilbakekrevingsvedtakMotregningService.oppdaterTilbakekrevingsvedtakMotregning(
                behandlingId = behandling.id,
                årsakTilFeilutbetaling = "ny årsak",
            )

            verify(exactly = 0) { loggService.loggUlovfestetMotregningBenyttet(behandling.id) }

            tilbakekrevingsvedtakMotregningService.oppdaterTilbakekrevingsvedtakMotregning(
                behandlingId = behandling.id,
                vurderingAvSkyld = "ny vurdering",
            )

            verify(exactly = 0) { loggService.loggUlovfestetMotregningBenyttet(behandling.id) }

            tilbakekrevingsvedtakMotregningService.oppdaterTilbakekrevingsvedtakMotregning(
                behandlingId = behandling.id,
                samtykke = false,
            )

            verify(exactly = 0) { loggService.loggUlovfestetMotregningBenyttet(behandling.id) }

            tilbakekrevingsvedtakMotregningService.oppdaterTilbakekrevingsvedtakMotregning(
                behandlingId = behandling.id,
                samtykke = true,
            )

            verify(exactly = 1) { loggService.loggUlovfestetMotregningBenyttet(behandling.id) }
        }

        @Test
        fun `Skal kaste funksjonell feil hvis Tilbakekrevingsvedtak motregning ikke finnes for behandling`() {
            // Arrange
            val behandling = lagBehandling(id = 1)

            every { tilbakekrevingsvedtakMotregningRepository.finnTilbakekrevingsvedtakMotregningForBehandling(behandling.id) } returns null

            // Act && Assert
            val feilmelding =
                assertThrows<FunksjonellFeil> {
                    tilbakekrevingsvedtakMotregningService.oppdaterTilbakekrevingsvedtakMotregning(behandling.id, true)
                }.melding

            assertThat(feilmelding).isEqualTo("Tilbakekrevingsvedtak motregning finnes ikke for behandling 1. Oppdater fanen og prøv igjen.")
        }

        @Test
        fun `Skal tilbakekstille behandling til simulering når heleBeløpetSkalKrevesTilbake blir oppdatert`() {
            // Arrange
            val behandling = lagBehandling(id = 1)
            val eksisterendeTilbakekrevingsvedtakMotregning =
                TilbakekrevingsvedtakMotregning(
                    behandling = behandling,
                    samtykke = true,
                    varselDato = LocalDate.now(),
                    heleBeløpetSkalKrevesTilbake = true,
                )

            every {
                tilbakekrevingsvedtakMotregningRepository.finnTilbakekrevingsvedtakMotregningForBehandling(
                    behandling.id,
                )
            } returns eksisterendeTilbakekrevingsvedtakMotregning
            every { loggService.loggUlovfestetMotregningBenyttet(behandling.id) } returns mockk()
            every { tilbakekrevingsvedtakMotregningRepository.save(any()) } returnsArgument (0)
            every { tilbakestillBehandlingTilSimuleringService.tilbakestillBehandlingTilSimuering(any()) } returns mockk()

            // Act
            tilbakekrevingsvedtakMotregningService.oppdaterTilbakekrevingsvedtakMotregning(
                behandlingId = behandling.id,
                heleBeløpetSkalKrevesTilbake = false,
            )

            // Assert
            verify { tilbakestillBehandlingTilSimuleringService.tilbakestillBehandlingTilSimuering(behandling.id) }
        }
    }

    @Nested
    inner class SlettTilbakekrevingsvedtakMotregningTest {
        @Test
        fun `Skal slette Tilbakekrevingsvedtak motregning og opprette logg når samtykke er true`() {
            // Arrange
            val behandling = lagBehandling(id = 1)
            val eksisterendeTilbakekrevingsvedtakMotregning =
                TilbakekrevingsvedtakMotregning(
                    behandling = behandling,
                    samtykke = true,
                    varselDato = LocalDate.now(),
                    heleBeløpetSkalKrevesTilbake = false,
                )

            every { tilbakekrevingsvedtakMotregningRepository.finnTilbakekrevingsvedtakMotregningForBehandling(behandling.id) } returns eksisterendeTilbakekrevingsvedtakMotregning
            every { loggService.loggUlovfestetMotregningAngret(behandling.id) } returns mockk()
            every { tilbakekrevingsvedtakMotregningRepository.delete(eksisterendeTilbakekrevingsvedtakMotregning) } just runs

            // Act
            tilbakekrevingsvedtakMotregningService.slettTilbakekrevingsvedtakMotregning(behandling.id)

            // Assert
            verify(exactly = 1) { loggService.loggUlovfestetMotregningAngret(behandling.id) }
            verify(exactly = 1) { tilbakekrevingsvedtakMotregningRepository.delete(eksisterendeTilbakekrevingsvedtakMotregning) }
        }

        @Test
        fun `Skal slette Tilbakekrevingsvedtak motregning, men ikke opprette logg når samtykke er false`() {
            // Arrange
            val behandling = lagBehandling(id = 1)
            val eksisterendeTilbakekrevingsvedtakMotregning =
                TilbakekrevingsvedtakMotregning(
                    behandling = behandling,
                    samtykke = false,
                    varselDato = LocalDate.now(),
                    heleBeløpetSkalKrevesTilbake = false,
                )

            every {
                tilbakekrevingsvedtakMotregningRepository.finnTilbakekrevingsvedtakMotregningForBehandling(
                    behandling.id,
                )
            } returns eksisterendeTilbakekrevingsvedtakMotregning
            every { loggService.loggUlovfestetMotregningAngret(behandling.id) } returns mockk()
            every { tilbakekrevingsvedtakMotregningRepository.delete(eksisterendeTilbakekrevingsvedtakMotregning) } just runs

            // Act
            tilbakekrevingsvedtakMotregningService.slettTilbakekrevingsvedtakMotregning(behandling.id)

            // Assert
            verify(exactly = 0) { loggService.loggUlovfestetMotregningAngret(behandling.id) }
            verify(exactly = 1) {
                tilbakekrevingsvedtakMotregningRepository.delete(
                    eksisterendeTilbakekrevingsvedtakMotregning,
                )
            }
        }

        @Test
        fun `Dersom det ikke eksisterer noe Tilbakekrevingsvedtak motregning så skal det ikke gjøres noe`() {
            // Arrange
            val behandling = lagBehandling(id = 1)

            every { tilbakekrevingsvedtakMotregningRepository.finnTilbakekrevingsvedtakMotregningForBehandling(behandling.id) } returns null

            // Act
            tilbakekrevingsvedtakMotregningService.slettTilbakekrevingsvedtakMotregning(behandling.id)

            // Assert
            verify(exactly = 0) { loggService.loggUlovfestetMotregningAngret(any()) }
            verify(exactly = 0) { tilbakekrevingsvedtakMotregningRepository.delete(any()) }
        }
    }

    @Nested
    inner class OpprettOgLagreTilbakekrevingsvedtakMotregningPdfTest {
        @Test
        fun `Skal lagre PDF i tilbakekrevingsvedtakMotregning`() {
            // Arrange
            val behandling = lagBehandling(id = 1)
            val tilbakekrevingsvedtakMotregning =
                TilbakekrevingsvedtakMotregning(
                    behandling = behandling,
                    samtykke = true,
                    vedtakPdf = null,
                    varselDato = LocalDate.now(),
                    årsakTilFeilutbetaling = "årsakTilFeilutbetaling",
                    vurderingAvSkyld = "vurderingAvSkyld",
                    heleBeløpetSkalKrevesTilbake = true,
                )
            val pdf = ByteArray(200)

            every { tilbakekrevingsvedtakMotregningRepository.finnTilbakekrevingsvedtakMotregningForBehandling(behandling.id) } returns tilbakekrevingsvedtakMotregning
            every { dokumentGenereringService.genererBrevForTilbakekrevingsvedtakMotregning(tilbakekrevingsvedtakMotregning) } returns pdf
            every { tilbakekrevingsvedtakMotregningRepository.saveAndFlush(tilbakekrevingsvedtakMotregning) } returnsArgument (0)

            // Act
            val tilbakekrevingsvedtakMotregningMedPdf = tilbakekrevingsvedtakMotregningBrevService.opprettOgLagreTilbakekrevingsvedtakMotregningPdf(behandling.id)

            // Assert
            assertThat(tilbakekrevingsvedtakMotregningMedPdf.vedtakPdf).isEqualTo(pdf)

            verify(exactly = 1) { tilbakekrevingsvedtakMotregningRepository.finnTilbakekrevingsvedtakMotregningForBehandling(behandling.id) }
            verify(exactly = 1) { dokumentGenereringService.genererBrevForTilbakekrevingsvedtakMotregning(tilbakekrevingsvedtakMotregning) }
            verify(exactly = 1) { tilbakekrevingsvedtakMotregningRepository.saveAndFlush(tilbakekrevingsvedtakMotregning) }
        }

        @Test
        fun `Skal feile hvis fritekstfelter ikke er utfylt ved generering av brev`() {
            // Arrange
            val behandling = lagBehandling(id = 1)
            val tilbakekrevingsvedtakMotregning =
                TilbakekrevingsvedtakMotregning(
                    behandling = behandling,
                    samtykke = true,
                    varselDato = LocalDate.now(),
                    heleBeløpetSkalKrevesTilbake = true,
                )

            every {
                tilbakekrevingsvedtakMotregningRepository.finnTilbakekrevingsvedtakMotregningForBehandling(behandling.id)
            } returns tilbakekrevingsvedtakMotregning

            // Act
            val feil =
                assertThrows<FunksjonellFeil> {
                    tilbakekrevingsvedtakMotregningBrevService.opprettOgLagreTilbakekrevingsvedtakMotregningPdf(
                        behandling.id,
                    )
                }

            // Assert
            assertThat(feil.melding).isEqualTo("Fritekstfeltene for årsak til feilutbetaling og vurdering av skyld må være utfylt for å generere brevet.")

            verify(exactly = 1) {
                tilbakekrevingsvedtakMotregningRepository.finnTilbakekrevingsvedtakMotregningForBehandling(behandling.id)
            }
            verify(exactly = 0) {
                dokumentGenereringService.genererBrevForTilbakekrevingsvedtakMotregning(tilbakekrevingsvedtakMotregning)
            }
            verify(exactly = 0) {
                tilbakekrevingsvedtakMotregningRepository.saveAndFlush(tilbakekrevingsvedtakMotregning)
            }
        }
    }

    @Nested
    inner class TilTilbakekrevingsvedtakMotregningDtoTest {
        @Test
        fun `tilTilbakekrevingsvedtakMotregningDto skal returnere verdiene fra TilbakekrevingsvedtakMotregning`() {
            // Arrange
            val tilbakekrevingsvedtakMotregning =
                TilbakekrevingsvedtakMotregning(
                    behandling = lagBehandling(),
                    samtykke = true,
                    varselDato = LocalDate.now(),
                    heleBeløpetSkalKrevesTilbake = true,
                    årsakTilFeilutbetaling = "ny årsak",
                    vurderingAvSkyld = "ny vurdering",
                )

            // Act
            val restTilbakekrevingsvedtakMotregning =
                tilbakekrevingsvedtakMotregning.tilTilbakekrevingsvedtakMotregningDto()

            // Assert
            assertThat(restTilbakekrevingsvedtakMotregning.id).isEqualTo(tilbakekrevingsvedtakMotregning.id)
            assertThat(restTilbakekrevingsvedtakMotregning.behandlingId).isEqualTo(tilbakekrevingsvedtakMotregning.behandling.id)
            assertThat(restTilbakekrevingsvedtakMotregning.samtykke).isEqualTo(tilbakekrevingsvedtakMotregning.samtykke)
            assertThat(restTilbakekrevingsvedtakMotregning.årsakTilFeilutbetaling).isEqualTo(
                tilbakekrevingsvedtakMotregning.årsakTilFeilutbetaling,
            )
            assertThat(restTilbakekrevingsvedtakMotregning.vurderingAvSkyld).isEqualTo(tilbakekrevingsvedtakMotregning.vurderingAvSkyld)
            assertThat(restTilbakekrevingsvedtakMotregning.varselDato).isEqualTo(tilbakekrevingsvedtakMotregning.varselDato)
            assertThat(restTilbakekrevingsvedtakMotregning.heleBeløpetSkalKrevesTilbake).isEqualTo(
                tilbakekrevingsvedtakMotregning.heleBeløpetSkalKrevesTilbake,
            )
        }
    }

    @Nested
    inner class ValiderAtTilbakekrevingsvedtakMotregningKanSendesTilBeslutter {
        @Test
        fun `skal kaste feil dersom samtykke er false`() {
            // Arrange
            val tilbakekrevingsvedtakMotregning =
                TilbakekrevingsvedtakMotregning(
                    behandling = lagBehandling(),
                    samtykke = false,
                    heleBeløpetSkalKrevesTilbake = false,
                )

            // Act & Assert
            val feil =
                assertThrows<FunksjonellFeil> {
                    tilbakekrevingsvedtakMotregning.validerAtTilbakekrevingsvedtakMotregningKanSendesTilBeslutter()
                }

            assertThat(feil.frontendFeilmelding).isEqualTo("Kan ikke sende tilbakekrevingsvedtak ved motregning til beslutter hvis samtykke ikke er bekreftet.")
        }

        @Test
        fun `skal kaste feil dersom heleBeløpetSkalKrevesTilbake er false`() {
            // Arrange
            val tilbakekrevingsvedtakMotregning =
                TilbakekrevingsvedtakMotregning(
                    behandling = lagBehandling(),
                    samtykke = true,
                    heleBeløpetSkalKrevesTilbake = false,
                )

            // Act & Assert
            val feil =
                assertThrows<FunksjonellFeil> {
                    tilbakekrevingsvedtakMotregning.validerAtTilbakekrevingsvedtakMotregningKanSendesTilBeslutter()
                }

            assertThat(feil.frontendFeilmelding).isEqualTo("Kan ikke sende tilbakekrevingsvedtak ved motregning til beslutter hvis ikke hele beløpet skal kreves tilbake.")
        }

        @Test
        fun `skal kaste feil dersom varseldato er i fremtiden`() {
            // Arrange
            val tilbakekrevingsvedtakMotregning =
                TilbakekrevingsvedtakMotregning(
                    behandling = lagBehandling(),
                    samtykke = true,
                    heleBeløpetSkalKrevesTilbake = true,
                    varselDato = LocalDate.now().plusDays(1),
                )

            // Act & Assert
            val feil =
                assertThrows<FunksjonellFeil> {
                    tilbakekrevingsvedtakMotregning.validerAtTilbakekrevingsvedtakMotregningKanSendesTilBeslutter()
                }

            assertThat(feil.frontendFeilmelding).isEqualTo("Kan ikke sende tilbakekrevingsvedtak ved motregning til beslutter med fremtidig varseldato.")
        }

        @Test
        fun `skal kaste feil dersom årsak til feilutbetaling ikke er utfylt`() {
            // Arrange
            val tilbakekrevingsvedtakMotregning =
                TilbakekrevingsvedtakMotregning(
                    behandling = lagBehandling(),
                    samtykke = true,
                    heleBeløpetSkalKrevesTilbake = true,
                    varselDato = LocalDate.now(),
                    årsakTilFeilutbetaling = null,
                )

            // Act & Assert
            val feil =
                assertThrows<FunksjonellFeil> {
                    tilbakekrevingsvedtakMotregning.validerAtTilbakekrevingsvedtakMotregningKanSendesTilBeslutter()
                }

            assertThat(feil.frontendFeilmelding).isEqualTo("Kan ikke sende tilbakekrevingsvedtak ved motregning til beslutter uten årsak til feilutbetaling.")
        }

        @Test
        fun `skal kaste feil dersom vurdering av skyld ikke er utfylt`() {
            // Arrange
            val tilbakekrevingsvedtakMotregning =
                TilbakekrevingsvedtakMotregning(
                    behandling = lagBehandling(),
                    samtykke = true,
                    heleBeløpetSkalKrevesTilbake = true,
                    varselDato = LocalDate.now(),
                    årsakTilFeilutbetaling = "årsak til feilutbetaling",
                    vurderingAvSkyld = null,
                )

            // Act & Assert
            val feil =
                assertThrows<FunksjonellFeil> {
                    tilbakekrevingsvedtakMotregning.validerAtTilbakekrevingsvedtakMotregningKanSendesTilBeslutter()
                }

            assertThat(feil.frontendFeilmelding).isEqualTo("Kan ikke sende tilbakekrevingsvedtak ved motregning til beslutter uten vurdering av skyld.")
        }

        @Test
        fun `skal ikke kaste feil selv om vedtakPdf er null`() {
            // Arrange
            val tilbakekrevingsvedtakMotregning =
                TilbakekrevingsvedtakMotregning(
                    behandling = lagBehandling(),
                    samtykke = true,
                    heleBeløpetSkalKrevesTilbake = true,
                    varselDato = LocalDate.now(),
                    årsakTilFeilutbetaling = "årsak til feilutbetaling",
                    vurderingAvSkyld = "vurdering av skyld",
                    vedtakPdf = null,
                )

            // Act & Assert
            assertDoesNotThrow {
                tilbakekrevingsvedtakMotregning.validerAtTilbakekrevingsvedtakMotregningKanSendesTilBeslutter()
            }
        }
    }
}
