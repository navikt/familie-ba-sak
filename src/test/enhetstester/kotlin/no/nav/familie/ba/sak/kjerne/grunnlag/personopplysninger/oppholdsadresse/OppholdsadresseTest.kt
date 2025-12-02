package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.oppholdsadresse

import io.mockk.mockk
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.oppholdsadresse.GrMatrikkeladresseOppholdsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.oppholdsadresse.GrOppholdsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.oppholdsadresse.GrUkjentAdresseOppholdsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.oppholdsadresse.GrUtenlandskAdresseOppholdsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.oppholdsadresse.GrVegadresseOppholdsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted
import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted.PAA_SVALBARD
import no.nav.familie.kontrakter.felles.personopplysning.Oppholdsadresse
import no.nav.familie.kontrakter.felles.personopplysning.UtenlandskAdresse
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE
import java.time.LocalDate

class OppholdsadresseTest {
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

    private val utenlandskAdresse =
        UtenlandskAdresse(
            adressenavnNummer = "123 Foreign St",
            bygningEtasjeLeilighet = "Apt 4B",
            postboksNummerNavn = "PO Box 567",
            postkode = "10001",
            bySted = "New York",
            regionDistriktOmraade = "NY",
            landkode = "USA",
        )

    private val oppholdsadresse = Oppholdsadresse(gyldigFraOgMed = fom, gyldigTilOgMed = tom)

