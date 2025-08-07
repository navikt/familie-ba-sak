package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import no.nav.familie.kontrakter.ba.finnmarkstillegg.KommunerIFinnmarkOgNordTroms
import no.nav.familie.kontrakter.ba.finnmarkstillegg.KommunerIFinnmarkOgNordTroms.ALTA
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.DeltBosted
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.UkjentBosted
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

class PdlBostedsadresseOgDeltBostedPersonTest {
    @ParameterizedTest
    @EnumSource(KommunerIFinnmarkOgNordTroms::class)
    fun `skal returnere true når vegadresse på bostedsadresse er i Finnmark eller Nord-Troms`(
        kommune: KommunerIFinnmarkOgNordTroms,
    ) {
        val bostedsadresseIFinnmark =
            lagBostedsadresse(
                kommunenummer = kommune.kommunenummer,
                gyldigFraOgMed = LocalDate.now().minusYears(1),
                gyldigTilOgMed = null,
                harVegadresse = true,
            )

        val adresser =
            PdlBostedsadresseOgDeltBostedPerson(
                bostedsadresse = listOf(bostedsadresseIFinnmark),
                deltBosted = emptyList(),
            )

        assertThat(adresser.nåværendeBostedEllerDeltBostedErIFinnmarkEllerNordTroms()).isTrue
    }

    @ParameterizedTest
    @EnumSource(KommunerIFinnmarkOgNordTroms::class)
    fun `skal returnere true når vegadresse på delt bosted er i Finnmark eller Nord-Troms`(
        kommune: KommunerIFinnmarkOgNordTroms,
    ) {
        val deltBostedIFinnmark =
            lagDeltBosted(
                kommunenummer = kommune.kommunenummer,
                startdatoForKontrakt = LocalDate.now().minusYears(1),
                sluttdatoForKontrakt = null,
                harVegadresse = true,
            )

        val adresser =
            PdlBostedsadresseOgDeltBostedPerson(
                bostedsadresse = emptyList(),
                deltBosted = listOf(deltBostedIFinnmark),
            )

        assertThat(adresser.nåværendeBostedEllerDeltBostedErIFinnmarkEllerNordTroms()).isTrue
    }

    @Test
    fun `skal returnere true når vegadresse på bostedsadresse mangler, men matrikkeladresse på bostedsadresse er i Finnmark eller Nord-Troms`() {
        val bostedsadresseIFinnmark =
            lagBostedsadresse(
                kommunenummer = ALTA.kommunenummer,
                gyldigFraOgMed = LocalDate.now().minusYears(1),
                gyldigTilOgMed = null,
                harMatrikkeladresse = true,
            )

        val adresser =
            PdlBostedsadresseOgDeltBostedPerson(
                bostedsadresse = listOf(bostedsadresseIFinnmark),
                deltBosted = emptyList(),
            )

        assertThat(adresser.nåværendeBostedEllerDeltBostedErIFinnmarkEllerNordTroms()).isTrue
    }

    @Test
    fun `skal returnere true når vegadresse på delt bosted mangler, men matrikkeladresse på delt bosted er i Finnmark eller Nord-Troms`() {
        val deltBostedIFinnmark =
            lagDeltBosted(
                kommunenummer = ALTA.kommunenummer,
                startdatoForKontrakt = LocalDate.now().minusYears(1),
                sluttdatoForKontrakt = null,
                harMatrikkeladresse = true,
            )

        val adresser =
            PdlBostedsadresseOgDeltBostedPerson(
                bostedsadresse = emptyList(),
                deltBosted = listOf(deltBostedIFinnmark),
            )

        assertThat(adresser.nåværendeBostedEllerDeltBostedErIFinnmarkEllerNordTroms()).isTrue
    }

    @Test
    fun `skal returnere true når vegadresse og matrikkeladresse på bostedsadresse mangler, men ukjent bosted på bostedsadresse er i Finnmark eller Nord-Troms`() {
        val bostedsadresseIFinnmark =
            lagBostedsadresse(
                kommunenummer = ALTA.kommunenummer,
                gyldigFraOgMed = LocalDate.now().minusYears(1),
                gyldigTilOgMed = null,
                harUkjentBosted = true,
            )

        val adresser =
            PdlBostedsadresseOgDeltBostedPerson(
                bostedsadresse = listOf(bostedsadresseIFinnmark),
                deltBosted = emptyList(),
            )

        assertThat(adresser.nåværendeBostedEllerDeltBostedErIFinnmarkEllerNordTroms()).isTrue
    }

    @Test
    fun `skal returnere true når vegadresse og matrikkeladresse på delt bosted mangler, men ukjent bosted på delt bosted er i Finnmark eller Nord-Troms`() {
        val deltBostedIFinnmark =
            lagDeltBosted(
                kommunenummer = ALTA.kommunenummer,
                startdatoForKontrakt = LocalDate.now().minusYears(1),
                sluttdatoForKontrakt = null,
                harUkjentBosted = true,
            )

        val adresser =
            PdlBostedsadresseOgDeltBostedPerson(
                bostedsadresse = emptyList(),
                deltBosted = listOf(deltBostedIFinnmark),
            )

        assertThat(adresser.nåværendeBostedEllerDeltBostedErIFinnmarkEllerNordTroms()).isTrue
    }

