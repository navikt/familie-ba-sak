package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class YtelsetypeBATest {
    @Test
    fun `ORDINÆR_BARNETRYGD skal ha riktig klassifisering`() {
        // Act
        val klassifisering = YtelsetypeBA.ORDINÆR_BARNETRYGD.klassifisering

        // Assert
        assertThat(klassifisering).isEqualTo("BATR")
    }

    @Test
    fun `UTVIDET_BARNETRYGD skal ha riktig klassifisering`() {
        // Act
        val klassifisering = YtelsetypeBA.UTVIDET_BARNETRYGD.klassifisering

        // Assert
        assertThat(klassifisering).isEqualTo("BAUTV-OP")
    }

    @Test
    fun `UTVIDET_BARNETRYGD_GAMMEL skal ha riktig klassifisering`() {
        // Act
        val klassifisering = YtelsetypeBA.UTVIDET_BARNETRYGD_GAMMEL.klassifisering

        // Assert
        assertThat(klassifisering).isEqualTo("BATR")
    }

    @Test
    fun `SMÅBARNSTILLEGG skal ha riktig klassifisering`() {
        // Act
        val klassifisering = YtelsetypeBA.SMÅBARNSTILLEGG.klassifisering

        // Assert
        assertThat(klassifisering).isEqualTo("BATRSMA")
    }
}
