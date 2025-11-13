package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse

import no.nav.familie.ba.sak.datagenerator.lagAdresse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AdresseKtTest {
    @Nested
    inner class HentForDato {
        @Test
        fun `skal hente adresse for dato'`() {
            // Arrange
            val dagensDato = LocalDate.of(2025, 9, 12)

            val adresse1 =
                lagAdresse(
                    gyldigFraOgMed = dagensDato.minusDays(2),
                    gyldigTilOgMed = dagensDato.minusDays(1),
                )

            val adresse2 =
                lagAdresse(
                    gyldigFraOgMed = null,
                    gyldigTilOgMed = dagensDato,
                )

            val adresse3 =
                lagAdresse(
                    gyldigFraOgMed = dagensDato.minusDays(1),
                    gyldigTilOgMed = dagensDato,
                )

            val adresse4 =
                lagAdresse(
                    gyldigFraOgMed = dagensDato,
                    gyldigTilOgMed = dagensDato.plusDays(1),
                )

            val adresse5 =
                lagAdresse(
                    gyldigFraOgMed = dagensDato.plusDays(1),
                    gyldigTilOgMed = dagensDato.plusDays(2),
                )

            val adresser = listOf(adresse1, adresse2, adresse3, adresse4, adresse5)

            // Act
            val hentetAdresse = adresser.hentForDato(dagensDato)

            // Assert
            assertThat(hentetAdresse).isEqualTo(adresse4)
        }
    }

    @Nested
    inner class FinnAdressehistorikkFraOgMedDato {
        @Test
        fun `skal finne adressehistorikk fra og med dato`() {
            // Arrange
            val dagensDato = LocalDate.of(2025, 9, 12)

            val adresse1 =
                lagAdresse(
                    gyldigFraOgMed = dagensDato.minusDays(2),
                    gyldigTilOgMed = dagensDato.minusDays(1),
                )

            val adresse2 =
                lagAdresse(
                    gyldigFraOgMed = null,
                    gyldigTilOgMed = dagensDato,
                )

            val adresse3 =
                lagAdresse(
                    gyldigFraOgMed = dagensDato.minusDays(1),
                    gyldigTilOgMed = dagensDato,
                )

            val adresse4 =
                lagAdresse(
                    gyldigFraOgMed = dagensDato,
                    gyldigTilOgMed = dagensDato.plusDays(1),
                )

            val adresse5 =
                lagAdresse(
                    gyldigFraOgMed = dagensDato.plusDays(1),
                    gyldigTilOgMed = dagensDato.plusDays(2),
                )

            val adresser = listOf(adresse1, adresse2, adresse3, adresse4, adresse5)

            // Act
            val funnetAdresser = finnAdressehistorikkFraOgMedDato(adresser, dagensDato)

            // Assert
            assertThat(funnetAdresser).hasSize(2)
            assertThat(funnetAdresser).containsAll(listOf(adresse4, adresse5))
        }

        @Test
        fun `skal finne adressehistorikk fra og med dato hvis alle addresser er etter dato`() {
            // Arrange
            val dagensDato = LocalDate.of(2025, 9, 30)

            val adresse =
                lagAdresse(
                    gyldigFraOgMed = dagensDato.plusDays(1),
                    gyldigTilOgMed = dagensDato.plusDays(2),
                )

            val adresser = listOf(adresse)

            // Act
            val funnetAdresser = finnAdressehistorikkFraOgMedDato(adresser, dagensDato)

            // Assert
            assertThat(funnetAdresser).hasSize(1)
            assertThat(funnetAdresser).containsExactly(adresse)
        }
    }
}
