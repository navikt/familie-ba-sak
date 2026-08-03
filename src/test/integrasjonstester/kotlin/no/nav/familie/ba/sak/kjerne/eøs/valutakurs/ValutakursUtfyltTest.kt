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
        // Arrange
        val valutakurs =
            lagValutakurs(
                valutakursdato = LocalDate.now(),
                kurs = BigDecimal.valueOf(10),
            )

        // Act
        val valutakursDto = valutakurs.tilValutakursDto()

        // Assert
        Assertions.assertEquals(UtfyltStatus.OK, valutakursDto.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til UFULLSTENDIG når ett felt er utfylt`() {
        // Arrange
        var valutakurs =
            lagValutakurs(
                valutakursdato = LocalDate.now(),
            )

        // Act
        var valutaKursDto = valutakurs.tilValutakursDto()

        // Assert
        Assertions.assertEquals(UtfyltStatus.UFULLSTENDIG, valutaKursDto.status)

        // Arrange
        valutakurs =
            lagValutakurs(
                kurs = BigDecimal.valueOf(10),
            )

        // Act
        valutaKursDto = valutakurs.tilValutakursDto()

        // Assert
        Assertions.assertEquals(UtfyltStatus.UFULLSTENDIG, valutaKursDto.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til IKKE_UTFYLT når ingen felter er utfylt`() {
        // Arrange
        val valutakurs = lagValutakurs()

        // Act
        val valutaKursDto = valutakurs.tilValutakursDto()

        // Assert
        Assertions.assertEquals(UtfyltStatus.IKKE_UTFYLT, valutaKursDto.status)
    }
}
