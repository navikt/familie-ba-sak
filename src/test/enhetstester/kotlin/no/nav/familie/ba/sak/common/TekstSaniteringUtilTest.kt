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

    @Test
    fun `erAlfanummeriskPlussKolon skal tillate tekst med kolon, mens erAlfanummerisk skal ikke tillate tekst med kolon`() {
        assertThat("Tekst:MedKolon".erAlfanummeriskPlussKolon()).isTrue()
        assertThat("TekstUtenKolon".erAlfanummeriskPlussKolon()).isTrue()
        assertThat("TekstUtenKolon".erAlfanummerisk()).isTrue()
        assertThat("Tekst:MedKolon".erAlfanummerisk()).isFalse()
        assertThat("".erAlfanummeriskPlussKolon()).isTrue()
    }
}
