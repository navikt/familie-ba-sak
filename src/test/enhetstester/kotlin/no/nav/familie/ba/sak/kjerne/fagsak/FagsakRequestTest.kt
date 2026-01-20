package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.ekstern.restDomene.InstitusjonDto
import no.nav.familie.ba.sak.ekstern.restDomene.SkjermetBarnSøkerDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class FagsakRequestTest {
    @Nested
    inner class Valider {
        @Test
        fun `skal kaste exception om det er et ugydlig fødselsnummer`() {
            // Act & assert
            val exception =
                assertThrows<IllegalStateException> {
                    FagsakRequest(
                        "",
                        fagsakType = FagsakType.NORMAL,
                        institusjon = null,
                        skjermetBarnSøker = null,
                    )
                }
            assertThat(exception.message).isEmpty()
        }

        @Test
        fun `skal ikke kaste exception om request er gyldig for normal fagsaktype`() {
            // Arrange
            val fagsakRequest =
                FagsakRequest(
                    "21100426738",
                    fagsakType = FagsakType.NORMAL,
                    institusjon = null,
                    skjermetBarnSøker = null,
                )

            // Act & assert
            assertDoesNotThrow { fagsakRequest.valider() }
        }

        @Test
        fun `skal ikke kaste exception om request er gyldig for institusjon fagsaktype`() {
            // Arrange
            val fagsakRequest =
                FagsakRequest(
                    "21100426738",
                    fagsakType = FagsakType.INSTITUSJON,
                    institusjon =
                        InstitusjonDto(
                            orgNummer = "889640782",
                            tssEksternId = "321",
                            navn = "orgnavn",
                        ),
                    skjermetBarnSøker = null,
                )

            // Act & assert
            assertDoesNotThrow { fagsakRequest.valider() }
        }

        @Test
        fun `skal ikke kaste exception om request er gyldig for skjermet fagsaktype`() {
            // Arrange
            val fagsakRequest =
                FagsakRequest(
                    "21100426738",
                    fagsakType = FagsakType.SKJERMET_BARN,
                    institusjon = null,
                    skjermetBarnSøker = SkjermetBarnSøkerDto(søkersIdent = "25050508792"),
                )

            // Act & assert
            assertDoesNotThrow { fagsakRequest.valider() }
        }

        @Test
        fun `skal kaste exception om man har fagsaktype institusjon uten institusjon`() {
            // Arrange
            val fagsakRequest =
                FagsakRequest(
                    "21100426738",
                    fagsakType = FagsakType.INSTITUSJON,
                    institusjon = null,
                    skjermetBarnSøker = null,
                )

            // Act & assert
            val exception = assertThrows<FunksjonellFeil> { fagsakRequest.valider() }
            assertThat(exception.message).isEqualTo("Institusjon mangler for fagsaktype institusjon.")
        }

        @Test
        fun `skal kaste exception om orgnummer er ugydlig for fagsaktype institusjon`() {
            // Arrange
            val fagsakRequest =
                FagsakRequest(
                    "21100426738",
                    fagsakType = FagsakType.INSTITUSJON,
                    institusjon =
                        InstitusjonDto(
                            orgNummer = "1",
                            tssEksternId = "321",
                            navn = "orgnavn",
                        ),
                    skjermetBarnSøker = null,
                )

            // Act & assert
            val exception = assertThrows<FunksjonellFeil> { fagsakRequest.valider() }
            assertThat(exception.message).isEqualTo("Organisasjonsnummeret er ugyldig.")
        }

        @ParameterizedTest
        @EnumSource(
            value = FagsakType::class,
            names = ["INSTITUSJON"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal kaste exception om man har institusjon med en annen fagsaktype enn institusjon`(fagsakType: FagsakType) {
            // Arrange
            val fagsakRequest =
                FagsakRequest(
                    "21100426738",
                    fagsakType = fagsakType,
                    institusjon =
                        InstitusjonDto(
                            orgNummer = "889640782",
                            tssEksternId = "321",
                            navn = "orgnavn",
                        ),
                    skjermetBarnSøker = null,
                )

            // Act & assert
            val exception = assertThrows<FunksjonellFeil> { fagsakRequest.valider() }
            assertThat(exception.message).isEqualTo("Forventer ikke at institusjon er satt for en annen fagsaktype enn institusjon.")
        }

        @Test
        fun `skal kaste exception om man har fagsaktype skjermet uten skjermet barn søker`() {
            // Arrange
            val fagsakRequest =
                FagsakRequest(
                    "21100426738",
                    fagsakType = FagsakType.SKJERMET_BARN,
                    institusjon = null,
                    skjermetBarnSøker = null,
                )

            // Act & assert
            val exception = assertThrows<FunksjonellFeil> { fagsakRequest.valider() }
            assertThat(exception.message).isEqualTo("Mangler informasjon om skjermet barn søker.")
        }

        @ParameterizedTest
        @EnumSource(
            value = FagsakType::class,
            names = ["SKJERMET_BARN"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal kaste exception om man har skjermet barn søker med en annen fagsaktype enn skjermet`(fagsakType: FagsakType) {
            // Arrange
            val institusjon =
                InstitusjonDto(
                    orgNummer = "889640782",
                    tssEksternId = "321",
                    navn = "orgnavn",
                )

            val fagsakRequest =
                FagsakRequest(
                    "21100426738",
                    fagsakType = fagsakType,
                    institusjon = if (fagsakType == FagsakType.INSTITUSJON) institusjon else null,
                    skjermetBarnSøker = SkjermetBarnSøkerDto(søkersIdent = "25050508792"),
                )

            // Act & assert
            val exception = assertThrows<FunksjonellFeil> { fagsakRequest.valider() }
            assertThat(exception.message).isEqualTo("Forventer ikke at skjermet barn søker er satt for en annen fagsaktype enn skjermet.")
        }

        @Test
        fun `skal kaste exception om man har fagsaktype skjermet og skjermet barn søker har et ugydlig fødselsnummer`() {
            // Arrange
            val fagsakRequest =
                FagsakRequest(
                    "21100426738",
                    fagsakType = FagsakType.SKJERMET_BARN,
                    institusjon = null,
                    skjermetBarnSøker = SkjermetBarnSøkerDto(søkersIdent = "123"),
                )

            // Act & assert
            val exception = assertThrows<FunksjonellFeil> { fagsakRequest.valider() }
            assertThat(exception.message).isEqualTo("Ugyldig fødselsnummer.")
        }
    }
}