    @Test
    fun `skal returnere false når gjeldende adresser ikke er i Finnmark eller Nord-Troms`() {
        val bostedsadresseIOslo =
            lagBostedsadresse(
                kommunenummer = "0301",
                gyldigFraOgMed = LocalDate.now().minusYears(1),
                gyldigTilOgMed = null,
                harVegadresse = true,
            )

        val deltBostedIOslo =
            lagDeltBosted(
                kommunenummer = "0301",
                startdatoForKontrakt = LocalDate.now().minusYears(1),
                sluttdatoForKontrakt = null,
                harVegadresse = true,
            )

        val adresser =
            PdlBostedsadresseOgDeltBostedPerson(
                bostedsadresse = listOf(bostedsadresseIOslo),
                deltBosted = listOf(deltBostedIOslo),
            )

        assertThat(adresser.nåværendeBostedEllerDeltBostedErIFinnmarkEllerNordTroms()).isFalse
    }

    @Test
    fun `skal returnere true når delt bosted er i Finnmark og bostedsadresse ikke er i Finnmark eller Nord-Troms`() {
        val bostedsadresseIOslo =
            lagBostedsadresse(
                kommunenummer = "0301",
                gyldigFraOgMed = LocalDate.now().minusYears(1),
                gyldigTilOgMed = null,
                harVegadresse = true,
            )

        val deltBostedIFinnmark =
            lagDeltBosted(
                kommunenummer = ALTA.kommunenummer,
                startdatoForKontrakt = LocalDate.now().minusYears(1),
                sluttdatoForKontrakt = null,
                harVegadresse = true,
            )

        val adresser =
            PdlBostedsadresseOgDeltBostedPerson(
                bostedsadresse = listOf(bostedsadresseIOslo),
                deltBosted = listOf(deltBostedIFinnmark),
            )

        assertThat(adresser.nåværendeBostedEllerDeltBostedErIFinnmarkEllerNordTroms()).isTrue
    }

    @Test
    fun `skal velge nyeste bostedsadresse når det finnes flere`() {
        val gammelBostedsadresseIFinnmark =
            lagBostedsadresse(
                kommunenummer = ALTA.kommunenummer,
                gyldigFraOgMed = LocalDate.now().minusYears(3),
                gyldigTilOgMed = null,
                harVegadresse = true,
            )

        val nyBostedsadresseIOslo =
            lagBostedsadresse(
                kommunenummer = "0301",
                gyldigFraOgMed = LocalDate.now().minusYears(1),
                gyldigTilOgMed = null,
                harVegadresse = true,
            )

        val adresser =
            PdlBostedsadresseOgDeltBostedPerson(
                bostedsadresse = listOf(gammelBostedsadresseIFinnmark, nyBostedsadresseIOslo),
                deltBosted = emptyList(),
            )

        assertThat(adresser.nåværendeBostedEllerDeltBostedErIFinnmarkEllerNordTroms()).isFalse
    }

    @Test
    fun `skal returnere false når det ikke finnes gjeldende adresser`() {
        val utløptBostedsadresseIFinnmark =
            lagBostedsadresse(
                kommunenummer = ALTA.kommunenummer,
                gyldigFraOgMed = LocalDate.now().minusYears(2),
                gyldigTilOgMed = LocalDate.now().minusYears(1),
                harVegadresse = true,
            )

        val utløptDeltBostedIFinnmark =
            lagDeltBosted(
                kommunenummer = ALTA.kommunenummer,
                startdatoForKontrakt = LocalDate.now().minusYears(2),
                sluttdatoForKontrakt = LocalDate.now().minusYears(1),
                harVegadresse = true,
            )

        val adresser =
            PdlBostedsadresseOgDeltBostedPerson(
                bostedsadresse = listOf(utløptBostedsadresseIFinnmark),
                deltBosted = listOf(utløptDeltBostedIFinnmark),
            )

        assertThat(adresser.nåværendeBostedEllerDeltBostedErIFinnmarkEllerNordTroms()).isFalse
    }

    private fun lagBostedsadresse(
        gyldigFraOgMed: LocalDate?,
        gyldigTilOgMed: LocalDate?,
        kommunenummer: String,
        harVegadresse: Boolean = false,
        harMatrikkeladresse: Boolean = false,
        harUkjentBosted: Boolean = false,
    ): Bostedsadresse {
        if (!harVegadresse && !harMatrikkeladresse && !harUkjentBosted) {
            throw IllegalArgumentException("Minst én av harVegadresse, harMatrikkeladresse eller harUkjentBosted må være true")
        }
        return Bostedsadresse(
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

    private fun lagDeltBosted(
        startdatoForKontrakt: LocalDate?,
        sluttdatoForKontrakt: LocalDate?,
        kommunenummer: String,
        harVegadresse: Boolean = false,
        harMatrikkeladresse: Boolean = false,
        harUkjentBosted: Boolean = false,
    ): DeltBosted {
        if (!harVegadresse && !harMatrikkeladresse && !harUkjentBosted) {
            throw IllegalArgumentException("Minst én av harVegadresse, harMatrikkeladresse eller harUkjentBosted må være true")
        }
        return DeltBosted(
            startdatoForKontrakt = startdatoForKontrakt,
            sluttdatoForKontrakt = sluttdatoForKontrakt,
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
}
