package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.oppholdsadresse

import io.mockk.mockk
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.OppholdAnnetSted
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.Oppholdsadresse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlUtenlandskAdresssePersonUtenlandskAdresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

class OppholdsadresseTest {
    private val person = mockk<Person>()
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
        PdlUtenlandskAdresssePersonUtenlandskAdresse(
            adressenavnNummer = "123 Foreign St",
            bygningEtasjeLeilighet = "Apt 4B",
            postkode = "10001",
            bySted = "New York",
            regionDistriktOmraade = "NY",
            landkode = "USA",
        )

    @ParameterizedTest
    @EnumSource(OppholdAnnetSted::class)
    fun `skal håndtere enum name og enum kode for oppholdAnnetSted`(
        oppholdAnnetSted: OppholdAnnetSted,
    ) {
        Oppholdsadresse(oppholdAnnetSted = oppholdAnnetSted.name).also {
            val grOppholdsadresse = GrOppholdsadresse.fraOppholdsadresse(it, person)
            assertThat(grOppholdsadresse.oppholdAnnetSted).isEqualTo(oppholdAnnetSted)
        }

        Oppholdsadresse(oppholdAnnetSted = oppholdAnnetSted.kode).also {
            val grOppholdsadresse = GrOppholdsadresse.fraOppholdsadresse(it, person)
            assertThat(grOppholdsadresse.oppholdAnnetSted).isEqualTo(oppholdAnnetSted)
        }
    }

    @Test
    fun `fraOppholdsadresse skal returnere GrVegadresse når oppholdsadresse har vegadresse`() {
        // Arrange
        val oppholdsadresse =
            Oppholdsadresse(
                gyldigFraOgMed = fom,
                gyldigTilOgMed = tom,
                vegadresse = vegadresse,
            )

        // Act
        val grOppholdsadresse = GrOppholdsadresse.fraOppholdsadresse(oppholdsadresse, person)

        // Assert
        assertThat(grOppholdsadresse).isInstanceOf(GrVegadresse::class.java)

        val grVegadresse = grOppholdsadresse as GrVegadresse
        assertThat(grVegadresse.matrikkelId).isEqualTo(12345L)
        assertThat(grVegadresse.husnummer).isEqualTo("10")
        assertThat(grVegadresse.husbokstav).isEqualTo("A")
        assertThat(grVegadresse.bruksenhetsnummer).isEqualTo("H101")
        assertThat(grVegadresse.adressenavn).isEqualTo("Testgata")
        assertThat(grVegadresse.kommunenummer).isEqualTo("0301")
        assertThat(grVegadresse.tilleggsnavn).isEqualTo("Bak butikken")
        assertThat(grVegadresse.postnummer).isEqualTo("0123")

        assertThat(grVegadresse.person).isEqualTo(person)
        assertThat(grVegadresse.periode?.fom).isEqualTo(fom)
        assertThat(grVegadresse.periode?.tom).isEqualTo(tom)
    }

    @Test
    fun `fraOppholdsadresse skal returnere GrMatrikkeladresse når oppholdsadresse har matrikkeladresse`() {
        // Arrange
        val oppholdsadresse =
            Oppholdsadresse(
                gyldigFraOgMed = fom,
                gyldigTilOgMed = tom,
                matrikkeladresse = matrikkeladresse,
            )

        // Act
        val grOppholdsadresse = GrOppholdsadresse.fraOppholdsadresse(oppholdsadresse, person)

        // Assert
        assertThat(grOppholdsadresse).isInstanceOf(GrMatrikkeladresse::class.java)

        val grMatrikkeladresse = grOppholdsadresse as GrMatrikkeladresse
        assertThat(grMatrikkeladresse.matrikkelId).isEqualTo(67890L)
        assertThat(grMatrikkeladresse.bruksenhetsnummer).isEqualTo("H202")
        assertThat(grMatrikkeladresse.tilleggsnavn).isEqualTo("Ved skogen")
        assertThat(grMatrikkeladresse.postnummer).isEqualTo("1234")
        assertThat(grMatrikkeladresse.kommunenummer).isEqualTo("0219")

        assertThat(grMatrikkeladresse.person).isEqualTo(person)
        assertThat(grMatrikkeladresse.periode?.fom).isEqualTo(fom)
        assertThat(grMatrikkeladresse.periode?.tom).isEqualTo(tom)
    }

