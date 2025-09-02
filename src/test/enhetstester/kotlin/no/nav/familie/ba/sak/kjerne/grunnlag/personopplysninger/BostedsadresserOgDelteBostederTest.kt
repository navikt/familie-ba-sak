package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.BostedsadresserOgDelteBosteder
import no.nav.familie.kontrakter.ba.finnmarkstillegg.KommunerIFinnmarkOgNordTroms
import no.nav.familie.kontrakter.ba.finnmarkstillegg.KommunerIFinnmarkOgNordTroms.ALTA
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.UkjentBosted
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

class BostedsadresserOgDelteBostederTest {
    private val kommunenummerOslo = "0301"

    @ParameterizedTest
    @EnumSource(KommunerIFinnmarkOgNordTroms::class)
    fun `skal returnere true når vegadresse på bostedsadresse er i tilleggssone`(
        kommune: KommunerIFinnmarkOgNordTroms,
    ) {
        val bostedsadresseIFinnmark =
            lagAdresse(
                kommunenummer = kommune.kommunenummer,
                gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                gyldigTilOgMed = null,
                harVegadresse = true,
            )

        val adresser =
            BostedsadresserOgDelteBosteder(
                bostedsadresser = listOf(bostedsadresseIFinnmark),
                delteBosteder = emptyList(),
            )

        assertThat(adresser.harBostedsadresseEllerDeltBostedSomErRelevantForFinnmarkstillegg()).isTrue
    }

    @ParameterizedTest
    @EnumSource(KommunerIFinnmarkOgNordTroms::class)
    fun `skal returnere true når vegadresse på delt bosted er i tilleggssone`(
        kommune: KommunerIFinnmarkOgNordTroms,
    ) {
        val deltBostedIFinnmark =
            lagAdresse(
                kommunenummer = kommune.kommunenummer,
                gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                gyldigTilOgMed = null,
                harVegadresse = true,
            )

        val adresser =
            BostedsadresserOgDelteBosteder(
                bostedsadresser = emptyList(),
                delteBosteder = listOf(deltBostedIFinnmark),
            )

        assertThat(adresser.harBostedsadresseEllerDeltBostedSomErRelevantForFinnmarkstillegg()).isTrue
    }

    @Test
    fun `skal returnere true når vegadresse på bostedsadresse mangler, men matrikkeladresse på bostedsadresse er i tilleggssone`() {
        val bostedsadresseIFinnmark =
            lagAdresse(
                kommunenummer = ALTA.kommunenummer,
                gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                gyldigTilOgMed = null,
                harMatrikkeladresse = true,
            )

        val adresser =
            BostedsadresserOgDelteBosteder(
                bostedsadresser = listOf(bostedsadresseIFinnmark),
                delteBosteder = emptyList(),
            )

        assertThat(adresser.harBostedsadresseEllerDeltBostedSomErRelevantForFinnmarkstillegg()).isTrue
    }

    @Test
    fun `skal returnere true når vegadresse på delt bosted mangler, men matrikkeladresse på delt bosted er i tilleggssone`() {
        val deltBostedIFinnmark =
            lagAdresse(
                kommunenummer = ALTA.kommunenummer,
                gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                gyldigTilOgMed = null,
                harMatrikkeladresse = true,
            )

        val adresser =
            BostedsadresserOgDelteBosteder(
                bostedsadresser = emptyList(),
                delteBosteder = listOf(deltBostedIFinnmark),
            )

        assertThat(adresser.harBostedsadresseEllerDeltBostedSomErRelevantForFinnmarkstillegg()).isTrue
    }

    @Test
    fun `skal returnere true når vegadresse og matrikkeladresse på bostedsadresse mangler, men ukjent bosted på bostedsadresse er i tilleggssone`() {
        val bostedsadresseIFinnmark =
            lagAdresse(
                kommunenummer = ALTA.kommunenummer,
                gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                gyldigTilOgMed = null,
                harUkjentBosted = true,
            )

        val adresser =
            BostedsadresserOgDelteBosteder(
                bostedsadresser = listOf(bostedsadresseIFinnmark),
                delteBosteder = emptyList(),
            )

        assertThat(adresser.harBostedsadresseEllerDeltBostedSomErRelevantForFinnmarkstillegg()).isTrue
    }