    @Nested
    inner class FraOppholdsadresse {
        @ParameterizedTest
        @EnumSource(OppholdAnnetSted::class)
        fun `skal håndtere enum name og enum kode for oppholdAnnetSted`(
            oppholdAnnetSted: OppholdAnnetSted,
        ) {
            oppholdsadresse.copy(oppholdAnnetSted = oppholdAnnetSted.name).also {
                val grOppholdsadresse = GrOppholdsadresse.fraOppholdsadresse(it, person1)
                assertOppholdsadresse(grOppholdsadresse, forventetOppholdAnnetSted = oppholdAnnetSted)
            }

            oppholdsadresse.copy(oppholdAnnetSted = oppholdAnnetSted.kode).also {
                val grOppholdsadresse = GrOppholdsadresse.fraOppholdsadresse(it, person1)
                assertOppholdsadresse(grOppholdsadresse, forventetOppholdAnnetSted = oppholdAnnetSted)
            }
        }

        @Test
        fun `fraOppholdsadresse skal returnere GrVegadresse når oppholdsadresse har vegadresse`() {
            // Arrange
            val oppholdsadresse = oppholdsadresse.copy(vegadresse = vegadresse)

            // Act
            val grOppholdsadresse = GrOppholdsadresse.fraOppholdsadresse(oppholdsadresse, person1)

            // Assert
            assertThat(grOppholdsadresse).isInstanceOf(GrVegadresseOppholdsadresse::class.java)

            val grVegadresse = grOppholdsadresse as GrVegadresseOppholdsadresse
            assertOppholdsadresse(grVegadresse)
            assertVegadresse(grVegadresse)
        }

        @Test
        fun `fraOppholdsadresse skal returnere GrMatrikkeladresse når oppholdsadresse har matrikkeladresse`() {
            // Arrange
            val oppholdsadresse = oppholdsadresse.copy(matrikkeladresse = matrikkeladresse)

            // Act
            val grOppholdsadresse = GrOppholdsadresse.fraOppholdsadresse(oppholdsadresse, person1)

            // Assert
            assertThat(grOppholdsadresse).isInstanceOf(GrMatrikkeladresseOppholdsadresse::class.java)

            val grMatrikkeladresse = grOppholdsadresse as GrMatrikkeladresseOppholdsadresse
            assertOppholdsadresse(grMatrikkeladresse)
            assertMatrikkeladresse(grMatrikkeladresse)
        }

        @Test
        fun `fraOppholdsadresse skal returnere GrUtenlandskAdresse når oppholdsadresse har utenlandskAdresse`() {
            // Arrange
            val oppholdsadresse = oppholdsadresse.copy(utenlandskAdresse = utenlandskAdresse)

            // Act
            val grOppholdsadresse = GrOppholdsadresse.fraOppholdsadresse(oppholdsadresse, person1)

            // Assert
            assertThat(grOppholdsadresse).isInstanceOf(GrUtenlandskAdresseOppholdsadresse::class.java)

            val grUtenlandskAdresse = grOppholdsadresse as GrUtenlandskAdresseOppholdsadresse
            assertOppholdsadresse(grUtenlandskAdresse)
            assertUtenlandskAdresse(grUtenlandskAdresse)
        }

        @Test
        fun `fraOppholdsadresse skal returnere GrUkjentAdresse når oppholdsadresse ikke har noen adressetyper`() {
            // Arrange
            val oppholdsadresse = oppholdsadresse

            // Act
            val grOppholdsadresse = GrOppholdsadresse.fraOppholdsadresse(oppholdsadresse, person1)

            // Assert
            assertThat(grOppholdsadresse).isInstanceOf(GrUkjentAdresseOppholdsadresse::class.java)
            assertOppholdsadresse(grOppholdsadresse)
        }

        @Test
        fun `fraOppholdsadresse skal håndtere null verdier for gyldigFraOgMed og gyldigTilOgMed`() {
            // Arrange
            val oppholdsadresse = oppholdsadresse.copy(gyldigFraOgMed = null, gyldigTilOgMed = null)

            // Act
            val grOppholdsadresse = GrOppholdsadresse.fraOppholdsadresse(oppholdsadresse, person1)

            // Assert
            assertOppholdsadresse(grOppholdsadresse, forventetFom = null, forventetTom = null)
        }

        @Test
        fun `fraOppholdsadresse skal håndtere ukjent oppholdAnnetSted verdi`() {
            // Arrange
            val oppholdsadresse = oppholdsadresse.copy(oppholdAnnetSted = "ukjentVerdi")

            // Act
            val grOppholdsadresse = GrOppholdsadresse.fraOppholdsadresse(oppholdsadresse, person1)

            // Assert
            assertThat(grOppholdsadresse.oppholdAnnetSted).isNull()
        }

        @Test
        fun `fraOppholdsadresse skal prioritere vegadresse over matrikkeladresse over utenlandsk adresse`() {
            // Arrange
            val oppholdsadresseMedUtenlandskAdresse =
                oppholdsadresse.copy(utenlandskAdresse = utenlandskAdresse)

            val oppholdsadresseMedMatrikkelOgUtenlandskAdresse =
                oppholdsadresse.copy(matrikkeladresse = matrikkeladresse, utenlandskAdresse = utenlandskAdresse)

            val oppholdsadresseMedVegMatrikkelOgUtenlandskAdresse =
                oppholdsadresse.copy(vegadresse = vegadresse, matrikkeladresse = matrikkeladresse, utenlandskAdresse = utenlandskAdresse)

            // Act
            val grUtenlandskAdresse = GrOppholdsadresse.fraOppholdsadresse(oppholdsadresseMedUtenlandskAdresse, person1)
            val grMatrikkeladresse = GrOppholdsadresse.fraOppholdsadresse(oppholdsadresseMedMatrikkelOgUtenlandskAdresse, person1)
            val grVegadresse = GrOppholdsadresse.fraOppholdsadresse(oppholdsadresseMedVegMatrikkelOgUtenlandskAdresse, person1)

            // Assert
            assertThat(grUtenlandskAdresse).isInstanceOf(GrUtenlandskAdresseOppholdsadresse::class.java)
            assertThat(grMatrikkeladresse).isInstanceOf(GrMatrikkeladresseOppholdsadresse::class.java)
            assertThat(grVegadresse).isInstanceOf(GrVegadresseOppholdsadresse::class.java)
        }
    }

    private val grVegadresse =
        GrOppholdsadresse.fraOppholdsadresse(
            oppholdsadresse = oppholdsadresse.copy(vegadresse = vegadresse),
            person = person1,
        ) as GrVegadresseOppholdsadresse

    private val grMatrikkeladresse =
        GrOppholdsadresse.fraOppholdsadresse(
            oppholdsadresse = oppholdsadresse.copy(matrikkeladresse = matrikkeladresse),
            person = person1,
        ) as GrMatrikkeladresseOppholdsadresse

    private val grUtenlandskAdresse =
        GrOppholdsadresse.fraOppholdsadresse(
            oppholdsadresse = oppholdsadresse.copy(utenlandskAdresse = utenlandskAdresse),
            person = person1,
        ) as GrUtenlandskAdresseOppholdsadresse

