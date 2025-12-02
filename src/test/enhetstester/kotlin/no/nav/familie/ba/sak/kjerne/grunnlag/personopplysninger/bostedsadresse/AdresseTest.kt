package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse

import no.nav.familie.ba.sak.datagenerator.lagAdresse
import no.nav.familie.ba.sak.datagenerator.lagBostedsadresse
import no.nav.familie.ba.sak.datagenerator.lagDeltBosted
import no.nav.familie.ba.sak.datagenerator.lagMatrikkeladresse
import no.nav.familie.ba.sak.datagenerator.lagOppholdsadresse
import no.nav.familie.ba.sak.datagenerator.lagUkjentBosted
import no.nav.familie.ba.sak.datagenerator.lagVegadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.Adresse
import no.nav.familie.kontrakter.ba.finnmarkstillegg.KommunerIFinnmarkOgNordTroms
import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted
import no.nav.familie.kontrakter.felles.svalbard.SvalbardKommune
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AdresseTest {
    @Nested
    inner class ErFomOgTomNull {
        @Test
        fun `skal returnere true når fom og tom er null`() {
            // Arrange
            val adresse =
                lagAdresse(
                    gyldigFraOgMed = null,
                    gyldigTilOgMed = null,
                )

            // Act
            val erFomOgTomNull = adresse.erFomOgTomNull()

            // Assert
            assertThat(erFomOgTomNull).isTrue()
        }

        @Test
        fun `skal returnere false når fom er null men tom ikke er null`() {
            // Arrange
            val adresse =
                lagAdresse(
                    gyldigFraOgMed = null,
                    gyldigTilOgMed = LocalDate.now(),
                )

            // Act
            val erFomOgTomNull = adresse.erFomOgTomNull()

            // Assert
            assertThat(erFomOgTomNull).isFalse()
        }

        @Test
        fun `skal returnere false når fom ikke er null men tom er null`() {
            // Arrange
            val adresse =
                lagAdresse(
                    gyldigFraOgMed = LocalDate.now(),
                    gyldigTilOgMed = null,
                )

            // Act
            val erFomOgTomNull = adresse.erFomOgTomNull()

            // Assert
            assertThat(erFomOgTomNull).isFalse()
        }

        @Test
        fun `skal returnere false når hverken fom eller tom er null`() {
            // Arrange
            val dagensDato = LocalDate.now()

            val adresse =
                lagAdresse(
                    gyldigFraOgMed = dagensDato,
                    gyldigTilOgMed = dagensDato,
                )

            // Act
            val erFomOgTomNull = adresse.erFomOgTomNull()

            // Assert
            assertThat(erFomOgTomNull).isFalse()
        }
    }

    @Nested
    inner class OverlapperMedDato {
        @Test
        fun `skal returnere true hvis adresse hverken har en fra og med dato eller en til og med dato`() {
            // Arrange
            val dagensDato = LocalDate.now()

            val adresse =
                lagAdresse(
                    gyldigFraOgMed = null,
                    gyldigTilOgMed = null,
                )

            // Act
            val overlapperMedDato = adresse.overlapperMedDato(dagensDato)

            // Assert
            assertThat(overlapperMedDato).isTrue()
        }

        @Test
        fun `skal returnere true når innsendt dato er mellom fom og tom, men fom er null`() {
            // Arrange
            val dagensDato = LocalDate.now()

            val adresse =
                lagAdresse(
                    gyldigFraOgMed = null,
                    gyldigTilOgMed = dagensDato.plusDays(1),
                )

            // Act
            val overlapperMedDato = adresse.overlapperMedDato(dagensDato)

            // Assert
            assertThat(overlapperMedDato).isTrue()
        }

        @Test
        fun `skal returnere true når innsendt dato er lik fom og tom er null`() {
            // Arrange
            val dagensDato = LocalDate.now()

            val adresse =
                lagAdresse(
                    gyldigFraOgMed = dagensDato,
                    gyldigTilOgMed = null,
                )

            // Act
            val overlapperMedDato = adresse.overlapperMedDato(dagensDato)

            // Assert
            assertThat(overlapperMedDato).isTrue()
        }

        @Test
        fun `skal returnere true når innsendt dato er mellom fom og tom, men tom er null`() {
            // Arrange
            val dagensDato = LocalDate.now()

            val adresse =
                lagAdresse(
                    gyldigFraOgMed = dagensDato.minusDays(1),
                    gyldigTilOgMed = null,
                )

            // Act
            val overlapperMedDato = adresse.overlapperMedDato(dagensDato)

            // Assert
            assertThat(overlapperMedDato).isTrue()
        }

        @Test
        fun `skal returnere true når innsendt dato er lik tom og fom er null`() {
            // Arrange
            val dagensDato = LocalDate.now()

            val adresse =
                lagAdresse(
                    gyldigFraOgMed = null,
                    gyldigTilOgMed = dagensDato,
                )

            // Act
            val overlapperMedDato = adresse.overlapperMedDato(dagensDato)

            // Assert
            assertThat(overlapperMedDato).isTrue()
        }

        @Test
        fun `skal returnere true når innsendt dato er mellom fom og tom`() {
            // Arrange
            val dagensDato = LocalDate.now()

            val adresse =
                lagAdresse(
                    gyldigFraOgMed = dagensDato.minusDays(1),
                    gyldigTilOgMed = dagensDato.plusDays(1),
                )

            // Act
            val overlapperMedDato = adresse.overlapperMedDato(dagensDato)

            // Assert
            assertThat(overlapperMedDato).isTrue()
        }

        @Test
        fun `skal returnere true når innsendt dato er lik både fom og tom`() {
            // Arrange
            val dagensDato = LocalDate.now()

            val adresse =
                lagAdresse(
                    gyldigFraOgMed = dagensDato,
                    gyldigTilOgMed = dagensDato,
                )

            // Act
            val overlapperMedDato = adresse.overlapperMedDato(dagensDato)

            // Assert
            assertThat(overlapperMedDato).isTrue()
        }

        @Test
        fun `skal returnere false når innsendt dato er før både fom og tom`() {
            // Arrange
            val dagensDato = LocalDate.now()

            val adresse =
                lagAdresse(
                    gyldigFraOgMed = dagensDato.plusDays(1),
                    gyldigTilOgMed = dagensDato.plusDays(2),
                )

            // Act
            val overlapperMedDato = adresse.overlapperMedDato(dagensDato)

            // Assert
            assertThat(overlapperMedDato).isFalse()
        }

        @Test
        fun `skal returnere false når innsendt dato er etter både fom og tom`() {
            // Arrange
            val dagensDato = LocalDate.now()

            val adresse =
                lagAdresse(
                    gyldigFraOgMed = dagensDato.minusDays(2),
                    gyldigTilOgMed = dagensDato.minusDays(1),
                )

            // Act
            val overlapperMedDato = adresse.overlapperMedDato(dagensDato)

            // Assert
            assertThat(overlapperMedDato).isFalse()
        }
    }

    @Nested
    inner class ErIFinnmarkEllerNordTroms {
        @Test
        fun `skal returnere true om vegadresse er i finnmark eller nord troms`() {
            // Arrange
            val adresse =
                lagAdresse(
                    vegadresse =
                        lagVegadresse(
                            kommunenummer = KommunerIFinnmarkOgNordTroms.ALTA.kommunenummer,
                        ),
                )

            // Act
            val erIFinnmarkEllerNordTroms = adresse.erIFinnmarkEllerNordTroms()

            // Assert
            assertThat(erIFinnmarkEllerNordTroms).isTrue()
        }

        @Test
        fun `skal returnere true om matrikkeladresse er i finnmark eller nord troms`() {
            // Arrange
            val adresse =
                lagAdresse(
                    matrikkeladresse =
                        lagMatrikkeladresse(
                            kommunenummer = KommunerIFinnmarkOgNordTroms.ALTA.kommunenummer,
                        ),
                )

            // Act
            val erIFinnmarkEllerNordTroms = adresse.erIFinnmarkEllerNordTroms()

            // Assert
            assertThat(erIFinnmarkEllerNordTroms).isTrue()
        }

        @Test
        fun `skal returnere true om ukjent bosted er i finnmark eller nord troms`() {
            // Arrange
            val adresse =
                lagAdresse(
                    ukjentBosted =
                        lagUkjentBosted(
                            bostedskommune = KommunerIFinnmarkOgNordTroms.ALTA.kommunenummer,
                        ),
                )

            // Act
            val erIFinnmarkEllerNordTroms = adresse.erIFinnmarkEllerNordTroms()

            // Assert
            assertThat(erIFinnmarkEllerNordTroms).isTrue()
        }

        @Test
        fun `skal returnere false om man kun har vegadresse og den ikke er i finnmark eller nord troms`() {
            // Arrange
            val adresse =
                lagAdresse(
                    vegadresse =
                        lagVegadresse(
                            kommunenummer = "123",
                        ),
                )

            // Act
            val erIFinnmarkEllerNordTroms = adresse.erIFinnmarkEllerNordTroms()

            // Assert
            assertThat(erIFinnmarkEllerNordTroms).isFalse()
        }

        @Test
        fun `skal returnere false om man kun har matrikkeladresse og den ikke er i finnmark eller nord troms`() {
            // Arrange
            val adresse =
                lagAdresse(
                    matrikkeladresse =
                        lagMatrikkeladresse(
                            kommunenummer = "123",
                        ),
                )

            // Act
            val erIFinnmarkEllerNordTroms = adresse.erIFinnmarkEllerNordTroms()

            // Assert
            assertThat(erIFinnmarkEllerNordTroms).isFalse()
        }

        @Test
        fun `skal returnere false om man kun har ukjent bosted og den ikke er i finnmark eller nord troms`() {
            // Arrange
            val adresse =
                lagAdresse(
                    ukjentBosted =
                        lagUkjentBosted(
                            bostedskommune = "123",
                        ),
                )

            // Act
            val erIFinnmarkEllerNordTroms = adresse.erIFinnmarkEllerNordTroms()

            // Assert
            assertThat(erIFinnmarkEllerNordTroms).isFalse()
        }
    }

    @Nested
    inner class ErPåSvalbard {
        @Test
        fun `skal returnere true om vegadresse er på svalbard`() {
            // Arrange
            val adresse =
                lagAdresse(
                    vegadresse =
                        lagVegadresse(
                            kommunenummer = SvalbardKommune.SVALBARD.kommunenummer,
                        ),
                )

            // Act
            val erPåSvalbard = adresse.erPåSvalbard()

            // Assert
            assertThat(erPåSvalbard).isTrue()
        }

        @Test
        fun `skal returnere true om matrikkeladresse er på svalbard`() {
            // Arrange
            val adresse =
                lagAdresse(
                    matrikkeladresse =
                        lagMatrikkeladresse(
                            kommunenummer = SvalbardKommune.SVALBARD.kommunenummer,
                        ),
                )

            // Act
            val erPåSvalbard = adresse.erPåSvalbard()

            // Assert
            assertThat(erPåSvalbard).isTrue()
        }

        @Test
        fun `skal returnere true om ukjent bosted er på svalbard`() {
            // Arrange
            val adresse =
                lagAdresse(
                    ukjentBosted =
                        lagUkjentBosted(
                            bostedskommune = SvalbardKommune.SVALBARD.kommunenummer,
                        ),
                )

            // Act
            val erPåSvalbard = adresse.erPåSvalbard()

            // Assert
            assertThat(erPåSvalbard).isTrue()
        }

        @Test
        fun `skal returnere true om opphold annet sted er på svalbard`() {
            // Arrange
            val adresse =
                lagAdresse(
                    oppholdAnnetSted = OppholdAnnetSted.PAA_SVALBARD,
                )

            // Act
            val erPåSvalbard = adresse.erPåSvalbard()

            // Assert
            assertThat(erPåSvalbard).isTrue()
        }

        @Test
        fun `skal returnere false om man kun har vegadresse og den ikke er på svalbard`() {
            // Arrange
            val adresse =
                lagAdresse(
                    vegadresse =
                        lagVegadresse(
                            kommunenummer = "123",
                        ),
                )

            // Act
            val erPåSvalbard = adresse.erPåSvalbard()

            // Assert
            assertThat(erPåSvalbard).isFalse()
        }

        @Test
        fun `skal returnere false om man kun har matrikkeladresse og den ikke er på svalbard`() {
            // Arrange
            val adresse =
                lagAdresse(
                    matrikkeladresse =
                        lagMatrikkeladresse(
                            kommunenummer = "123",
                        ),
                )

            // Act
            val erPåSvalbard = adresse.erPåSvalbard()

            // Assert
            assertThat(erPåSvalbard).isFalse()
        }

        @Test
        fun `skal returnere false om man kun har ukjent bosted og den ikke er på svalbard`() {
            // Arrange
            val adresse =
                lagAdresse(
                    ukjentBosted =
                        lagUkjentBosted(
                            bostedskommune = "123",
                        ),
                )

            // Act
            val erPåSvalbard = adresse.erPåSvalbard()

            // Assert
            assertThat(erPåSvalbard).isFalse()
        }

        @Test
        fun `skal returnere false om man kun har opphold annet sted og den ikke er på svalbard`() {
            // Arrange
            val adresse =
                lagAdresse(
                    oppholdAnnetSted = OppholdAnnetSted.MILITAER,
                )

            // Act
            val erPåSvalbard = adresse.erPåSvalbard()

            // Assert
            assertThat(erPåSvalbard).isFalse()
        }
    }

    @Nested
    inner class ErINorge {
        @Test
        fun `skal returnere true om vegadresse ikke er null`() {
            // Arrange
            val adresse = lagAdresse(vegadresse = lagVegadresse())

            // Act
            val erINorge = adresse.erINorge()

            // Assert
            assertThat(erINorge).isTrue()
        }

        @Test
        fun `skal returnere true om matrikkeladresse ikke er null`() {
            // Arrange
            val adresse = lagAdresse(matrikkeladresse = lagMatrikkeladresse())

            // Act
            val erINorge = adresse.erINorge()

            // Assert
            assertThat(erINorge).isTrue()
        }

        @Test
        fun `skal returnere true om ukjent bosted ikke er null`() {
            // Arrange
            val adresse = lagAdresse(ukjentBosted = lagUkjentBosted("123"))

            // Act
            val erINorge = adresse.erINorge()

            // Assert
            assertThat(erINorge).isTrue()
        }

        @Test
        fun `skal returnere false om vegadresse, matrikkeladresse, og ukjent bosted er null`() {
            // Arrange
            val adresse = lagAdresse(vegadresse = null, matrikkeladresse = null, ukjentBosted = null)

            // Act
            val erINorge = adresse.erINorge()

            // Assert
            assertThat(erINorge).isFalse()
        }
    }

    @Nested
    inner class OpprettFra {
        @Nested
        inner class Bostedsadresse {
            @Test
            fun `skal opprette fra bostedsadresse med vegadresse`() {
                // Arrange
                val dagensDato = LocalDate.now()
                val gyldigFraOgMed = dagensDato.minusYears(1)
                val gyldigTilOgMed = dagensDato.plusYears(1)
                val angittFlyttedato = dagensDato.minusYears(1)
                val vegadresse = lagVegadresse()

                val bostedsadresse =
                    lagBostedsadresse(
                        gyldigFraOgMed = gyldigFraOgMed,
                        gyldigTilOgMed = gyldigTilOgMed,
                        angittFlyttedato = angittFlyttedato,
                        vegadresse = vegadresse,
                    )

                // Act
                val adresse = Adresse.opprettFra(bostedsadresse)

                // Assert
                assertThat(adresse.gyldigFraOgMed).isEqualTo(gyldigFraOgMed)
                assertThat(adresse.gyldigTilOgMed).isEqualTo(gyldigTilOgMed)
                assertThat(adresse.vegadresse).isEqualTo(vegadresse)
                assertThat(adresse.matrikkeladresse).isNull()
                assertThat(adresse.ukjentBosted).isNull()
                assertThat(adresse.oppholdAnnetSted).isNull()
            }

            @Test
            fun `skal opprette fra bostedsadresse med matrikkeladresse`() {
                // Arrange
                val dagensDato = LocalDate.now()
                val gyldigFraOgMed = dagensDato.minusYears(1)
                val gyldigTilOgMed = dagensDato.plusYears(1)
                val angittFlyttedato = dagensDato.minusYears(1)
                val matrikkeladresse = lagMatrikkeladresse()

                val bostedsadresse =
                    lagBostedsadresse(
                        gyldigFraOgMed = gyldigFraOgMed,
                        gyldigTilOgMed = gyldigTilOgMed,
                        angittFlyttedato = angittFlyttedato,
                        matrikkeladresse = matrikkeladresse,
                    )

                // Act
                val adresse = Adresse.opprettFra(bostedsadresse)

                // Assert
                assertThat(adresse.gyldigFraOgMed).isEqualTo(gyldigFraOgMed)
                assertThat(adresse.gyldigTilOgMed).isEqualTo(gyldigTilOgMed)
                assertThat(adresse.vegadresse).isNull()
                assertThat(adresse.matrikkeladresse).isEqualTo(matrikkeladresse)
                assertThat(adresse.ukjentBosted).isNull()
                assertThat(adresse.oppholdAnnetSted).isNull()
            }

            @Test
            fun `skal opprette fra bostedsadresse med ukjent bosted`() {
                // Arrange
                val dagensDato = LocalDate.now()
                val gyldigFraOgMed = dagensDato.minusYears(1)
                val gyldigTilOgMed = dagensDato.plusYears(1)
                val angittFlyttedato = dagensDato.minusYears(1)
                val ukjentBosted = lagUkjentBosted("123")

                val bostedsadresse =
                    lagBostedsadresse(
                        gyldigFraOgMed = gyldigFraOgMed,
                        gyldigTilOgMed = gyldigTilOgMed,
                        angittFlyttedato = angittFlyttedato,
                        ukjentBosted = ukjentBosted,
                    )

                // Act
                val adresse = Adresse.opprettFra(bostedsadresse)

                // Assert
                assertThat(adresse.gyldigFraOgMed).isEqualTo(gyldigFraOgMed)
                assertThat(adresse.gyldigTilOgMed).isEqualTo(gyldigTilOgMed)
                assertThat(adresse.vegadresse).isNull()
                assertThat(adresse.matrikkeladresse).isNull()
                assertThat(adresse.ukjentBosted).isEqualTo(ukjentBosted)
                assertThat(adresse.oppholdAnnetSted).isNull()
            }
        }

        @Nested
        inner class DeltBosted {
            @Test
            fun `skal opprette fra delt bosted med vegadresse`() {
                // Arrange
                val dagensDato = LocalDate.now()
                val gyldigFraOgMed = dagensDato.minusYears(1)
                val gyldigTilOgMed = dagensDato.plusYears(1)
                val vegadresse = lagVegadresse()

                val deltBosted =
                    lagDeltBosted(
                        startdatoForKontrakt = gyldigFraOgMed,
                        sluttdatoForKontrakt = gyldigTilOgMed,
                        vegadresse = vegadresse,
                    )

                // Act
                val adresse = Adresse.opprettFra(deltBosted)

                // Assert
                assertThat(adresse.gyldigFraOgMed).isEqualTo(gyldigFraOgMed)
                assertThat(adresse.gyldigTilOgMed).isEqualTo(gyldigTilOgMed)
                assertThat(adresse.vegadresse).isEqualTo(vegadresse)
                assertThat(adresse.matrikkeladresse).isNull()
                assertThat(adresse.ukjentBosted).isNull()
                assertThat(adresse.oppholdAnnetSted).isNull()
            }

            @Test
            fun `skal opprette fra delt bosted med matrikkeladresse`() {
                // Arrange
                val dagensDato = LocalDate.now()
                val gyldigFraOgMed = dagensDato.minusYears(1)
                val gyldigTilOgMed = dagensDato.plusYears(1)
                val matrikkeladresse = lagMatrikkeladresse()

                val deltBosted =
                    lagDeltBosted(
                        startdatoForKontrakt = gyldigFraOgMed,
                        sluttdatoForKontrakt = gyldigTilOgMed,
                        matrikkeladresse = matrikkeladresse,
                    )

                // Act
                val adresse = Adresse.opprettFra(deltBosted)

                // Assert
                assertThat(adresse.gyldigFraOgMed).isEqualTo(gyldigFraOgMed)
                assertThat(adresse.gyldigTilOgMed).isEqualTo(gyldigTilOgMed)
                assertThat(adresse.vegadresse).isNull()
                assertThat(adresse.matrikkeladresse).isEqualTo(matrikkeladresse)
                assertThat(adresse.ukjentBosted).isNull()
                assertThat(adresse.oppholdAnnetSted).isNull()
            }

            @Test
            fun `skal opprette fra delt bosted med ukjent bosted`() {
                // Arrange
                val dagensDato = LocalDate.now()
                val gyldigFraOgMed = dagensDato.minusYears(1)
                val gyldigTilOgMed = dagensDato.plusYears(1)
                val ukjentBosted = lagUkjentBosted("123")

                val deltBosted =
                    lagDeltBosted(
                        startdatoForKontrakt = gyldigFraOgMed,
                        sluttdatoForKontrakt = gyldigTilOgMed,
                        ukjentBosted = ukjentBosted,
                    )

                // Act
                val adresse = Adresse.opprettFra(deltBosted)

                // Assert
                assertThat(adresse.gyldigFraOgMed).isEqualTo(gyldigFraOgMed)
                assertThat(adresse.gyldigTilOgMed).isEqualTo(gyldigTilOgMed)
                assertThat(adresse.vegadresse).isNull()
                assertThat(adresse.matrikkeladresse).isNull()
                assertThat(adresse.ukjentBosted).isEqualTo(ukjentBosted)
                assertThat(adresse.oppholdAnnetSted).isNull()
            }
        }

        @Nested
        inner class Oppholdsadresse {
            @Test
            fun `skal opprette fra oppholdsadresse med vegadresse`() {
                // Arrange
                val dagensDato = LocalDate.now()
                val gyldigFraOgMed = dagensDato.minusYears(1)
                val gyldigTilOgMed = dagensDato.plusYears(1)
                val vegadresse = lagVegadresse()

                val oppholdsadresse =
                    lagOppholdsadresse(
                        gyldigFraOgMed = gyldigFraOgMed,
                        gyldigTilOgMed = gyldigTilOgMed,
                        vegadresse = vegadresse,
                    )

                // Act
                val adresse = Adresse.opprettFra(oppholdsadresse)

                // Assert
                assertThat(adresse.gyldigFraOgMed).isEqualTo(gyldigFraOgMed)
                assertThat(adresse.gyldigTilOgMed).isEqualTo(gyldigTilOgMed)
                assertThat(adresse.vegadresse).isEqualTo(vegadresse)
                assertThat(adresse.matrikkeladresse).isNull()
                assertThat(adresse.ukjentBosted).isNull()
                assertThat(adresse.oppholdAnnetSted).isNull()
            }

            @Test
            fun `skal opprette fra oppholdsadresse med matrikkeladresse`() {
                // Arrange
                val dagensDato = LocalDate.now()
                val gyldigFraOgMed = dagensDato.minusYears(1)
                val gyldigTilOgMed = dagensDato.plusYears(1)
                val matrikkeladresse = lagMatrikkeladresse()

                val oppholdsadresse =
                    lagOppholdsadresse(
                        gyldigFraOgMed = gyldigFraOgMed,
                        gyldigTilOgMed = gyldigTilOgMed,
                        matrikkeladresse = matrikkeladresse,
                    )

                // Act
                val adresse = Adresse.opprettFra(oppholdsadresse)

                // Assert
                assertThat(adresse.gyldigFraOgMed).isEqualTo(gyldigFraOgMed)
                assertThat(adresse.gyldigTilOgMed).isEqualTo(gyldigTilOgMed)
                assertThat(adresse.vegadresse).isNull()
                assertThat(adresse.matrikkeladresse).isEqualTo(matrikkeladresse)
                assertThat(adresse.ukjentBosted).isNull()
                assertThat(adresse.oppholdAnnetSted).isNull()
            }

            @Test
            fun `skal opprette fra oppholdsadresse med opphold annet sted lik PAA_SVALBARD`() {
                // Arrange
                val dagensDato = LocalDate.now()
                val gyldigFraOgMed = dagensDato.minusYears(1)
                val gyldigTilOgMed = dagensDato.plusYears(1)

                val oppholdsadresse =
                    lagOppholdsadresse(
                        gyldigFraOgMed = gyldigFraOgMed,
                        gyldigTilOgMed = gyldigTilOgMed,
                        oppholdAnnetSted = OppholdAnnetSted.PAA_SVALBARD.name,
                    )

                // Act
                val adresse = Adresse.opprettFra(oppholdsadresse)

                // Assert
                assertThat(adresse.gyldigFraOgMed).isEqualTo(gyldigFraOgMed)
                assertThat(adresse.gyldigTilOgMed).isEqualTo(gyldigTilOgMed)
                assertThat(adresse.vegadresse).isNull()
                assertThat(adresse.matrikkeladresse).isNull()
                assertThat(adresse.ukjentBosted).isNull()
                assertThat(adresse.oppholdAnnetSted).isEqualTo(OppholdAnnetSted.PAA_SVALBARD)
            }

            @Test
            fun `skal opprette fra oppholdsadresse med opphold annet sted lik paaSvalbard`() {
                // Arrange
                val dagensDato = LocalDate.now()
                val gyldigFraOgMed = dagensDato.minusYears(1)
                val gyldigTilOgMed = dagensDato.plusYears(1)

                val oppholdsadresse =
                    lagOppholdsadresse(
                        gyldigFraOgMed = gyldigFraOgMed,
                        gyldigTilOgMed = gyldigTilOgMed,
                        oppholdAnnetSted = OppholdAnnetSted.PAA_SVALBARD.kode,
                    )

                // Act
                val adresse = Adresse.opprettFra(oppholdsadresse)

                // Assert
                assertThat(adresse.gyldigFraOgMed).isEqualTo(gyldigFraOgMed)
                assertThat(adresse.gyldigTilOgMed).isEqualTo(gyldigTilOgMed)
                assertThat(adresse.vegadresse).isNull()
                assertThat(adresse.matrikkeladresse).isNull()
                assertThat(adresse.ukjentBosted).isNull()
                assertThat(adresse.oppholdAnnetSted).isEqualTo(OppholdAnnetSted.PAA_SVALBARD)
            }
        }
    }
}
