package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FagsystemBATest {
    @Test
    fun `BARNETRYGD skal ha riktig kode`() {
        // Act
        val kode = FagsystemBA.BARNETRYGD.kode

        // Assert
        assertThat(kode).isEqualTo("BA")
    }

    @Test
    fun `BARNETRYGD skal ha riktige gyldige sats typer`() {
        // Act
        val gyldigeSatstyper = FagsystemBA.BARNETRYGD.gyldigeSatstyper

        // Assert
        assertThat(gyldigeSatstyper).containsOnly(
            YtelsetypeBA.ORDINÆR_BARNETRYGD,
            YtelsetypeBA.UTVIDET_BARNETRYGD,
            YtelsetypeBA.UTVIDET_BARNETRYGD_GAMMEL,
            YtelsetypeBA.SMÅBARNSTILLEGG,
        )
    }
}
