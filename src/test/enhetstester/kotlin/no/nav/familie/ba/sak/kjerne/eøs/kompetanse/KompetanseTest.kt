package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.datagenerator.lagKompetanse
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.utbetalingsland
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KompetanseTest {
    @Test
    fun `utbetalingsland - skal returnere landkode for utbetalingsland dersom kompetanse er utfylt`() {
        // Arrange
        val kompetanse =
            lagKompetanse(
                barnAktører = setOf(randomAktør()),
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.ARBEIDER,
                annenForeldersAktivitetsland = "SE",
                barnetsBostedsland = "SE",
                kompetanseResultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
                erAnnenForelderOmfattetAvNorskLovgivning = false,
            )

        // Act
        val utbetalingsland = kompetanse.utbetalingsland()

        // Assert
        assertThat(utbetalingsland).isNotNull
        assertThat(utbetalingsland).isEqualTo("SE")
    }

    @Test
    fun `utbetalingsland - skal returnere null for utbetalingsland dersom kompetanse ikke er utfylt`() {
        // Arrange
        val kompetanse =
            lagKompetanse()

        // Act
        val utbetalingsland = kompetanse.utbetalingsland()

        // Assert
        assertThat(utbetalingsland).isNull()
    }

    @Test
    fun `utbetalingsland - skal returnere Norge for utbetalingsland dersom kompetanse er utfylt og resultatet er at norge er primærland`() {
        // Arrange
        val kompetanse =
            lagKompetanse(
                barnAktører = setOf(randomAktør()),
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.ARBEIDER,
                annenForeldersAktivitetsland = "SE",
                barnetsBostedsland = "SE",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                erAnnenForelderOmfattetAvNorskLovgivning = false,
            )

        // Act
        val utbetalingsland = kompetanse.utbetalingsland()

        // Assert
        assertThat(utbetalingsland).isNotNull
        assertThat(utbetalingsland).isEqualTo("NO")
    }

    @Test
    fun `utbetalingsland - skal returnere utbetalingsland ulikt Norge dersom kompetanse er utfylt og hovedregelen fortsatt gir Norge`() {
        // Arrange
        val kompetanse =
            lagKompetanse(
                barnAktører = setOf(randomAktør()),
                søkersAktivitet = KompetanseAktivitet.MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN,
                søkersAktivitetsland = "SE",
                annenForeldersAktivitet = KompetanseAktivitet.IKKE_AKTUELT,
                annenForeldersAktivitetsland = null,
                barnetsBostedsland = "NO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
                erAnnenForelderOmfattetAvNorskLovgivning = false,
            )

        // Act
        val utbetalingsland = kompetanse.utbetalingsland()

        // Assert
        assertThat(utbetalingsland).isNotNull
        assertThat(utbetalingsland).isEqualTo("SE")
    }
}