    @Test
    fun `skal returnere true når vegadresse og matrikkeladresse på delt bosted mangler, men ukjent bosted på delt bosted er i tilleggssone`() {
        val deltBostedIFinnmark =
            lagAdresse(
                kommunenummer = ALTA.kommunenummer,
                gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                gyldigTilOgMed = null,
                harUkjentBosted = true,
            )

        val adresser =
            BostedsadresserOgDelteBosteder(
                bostedsadresser = emptyList(),
                delteBosteder = listOf(deltBostedIFinnmark),
            )

        assertThat(adresser.harBostedsadresseEllerDeltBostedSomErRelevantForFinnmarkstillegg()).isTrue
    }

    @Test
    fun `skal returnere false når bostedsadresser og delt bosted er utenfor tilleggssone`() {
        val bostedsadresseIOslo =
            lagAdresse(
                kommunenummer = kommunenummerOslo,
                gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                gyldigTilOgMed = null,
                harVegadresse = true,
            )

        val deltBostedIOslo =
            lagAdresse(
                kommunenummer = kommunenummerOslo,
                gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                gyldigTilOgMed = null,
                harVegadresse = true,
            )

        val adresser =
            BostedsadresserOgDelteBosteder(
                bostedsadresser = listOf(bostedsadresseIOslo),
                delteBosteder = listOf(deltBostedIOslo),
            )

        assertThat(adresser.harBostedsadresseEllerDeltBostedSomErRelevantForFinnmarkstillegg()).isFalse
    }

    @Test
    fun `skal returnere true når bostedsadresse er i tilleggssone og delt bosted er utenfor tilleggssone`() {
        val bostedsadresseIFinnmark =
            lagAdresse(
                kommunenummer = ALTA.kommunenummer,
                gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                gyldigTilOgMed = null,
                harVegadresse = true,
            )

        val deltBostedIOslo =
            lagAdresse(
                kommunenummer = kommunenummerOslo,
                gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                gyldigTilOgMed = null,
                harVegadresse = true,
            )

        val adresser =
            BostedsadresserOgDelteBosteder(
                bostedsadresser = listOf(bostedsadresseIFinnmark),
                delteBosteder = listOf(deltBostedIOslo),
            )

        assertThat(adresser.harBostedsadresseEllerDeltBostedSomErRelevantForFinnmarkstillegg()).isTrue
    }

    @Test
    fun `skal returnere true når delt bosted er i tilleggssone og bostedsadresse er utenfor tilleggssone`() {
        val bostedsadresseIOslo =
            lagAdresse(
                kommunenummer = kommunenummerOslo,
                gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                gyldigTilOgMed = null,
                harVegadresse = true,
            )

        val deltBostedIFinnmark =
            lagAdresse(
                kommunenummer = ALTA.kommunenummer,
                gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                gyldigTilOgMed = null,
                harVegadresse = true,
            )

        val adresser =
            BostedsadresserOgDelteBosteder(
                bostedsadresser = listOf(bostedsadresseIOslo),
                delteBosteder = listOf(deltBostedIFinnmark),
            )

        assertThat(adresser.harBostedsadresseEllerDeltBostedSomErRelevantForFinnmarkstillegg()).isTrue
    }

    @Test
    fun `skal returnere false når adresse i tilleggssone ble overskrevet av adresse utenfor tilleggssone til og med 30 september 2025`() {
        val gammelBostedsadresseIFinnmark =
            lagAdresse(
                kommunenummer = ALTA.kommunenummer,
                gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                gyldigTilOgMed = null,
                harVegadresse = true,
            )

        val nyBostedsadresseIOslo =
            lagAdresse(
                kommunenummer = kommunenummerOslo,
                gyldigFraOgMed = LocalDate.of(2025, 9, 30),
                gyldigTilOgMed = null,
                harVegadresse = true,
            )

        val adresser =
            BostedsadresserOgDelteBosteder(
                bostedsadresser = listOf(gammelBostedsadresseIFinnmark, nyBostedsadresseIOslo),
                delteBosteder = listOf(),
            )

        assertThat(adresser.harBostedsadresseEllerDeltBostedSomErRelevantForFinnmarkstillegg()).isFalse
    }

