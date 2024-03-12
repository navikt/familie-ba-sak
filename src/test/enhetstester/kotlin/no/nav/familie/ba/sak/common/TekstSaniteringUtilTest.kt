package no.nav.familie.ba.sak.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TekstSaniteringUtilTest {
    @Test
    fun `Sanitering av tekst fungerer som forventet`() {
        val tekst = "Dette er en tekst med 1234 og spesialtegn som: !@#¤%&/()=?`^*¨'\""
        val forventetResultat = "Detteerentekstmed1234ogspesialtegnsom"

        assertThat(tekst.saniter()).isEqualTo(forventetResultat)
    }
}
