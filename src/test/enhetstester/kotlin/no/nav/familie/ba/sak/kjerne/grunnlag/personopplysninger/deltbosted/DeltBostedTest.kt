package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.deltbosted

import io.mockk.mockk
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.deltbosted.GrDeltBosted
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.deltbosted.GrDeltBosted.Companion.fraDeltBosted
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.deltbosted.GrMatrikkeladresseDeltBosted
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.deltbosted.GrUkjentBostedDeltBosted
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.deltbosted.GrVegadresseDeltBosted
import no.nav.familie.kontrakter.felles.personopplysning.DeltBosted
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.UkjentBosted
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class DeltBostedTest {
    private val person1 = mockk<Person>()
    private val person2 = mockk<Person>()
    private val fom = LocalDate.of(2024, 1, 1)
    private val tom = LocalDate.of(2024, 12, 31)
    private val vegadresse =
        Vegadresse(
            matrikkelId = 12345L,
            husnummer = "10",
            husbokstav = "A",
            bruksenhetsnummer = "H101",
            adressenavn = "Testgata",
            kommunenummer = "0301",
            tilleggsnavn = "Bak butikken",
            postnummer = "0123",
        )

    private val matrikkeladresse =
        Matrikkeladresse(
            matrikkelId = 67890L,
            bruksenhetsnummer = "H202",
            tilleggsnavn = "Ved skogen",
            postnummer = "1234",
            kommunenummer = "0219",
        )

    private val ukjentBosted =
        UkjentBosted(bostedskommune = "0301")

    private val deltBosted = DeltBosted(startdatoForKontrakt = fom, sluttdatoForKontrakt = tom)

    @Nested
    inner class FraOppholdsadresse {
        @Test
        fun `fraDeltBosted skal returnere GrVegadresse når delt bosted har vegadresse`() {
            // Arrange
            val deltBosted = deltBosted.copy(vegadresse = vegadresse)

            // Act
            val grDeltBosted = fraDeltBosted(deltBosted, person1)

            // Assert
            assertThat(grDeltBosted).isInstanceOf(GrVegadresseDeltBosted::class.java)

            val grVegadresse = grDeltBosted as GrVegadresseDeltBosted
            assertDeltBosted(grVegadresse)
            assertVegadresse(grVegadresse)
        }

        @Test
        fun `fraDeltBosted skal returnere GrMatrikkeladresse når delt bosted har matrikkeladresse`() {
            // Arrange
            val deltBosted = deltBosted.copy(matrikkeladresse = matrikkeladresse)

            // Act
            val grDeltBosted = fraDeltBosted(deltBosted, person1)

            // Assert
            assertThat(grDeltBosted).isInstanceOf(GrMatrikkeladresseDeltBosted::class.java)

            val grMatrikkeladresse = grDeltBosted as GrMatrikkeladresseDeltBosted
            assertDeltBosted(grMatrikkeladresse)
            assertMatrikkeladresse(grMatrikkeladresse)
        }

        @Test
        fun `fraDeltBosted skal returnere GrUkjentBosted når delt bosted har ukjentBosted`() {
            // Arrange
            val deltBosted = deltBosted.copy(ukjentBosted = ukjentBosted)

            // Act
            val grDeltBosted = fraDeltBosted(deltBosted, person1)

            // Assert
            assertThat(grDeltBosted).isInstanceOf(GrUkjentBostedDeltBosted::class.java)

            val grUkjentBosted = grDeltBosted as GrUkjentBostedDeltBosted
            assertDeltBosted(grUkjentBosted)
            assertUkjentBosted(grUkjentBosted)
        }

        @Test
        fun `fraDeltBosted skal kaste feil når delt bosted ikke har noen adressetyper`() {
            // Act
            val feil =
                assertThrows<Feil> {
                    fraDeltBosted(deltBosted, person1)
                }

            // Assert
            assertThat(feil.message).isEqualTo("Vegadresse, matrikkeladresse og ukjent bosted har verdi null ved mapping fra delt bosted")
        }

        @Test
        fun `fraDeltBosted skal håndtere null verdier for startdatoForKontrakt og sluttdatoForKontrakt`() {
            // Arrange
            val deltBosted = deltBosted.copy(startdatoForKontrakt = null, sluttdatoForKontrakt = null, vegadresse = vegadresse)

            // Act
            val grDeltBosted = fraDeltBosted(deltBosted, person1)

            // Assert
            assertDeltBosted(grDeltBosted, forventetFom = null, forventetTom = null)
        }

        @Test
        fun `fraDeltBosted skal prioritere vegadresse over matrikkeladresse over ukjent bosted`() {
            // Arrange
            val deltBostedMedUkjentBosted =
                deltBosted.copy(ukjentBosted = ukjentBosted)

            val deltBostedMedMatrikkelOgUkjentBosted =
                deltBosted.copy(matrikkeladresse = matrikkeladresse, ukjentBosted = ukjentBosted)

            val deltBostedMedVegMatrikkelOgUkjentBosted =
                deltBosted.copy(vegadresse = vegadresse, matrikkeladresse = matrikkeladresse, ukjentBosted = ukjentBosted)

            // Act
            val grUkjentBosted = fraDeltBosted(deltBostedMedUkjentBosted, person1)
            val grMatrikkeladresse = fraDeltBosted(deltBostedMedMatrikkelOgUkjentBosted, person1)
            val grVegadresse = fraDeltBosted(deltBostedMedVegMatrikkelOgUkjentBosted, person1)

            // Assert
            assertThat(grUkjentBosted).isInstanceOf(GrUkjentBostedDeltBosted::class.java)
            assertThat(grMatrikkeladresse).isInstanceOf(GrMatrikkeladresseDeltBosted::class.java)
            assertThat(grVegadresse).isInstanceOf(GrVegadresseDeltBosted::class.java)
        }
    }

    private val grVegadresse =
        fraDeltBosted(
            deltBosted = deltBosted.copy(vegadresse = vegadresse),
            person = person1,
        ) as GrVegadresseDeltBosted

    private val grMatrikkeladresse =
        fraDeltBosted(
            deltBosted = deltBosted.copy(matrikkeladresse = matrikkeladresse),
            person = person1,
        ) as GrMatrikkeladresseDeltBosted

    private val grUkjentBosted =
        fraDeltBosted(
            deltBosted = deltBosted.copy(ukjentBosted = ukjentBosted),
            person = person1,
        ) as GrUkjentBostedDeltBosted

    @Nested
    inner class ToString {
        @Test
        fun `GrVegadresse toString skal returnere riktig format`() {
            // Arrange
            val vegadresse = grVegadresse

            // Act
            val result = vegadresse.toString()

            // Assert
            assertThat(result).isEqualTo("GrVegadresseDeltBosted(detaljer skjult)")
        }

        @Test
        fun `GrMatrikkeladresse toString skal returnere riktig format`() {
            // Arrange
            val matrikkeladresse = grMatrikkeladresse

            // Act
            val result = matrikkeladresse.toString()

            // Assert
            assertThat(result).isEqualTo("GrMatrikkeladresseDeltBosted(detaljer skjult)")
        }

        @Test
        fun `GrUkjentBosted toString skal returnere riktig format`() {
            // Arrange
            val ukjentBosted = grUkjentBosted

            // Act
            val result = ukjentBosted.toString()
            // Assert
            assertThat(result).isEqualTo("GrUkjentBostedDeltBosted(detaljer skjult)")
        }
    }

    @Nested
    inner class ToSecureString {
        @Test
        fun `GrVegadresse toSecureString skal returnere riktig format`() {
            // Arrange
            val vegadresse = grVegadresse.copy(poststed = "Oslo")

            // Act
            val result = vegadresse.toSecureString()

            // Assert
            assertThat(result).isEqualTo(
                "GrVegadresseDeltBosted(husnummer=10, husbokstav=A, matrikkelId=12345, " +
                    "bruksenhetsnummer=H101, adressenavn=Testgata, kommunenummer=0301, " +
                    "tilleggsnavn=Bak butikken, postnummer=0123, poststed=Oslo)",
            )
        }

        @Test
        fun `GrMatrikkeladresse toSecureString skal returnere riktig format`() {
            // Arrange
            val matrikkeladresse = grMatrikkeladresse.copy(poststed = "Oslo")

            // Act
            val result = matrikkeladresse.toSecureString()

            // Assert
            assertThat(result).isEqualTo(
                "GrMatrikkeladresseDeltBosted(matrikkelId=67890, bruksenhetsnummer=H202, " +
                    "tilleggsnavn=Ved skogen, postnummer=1234, poststed=Oslo, " +
                    "kommunenummer=0219)",
            )
        }

        @Test
        fun `GrUkjentBosted toSecureString skal returnere riktig format`() {
            // Arrange
            val ukjentBosted = grUkjentBosted.copy()

            // Act
            val result = ukjentBosted.toSecureString()

            // Assert
            assertThat(result).isEqualTo(
                "GrUkjentBostedDeltBosted(bostedskommune=0301)",
            )
        }
    }

    @Nested
    inner class TilFrontendString {
        @Test
        fun `GrVegadresse tilFrontendString uten adressenavn`() {
            // Arrange
            val vegadresse = grVegadresse.copy(adressenavn = null)

            // Act
            val result = vegadresse.tilFrontendString()

            // Assert
            assertThat(result).isEqualTo("Ukjent adresse")
        }

        @Test
        fun `GrMatrikkeladresse tilFrontendString med postnummer skal returnere formatert streng`() {
            // Arrange
            val matrikkeladresse = grMatrikkeladresse

            // Act
            val result = matrikkeladresse.tilFrontendString()

            // Assert
            assertThat(result).isEqualTo("Postnummer 1234")
        }

        @Test
        fun `GrMatrikkeladresse tilFrontendString uten postnummer skal returnere 'Ukjent adresse'`() {
            // Arrange
            val matrikkeladresse = grMatrikkeladresse.copy(postnummer = null)

            // Act
            val result = matrikkeladresse.tilFrontendString()

            // Assert
            assertThat(result).isEqualTo("Ukjent adresse")
        }

        @Test
        fun `GrUkjentBosted tilFrontendString med komplett adresse skal returnere formatert streng`() {
            // Arrange
            val ukjentBosted = grUkjentBosted

            // Act
            val result = ukjentBosted.tilFrontendString()

            // Assert
            assertThat(result).isEqualTo("Ukjent adresse, kommune 0301")
        }
    }

    @Nested
    inner class TilKopiForNyPerson {
        @Test
        fun `GrUkjentAdresse tilKopiForNyPerson skal returnere ny instans med samme data`() {
            // Arrange
            val originalAdresse =
                grUkjentBosted.copy().apply {
                    periode = DatoIntervallEntitet(fom, tom)
                    person = person1
                }

            // Act
            val kopiForNyPerson = originalAdresse.tilKopiForNyPerson(person2)

            // Assert
            assertThat(kopiForNyPerson).isInstanceOf(GrUkjentBostedDeltBosted::class.java)
            assertThat(kopiForNyPerson).isNotSameAs(originalAdresse)
            assertDeltBosted(kopiForNyPerson, forventetPerson = person2)
        }

        @Test
        fun `GrVegadresse tilKopiForNyPerson skal returnere ny instans med samme data`() {
            // Arrange
            val originalAdresse =
                grVegadresse.copy().apply {
                    periode = DatoIntervallEntitet(fom, tom)
                    person = person1
                }

            // Act
            val kopiForNyPerson = originalAdresse.tilKopiForNyPerson(person2)

            // Assert
            assertThat(kopiForNyPerson).isInstanceOf(GrVegadresseDeltBosted::class.java)
            assertThat(kopiForNyPerson).isNotSameAs(originalAdresse)

            val kopierteVegadresse = kopiForNyPerson as GrVegadresseDeltBosted
            assertVegadresse(kopierteVegadresse)
            assertDeltBosted(kopierteVegadresse, forventetPerson = person2)
        }

        @Test
        fun `GrMatrikkeladresse tilKopiForNyPerson skal returnere ny instans med samme data`() {
            // Arrange
            val originalAdresse =
                grMatrikkeladresse.copy().apply {
                    periode = DatoIntervallEntitet(fom, tom)
                    person = person1
                }

            // Act
            val kopiForNyPerson = originalAdresse.tilKopiForNyPerson(person2)

            // Assert
            assertThat(kopiForNyPerson).isInstanceOf(GrMatrikkeladresseDeltBosted::class.java)
            assertThat(kopiForNyPerson).isNotSameAs(originalAdresse)

            val kopierteMatrikkeladresse = kopiForNyPerson as GrMatrikkeladresseDeltBosted
            assertMatrikkeladresse(kopierteMatrikkeladresse)
            assertDeltBosted(kopierteMatrikkeladresse, forventetPerson = person2)
        }

        @Test
        fun `GrUkjentBosted tilKopiForNyPerson skal returnere ny instans med samme data`() {
            // Arrange
            val originalAdresse =
                grUkjentBosted.copy().apply {
                    periode = DatoIntervallEntitet(fom, tom)
                    person = person1
                }

            // Act
            val kopiForNyPerson = originalAdresse.tilKopiForNyPerson(person2)

            // Assert
            assertThat(kopiForNyPerson).isInstanceOf(GrUkjentBostedDeltBosted::class.java)
            assertThat(kopiForNyPerson).isNotSameAs(originalAdresse)

            val kopiertUkjentBosted = kopiForNyPerson as GrUkjentBostedDeltBosted
            assertUkjentBosted(kopiertUkjentBosted)
            assertDeltBosted(kopiertUkjentBosted, forventetPerson = person2)
        }

        @Test
        fun `tilKopiForNyPerson skal håndtere null verdier korrekt`() {
            // Arrange
            val originalAdresse =
                GrVegadresseDeltBosted(
                    matrikkelId = null,
                    husnummer = null,
                    husbokstav = null,
                    bruksenhetsnummer = null,
                    adressenavn = null,
                    kommunenummer = null,
                    tilleggsnavn = null,
                    postnummer = null,
                    poststed = null,
                ).apply {
                    periode = null
                    person = person1
                }

            // Act
            val kopiForNyPerson = originalAdresse.tilKopiForNyPerson(person2)

            // Assert
            assertDeltBosted(kopiForNyPerson, forventetPerson = person2, forventetFom = null, forventetTom = null)
        }
    }

    fun assertDeltBosted(
        grDeltBosted: GrDeltBosted,
        forventetPerson: Person = person1,
        forventetFom: LocalDate? = fom,
        forventetTom: LocalDate? = tom,
    ) {
        assertThat(grDeltBosted.person).isEqualTo(forventetPerson)
        assertThat(grDeltBosted.periode?.fom).isEqualTo(forventetFom)
        assertThat(grDeltBosted.periode?.tom).isEqualTo(forventetTom)
    }

    fun assertVegadresse(
        grVegadresse: GrVegadresseDeltBosted,
        forventetMatrikkelId: Long? = vegadresse.matrikkelId,
        forventetHusnummer: String? = vegadresse.husnummer,
        forventetHusbokstav: String? = vegadresse.husbokstav,
        forventetBruksenhetsnummer: String? = vegadresse.bruksenhetsnummer,
        forventetAdressename: String? = vegadresse.adressenavn,
        forventetKommunenummer: String? = vegadresse.kommunenummer,
        forventetTilleggsnavn: String? = vegadresse.tilleggsnavn,
        forventetPostnummer: String? = vegadresse.postnummer,
    ) {
        assertThat(grVegadresse.matrikkelId).isEqualTo(forventetMatrikkelId)
        assertThat(grVegadresse.husnummer).isEqualTo(forventetHusnummer)
        assertThat(grVegadresse.husbokstav).isEqualTo(forventetHusbokstav)
        assertThat(grVegadresse.bruksenhetsnummer).isEqualTo(forventetBruksenhetsnummer)
        assertThat(grVegadresse.adressenavn).isEqualTo(forventetAdressename)
        assertThat(grVegadresse.kommunenummer).isEqualTo(forventetKommunenummer)
        assertThat(grVegadresse.tilleggsnavn).isEqualTo(forventetTilleggsnavn)
        assertThat(grVegadresse.postnummer).isEqualTo(forventetPostnummer)
    }

    fun assertMatrikkeladresse(
        grMatrikkeladresse: GrMatrikkeladresseDeltBosted,
        forventetMatrikkelId: Long? = matrikkeladresse.matrikkelId,
        forventetBruksenhetsnummer: String? = matrikkeladresse.bruksenhetsnummer,
        forventetTilleggsnavn: String? = matrikkeladresse.tilleggsnavn,
        forventetPostnummer: String? = matrikkeladresse.postnummer,
        forventetKommunenummer: String? = matrikkeladresse.kommunenummer,
    ) {
        assertThat(grMatrikkeladresse.matrikkelId).isEqualTo(forventetMatrikkelId)
        assertThat(grMatrikkeladresse.bruksenhetsnummer).isEqualTo(forventetBruksenhetsnummer)
        assertThat(grMatrikkeladresse.tilleggsnavn).isEqualTo(forventetTilleggsnavn)
        assertThat(grMatrikkeladresse.postnummer).isEqualTo(forventetPostnummer)
        assertThat(grMatrikkeladresse.kommunenummer).isEqualTo(forventetKommunenummer)
    }

    fun assertUkjentBosted(
        grUkjentBosted: GrUkjentBostedDeltBosted,
        forventetBostedskommune: String? = ukjentBosted.bostedskommune,
    ) {
        assertThat(grUkjentBosted.bostedskommune).isEqualTo(forventetBostedskommune)
    }
}
