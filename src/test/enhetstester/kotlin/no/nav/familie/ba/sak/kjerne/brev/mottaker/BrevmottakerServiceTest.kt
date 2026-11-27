package no.nav.familie.ba.sak.kjerne.brev.mottaker

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.datagenerator.lagBrevmottakerDb
import no.nav.familie.ba.sak.kjerne.brev.domene.ManuellBrevmottaker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BrevmottakerServiceTest {
    private val brevmottakerHenter = mockk<BrevmottakerHenter>()
    private val brevmottakerOppretter = mockk<BrevmottakerOppretter>()
    private val brevmottakerSletter = mockk<BrevmottakerSletter>()
    private val brevmottakerService =
        BrevmottakerService(
            brevmottakerHenter = brevmottakerHenter,
            brevmottakerOppretter = brevmottakerOppretter,
            brevmottakerSletter = brevmottakerSletter,
        )

    @Nested
    inner class OpprettBrevmottaker {
        @Test
        fun `skal opprette brevmottaker og returnere lagret brevmottaker`() {
            // Arrange
            val brevmottaker = lagBrevmottakerDb(behandlingId = 1, type = MottakerType.FULLMEKTIG)
            val lagretBrevmottaker = brevmottaker.copy(id = 1)
            every { brevmottakerOppretter.opprettBrevmottaker(brevmottaker) } returns lagretBrevmottaker

            // Act
            val resultat = brevmottakerService.opprettBrevmottaker(brevmottaker)

            // Assert
            assertThat(resultat).isEqualTo(lagretBrevmottaker)
            verify(exactly = 1) { brevmottakerOppretter.opprettBrevmottaker(brevmottaker) }
        }
    }

    @Nested
    inner class SlettBrevmottaker {
        @Test
        fun `skal slette brevmottaker`() {
            // Arrange
            every { brevmottakerSletter.slettBrevmottaker(1, 2) } just runs

            // Act
            brevmottakerService.slettBrevmottaker(1, 2)

            // Assert
            verify(exactly = 1) { brevmottakerSletter.slettBrevmottaker(1, 2) }
        }
    }

    @Nested
    inner class HentBrevmottakere {
        @Test
        fun `skal hente brevmottakere for behandling`() {
            // Arrange
            val brevmottakere = listOf(lagBrevmottakerDb(behandlingId = 1, type = MottakerType.FULLMEKTIG))
            every { brevmottakerHenter.hentBrevmottakere(1) } returns brevmottakere

            // Act
            val resultat = brevmottakerService.hentBrevmottakere(1)

            // Assert
            assertThat(resultat).isEqualTo(brevmottakere)
            verify(exactly = 1) { brevmottakerHenter.hentBrevmottakere(1) }
        }
    }

    @Nested
    inner class LagMottakereFraBrevMottakere {
        @Test
        fun `skal returnere bruker når det ikke finnes manuelle brevmottakere`() {
            // Act
            val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(emptyList())

            // Assert
            assertThat(mottakerInfo).containsExactly(Bruker)
        }

        @Test
        fun `skal lage mottakere når brevmottaker er FULLMEKTIG og bruker har norsk adresse`() {
            // Arrange
            val brevmottakere = listOf(lagBrevmottakerDb(behandlingId = 1, type = MottakerType.FULLMEKTIG, navn = "John Doe"))

            // Act
            val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere.map { ManuellBrevmottaker(it) })

            // Assert
            assertThat(mottakerInfo).hasSize(2)
            assertThat(mottakerInfo.first().manuellAdresseInfo).isNull()
            assertThat(mottakerInfo.last().navn).isEqualTo("John Doe")
            assertThat(mottakerInfo.last().manuellAdresseInfo).isNotNull()
        }

        @Test
        fun `skal lage mottakere når brevmottaker er FULLMEKTIG og bruker har utenlandsk adresse`() {
            // Arrange
            val brevmottakere =
                listOf(
                    lagBrevmottakerDb(behandlingId = 1, type = MottakerType.FULLMEKTIG, navn = "John Doe"),
                    lagBrevmottakerDb(
                        behandlingId = 1,
                        type = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                        poststed = "Munchen",
                        landkode = "DE",
                    ),
                )

            // Act
            val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere.map { ManuellBrevmottaker(it) })

            // Assert
            assertThat(mottakerInfo).hasSize(2)
            assertThat(mottakerInfo.first().manuellAdresseInfo?.landkode).isEqualTo("DE")
            assertThat(mottakerInfo.last().navn).isEqualTo("John Doe")
            assertThat(mottakerInfo.last().manuellAdresseInfo).isNotNull()
        }

        @Test
        fun `skal lage mottakere når brevmottaker er VERGE og bruker har utenlandsk adresse`() {
            // Arrange
            val brevmottakere =
                listOf(
                    lagBrevmottakerDb(behandlingId = 1, type = MottakerType.VERGE, navn = "John Doe"),
                    lagBrevmottakerDb(
                        behandlingId = 1,
                        type = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                        poststed = "Munchen",
                        landkode = "DE",
                    ),
                )

            // Act
            val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere.map { ManuellBrevmottaker(it) })

            // Assert
            assertThat(mottakerInfo).hasSize(2)
            assertThat(mottakerInfo.first().manuellAdresseInfo?.landkode).isEqualTo("DE")
            assertThat(mottakerInfo.last().navn).isEqualTo("John Doe")
            assertThat(mottakerInfo.last().manuellAdresseInfo).isNotNull()
        }

        @Test
        fun `skal lage mottakere når bruker har utenlandsk adresse`() {
            // Arrange
            val brevmottakere =
                listOf(
                    lagBrevmottakerDb(
                        behandlingId = 1,
                        type = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                        poststed = "Munchen",
                        landkode = "DE",
                    ),
                )

            // Act
            val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere.map { ManuellBrevmottaker(it) })

            // Assert
            assertThat(mottakerInfo).hasSize(1)
            assertThat(mottakerInfo.single().manuellAdresseInfo?.landkode).isEqualTo("DE")
        }

        @Test
        fun `skal lage mottakere når bruker har dødsbo`() {
            // Arrange
            val brevmottakere =
                listOf(
                    lagBrevmottakerDb(
                        behandlingId = 1,
                        type = MottakerType.DØDSBO,
                        poststed = "Munchen",
                        landkode = "DE",
                    ),
                )

            // Act
            val mottakerInfo = brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere.map { ManuellBrevmottaker(it) })

            // Assert
            assertThat(mottakerInfo).hasSize(1)
            assertThat(mottakerInfo.single().manuellAdresseInfo?.landkode).isEqualTo("DE")
        }

        @Test
        fun `skal kaste feil når brevmottakere inneholder ugyldig kombinasjon`() {
            // Arrange
            val brevmottakere =
                listOf(
                    lagBrevmottakerDb(
                        behandlingId = 1,
                        type = MottakerType.VERGE,
                        poststed = "Munchen",
                        landkode = "DE",
                    ),
                    lagBrevmottakerDb(
                        behandlingId = 1,
                        type = MottakerType.FULLMEKTIG,
                        poststed = "Munchen",
                        landkode = "DE",
                    ),
                )

            // Act & Assert
            val feil =
                assertThrows<FunksjonellFeil> {
                    brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere.map { ManuellBrevmottaker(it) })
                }
            assertThat(feil.frontendFeilmelding).contains("kan ikke kombineres")
        }

        @Test
        fun `skal kaste feil dersom brevmottakere inneholder to av typen BrukerMedUtenlandskAdresse`() {
            // Arrange
            val brevmottakere =
                listOf(
                    lagBrevmottakerDb(
                        behandlingId = 1,
                        type = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                        poststed = "Munchen",
                        landkode = "DE",
                    ),
                    lagBrevmottakerDb(
                        behandlingId = 1,
                        type = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                        poststed = "Munchen",
                        landkode = "DE",
                    ),
                )

            // Act & Assert
            val feil =
                assertThrows<FunksjonellFeil> {
                    brevmottakerService.lagMottakereFraBrevMottakere(brevmottakere.map { ManuellBrevmottaker(it) })
                }
            assertThat(feil.frontendFeilmelding).contains("Mottakerfeil: Det er registrert mer enn en utenlandsk adresse tilhørende bruker")
        }
    }
}
