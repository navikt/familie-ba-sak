package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.ekstern.restDomene.tilRestValutakurs
import no.nav.familie.ba.sak.kjerne.eøs.felles.UtfyltStatus
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.lagValutakurs
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class ValutakursUtfyltTest {

    @Test
    fun `Skal sette UtfyltStatus til OK når alle felter er utfylt`() {
        val valutakurs = lagValutakurs(
            valutakursdato = LocalDate.now(),
            valutakode = "NOK",
            kurs = BigDecimal.valueOf(10)
        )

        val restValutakurs = valutakurs.tilRestValutakurs()

        Assertions.assertEquals(UtfyltStatus.OK, restValutakurs.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til UFULLSTENDIG når ett eller to felter er utfylt`() {
        var valutakurs = lagValutakurs(
            valutakursdato = LocalDate.now(),
        )

        var restValutakurs = valutakurs.tilRestValutakurs()

        Assertions.assertEquals(UtfyltStatus.UFULLSTENDIG, restValutakurs.status)

        valutakurs = lagValutakurs(
            valutakursdato = LocalDate.now(),
            valutakode = "NOK",
        )

        restValutakurs = valutakurs.tilRestValutakurs()

        Assertions.assertEquals(UtfyltStatus.UFULLSTENDIG, restValutakurs.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til IKKE_UTFYLT når ingen felter er utfylt`() {
        val valutakurs = lagValutakurs()

        val restValutakurs = valutakurs.tilRestValutakurs()

        Assertions.assertEquals(UtfyltStatus.IKKE_UTFYLT, restValutakurs.status)
    }
}

