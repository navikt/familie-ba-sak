package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.YtelsetypeBA
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class YtelseTypeTest {
    @Test
    fun `skal mappe ORDINÆR_BARNETRYGD ytelsestype til ORDINÆR_BARNETRYGD ytelsetypeBA`() {
        // Act
        val ytelsetypeBA = YtelseType.ORDINÆR_BARNETRYGD.tilYtelseType()

        // Assert
        assertThat(ytelsetypeBA).isEqualTo(YtelsetypeBA.ORDINÆR_BARNETRYGD)
    }

    @Test
    fun `skal mappe UTVIDET_BARNETRYGD ytelsestype til UTVIDET_BARNETRYGD ytelsetypeBA`() {
        // Act
        val ytelsetypeBA = YtelseType.UTVIDET_BARNETRYGD.tilYtelseType()

        // Assert
        assertThat(ytelsetypeBA).isEqualTo(YtelsetypeBA.UTVIDET_BARNETRYGD)
    }

    @Test
    fun `skal mappe SMÅBARNSTILLEGG ytelsestype til SMÅBARNSTILLEGG ytelsetypeBA`() {
        // Act
        val ytelsetypeBA = YtelseType.SMÅBARNSTILLEGG.tilYtelseType()

        // Assert
        assertThat(ytelsetypeBA).isEqualTo(YtelsetypeBA.SMÅBARNSTILLEGG)
    }
}
