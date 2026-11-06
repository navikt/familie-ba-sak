package no.nav.familie.ba.sak.kjerne.søknad

import no.nav.familie.ba.sak.datagenerator.lagSøknad
import no.nav.familie.ba.sak.datagenerator.randomFnr
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SøknadTest {
    @Nested
    inner class HarKryssetForDeltBostedForMinstEttBarn {
        @Test
        fun `skal gi true når kun det siste barnet har krysset for delt bosted`() {
            // Arrange
            val søknad =
                lagSøknadMedBarn(
                    lagBarn(harKryssetForDeltBosted = false),
                    lagBarn(harKryssetForDeltBosted = true),
                )

            // Act & Assert
            assertThat(søknad.harKryssetForDeltBostedForMinstEttBarn()).isTrue()
        }

        @Test
        fun `skal gi false når ingen av barna har krysset for delt bosted`() {
            // Arrange
            val søknad =
                lagSøknadMedBarn(
                    lagBarn(harKryssetForDeltBosted = false),
                    lagBarn(harKryssetForDeltBosted = false),
                )

            // Act & Assert
            assertThat(søknad.harKryssetForDeltBostedForMinstEttBarn()).isFalse()
        }

        @Test
        fun `skal gi false når søknaden ikke har barn`() {
            // Arrange
            val søknad = lagSøknadMedBarn()

            // Act & Assert
            assertThat(søknad.harKryssetForDeltBostedForMinstEttBarn()).isFalse()
        }
    }

    @Nested
    inner class HarKryssetForFosterhjemEllerBeredskapshjemForMinstEttBarn {
        @Test
        fun `skal gi true når kun det siste barnet er fosterbarn`() {
            // Arrange
            val søknad =
                lagSøknadMedBarn(
                    lagBarn(erFosterbarn = false),
                    lagBarn(erFosterbarn = true),
                )

            // Act & Assert
            assertThat(søknad.harKryssetForFosterhjemEllerBeredskapshjemForMinstEttBarn()).isTrue()
        }

        @Test
        fun `skal gi false når ingen av barna er fosterbarn`() {
            // Arrange
            val søknad =
                lagSøknadMedBarn(
                    lagBarn(erFosterbarn = false),
                    lagBarn(erFosterbarn = false),
                )

            // Act & Assert
            assertThat(søknad.harKryssetForFosterhjemEllerBeredskapshjemForMinstEttBarn()).isFalse()
        }

        @Test
        fun `skal gi false når ingen av barna er fosterbarn eller bor i beredskapshjem`() {
            // Arrange
            val søknad =
                lagSøknadMedBarn(
                    lagBarn(erFosterbarn = false),
                    lagBarn(erIBeredskapshjem = false),
                )

            // Act & Assert
            assertThat(søknad.harKryssetForFosterhjemEllerBeredskapshjemForMinstEttBarn()).isFalse()
        }

        @Test
        fun `skal gi true når minst ett barn bor i beredskapshjem`() {
            // Arrange
            val søknad =
                lagSøknadMedBarn(
                    lagBarn(),
                    lagBarn(erIBeredskapshjem = true),
                )

            // Act & Assert
            assertThat(søknad.harKryssetForFosterhjemEllerBeredskapshjemForMinstEttBarn()).isTrue()
        }

        @Test
        fun `skal gi true når barn er registrert i både fosterhjem og beredskapshjem`() {
            // Arrange
            val søknad =
                lagSøknadMedBarn(
                    lagBarn(
                        erFosterbarn = true,
                        erIBeredskapshjem = true,
                    ),
                )

            // Act & Assert
            assertThat(søknad.harKryssetForFosterhjemEllerBeredskapshjemForMinstEttBarn()).isTrue()
        }

        @Test
        fun `skal gi false når søknaden ikke har barn`() {
            // Arrange
            val søknad = lagSøknadMedBarn()

            // Act & Assert
            assertThat(søknad.harKryssetForFosterhjemEllerBeredskapshjemForMinstEttBarn()).isFalse()
        }
    }

    @Nested
    inner class HarKryssetForInstitusjonForMinstEttBarn {
        @Test
        fun `skal gi true når kun det siste barnet oppholder seg i institusjon`() {
            // Arrange
            val søknad =
                lagSøknadMedBarn(
                    lagBarn(oppholderSegIInstitusjon = false),
                    lagBarn(oppholderSegIInstitusjon = true),
                )

            // Act & Assert
            assertThat(søknad.harKryssetForInstitusjonForMinstEttBarn()).isTrue()
        }

        @Test
        fun `skal gi false når ingen av barna oppholder seg i institusjon`() {
            // Arrange
            val søknad =
                lagSøknadMedBarn(
                    lagBarn(oppholderSegIInstitusjon = false),
                    lagBarn(oppholderSegIInstitusjon = false),
                )

            // Act & Assert
            assertThat(søknad.harKryssetForInstitusjonForMinstEttBarn()).isFalse()
        }

        @Test
        fun `skal gi false når søknaden ikke har barn`() {
            // Arrange
            val søknad = lagSøknadMedBarn()

            // Act & Assert
            assertThat(søknad.harKryssetForInstitusjonForMinstEttBarn()).isFalse()
        }
    }

    @Nested
    inner class HarKryssetForAsylsøkerForSøkerEllerMinstEttBarn {
        @Test
        fun `skal gi true når søker er asylsøker`() {
            // Arrange
            val søknad = lagSøknad(søkerErAsylsøker = true)

            // Act & Assert
            assertThat(søknad.harKryssetForAsylsøkerForSøkerEllerMinstEttBarn()).isTrue()
        }

        @Test
        fun `skal gi true når kun det siste barnet er asylsøker`() {
            // Arrange
            val søknad =
                lagSøknadMedBarn(
                    lagBarn(erAsylsøker = false),
                    lagBarn(erAsylsøker = true),
                )

            // Act & Assert
            assertThat(søknad.harKryssetForAsylsøkerForSøkerEllerMinstEttBarn()).isTrue()
        }

        @Test
        fun `skal gi false når verken søker eller barna er asylsøkere`() {
            // Arrange
            val søknad =
                lagSøknadMedBarn(
                    lagBarn(erAsylsøker = false),
                    lagBarn(erAsylsøker = false),
                )

            // Act & Assert
            assertThat(søknad.harKryssetForAsylsøkerForSøkerEllerMinstEttBarn()).isFalse()
        }
    }

    @Nested
    inner class HarKryssetForAdoptertFraUtlandetForMinstEttBarn {
        @Test
        fun `skal gi true når kun det siste barnet er adoptert fra utlandet`() {
            // Arrange
            val søknad =
                lagSøknadMedBarn(
                    lagBarn(erAdoptertFraUtland = false),
                    lagBarn(erAdoptertFraUtland = true),
                )

            // Act & Assert
            assertThat(søknad.harKryssetForAdoptertFraUtlandetForMinstEttBarn()).isTrue()
        }

        @Test
        fun `skal gi false når ingen av barna er adoptert fra utlandet`() {
            // Arrange
            val søknad =
                lagSøknadMedBarn(
                    lagBarn(erAdoptertFraUtland = false),
                    lagBarn(erAdoptertFraUtland = false),
                )

            // Act & Assert
            assertThat(søknad.harKryssetForAdoptertFraUtlandetForMinstEttBarn()).isFalse()
        }

        @Test
        fun `skal gi false når søknaden ikke har barn`() {
            // Arrange
            val søknad = lagSøknadMedBarn()

            // Act & Assert
            assertThat(søknad.harKryssetForAdoptertFraUtlandetForMinstEttBarn()).isFalse()
        }
    }

    private fun lagSøknadMedBarn(vararg barn: Barn): Søknad = lagSøknad().copy(barn = barn.toList())

    private fun lagBarn(
        erFosterbarn: Boolean = false,
        erIBeredskapshjem: Boolean = false,
        harKryssetForDeltBosted: Boolean = false,
        oppholderSegIInstitusjon: Boolean = false,
        erAsylsøker: Boolean = false,
        erAdoptertFraUtland: Boolean = false,
    ): Barn =
        Barn(
            fnr = randomFnr(),
            planleggerÅBoINorge12Mnd = true,
            erFosterbarn = erFosterbarn,
            erIBeredskapshjem = erIBeredskapshjem,
            harKryssetForDeltBosted = harKryssetForDeltBosted,
            oppholderSegIInstitusjon = oppholderSegIInstitusjon,
            erAsylsøker = erAsylsøker,
            erAdoptertFraUtland = erAdoptertFraUtland,
        )
}
