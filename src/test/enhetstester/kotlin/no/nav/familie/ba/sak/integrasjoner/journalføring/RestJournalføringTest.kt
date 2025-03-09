package no.nav.familie.ba.sak.integrasjoner.journalføring

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.datagenerator.lagMockRestJournalføring
import no.nav.familie.ba.sak.datagenerator.lagRestJournalpostDokument
import no.nav.familie.ba.sak.datagenerator.lagTestJournalpost
import no.nav.familie.ba.sak.ekstern.restDomene.NavnOgIdent
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType
import no.nav.familie.kontrakter.felles.journalpost.Sak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class RestJournalføringTest {
    @Nested
    inner class Valider {
        @Test
        fun `skal kaste exception om man pørver å opprette en ny behandling uten behandlingstype`() {
            // Arrange
            val restJournalføring =
                lagMockRestJournalføring(
                    opprettOgKnyttTilNyBehandling = true,
                    nyBehandlingstype = null,
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    restJournalføring.valider()
                }
            assertThat(exception.message).isEqualTo("Mangler behandlingstype ved oppretting av ny behandling.")
        }

        @Test
        fun `skal kaste exception om man prøver å opprette en ny revurdering uten behandlingsårsak`() {
            // Arrange
            val restJournalføring =
                lagMockRestJournalføring(
                    opprettOgKnyttTilNyBehandling = true,
                    nyBehandlingstype = BehandlingType.REVURDERING,
                    nyBehandlingsårsak = null,
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    restJournalføring.valider()
                }
            assertThat(exception.message).isEqualTo("Mangler behandlingsårsak ved oppretting av ny revurdering.")
        }

        @Test
        fun `skal kaste exception om man prøver å opprette en ny behandling som ikke er revurdering med behandlingsårsak`() {
            // Arrange
            val restJournalføring =
                lagMockRestJournalføring(
                    opprettOgKnyttTilNyBehandling = true,
                    nyBehandlingstype = BehandlingType.FØRSTEGANGSBEHANDLING,
                    nyBehandlingsårsak = BehandlingÅrsak.MIGRERING,
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    restJournalføring.valider()
                }
            assertThat(exception.message).isEqualTo("Forventer kun behandlingsårsak ved oppretting av ny revurdering.")
        }

        @Test
        fun `skal kaste exception om behandlingstype er satt når man ikke prøver å opprette en ny behandling`() {
            // Arrange
            val restJournalføring =
                lagMockRestJournalføring(
                    opprettOgKnyttTilNyBehandling = false,
                    nyBehandlingstype = BehandlingType.FØRSTEGANGSBEHANDLING,
                    nyBehandlingsårsak = null,
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    restJournalføring.valider()
                }
            assertThat(exception.message).isEqualTo("Forventet ikke behandlingstype når man ikke skal opprette en ny behandling.")
        }

        @Test
        fun `skal kaste exception om behandlingsårsak er satt når man ikke prøver å opprette en ny behandling`() {
            // Arrange
            val restJournalføring =
                lagMockRestJournalføring(
                    opprettOgKnyttTilNyBehandling = false,
                    nyBehandlingstype = null,
                    nyBehandlingsårsak = BehandlingÅrsak.SØKNAD,
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    restJournalføring.valider()
                }
            assertThat(exception.message).isEqualTo("Forventet ikke behandlingsårsak når man ikke skal opprette en ny behandling.")
        }

        @Test
        fun `skal kaste exception en eller flere dokumenter mangler tittel`() {
            // Arrange
            val restJournalføring =
                lagMockRestJournalføring(
                    dokumenter =
                        listOf(
                            lagRestJournalpostDokument(
                                dokumentTittel = null,
                            ),
                        ),
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    restJournalføring.valider()
                }
            assertThat(exception.message).isEqualTo("Minst ett av dokumentene mangler dokumenttittel.")
        }

        @Test
        fun `skal kaste exception en eller flere dokumenter har en tom tittel`() {
            // Arrange
            val restJournalføring =
                lagMockRestJournalføring(
                    dokumenter =
                        listOf(
                            lagRestJournalpostDokument(
                                dokumentTittel = "",
                            ),
                        ),
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    restJournalføring.valider()
                }
            assertThat(exception.message).isEqualTo("Minst ett av dokumentene mangler dokumenttittel.")
        }

        @Test
        fun `skal ikke kaste exception om alle felter er gyldig`() {
            // Arrange
            val restJournalføring = lagMockRestJournalføring()

            // Act & assert
            assertDoesNotThrow { restJournalføring.valider() }
        }
    }

    @Nested
    inner class OppdaterMedDokumentOgSak {
        @Test
        fun `Skal beholde originalt avsender mottaker type dersom kanal er EESSI`() {
            // Arrange
            val sak =
                Sak(
                    arkivsaksnummer = "arkivsaksnummer",
                    arkivsaksystem = "arkivsaksystem",
                    fagsakId = "1",
                    sakstype = "sakstype",
                    fagsaksystem = "BA",
                )

            val journalpost = lagTestJournalpost("testIdent", "1", AvsenderMottakerIdType.UTL_ORG, "EESSI")
            val restJournalføring = lagMockRestJournalføring(NavnOgIdent("testbruker", "testIdent"))

            // Act
            val oppdaterJournalpostRequest = restJournalføring.oppdaterMedDokumentOgSak(sak, journalpost)

            // Assert
            assertThat(oppdaterJournalpostRequest.avsenderMottaker?.idType).isEqualTo(AvsenderMottakerIdType.UTL_ORG)
        }

        @Test
        fun `Skal sette AvsenderMottakerIdType i AvsenderMottaker til FNR dersom ident er fylt ut`() {
            // Arrange
            val sak =
                Sak(
                    arkivsaksnummer = "arkivsaksnummer",
                    arkivsaksystem = "arkivsaksystem",
                    fagsakId = "1",
                    sakstype = "sakstype",
                    fagsaksystem = "BA",
                )

            val restJournalføring = lagMockRestJournalføring(NavnOgIdent("testbruker", "testIdent"))
            val journalpost = lagTestJournalpost("testIdent", "1", null, "NAV_NO")

            // Act
            val oppdaterJournalpostRequest = restJournalføring.oppdaterMedDokumentOgSak(sak, journalpost)

            // Assert
            assertThat(oppdaterJournalpostRequest.avsenderMottaker?.idType).isEqualTo(AvsenderMottakerIdType.FNR)
        }

        @Test
        fun `Skal sette AvsenderMottakerIdType i AvsenderMottaker til null dersom ident er blank`() {
            // Arrange
            val sak =
                Sak(
                    arkivsaksnummer = "arkivsaksnummer",
                    arkivsaksystem = "arkivsaksystem",
                    fagsakId = "1",
                    sakstype = "sakstype",
                    fagsaksystem = "BA",
                )

            val journalpost = lagTestJournalpost("", "1", AvsenderMottakerIdType.FNR, "NAV_NO")
            val restJournalføring = lagMockRestJournalføring(NavnOgIdent("testbruker", ""))

            // Act
            val oppdaterJournalpostRequest = restJournalføring.oppdaterMedDokumentOgSak(sak, journalpost)

            // Assert
            assertThat(oppdaterJournalpostRequest.avsenderMottaker?.idType).isNull()
        }
    }
}