    @Test
    fun `fraOppholdsadresse skal returnere GrUtenlandskAdresse når oppholdsadresse har utenlandskAdresse`() {
        // Arrange
        val oppholdsadresse =
            Oppholdsadresse(
                gyldigFraOgMed = fom,
                gyldigTilOgMed = tom,
                utenlandskAdresse = utenlandskAdresse,
            )

        // Act
        val grOppholdsadresse = GrOppholdsadresse.fraOppholdsadresse(oppholdsadresse, person)

        // Assert
        assertThat(grOppholdsadresse).isInstanceOf(GrUtenlandskAdresse::class.java)

        val grUtenlandskAdresse = grOppholdsadresse as GrUtenlandskAdresse
        assertThat(grUtenlandskAdresse.adressenavnNummer).isEqualTo("123 Foreign St")
        assertThat(grUtenlandskAdresse.bygningEtasjeLeilighet).isEqualTo("Apt 4B")
        assertThat(grUtenlandskAdresse.postkode).isEqualTo("10001")
        assertThat(grUtenlandskAdresse.bySted).isEqualTo("New York")
        assertThat(grUtenlandskAdresse.regionDistriktOmraade).isEqualTo("NY")
        assertThat(grUtenlandskAdresse.landkode).isEqualTo("USA")

        assertThat(grUtenlandskAdresse.person).isEqualTo(person)
        assertThat(grUtenlandskAdresse.periode?.fom).isEqualTo(fom)
        assertThat(grUtenlandskAdresse.periode?.tom).isEqualTo(tom)
    }

    @Test
    fun `fraOppholdsadresse skal returnere GrUkjentAdresse når oppholdsadresse ikke har noen adressetyper`() {
        // Arrange
        val oppholdsadresse = Oppholdsadresse(gyldigFraOgMed = fom, gyldigTilOgMed = tom)

        // Act
        val grOppholdsadresse = GrOppholdsadresse.fraOppholdsadresse(oppholdsadresse, person)

        // Assert
        assertThat(grOppholdsadresse).isInstanceOf(GrUkjentAdresse::class.java)

        assertThat(grOppholdsadresse.person).isEqualTo(person)
        assertThat(grOppholdsadresse.periode?.fom).isEqualTo(fom)
        assertThat(grOppholdsadresse.periode?.tom).isEqualTo(tom)
    }

    @Test
    fun `fraOppholdsadresse skal håndtere null verdier for gyldigFraOgMed og gyldigTilOgMed`() {
        // Arrange
        val oppholdsadresse = Oppholdsadresse(gyldigFraOgMed = null, gyldigTilOgMed = null)

        // Act
        val grOppholdsadresse = GrOppholdsadresse.fraOppholdsadresse(oppholdsadresse, person)

        // Assert
        assertThat(grOppholdsadresse.periode?.fom).isNull()
        assertThat(grOppholdsadresse.periode?.tom).isNull()
    }

    @Test
    fun `fraOppholdsadresse skal håndtere ukjent oppholdAnnetSted verdi`() {
        // Arrange
        val oppholdsadresse = Oppholdsadresse(oppholdAnnetSted = "ukjentVerdi")

        // Act
        val grOppholdsadresse = GrOppholdsadresse.fraOppholdsadresse(oppholdsadresse, person)

        // Assert
        assertThat(grOppholdsadresse.oppholdAnnetSted).isNull()
    }

    @Test
    fun `fraOppholdsadresse skal prioritere vegadresse over matrikkeladresse over utenlandsk adresse`() {
        // Arrange
        val oppholdsadresseUtenlandskAdresse =
            Oppholdsadresse(utenlandskAdresse = utenlandskAdresse)

        val oppholdsadresseMatrikkelOgUtenlandskAdresse =
            oppholdsadresseUtenlandskAdresse.copy(matrikkeladresse = matrikkeladresse)

        val oppholdsadresseVegMatrikkelOgUtenlandskAdresse =
            oppholdsadresseMatrikkelOgUtenlandskAdresse.copy(vegadresse = vegadresse)

        // Act
        val grUtenlandskAdresse = GrOppholdsadresse.fraOppholdsadresse(oppholdsadresseUtenlandskAdresse, person)
        val grMatrikkeladresse = GrOppholdsadresse.fraOppholdsadresse(oppholdsadresseMatrikkelOgUtenlandskAdresse, person)
        val grVegadresse = GrOppholdsadresse.fraOppholdsadresse(oppholdsadresseVegMatrikkelOgUtenlandskAdresse, person)

        // Assert
        assertThat(grUtenlandskAdresse).isInstanceOf(GrUtenlandskAdresse::class.java)
        assertThat(grMatrikkeladresse).isInstanceOf(GrMatrikkeladresse::class.java)
        assertThat(grVegadresse).isInstanceOf(GrVegadresse::class.java)
    }
}
