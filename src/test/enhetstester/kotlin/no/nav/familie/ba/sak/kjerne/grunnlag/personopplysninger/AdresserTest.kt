package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.datagenerator.lagAdresse
import no.nav.familie.ba.sak.datagenerator.lagAdresser
import no.nav.familie.ba.sak.datagenerator.lagBostedsadresse
import no.nav.familie.ba.sak.datagenerator.lagDeltBosted
import no.nav.familie.ba.sak.datagenerator.lagMatrikkeladresse
import no.nav.familie.ba.sak.datagenerator.lagOppholdsadresse
import no.nav.familie.ba.sak.datagenerator.lagUkjentBosted
import no.nav.familie.ba.sak.datagenerator.lagVegadresse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlAdresserPerson
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresser
import no.nav.familie.kontrakter.ba.finnmarkstillegg.KommunerIFinnmarkOgNordTroms
import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted
import no.nav.familie.kontrakter.felles.svalbard.SvalbardKommune
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

class AdresserTest {
    @Nested
    inner class HarAdresserSomErRelevantForFinnmarkstillegg {
        private val kommunenummerOslo = "0301"

        @ParameterizedTest
        @EnumSource(KommunerIFinnmarkOgNordTroms::class)
        fun `skal returnere true når vegadresse på bostedsadresse er i tilleggssone`(
            kommune: KommunerIFinnmarkOgNordTroms,
        ) {
            // Arrange
            val bostedsadresseIFinnmark =
                lagAdresse(
                    gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                    gyldigTilOgMed = null,
                    vegadresse = lagVegadresse(kommunenummer = kommune.kommunenummer),
                )

            val adresser =
                Adresser(
                    bostedsadresser = listOf(bostedsadresseIFinnmark),
                    delteBosteder = emptyList(),
                    oppholdsadresse = emptyList(),
                )

            // Act
            val harAdresserSomErRelevantForFinnmarkstillegg = adresser.harAdresserSomErRelevantForFinnmarkstillegg()

            // Assert
            assertThat(harAdresserSomErRelevantForFinnmarkstillegg).isTrue()
        }

        @ParameterizedTest
        @EnumSource(KommunerIFinnmarkOgNordTroms::class)
        fun `skal returnere true når vegadresse på delt bosted er i tilleggssone`(
            kommune: KommunerIFinnmarkOgNordTroms,
        ) {
            // Arrange
            val deltBostedIFinnmark =
                lagAdresse(
                    gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                    gyldigTilOgMed = null,
                    vegadresse = lagVegadresse(kommunenummer = kommune.kommunenummer),
                )

            val adresser =
                Adresser(
                    bostedsadresser = emptyList(),
                    delteBosteder = listOf(deltBostedIFinnmark),
                    oppholdsadresse = emptyList(),
                )

            // Act
            val harAdresserSomErRelevantForFinnmarkstillegg = adresser.harAdresserSomErRelevantForFinnmarkstillegg()

            // Assert
            assertThat(harAdresserSomErRelevantForFinnmarkstillegg).isTrue()
        }

        @Test
        fun `skal returnere true når vegadresse på bostedsadresse mangler, men matrikkeladresse på bostedsadresse er i tilleggssone`() {
            // Arrange
            val bostedsadresseIFinnmark =
                lagAdresse(
                    gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                    gyldigTilOgMed = null,
                    matrikkeladresse = lagMatrikkeladresse(kommunenummer = KommunerIFinnmarkOgNordTroms.ALTA.kommunenummer),
                )

            val adresser =
                Adresser(
                    bostedsadresser = listOf(bostedsadresseIFinnmark),
                    delteBosteder = emptyList(),
                    oppholdsadresse = emptyList(),
                )

            // Act
            val harAdresserSomErRelevantForFinnmarkstillegg = adresser.harAdresserSomErRelevantForFinnmarkstillegg()

            // Assert
            assertThat(harAdresserSomErRelevantForFinnmarkstillegg).isTrue()
        }

        @Test
        fun `skal returnere true når vegadresse på delt bosted mangler, men matrikkeladresse på delt bosted er i tilleggssone`() {
            // Arrange
            val deltBostedIFinnmark =
                lagAdresse(
                    gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                    gyldigTilOgMed = null,
                    matrikkeladresse = lagMatrikkeladresse(kommunenummer = KommunerIFinnmarkOgNordTroms.ALTA.kommunenummer),
                )

            val adresser =
                Adresser(
                    bostedsadresser = emptyList(),
                    delteBosteder = listOf(deltBostedIFinnmark),
                    oppholdsadresse = emptyList(),
                )

            // Act
            val harAdresserSomErRelevantForFinnmarkstillegg = adresser.harAdresserSomErRelevantForFinnmarkstillegg()

            // Assert
            assertThat(harAdresserSomErRelevantForFinnmarkstillegg).isTrue()
        }

        @Test
        fun `skal returnere true når vegadresse og matrikkeladresse på bostedsadresse mangler, men ukjent bosted på bostedsadresse er i tilleggssone`() {
            // Arrange
            val bostedsadresseIFinnmark =
                lagAdresse(
                    gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                    gyldigTilOgMed = null,
                    ukjentBosted = lagUkjentBosted(KommunerIFinnmarkOgNordTroms.ALTA.kommunenummer),
                )

            val adresser =
                Adresser(
                    bostedsadresser = listOf(bostedsadresseIFinnmark),
                    delteBosteder = emptyList(),
                    oppholdsadresse = emptyList(),
                )

            // Act
            val harAdresserSomErRelevantForFinnmarkstillegg = adresser.harAdresserSomErRelevantForFinnmarkstillegg()

            // Assert
            assertThat(harAdresserSomErRelevantForFinnmarkstillegg).isTrue()
        }

        @Test
        fun `skal returnere true når vegadresse og matrikkeladresse på delt bosted mangler, men ukjent bosted på delt bosted er i tilleggssone`() {
            // Arrange
            val deltBostedIFinnmark =
                lagAdresse(
                    gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                    gyldigTilOgMed = null,
                    ukjentBosted = lagUkjentBosted(KommunerIFinnmarkOgNordTroms.ALTA.kommunenummer),
                )

            val adresser =
                Adresser(
                    bostedsadresser = emptyList(),
                    delteBosteder = listOf(deltBostedIFinnmark),
                    oppholdsadresse = emptyList(),
                )

            // Act
            val harAdresserSomErRelevantForFinnmarkstillegg = adresser.harAdresserSomErRelevantForFinnmarkstillegg()

            // Assert
            assertThat(harAdresserSomErRelevantForFinnmarkstillegg).isTrue
        }

        @Test
        fun `skal returnere false når bostedsadresser og delt bosted er utenfor tilleggssone`() {
            // Arrange
            val bostedsadresseIOslo =
                lagAdresse(
                    gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                    gyldigTilOgMed = null,
                    vegadresse = lagVegadresse(kommunenummer = kommunenummerOslo),
                )

            val deltBostedIOslo =
                lagAdresse(
                    gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                    gyldigTilOgMed = null,
                    vegadresse = lagVegadresse(kommunenummer = kommunenummerOslo),
                )

            val adresser =
                Adresser(
                    bostedsadresser = listOf(bostedsadresseIOslo),
                    delteBosteder = listOf(deltBostedIOslo),
                    oppholdsadresse = emptyList(),
                )

            // Act
            val harAdresserSomErRelevantForFinnmarkstillegg = adresser.harAdresserSomErRelevantForFinnmarkstillegg()

            // Assert
            assertThat(harAdresserSomErRelevantForFinnmarkstillegg).isFalse()
        }

        @Test
        fun `skal returnere true når bostedsadresse er i tilleggssone og delt bosted er utenfor tilleggssone`() {
            // Arrange
            val bostedsadresseIFinnmark =
                lagAdresse(
                    gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                    gyldigTilOgMed = null,
                    vegadresse = lagVegadresse(kommunenummer = KommunerIFinnmarkOgNordTroms.ALTA.kommunenummer),
                )

            val deltBostedIOslo =
                lagAdresse(
                    gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                    gyldigTilOgMed = null,
                    vegadresse = lagVegadresse(kommunenummer = kommunenummerOslo),
                )

            val adresser =
                Adresser(
                    bostedsadresser = listOf(bostedsadresseIFinnmark),
                    delteBosteder = listOf(deltBostedIOslo),
                    oppholdsadresse = emptyList(),
                )

            // Act
            val harAdresserSomErRelevantForFinnmarkstillegg = adresser.harAdresserSomErRelevantForFinnmarkstillegg()

            // Assert
            assertThat(harAdresserSomErRelevantForFinnmarkstillegg).isTrue()
        }

        @Test
        fun `skal returnere true når delt bosted er i tilleggssone og bostedsadresse er utenfor tilleggssone`() {
            // Arrange
            val bostedsadresseIOslo =
                lagAdresse(
                    gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                    gyldigTilOgMed = null,
                    vegadresse = lagVegadresse(kommunenummer = kommunenummerOslo),
                )

            val deltBostedIFinnmark =
                lagAdresse(
                    gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                    gyldigTilOgMed = null,
                    vegadresse = lagVegadresse(kommunenummer = KommunerIFinnmarkOgNordTroms.ALTA.kommunenummer),
                )

            val adresser =
                Adresser(
                    bostedsadresser = listOf(bostedsadresseIOslo),
                    delteBosteder = listOf(deltBostedIFinnmark),
                    oppholdsadresse = emptyList(),
                )

            // Act
            val harAdresserSomErRelevantForFinnmarkstillegg = adresser.harAdresserSomErRelevantForFinnmarkstillegg()

            // Assert
            assertThat(harAdresserSomErRelevantForFinnmarkstillegg).isTrue()
        }

        @Test
        fun `skal returnere false når adresse i tilleggssone ble overskrevet av adresse utenfor tilleggssone til og med 30 september 2025`() {
            // Arrange
            val gammelBostedsadresseIFinnmark =
                lagAdresse(
                    gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                    gyldigTilOgMed = null,
                    vegadresse = lagVegadresse(kommunenummer = KommunerIFinnmarkOgNordTroms.ALTA.kommunenummer),
                )

            val nyBostedsadresseIOslo =
                lagAdresse(
                    gyldigFraOgMed = LocalDate.of(2025, 9, 30),
                    gyldigTilOgMed = null,
                    vegadresse = lagVegadresse(kommunenummer = kommunenummerOslo),
                )

            val adresser =
                Adresser(
                    bostedsadresser = listOf(gammelBostedsadresseIFinnmark, nyBostedsadresseIOslo),
                    delteBosteder = listOf(),
                    oppholdsadresse = emptyList(),
                )

            // Act
            val harAdresserSomErRelevantForFinnmarkstillegg = adresser.harAdresserSomErRelevantForFinnmarkstillegg()

            // Assert
            assertThat(harAdresserSomErRelevantForFinnmarkstillegg).isFalse()
        }

        @Test
        fun `skal returnere true når adresse i tilleggssone startet 30 september 2025`() {
            // Arrange
            val gammelBostedsadresseIOslo =
                lagAdresse(
                    gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                    gyldigTilOgMed = null,
                    vegadresse = lagVegadresse(kommunenummer = kommunenummerOslo),
                )

            val nyBostedsadresseIFinnmark =
                lagAdresse(
                    gyldigFraOgMed = LocalDate.of(2025, 9, 30),
                    gyldigTilOgMed = null,
                    vegadresse = lagVegadresse(kommunenummer = KommunerIFinnmarkOgNordTroms.ALTA.kommunenummer),
                )

            val adresser =
                Adresser(
                    bostedsadresser = listOf(gammelBostedsadresseIOslo, nyBostedsadresseIFinnmark),
                    delteBosteder = listOf(),
                    oppholdsadresse = emptyList(),
                )

            // Act
            val harAdresserSomErRelevantForFinnmarkstillegg = adresser.harAdresserSomErRelevantForFinnmarkstillegg()

            // Assert
            assertThat(harAdresserSomErRelevantForFinnmarkstillegg).isTrue()
        }

        @Test
        fun `skal returnere true når adresse i tilleggssone har til og med dato 30 september 2025`() {
            // Arrange
            val bostedsadresseIFinnmark =
                lagAdresse(
                    gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                    gyldigTilOgMed = LocalDate.of(2025, 9, 30),
                    vegadresse = lagVegadresse(kommunenummer = KommunerIFinnmarkOgNordTroms.ALTA.kommunenummer),
                )

            val adresser =
                Adresser(
                    bostedsadresser = listOf(bostedsadresseIFinnmark),
                    delteBosteder = listOf(),
                    oppholdsadresse = emptyList(),
                )

            // Act
            val harAdresserSomErRelevantForFinnmarkstillegg = adresser.harAdresserSomErRelevantForFinnmarkstillegg()

            // Assert
            assertThat(harAdresserSomErRelevantForFinnmarkstillegg).isTrue
        }

        @Test
        fun `skal returnere true når fremtidig adresse er i tilleggssone`() {
            // Assert
            val bostedsadresseIOslo =
                lagAdresse(
                    gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                    gyldigTilOgMed = null,
                    vegadresse = lagVegadresse(kommunenummer = kommunenummerOslo),
                )

            val fremtidigBostedsadresseIFinnmark =
                lagAdresse(
                    gyldigFraOgMed = LocalDate.of(2025, 11, 1),
                    gyldigTilOgMed = null,
                    vegadresse = lagVegadresse(kommunenummer = KommunerIFinnmarkOgNordTroms.ALTA.kommunenummer),
                )

            val adresser =
                Adresser(
                    bostedsadresser = listOf(bostedsadresseIOslo, fremtidigBostedsadresseIFinnmark),
                    delteBosteder = listOf(),
                    oppholdsadresse = emptyList(),
                )

            // Act
            val harAdresserSomErRelevantForFinnmarkstillegg = adresser.harAdresserSomErRelevantForFinnmarkstillegg()

            // Arrange
            assertThat(harAdresserSomErRelevantForFinnmarkstillegg).isTrue()
        }
    }