    @Test
    fun `skal returnere true når adresse i tilleggssone startet 30 september 2025`() {
        val gammelBostedsadresseIOslo =
            lagAdresse(
                kommunenummer = kommunenummerOslo,
                gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                gyldigTilOgMed = null,
                harVegadresse = true,
            )

        val nyBostedsadresseIFinnmark =
            lagAdresse(
                kommunenummer = ALTA.kommunenummer,
                gyldigFraOgMed = LocalDate.of(2025, 9, 30),
                gyldigTilOgMed = null,
                harVegadresse = true,
            )

        val adresser =
            BostedsadresserOgDelteBosteder(
                bostedsadresser = listOf(gammelBostedsadresseIOslo, nyBostedsadresseIFinnmark),
                delteBosteder = listOf(),
            )

        assertThat(adresser.harBostedsadresseEllerDeltBostedSomErRelevantForFinnmarkstillegg()).isTrue
    }

    @Test
    fun `skal returnere true når adresse i tilleggssone har til og med dato 30 september 2025`() {
        val bostedsadresseIFinnmark =
            lagAdresse(
                kommunenummer = ALTA.kommunenummer,
                gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                gyldigTilOgMed = LocalDate.of(2025, 9, 30),
                harVegadresse = true,
            )

        val adresser =
            BostedsadresserOgDelteBosteder(
                bostedsadresser = listOf(bostedsadresseIFinnmark),
                delteBosteder = listOf(),
            )

        assertThat(adresser.harBostedsadresseEllerDeltBostedSomErRelevantForFinnmarkstillegg()).isTrue
    }

    @Test
    fun `skal returnere true når fremtidig adresse er i tilleggssone`() {
        val bostedsadresseIOslo =
            lagAdresse(
                kommunenummer = kommunenummerOslo,
                gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                gyldigTilOgMed = null,
                harVegadresse = true,
            )

        val fremtidigBostedsadresseIFinnmark =
            lagAdresse(
                kommunenummer = ALTA.kommunenummer,
                gyldigFraOgMed = LocalDate.of(2025, 11, 1),
                gyldigTilOgMed = null,
                harVegadresse = true,
            )

        val adresser =
            BostedsadresserOgDelteBosteder(
                bostedsadresser = listOf(bostedsadresseIOslo, fremtidigBostedsadresseIFinnmark),
                delteBosteder = listOf(),
            )

        assertThat(adresser.harBostedsadresseEllerDeltBostedSomErRelevantForFinnmarkstillegg()).isTrue
    }
}

private fun lagAdresse(
    gyldigFraOgMed: LocalDate?,
    gyldigTilOgMed: LocalDate?,
    kommunenummer: String,
    harVegadresse: Boolean = false,
    harMatrikkeladresse: Boolean = false,
    harUkjentBosted: Boolean = false,
): Adresse {
    if (!harVegadresse && !harMatrikkeladresse && !harUkjentBosted) {
        throw IllegalArgumentException("Minst én av harVegadresse, harMatrikkeladresse eller harUkjentBosted må være true")
    }
    return Adresse(
        gyldigFraOgMed = gyldigFraOgMed,
        gyldigTilOgMed = gyldigTilOgMed,
        vegadresse =
            Vegadresse(
                matrikkelId = null,
                husnummer = null,
                husbokstav = null,
                bruksenhetsnummer = null,
                adressenavn = null,
                kommunenummer = kommunenummer,
                tilleggsnavn = null,
                postnummer = null,
            ).takeIf { harVegadresse },
        matrikkeladresse =
            Matrikkeladresse(
                matrikkelId = null,
                bruksenhetsnummer = null,
                tilleggsnavn = null,
                postnummer = null,
                kommunenummer = kommunenummer,
            ).takeIf { harMatrikkeladresse },
        ukjentBosted =
            UkjentBosted(
                bostedskommune = kommunenummer,
            ).takeIf { harUkjentBosted },
    )
}
