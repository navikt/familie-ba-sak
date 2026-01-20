package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.common.FunksjonellFeil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class InstitusjonDtoTest {
    @Nested
    inner class Valider {
        @Test
        fun `skal ikke kaste exception om objektet er gydlig`() {
            // Arrange
            val institusjonDto =
                InstitusjonDto(
                    orgNummer = "889640782",
                    tssEksternId = "1",
                )

            // Act & assert
            assertDoesNotThrow { institusjonDto.valider() }
        }

        @Test
        fun `skal kaste exception orgnummer er null`() {
            // Arrange
            val institusjonDto =
                InstitusjonDto(
                    orgNummer = null,
                    tssEksternId = "1",
                )

            // Act & assert
            val exception = assertThrows<FunksjonellFeil> { institusjonDto.valider() }
            assertThat(exception.message).isEqualTo("Mangler organisasjonsnummer.")
        }

        @Test
        fun `skal kaste exception orgnummer er blank`() {
            // Arrange
            val institusjonDto =
                InstitusjonDto(
                    orgNummer = "",
                    tssEksternId = "1",
                )

            // Act & assert
            val exception = assertThrows<FunksjonellFeil> { institusjonDto.valider() }
            assertThat(exception.message).isEqualTo("Mangler organisasjonsnummer.")
        }

        @Test
        fun `skal kaste exception orgnummer er ugyldig`() {
            // Arrange
            val institusjonDto =
                InstitusjonDto(
                    orgNummer = "1",
                    tssEksternId = "1",
                )

            // Act & assert
            val exception = assertThrows<FunksjonellFeil> { institusjonDto.valider() }
            assertThat(exception.message).isEqualTo("Organisasjonsnummeret er ugyldig.")
        }

        @Test
        fun `skal kaste exception om tssEksternId er null`() {
            // Arrange
            val institusjonDto =
                InstitusjonDto(
                    orgNummer = "889640782",
                    tssEksternId = null,
                )

            // Act & assert
            val exception = assertThrows<FunksjonellFeil> { institusjonDto.valider() }
            assertThat(exception.message).isEqualTo("Mangler tssEksternId.")
        }

        @Test
        fun `skal kaste exception om tssEksternId er blank`() {
            // Arrange
            val institusjonDto =
                InstitusjonDto(
                    orgNummer = "889640782",
                    tssEksternId = "",
                )

            // Act & assert
            val exception = assertThrows<FunksjonellFeil> { institusjonDto.valider() }
            assertThat(exception.message).isEqualTo("Mangler tssEksternId.")
        }
    }
}