    @Nested
    inner class HarAdresserSomErRelevantForSvalbardtillegg {
        private val svalbardCutoffDato = LocalDate.of(2025, 9, 30)

        @Test
        fun `skal returnere true om oppholdsadresse har en vegadresse som er på Svalbard`() {
            // Arrange
            val adresser =
                lagAdresser(
                    oppholdsadresse =
                        listOf(
                            lagAdresse(
                                gyldigFraOgMed = null,
                                gyldigTilOgMed = null,
                                vegadresse = lagVegadresse(kommunenummer = SvalbardKommune.SVALBARD.kommunenummer),
                            ),
                            lagAdresse(
                                gyldigFraOgMed = svalbardCutoffDato.minusYears(1),
                                gyldigTilOgMed = svalbardCutoffDato.plusYears(2),
                                vegadresse = lagVegadresse(kommunenummer = SvalbardKommune.SVALBARD.kommunenummer),
                            ),
                            lagAdresse(
                                gyldigFraOgMed = svalbardCutoffDato.plusYears(2),
                                gyldigTilOgMed = svalbardCutoffDato.plusYears(3),
                                vegadresse = lagVegadresse(kommunenummer = KommunerIFinnmarkOgNordTroms.ALTA.kommunenummer),
                            ),
                        ),
                )

            // Act
            val harAdresserSomErRelevantForSvalbardstillegg = adresser.harAdresserSomErRelevantForSvalbardtillegg()

            // Assert
            assertThat(harAdresserSomErRelevantForSvalbardstillegg).isTrue()
        }

        @Test
        fun `skal returnere true om oppholdsadresse har en matrikkeladresse som er på Svalbard`() {
            // Arrange
            val adresser =
                lagAdresser(
                    oppholdsadresse =
                        listOf(
                            lagAdresse(
                                gyldigFraOgMed = null,
                                gyldigTilOgMed = null,
                                matrikkeladresse = lagMatrikkeladresse(kommunenummer = SvalbardKommune.SVALBARD.kommunenummer),
                            ),
                            lagAdresse(
                                gyldigFraOgMed = svalbardCutoffDato.minusYears(1),
                                gyldigTilOgMed = svalbardCutoffDato.plusYears(2),
                                matrikkeladresse = lagMatrikkeladresse(kommunenummer = SvalbardKommune.SVALBARD.kommunenummer),
                            ),
                            lagAdresse(
                                gyldigFraOgMed = svalbardCutoffDato.plusYears(2),
                                gyldigTilOgMed = svalbardCutoffDato.plusYears(3),
                                matrikkeladresse = lagMatrikkeladresse(kommunenummer = "123"),
                            ),
                        ),
                )

            // Act
            val harAdresserSomErRelevantForSvalbardstillegg = adresser.harAdresserSomErRelevantForSvalbardtillegg()

            // Assert
            assertThat(harAdresserSomErRelevantForSvalbardstillegg).isTrue()
        }

        @Test
        fun `skal returnere true om oppholdsadresse har et ukjent bosted som er på Svalbard`() {
            // Arrange
            val adresser =
                lagAdresser(
                    oppholdsadresse =
                        listOf(
                            lagAdresse(
                                gyldigFraOgMed = null,
                                gyldigTilOgMed = null,
                                ukjentBosted = lagUkjentBosted(bostedskommune = SvalbardKommune.SVALBARD.kommunenummer),
                            ),
                            lagAdresse(
                                gyldigFraOgMed = svalbardCutoffDato.minusYears(1),
                                gyldigTilOgMed = svalbardCutoffDato.plusYears(2),
                                ukjentBosted = lagUkjentBosted(bostedskommune = SvalbardKommune.SVALBARD.kommunenummer),
                            ),
                            lagAdresse(
                                gyldigFraOgMed = svalbardCutoffDato.plusYears(2),
                                gyldigTilOgMed = svalbardCutoffDato.plusYears(3),
                                ukjentBosted = lagUkjentBosted(bostedskommune = "123"),
                            ),
                        ),
                )

            // Act
            val harAdresserSomErRelevantForSvalbardstillegg = adresser.harAdresserSomErRelevantForSvalbardtillegg()

            // Assert
            assertThat(harAdresserSomErRelevantForSvalbardstillegg).isTrue()
        }

        @Test
        fun `skal returnere true om oppholdsadresse opphold annet sted som er på Svalbard`() {
            // Arrange
            val adresser =
                lagAdresser(
                    oppholdsadresse =
                        listOf(
                            lagAdresse(
                                gyldigFraOgMed = null,
                                gyldigTilOgMed = null,
                                oppholdAnnetSted = OppholdAnnetSted.MILITAER,
                            ),
                            lagAdresse(
                                gyldigFraOgMed = svalbardCutoffDato.minusYears(1),
                                gyldigTilOgMed = svalbardCutoffDato.plusYears(2),
                                oppholdAnnetSted = OppholdAnnetSted.PAA_SVALBARD,
                            ),
                            lagAdresse(
                                gyldigFraOgMed = svalbardCutoffDato.plusYears(2),
                                gyldigTilOgMed = svalbardCutoffDato.plusYears(3),
                                oppholdAnnetSted = OppholdAnnetSted.UTENRIKS,
                            ),
                        ),
                )

            // Act
            val harAdresserSomErRelevantForSvalbardstillegg = adresser.harAdresserSomErRelevantForSvalbardtillegg()

            // Assert
            assertThat(harAdresserSomErRelevantForSvalbardstillegg).isTrue()
        }

        @Test
        fun `skal returnere false om oppholdsadresse kun har vegadresse som ikke er på Svalbard`() {
            // Arrange
            val adresser =
                lagAdresser(
                    oppholdsadresse =
                        listOf(
                            lagAdresse(
                                gyldigFraOgMed = null,
                                gyldigTilOgMed = null,
                                vegadresse = lagVegadresse(kommunenummer = KommunerIFinnmarkOgNordTroms.KARASJOK.kommunenummer),
                            ),
                            lagAdresse(
                                gyldigFraOgMed = svalbardCutoffDato.minusYears(1),
                                gyldigTilOgMed = svalbardCutoffDato.plusYears(2),
                                vegadresse = lagVegadresse(kommunenummer = KommunerIFinnmarkOgNordTroms.ALTA.kommunenummer),
                            ),
                            lagAdresse(
                                gyldigFraOgMed = svalbardCutoffDato.plusYears(2),
                                gyldigTilOgMed = svalbardCutoffDato.plusYears(3),
                                vegadresse = lagVegadresse(kommunenummer = KommunerIFinnmarkOgNordTroms.HAMMERFEST.kommunenummer),
                            ),
                        ),
                )

            // Act
            val harAdresserSomErRelevantForSvalbardstillegg = adresser.harAdresserSomErRelevantForSvalbardtillegg()

            // Assert
            assertThat(harAdresserSomErRelevantForSvalbardstillegg).isFalse()
        }

        @Test
        fun `skal returnere false om oppholdsadresse kun har matrikkeladresse som ikke er på Svalbard`() {
            // Arrange
            val adresser =
                lagAdresser(
                    oppholdsadresse =
                        listOf(
                            lagAdresse(
                                gyldigFraOgMed = null,
                                gyldigTilOgMed = null,
                                matrikkeladresse = lagMatrikkeladresse(kommunenummer = KommunerIFinnmarkOgNordTroms.KARASJOK.kommunenummer),
                            ),
                            lagAdresse(
                                gyldigFraOgMed = svalbardCutoffDato.minusYears(1),
                                gyldigTilOgMed = svalbardCutoffDato.plusYears(2),
                                matrikkeladresse = lagMatrikkeladresse(kommunenummer = KommunerIFinnmarkOgNordTroms.ALTA.kommunenummer),
                            ),
                            lagAdresse(
                                gyldigFraOgMed = svalbardCutoffDato.plusYears(2),
                                gyldigTilOgMed = svalbardCutoffDato.plusYears(3),
                                matrikkeladresse = lagMatrikkeladresse(kommunenummer = KommunerIFinnmarkOgNordTroms.HAMMERFEST.kommunenummer),
                            ),
                        ),
                )

            // Act
            val harAdresserSomErRelevantForSvalbardstillegg = adresser.harAdresserSomErRelevantForSvalbardtillegg()

            // Assert
            assertThat(harAdresserSomErRelevantForSvalbardstillegg).isFalse()
        }

        @Test
        fun `skal returnere false om oppholdsadresse kun har ukjent bosted som ikke er på Svalbard`() {
            // Arrange
            val adresser =
                lagAdresser(
                    oppholdsadresse =
                        listOf(
                            lagAdresse(
                                gyldigFraOgMed = null,
                                gyldigTilOgMed = null,
                                ukjentBosted = lagUkjentBosted(bostedskommune = KommunerIFinnmarkOgNordTroms.VADSØ.kommunenummer),
                            ),
                            lagAdresse(
                                gyldigFraOgMed = svalbardCutoffDato.minusYears(1),
                                gyldigTilOgMed = svalbardCutoffDato.plusYears(2),
                                ukjentBosted = lagUkjentBosted(bostedskommune = KommunerIFinnmarkOgNordTroms.ALTA.kommunenummer),
                            ),
                            lagAdresse(
                                gyldigFraOgMed = svalbardCutoffDato.plusYears(2),
                                gyldigTilOgMed = svalbardCutoffDato.plusYears(3),
                                ukjentBosted = lagUkjentBosted(bostedskommune = KommunerIFinnmarkOgNordTroms.HASVIK.kommunenummer),
                            ),
                        ),
                )

            // Act
            val harAdresserSomErRelevantForSvalbardstillegg = adresser.harAdresserSomErRelevantForSvalbardtillegg()

            // Assert
            assertThat(harAdresserSomErRelevantForSvalbardstillegg).isFalse()
        }

        @Test
        fun `skal returnere false om oppholdsadresse kun har opphold annet sted som ikke er på Svalbard`() {
            // Arrange
            val adresser =
                lagAdresser(
                    oppholdsadresse =
                        listOf(
                            lagAdresse(
                                gyldigFraOgMed = null,
                                gyldigTilOgMed = null,
                                oppholdAnnetSted = OppholdAnnetSted.MILITAER,
                            ),
                            lagAdresse(
                                gyldigFraOgMed = svalbardCutoffDato.minusYears(1),
                                gyldigTilOgMed = svalbardCutoffDato.plusYears(2),
                                oppholdAnnetSted = OppholdAnnetSted.PENDLER,
                            ),
                            lagAdresse(
                                gyldigFraOgMed = svalbardCutoffDato.plusYears(2),
                                gyldigTilOgMed = svalbardCutoffDato.plusYears(3),
                                oppholdAnnetSted = OppholdAnnetSted.UTENRIKS,
                            ),
                        ),
                )

            // Act
            val harAdresserSomErRelevantForSvalbardstillegg = adresser.harAdresserSomErRelevantForSvalbardtillegg()

            // Assert
            assertThat(harAdresserSomErRelevantForSvalbardstillegg).isFalse()
        }
    }

