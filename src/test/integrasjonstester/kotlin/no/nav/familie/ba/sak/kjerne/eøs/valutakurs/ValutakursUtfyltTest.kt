package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.datagenerator.lagValutakurs
import no.nav.familie.ba.sak.ekstern.restDomene.UtfyltStatus
import no.nav.familie.ba.sak.ekstern.restDomene.tilValutakursDto
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class ValutakursUtfyltTest {
    @Test
    fun `Skal sette UtfyltStatus til OK når alle felter er utfylt`() {
        val valutakurs =
            lagValutakurs(
                valutakursdato = LocalDate.now(),
                kurs = BigDecimal.valueOf(10),
            )

        val valutakursDto = valutakurs.tilValutakursDto()

        Assertions.assertEquals(UtfyltStatus.OK, valutakursDto.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til UFULLSTENDIG når ett felt er utfylt`() {
        var valutakurs =
            lagValutakurs(
                valutakursdato = LocalDate.now(),
            )

        var valutaKursDto = valutakurs.tilValutakursDto()

        Assertions.assertEquals(UtfyltStatus.UFULLSTENDIG, valutaKursDto.status)

        valutakurs =
            lagValutakurs(
                kurs = BigDecimal.valueOf(10),
            )

        valutaKursDto = valutakurs.tilValutakursDto()

        Assertions.assertEquals(UtfyltStatus.UFULLSTENDIG, valutaKursDto.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til IKKE_UTFYLT når ingen felter er utfylt`() {
        val valutakurs = lagValutakurs()

        val valutaKursDto = valutakurs.tilValutakursDto()

        Assertions.assertEquals(UtfyltStatus.IKKE_UTFYLT, valutaKursDto.status)
    }
}
