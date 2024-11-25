package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.YtelsetypeBA
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class YtelseTypeTest {
    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `skal mappe ORDINÆR_BARNETRYGD ytelsestype til ORDINÆR_BARNETRYGD ytelsetypeBA`(skalBrukeNyKlassekodeForUtvidetBarnetrygd: Boolean) {
        // Act
        val ytelsetypeBA = YtelseType.ORDINÆR_BARNETRYGD.tilYtelseType(skalBrukeNyKlassekodeForUtvidetBarnetrygd)

        // Assert
        assertThat(ytelsetypeBA).isEqualTo(YtelsetypeBA.ORDINÆR_BARNETRYGD)
    }

    @Test
    fun `skal mappe UTVIDET_BARNETRYGD ytelsestype til UTVIDET_BARNETRYGD ytelsetypeBA når skalBrukeNyKlassekodeForUtvidetBarnetrygd er true`() {
        // Act
        val ytelsetypeBA = YtelseType.UTVIDET_BARNETRYGD.tilYtelseType(true)

        // Assert
        assertThat(ytelsetypeBA).isEqualTo(YtelsetypeBA.UTVIDET_BARNETRYGD)
    }

    @Test
    fun `skal mappe UTVIDET_BARNETRYGD ytelsestype til UTVIDET_BARNETRYGD_GAMMEL ytelsetypeBA når skalBrukeNyKlassekodeForUtvidetBarnetrygd er false`() {
        // Act
        val ytelsetypeBA = YtelseType.UTVIDET_BARNETRYGD.tilYtelseType(false)

        // Assert
        assertThat(ytelsetypeBA).isEqualTo(YtelsetypeBA.UTVIDET_BARNETRYGD_GAMMEL)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `skal mappe SMÅBARNSTILLEGG ytelsestype til SMÅBARNSTILLEGG ytelsetypeBA`(skalBrukeNyKlassekodeForUtvidetBarnetrygd: Boolean) {
        // Act
        val ytelsetypeBA = YtelseType.SMÅBARNSTILLEGG.tilYtelseType(skalBrukeNyKlassekodeForUtvidetBarnetrygd)

        // Assert
        assertThat(ytelsetypeBA).isEqualTo(YtelsetypeBA.SMÅBARNSTILLEGG)
    }
}
