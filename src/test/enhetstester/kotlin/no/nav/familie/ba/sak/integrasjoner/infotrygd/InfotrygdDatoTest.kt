package no.nav.familie.ba.sak.integrasjoner.infotrygd

import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.YearMonth

class InfotrygdDatoTest {
    @Test
    fun `skal konvertere inverterte virkningFom og opphørtFom til YearMonth`() {
        // Arrange
        val stønad = Stønad(virkningFom = "797998", opphørtFom = "032021")

        // Act
        val virkningFom = stønad.virkningFomSomYearMonth
        val opphørtFom = stønad.opphørtFomSomYearMonth

        // Assert
        assertThat(virkningFom).isEqualTo(YearMonth.of(2020, 1))
        assertThat(opphørtFom).isEqualTo(YearMonth.of(2021, 3))
    }

    @Test
    fun `skal gi null når Infotrygd-dato er 000000`() {
        // Arrange
        val stønad = Stønad(virkningFom = "000000", opphørtFom = "000000")

        // Act
        val virkningFom = stønad.virkningFomSomYearMonth
        val opphørtFom = stønad.opphørtFomSomYearMonth

        // Assert
        assertThat(virkningFom).isNull()
        assertThat(opphørtFom).isNull()
    }
}
