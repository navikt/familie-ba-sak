package no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp

import no.nav.familie.ba.sak.datagenerator.lagUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.ekstern.restDomene.UtfyltStatus
import no.nav.familie.ba.sak.ekstern.restDomene.tilUtenlandskPeriodebeløpDto
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class UtenlandskePeriodebeløpUtfyltTest {
    @Test
    fun `Skal sette UtfyltStatus til OK når alle felter er utfylt`() {
        val utenlandskPeriodebeløp =
            lagUtenlandskPeriodebeløp(
                beløp = BigDecimal.valueOf(500),
                valutakode = "NOK",
                intervall = Intervall.MÅNEDLIG,
            )

        val utenlandskPeriodebeløpDto = utenlandskPeriodebeløp.tilUtenlandskPeriodebeløpDto()

        assertEquals(UtfyltStatus.OK, utenlandskPeriodebeløpDto.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til UFULLSTENDIG når ett eller to felter er utfylt`() {
        var utenlandskPeriodebeløp =
            lagUtenlandskPeriodebeløp(
                beløp = BigDecimal.valueOf(500),
            )

        var utenlandskPeriodebeløpDto = utenlandskPeriodebeløp.tilUtenlandskPeriodebeløpDto()

        assertEquals(UtfyltStatus.UFULLSTENDIG, utenlandskPeriodebeløpDto.status)

        utenlandskPeriodebeløp =
            lagUtenlandskPeriodebeløp(
                beløp = BigDecimal.valueOf(500),
                valutakode = "NOK",
            )

        utenlandskPeriodebeløpDto = utenlandskPeriodebeløp.tilUtenlandskPeriodebeløpDto()

        assertEquals(UtfyltStatus.UFULLSTENDIG, utenlandskPeriodebeløpDto.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til IKKE_UTFYLT når ingen felter er utfylt`() {
        val utenlandskPeriodebeløp = lagUtenlandskPeriodebeløp()

        val utenlandskPeriodebeløpDto = utenlandskPeriodebeløp.tilUtenlandskPeriodebeløpDto()

        assertEquals(UtfyltStatus.IKKE_UTFYLT, utenlandskPeriodebeløpDto.status)
    }
}