    @Nested
    inner class OpprettFra {
        @Test
        fun `skal opprette adresser fra PDL adresser med bostedsadresser`() {
            // Arrange
            val vegadresse = lagVegadresse()
            val bostedsadresse = lagBostedsadresse(vegadresse = vegadresse)

            val pdlAdresser =
                PdlAdresserPerson(
                    bostedsadresse = listOf(bostedsadresse),
                    deltBosted = emptyList(),
                    oppholdsadresse = emptyList(),
                )

            // Act
            val adresser = Adresser.opprettFra(pdlAdresser)

            // Assert
            assertThat(adresser.bostedsadresser).hasSize(1)
            assertThat(adresser.bostedsadresser).anySatisfy {
                assertThat(it.gyldigFraOgMed).isEqualTo(bostedsadresse.gyldigFraOgMed)
                assertThat(it.gyldigTilOgMed).isEqualTo(bostedsadresse.gyldigTilOgMed)
                assertThat(it.vegadresse).isEqualTo(vegadresse)
                assertThat(it.matrikkeladresse).isNull()
                assertThat(it.ukjentBosted).isNull()
                assertThat(it.oppholdAnnetSted).isNull()
            }
            assertThat(adresser.delteBosteder).isEmpty()
            assertThat(adresser.oppholdsadresse).isEmpty()
        }

        @Test
        fun `skal opprette adresser fra PDL adresser med delte bosteder`() {
            // Arrange
            val vegadresse = lagVegadresse()
            val deltBosted = lagDeltBosted(vegadresse = vegadresse)

            val pdlAdresser =
                PdlAdresserPerson(
                    bostedsadresse = emptyList(),
                    deltBosted = listOf(deltBosted),
                    oppholdsadresse = emptyList(),
                )

            // Act
            val adresser = Adresser.opprettFra(pdlAdresser)

            // Assert
            assertThat(adresser.bostedsadresser).isEmpty()
            assertThat(adresser.delteBosteder).hasSize(1)
            assertThat(adresser.delteBosteder).anySatisfy {
                assertThat(it.gyldigFraOgMed).isEqualTo(deltBosted.startdatoForKontrakt)
                assertThat(it.gyldigTilOgMed).isEqualTo(deltBosted.sluttdatoForKontrakt)
                assertThat(it.vegadresse).isEqualTo(vegadresse)
                assertThat(it.matrikkeladresse).isNull()
                assertThat(it.ukjentBosted).isNull()
                assertThat(it.oppholdAnnetSted).isNull()
            }
            assertThat(adresser.oppholdsadresse).isEmpty()
        }

        @Test
        fun `skal opprette adresser fra PDL adresser med oppholdsadresse`() {
            // Arrange
            val vegadresse = lagVegadresse()
            val oppholdsadresse = lagOppholdsadresse(vegadresse = vegadresse)

            val pdlAdresser =
                PdlAdresserPerson(
                    bostedsadresse = emptyList(),
                    deltBosted = emptyList(),
                    oppholdsadresse = listOf(oppholdsadresse),
                )

            // Act
            val adresser = Adresser.opprettFra(pdlAdresser)

            // Assert
            assertThat(adresser.bostedsadresser).isEmpty()
            assertThat(adresser.delteBosteder).isEmpty()
            assertThat(adresser.oppholdsadresse).hasSize(1)
            assertThat(adresser.oppholdsadresse).anySatisfy {
                assertThat(it.gyldigFraOgMed).isEqualTo(oppholdsadresse.gyldigFraOgMed)
                assertThat(it.gyldigTilOgMed).isEqualTo(oppholdsadresse.gyldigTilOgMed)
                assertThat(it.vegadresse).isEqualTo(vegadresse)
                assertThat(it.matrikkeladresse).isNull()
                assertThat(it.ukjentBosted).isNull()
                assertThat(it.oppholdAnnetSted).isNull()
            }
        }

        @Test
        fun `skal opprette et tomt adresser objekt hvis PDL adresser er null`() {
            // Act
            val adresser = Adresser.opprettFra(null)

            // Assert
            assertThat(adresser.bostedsadresser).isEmpty()
            assertThat(adresser.delteBosteder).isEmpty()
            assertThat(adresser.oppholdsadresse).isEmpty()
        }
    }
}
