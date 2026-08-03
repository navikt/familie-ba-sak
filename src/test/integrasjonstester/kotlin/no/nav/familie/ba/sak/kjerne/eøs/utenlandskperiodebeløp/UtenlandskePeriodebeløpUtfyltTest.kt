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
        // Arrange
        val utenlandskPeriodebeløp =
            lagUtenlandskPeriodebeløp(
                beløp = BigDecimal.valueOf(500),
                valutakode = "NOK",
                intervall = Intervall.MÅNEDLIG,
            )

        // Act
        val utenlandskPeriodebeløpDto = utenlandskPeriodebeløp.tilUtenlandskPeriodebeløpDto()

        // Assert
        assertEquals(UtfyltStatus.OK, utenlandskPeriodebeløpDto.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til UFULLSTENDIG når ett eller to felter er utfylt`() {
        // Arrange
        var utenlandskPeriodebeløp =
            lagUtenlandskPeriodebeløp(
                beløp = BigDecimal.valueOf(500),
            )

        // Act
        var utenlandskPeriodebeløpDto = utenlandskPeriodebeløp.tilUtenlandskPeriodebeløpDto()

        // Assert
        assertEquals(UtfyltStatus.UFULLSTENDIG, utenlandskPeriodebeløpDto.status)

        // Arrange
        utenlandskPeriodebeløp =
            lagUtenlandskPeriodebeløp(
                beløp = BigDecimal.valueOf(500),
                valutakode = "NOK",
            )

        // Act
        utenlandskPeriodebeløpDto = utenlandskPeriodebeløp.tilUtenlandskPeriodebeløpDto()

        // Assert
        assertEquals(UtfyltStatus.UFULLSTENDIG, utenlandskPeriodebeløpDto.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til IKKE_UTFYLT når ingen felter er utfylt`() {
        // Arrange
        val utenlandskPeriodebeløp = lagUtenlandskPeriodebeløp()

        // Act
        val utenlandskPeriodebeløpDto = utenlandskPeriodebeløp.tilUtenlandskPeriodebeløpDto()

        // Assert
        assertEquals(UtfyltStatus.IKKE_UTFYLT, utenlandskPeriodebeløpDto.status)
    }
}