    @Nested
    inner class ToString {
        @Test
        fun `GrUkjentAdresse toString skal returnere riktig format`() {
            // Arrange
            val ukjentAdresse = GrUkjentAdresseOppholdsadresse()

            // Act
            val result = ukjentAdresse.toString()

            // Assert
            assertThat(result).isEqualTo("GrUkjentAdresseOppholdsadresse(detaljer skjult)")
        }

        @Test
        fun `GrVegadresse toString skal returnere riktig format`() {
            // Arrange
            val vegadresse = grVegadresse

            // Act
            val result = vegadresse.toString()

            // Assert
            assertThat(result).isEqualTo("GrVegadresseOppholdsadresse(detaljer skjult)")
        }

        @Test
        fun `GrMatrikkeladresse toString skal returnere riktig format`() {
            // Arrange
            val matrikkeladresse = grMatrikkeladresse

            // Act
            val result = matrikkeladresse.toString()

            // Assert
            assertThat(result).isEqualTo("GrMatrikkeladresseOppholdsadresse(detaljer skjult)")
        }

        @Test
        fun `GrUtenlandskAdresse toString skal returnere riktig format`() {
            // Arrange
            val utenlandskAdresse = grUtenlandskAdresse

            // Act
            val result = utenlandskAdresse.toString()
            // Assert
            assertThat(result).isEqualTo("GrUtenlandskAdresseOppholdsadresse(detaljer skjult)")
        }
    }

    @Nested
    inner class ToSecureString {
        @Test
        fun `GrUkjentAdresse toSecureString skal returnere riktig format`() {
            // Arrange
            val ukjentAdresse = GrUkjentAdresseOppholdsadresse().apply { oppholdAnnetSted = PAA_SVALBARD }

            // Act
            val result = ukjentAdresse.toSecureString()

            // Assert
            assertThat(result).isEqualTo("GrUkjentAdresseOppholdsadresse(Svalbard)")
        }

        @Test
        fun `GrUkjentAdresse toSecureString uten oppholdAnnetSted skal returnere riktig format`() {
            // Arrange
            val ukjentAdresse = GrUkjentAdresseOppholdsadresse()

            // Act
            val result = ukjentAdresse.toSecureString()

            // Assert
            assertThat(result).isEqualTo("GrUkjentAdresseOppholdsadresse()")
        }

        @Test
        fun `GrVegadresse toSecureString skal returnere riktig format`() {
            // Arrange
            val vegadresse = grVegadresse.copy(poststed = "Oslo").apply { oppholdAnnetSted = PAA_SVALBARD }

            // Act
            val result = vegadresse.toSecureString()

            // Assert
            assertThat(result).isEqualTo(
                "GrVegadresseOppholdsadresse(husnummer=10, husbokstav=A, matrikkelId=12345, " +
                    "bruksenhetsnummer=H101, adressenavn=Testgata, kommunenummer=0301, " +
                    "tilleggsnavn=Bak butikken, postnummer=0123, poststed=Oslo, oppholdAnnetSted=Svalbard)",
            )
        }

        @Test
        fun `GrMatrikkeladresse toSecureString skal returnere riktig format`() {
            // Arrange
            val matrikkeladresse = grMatrikkeladresse.copy(poststed = "Oslo").apply { oppholdAnnetSted = PAA_SVALBARD }

            // Act
            val result = matrikkeladresse.toSecureString()

            // Assert
            assertThat(result).isEqualTo(
                "GrMatrikkeladresseOppholdsadresse(matrikkelId=67890, bruksenhetsnummer=H202, " +
                    "tilleggsnavn=Ved skogen, postnummer=1234, poststed=Oslo, " +
                    "kommunenummer=0219, oppholdAnnetSted=Svalbard)",
            )
        }

        @Test
        fun `GrUtenlandskAdresse toSecureString skal returnere riktig format`() {
            // Arrange
            val utenlandskAdresse = grUtenlandskAdresse.copy().apply { oppholdAnnetSted = PAA_SVALBARD }

            // Act
            val result = utenlandskAdresse.toSecureString()

            // Assert
            assertThat(result).isEqualTo(
                "GrUtenlandskAdresseOppholdsadresse(adressenavnNummer=123 Foreign St, " +
                    "bygningEtasjeLeilighet=Apt 4B, postboksNummerNavn=PO Box 567, " +
                    "postkode=10001, bySted=New York, regionDistriktOmraade=NY, " +
                    "landkode=USA, oppholdAnnetSted=Svalbard)",
            )
        }
    }

