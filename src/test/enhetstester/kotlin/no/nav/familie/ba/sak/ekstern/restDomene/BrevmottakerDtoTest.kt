package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.datagenerator.lagBrevmottakerDb
import no.nav.familie.ba.sak.datagenerator.lagBrevmottakerDto
import no.nav.familie.ba.sak.kjerne.brev.mottaker.MottakerType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BrevmottakerDtoTest {
    @Nested
    inner class TilDomene {
        @Test
        fun `skal mappe alle felter og sette behandlingId`() {
            // Arrange
            val brevmottakerDto =
                lagBrevmottakerDto(
                    id = 5,
                    type = MottakerType.VERGE,
                    navn = "Verge Vergesen",
                    adresselinje1 = "Adresse 1",
                    adresselinje2 = "Adresse 2",
                    postnummer = "0661",
                    poststed = "Oslo",
                    landkode = "NO",
                )

            // Act
            val brevmottaker = brevmottakerDto.tilDomene(behandlingId = 1)

            // Assert
            assertThat(brevmottaker.id).isEqualTo(0)
            assertThat(brevmottaker.behandlingId).isEqualTo(1)
            assertThat(brevmottaker.type).isEqualTo(MottakerType.VERGE)
            assertThat(brevmottaker.navn).isEqualTo("Verge Vergesen")
            assertThat(brevmottaker.adresselinje1).isEqualTo("Adresse 1")
            assertThat(brevmottaker.adresselinje2).isEqualTo("Adresse 2")
            assertThat(brevmottaker.postnummer).isEqualTo("0661")
            assertThat(brevmottaker.poststed).isEqualTo("Oslo")
            assertThat(brevmottaker.landkode).isEqualTo("NO")
        }

        @Test
        fun `skal fjerne mellomrom rundt postnummer og poststed`() {
            // Arrange
            val brevmottakerDto = lagBrevmottakerDto(postnummer = " 0661 ", poststed = " Oslo ")

            // Act
            val brevmottaker = brevmottakerDto.tilDomene(behandlingId = 1)

            // Assert
            assertThat(brevmottaker.postnummer).isEqualTo("0661")
            assertThat(brevmottaker.poststed).isEqualTo("Oslo")
        }
    }

    @Nested
    inner class Fra {
        @Test
        fun `skal mappe alle felter fra brevmottaker`() {
            // Arrange
            val brevmottaker =
                lagBrevmottakerDb(
                    behandlingId = 1,
                    type = MottakerType.DØDSBO,
                    navn = "Dødsbo Dødsbosen",
                    adresselinje1 = "Adresse 1",
                    adresselinje2 = "Adresse 2",
                    postnummer = "12345",
                    poststed = "München",
                    landkode = "DE",
                ).copy(id = 7)

            // Act
            val brevmottakerDto = BrevmottakerDto.fra(brevmottaker)

            // Assert
            assertThat(brevmottakerDto).isEqualTo(
                BrevmottakerDto(
                    id = 7,
                    type = MottakerType.DØDSBO,
                    navn = "Dødsbo Dødsbosen",
                    adresselinje1 = "Adresse 1",
                    adresselinje2 = "Adresse 2",
                    postnummer = "12345",
                    poststed = "München",
                    landkode = "DE",
                ),
            )
        }
    }
}
