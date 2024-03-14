package no.nav.familie.ba.sak.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TekstSaniteringUtilTest {
    @Test
    fun `Sanering av tekst fungerer som forventet`() {
        val tekst = "Dette er en tekst med 1234 og spesialtegn som: !@#¤%&/()=?`^*¨'\""
        val forventetResultat = "Detteerentekstmed1234ogspesialtegnsom"

        assertThat(tekst.saner()).isEqualTo(forventetResultat)
    }

    @Test
    fun `erSanert fungerer som forventet`() {
        val tekst = "Dette er en tekst med 1234 og spesialtegn som: !@#¤%&/()=?`^*¨'\""
        val sanertTekst = "Detteerentekstmed1234ogspesialtegnsom"
        val tomStreng = ""

        assertThat(tekst.erAlfanummerisk()).isFalse()
        assertThat(sanertTekst.erAlfanummerisk()).isTrue()
        assertThat(tomStreng.erAlfanummerisk()).isTrue()
    }
}
