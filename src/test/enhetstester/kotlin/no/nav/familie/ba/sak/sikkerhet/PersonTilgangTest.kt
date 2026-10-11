package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.tilgangsmaskin.Avvisningskode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class PersonTilgangTest {
    @Test
    fun `skal ha tilgang uten avvisning`() {
        // Act
        val tilgang = PersonTilgang.medTilgang(PERSONIDENT)

        // Assert
        assertThat(tilgang.harTilgang).isTrue
        assertThat(tilgang.avvisning).isNull()
    }

    @Test
    fun `skal ikke ha tilgang med avvisning`() {
        // Act
        val tilgang = PersonTilgang.avvist(PERSONIDENT, Avvisningskode.AVVIST_SKJERMING, BEGRUNNELSE)

        // Assert
        assertThat(tilgang.harTilgang).isFalse
        assertThat(tilgang.avvisning).isEqualTo(PersonTilgang.Avvisning(Avvisningskode.AVVIST_SKJERMING, BEGRUNNELSE))
    }

    @ParameterizedTest
    @EnumSource(value = Avvisningskode::class, names = ["AVVIST_STRENGT_FORTROLIG_ADRESSE", "AVVIST_STRENGT_FORTROLIG_UTLAND"])
    fun `skal være avvist grunnet strengt fortrolig for strengt fortrolige avvisningskoder`(avvisningskode: Avvisningskode) {
        // Arrange
        val tilgang = PersonTilgang.avvist(PERSONIDENT, avvisningskode, BEGRUNNELSE)

        // Act & Assert
        assertThat(tilgang.erAvvistGrunnetStrengtFortrolig()).isTrue
    }

    @ParameterizedTest
    @EnumSource(value = Avvisningskode::class, names = ["AVVIST_STRENGT_FORTROLIG_ADRESSE", "AVVIST_STRENGT_FORTROLIG_UTLAND"], mode = EnumSource.Mode.EXCLUDE)
    fun `skal ikke være avvist grunnet strengt fortrolig for andre avvisningskoder`(avvisningskode: Avvisningskode) {
        // Arrange
        val tilgang = PersonTilgang.avvist(PERSONIDENT, avvisningskode, BEGRUNNELSE)

        // Act & Assert
        assertThat(tilgang.erAvvistGrunnetStrengtFortrolig()).isFalse
    }

    @Test
    fun `skal ikke være avvist grunnet strengt fortrolig når saksbehandler har tilgang`() {
        // Arrange
        val tilgang = PersonTilgang.medTilgang(PERSONIDENT)

        // Act & Assert
        assertThat(tilgang.erAvvistGrunnetStrengtFortrolig()).isFalse
    }

    @Test
    fun `skal maskere personidenten i toString`() {
        // Arrange
        val tilgang = PersonTilgang.avvist(PERSONIDENT, Avvisningskode.AVVIST_SKJERMING, BEGRUNNELSE)

        // Act
        val tekst = tilgang.toString()

        // Assert
        assertThat(tekst).doesNotContain(PERSONIDENT)
        assertThat(tekst).contains("avvisningskode=AVVIST_SKJERMING")
    }

    companion object {
        private const val PERSONIDENT = "12345678910"
        private const val BEGRUNNELSE = "Du har ikke tilgang til Nav-ansatte og deres nærmeste familie"
    }
}