    @Nested
    inner class TilFrontendString {
        @Test
        fun `GrUkjentAdresse tilFrontendString uten oppholdAnnetSted skal returnere 'Ukjent adresse'`() {
            // Arrange
            val ukjentAdresse = GrUkjentAdresseOppholdsadresse()

            // Act
            val result = ukjentAdresse.tilFrontendString()

            // Assert
            assertThat(result).isEqualTo("Ukjent adresse")
        }

        @ParameterizedTest
        @EnumSource(value = OppholdAnnetSted::class, names = ["PAA_SVALBARD"], mode = EXCLUDE)
        fun `GrUkjentAdresse tilFrontendString med oppholdAnnetSted annet enn Svalbard skal ikke inkludere det`(
            opphold: OppholdAnnetSted,
        ) {
            // Arrange
            val ukjentAdresse = GrUkjentAdresseOppholdsadresse().apply { oppholdAnnetSted = opphold }

            // Act
            val result = ukjentAdresse.tilFrontendString()

            // Assert
            assertThat(result).isEqualTo("Ukjent adresse")
        }

        @Test
        fun `GrUkjentAdresse tilFrontendString med oppholdAnnetSted Svalbard skal inkludere det`() {
            // Arrange
            val ukjentAdresse = GrUkjentAdresseOppholdsadresse().apply { oppholdAnnetSted = PAA_SVALBARD }

            // Act
            val result = ukjentAdresse.tilFrontendString()

            // Assert
            assertThat(result).isEqualTo("Ukjent adresse, Svalbard")
        }

        @Test
        fun `GrVegadresse tilFrontendString uten adressenavn eller oppholdAnnetSted`() {
            // Arrange
            val vegadresse = grVegadresse.copy(adressenavn = null)

            // Act
            val result = vegadresse.tilFrontendString()

            // Assert
            assertThat(result).isEqualTo("Ukjent adresse")
        }

        @ParameterizedTest
        @EnumSource(value = OppholdAnnetSted::class, names = ["PAA_SVALBARD"], mode = EXCLUDE)
        fun `GrVegadresse tilFrontendString uten adressenavn med oppholdAnnetSted annet enn Svalbard skal ikke inkludere det`(
            opphold: OppholdAnnetSted,
        ) {
            // Arrange
            val vegadresse = grVegadresse.copy(adressenavn = null).apply { oppholdAnnetSted = opphold }

            // Act
            val result = vegadresse.tilFrontendString()

            // Assert
            assertThat(result).isEqualTo("Ukjent adresse")
        }

        @Test
        fun `GrVegadresse tilFrontendString uten adressenavn med oppholdAnnetSted Svalbard skal inkludere det`() {
            // Arrange
            val vegadresse = grVegadresse.copy(adressenavn = null).apply { oppholdAnnetSted = PAA_SVALBARD }

            // Act
            val result = vegadresse.tilFrontendString()

            // Assert
            assertThat(result).isEqualTo("Ukjent adresse, Svalbard")
        }

        @Test
        fun `GrVegadresse tilFrontendString med komplett adresse og oppholdAnnetSted Svalbard skal returnere formatert streng`() {
            // Arrange
            val vegadresse = grVegadresse.copy().apply { oppholdAnnetSted = PAA_SVALBARD }

            // Act
            val result = vegadresse.tilFrontendString()

            // Assert
            assertThat(result).isEqualTo("Testgata 10A, 0123, Svalbard")
        }

        @ParameterizedTest
        @EnumSource(value = OppholdAnnetSted::class, names = ["PAA_SVALBARD"], mode = EXCLUDE)
        fun `GrVegadresse tilFrontendString med komplett adresse og oppholdAnnetSted annet enn Svalbard skal returnere formatert streng`(
            opphold: OppholdAnnetSted,
        ) {
            // Arrange
            val vegadresse = grVegadresse.copy().apply { oppholdAnnetSted = opphold }

            // Act
            val result = vegadresse.tilFrontendString()

            // Assert
            assertThat(result).isEqualTo("Testgata 10A, 0123")
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
        fun `GrMatrikkeladresse tilFrontendString med postnummer og oppholdAnnetSted Svalbard skal returnere formatert streng`() {
            // Arrange
            val matrikkeladresse = grMatrikkeladresse.copy().apply { oppholdAnnetSted = PAA_SVALBARD }

            // Act
            val result = matrikkeladresse.tilFrontendString()

            // Assert
            assertThat(result).isEqualTo("Postnummer 1234, Svalbard")
        }

        @Test
        fun `GrMatrikkeladresse tilFrontendString uten postnummer eller oppholdAnnetSted skal returnere 'Ukjent adresse'`() {
            // Arrange
            val matrikkeladresse = grMatrikkeladresse.copy(postnummer = null)

            // Act
            val result = matrikkeladresse.tilFrontendString()

            // Assert
            assertThat(result).isEqualTo("Ukjent adresse")
        }

        @Test
        fun `GrMatrikkeladresse tilFrontendString uten postnummer med oppholdAnnetSted Svalbard skal returnere formatert streng`() {
            // Arrange
            val matrikkeladresse = grMatrikkeladresse.copy(postnummer = null).apply { oppholdAnnetSted = PAA_SVALBARD }

            // Act
            val result = matrikkeladresse.tilFrontendString()

            // Assert
            assertThat(result).isEqualTo("Ukjent adresse, Svalbard")
        }

        @Test
        fun `GrUtenlandskAdresse tilFrontendString med komplett adresse skal returnere formatert streng`() {
            // Arrange
            val utenlandskAdresse = grUtenlandskAdresse

            // Act
            val result = utenlandskAdresse.tilFrontendString()

            // Assert
            assertThat(result).isEqualTo("123 Foreign St, Apt 4B, PO Box 567, 10001, New York, NY, USA")
        }

        @Test
        fun `GrUtenlandskAdresse tilFrontendString uten adressenavnNummer skal returnere formatert streng`() {
            // Arrange
            val utenlandskAdresse = grUtenlandskAdresse.copy(adressenavnNummer = null)

            // Act
            val result = utenlandskAdresse.tilFrontendString()

            // Assert
            assertThat(result).isEqualTo("Ukjent utenlandsk adresse, USA")
        }
    }

    @Nested
    inner class TilKopiForNyPerson {
        @Test
        fun `GrUkjentAdresse tilKopiForNyPerson skal returnere ny instans med samme data`() {
            // Arrange
            val originalAdresse =
                GrUkjentAdresseOppholdsadresse().apply {
                    periode = DatoIntervallEntitet(fom, tom)
                    person = person1
                    oppholdAnnetSted = PAA_SVALBARD
                }

            // Act
            val kopiForNyPerson = originalAdresse.tilKopiForNyPerson(person2)

            // Assert
            assertThat(kopiForNyPerson).isInstanceOf(GrUkjentAdresseOppholdsadresse::class.java)
            assertThat(kopiForNyPerson).isNotSameAs(originalAdresse)
            assertOppholdsadresse(kopiForNyPerson, forventetPerson = person2, forventetOppholdAnnetSted = PAA_SVALBARD)
        }

        @Test
        fun `GrVegadresse tilKopiForNyPerson skal returnere ny instans med samme data`() {
            // Arrange
            val originalAdresse =
                grVegadresse.copy().apply {
                    periode = DatoIntervallEntitet(fom, tom)
                    person = person1
                    oppholdAnnetSted = PAA_SVALBARD
                }

            // Act
            val kopiForNyPerson = originalAdresse.tilKopiForNyPerson(person2)

            // Assert
            assertThat(kopiForNyPerson).isInstanceOf(GrVegadresseOppholdsadresse::class.java)
            assertThat(kopiForNyPerson).isNotSameAs(originalAdresse)

            val kopierteVegadresse = kopiForNyPerson as GrVegadresseOppholdsadresse
            assertVegadresse(kopierteVegadresse)
            assertOppholdsadresse(kopierteVegadresse, forventetPerson = person2, forventetOppholdAnnetSted = PAA_SVALBARD)
        }

        @Test
        fun `GrMatrikkeladresse tilKopiForNyPerson skal returnere ny instans med samme data`() {
            // Arrange
            val originalAdresse =
                grMatrikkeladresse.copy().apply {
                    periode = DatoIntervallEntitet(fom, tom)
                    person = person1
                    oppholdAnnetSted = PAA_SVALBARD
                }

            // Act
            val kopiForNyPerson = originalAdresse.tilKopiForNyPerson(person2)

            // Assert
            assertThat(kopiForNyPerson).isInstanceOf(GrMatrikkeladresseOppholdsadresse::class.java)
            assertThat(kopiForNyPerson).isNotSameAs(originalAdresse)

            val kopierteMatrikkeladresse = kopiForNyPerson as GrMatrikkeladresseOppholdsadresse
            assertMatrikkeladresse(kopierteMatrikkeladresse)
            assertOppholdsadresse(kopierteMatrikkeladresse, forventetPerson = person2, forventetOppholdAnnetSted = PAA_SVALBARD)
        }

        @Test
        fun `GrUtenlandskAdresse tilKopiForNyPerson skal returnere ny instans med samme data`() {
            // Arrange
            val originalAdresse =
                grUtenlandskAdresse.copy().apply {
                    periode = DatoIntervallEntitet(fom, tom)
                    person = person1
                    oppholdAnnetSted = PAA_SVALBARD
                }

            // Act
            val kopiForNyPerson = originalAdresse.tilKopiForNyPerson(person2)

            // Assert
            assertThat(kopiForNyPerson).isInstanceOf(GrUtenlandskAdresseOppholdsadresse::class.java)
            assertThat(kopiForNyPerson).isNotSameAs(originalAdresse)

            val kopierteUtenlandskAdresse = kopiForNyPerson as GrUtenlandskAdresseOppholdsadresse
            assertUtenlandskAdresse(kopierteUtenlandskAdresse)
            assertOppholdsadresse(kopierteUtenlandskAdresse, forventetPerson = person2, forventetOppholdAnnetSted = PAA_SVALBARD)
        }

        @Test
        fun `tilKopiForNyPerson skal håndtere null verdier korrekt`() {
            // Arrange
            val originalAdresse =
                GrVegadresseOppholdsadresse(
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
                    oppholdAnnetSted = null
                }

            // Act
            val kopiForNyPerson = originalAdresse.tilKopiForNyPerson(person2)

            // Assert
            assertOppholdsadresse(kopiForNyPerson, forventetPerson = person2, forventetFom = null, forventetTom = null)
        }
    }

    fun assertOppholdsadresse(
        grOppholdsadresse: GrOppholdsadresse,
        forventetPerson: Person = person1,
        forventetFom: LocalDate? = fom,
        forventetTom: LocalDate? = tom,
        forventetOppholdAnnetSted: OppholdAnnetSted? = null,
    ) {
        assertThat(grOppholdsadresse.person).isEqualTo(forventetPerson)
        assertThat(grOppholdsadresse.periode?.fom).isEqualTo(forventetFom)
        assertThat(grOppholdsadresse.periode?.tom).isEqualTo(forventetTom)
        assertThat(grOppholdsadresse.oppholdAnnetSted).isEqualTo(forventetOppholdAnnetSted)
    }

    fun assertVegadresse(
        grVegadresse: GrVegadresseOppholdsadresse,
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
        grMatrikkeladresse: GrMatrikkeladresseOppholdsadresse,
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

    fun assertUtenlandskAdresse(
        grUtenlandskAdresse: GrUtenlandskAdresseOppholdsadresse,
        forventetAdressenavnNummer: String? = grUtenlandskAdresse.adressenavnNummer,
        forventetBygningEtasjeLeilighet: String? = grUtenlandskAdresse.bygningEtasjeLeilighet,
        forventetPostkode: String? = grUtenlandskAdresse.postkode,
        forventetBySted: String? = grUtenlandskAdresse.bySted,
        forventetRegionDistriktOmraade: String? = grUtenlandskAdresse.regionDistriktOmraade,
        forventetLandkode: String? = grUtenlandskAdresse.landkode,
    ) {
        assertThat(grUtenlandskAdresse.adressenavnNummer).isEqualTo(forventetAdressenavnNummer)
        assertThat(grUtenlandskAdresse.bygningEtasjeLeilighet).isEqualTo(forventetBygningEtasjeLeilighet)
        assertThat(grUtenlandskAdresse.postkode).isEqualTo(forventetPostkode)
        assertThat(grUtenlandskAdresse.bySted).isEqualTo(forventetBySted)
        assertThat(grUtenlandskAdresse.regionDistriktOmraade).isEqualTo(forventetRegionDistriktOmraade)
        assertThat(grUtenlandskAdresse.landkode).isEqualTo(forventetLandkode)
    }
}
