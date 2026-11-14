package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.datagenerator.lagSøknadDTO
import no.nav.familie.ba.sak.datagenerator.randomFnr
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RegistrerSøknadDtoTest {
    @Test
    fun `skal kaste feil når et barn som er valgt i søknaden har ugyldig ident`() {
        // Arrange
        val søknad = lagSøknadDTO(søkerIdent = randomFnr(), barnasIdenter = listOf(randomFnr(), "12345678901 suid=Z999999"))

        // Act & Assert
        assertThrows<IllegalStateException> {
            RegistrerSøknadDto(søknad = søknad, bekreftEndringerViaFrontend = false)
        }
    }

    @Test
    fun `skal kun validere identen til barn som er valgt i søknaden`() {
        // Arrange
        val valgtBarnIdent = randomFnr()
        val søknad =
            lagSøknadDTO(søkerIdent = randomFnr(), barnasIdenter = listOf(valgtBarnIdent)).let {
                it.copy(
                    barnaMedOpplysninger =
                        it.barnaMedOpplysninger +
                            listOf(
                                BarnMedOpplysninger(ident = "", erFolkeregistrert = false),
                                BarnMedOpplysninger(ident = "SKJERMET BARN 1", inkludertISøknaden = false),
                            ),
                )
            }

        // Act
        val registrerSøknadDto = RegistrerSøknadDto(søknad = søknad, bekreftEndringerViaFrontend = false)

        // Assert
        assertThat(registrerSøknadDto.søknad.hentIdenterTilValgteBarn()).containsExactly(valgtBarnIdent)
    }
}
