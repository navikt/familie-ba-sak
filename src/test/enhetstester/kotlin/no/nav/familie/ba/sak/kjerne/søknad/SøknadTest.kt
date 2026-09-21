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
        fun `skal gi false når søknaden ikke har barn`() {
            // Arrange
            val søknad = lagSøknadMedBarn()

            // Act & Assert
            assertThat(søknad.harKryssetForFosterhjemEllerBeredskapshjemForMinstEttBarn()).isFalse()
        }
    }

    private fun lagSøknadMedBarn(vararg barn: Barn): Søknad = lagSøknad().copy(barn = barn.toList())

    private fun lagBarn(
        erFosterbarn: Boolean = false,
        harKryssetForDeltBosted: Boolean = false,
    ): Barn =
        Barn(
            fnr = randomFnr(),
            planleggerÅBoINorge12Mnd = true,
            erFosterbarn = erFosterbarn,
            harKryssetForDeltBosted = harKryssetForDeltBosted,
        )
}
