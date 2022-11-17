package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class SatsServiceIntegrationTest : AbstractSpringIntegrationTest() {

    @Test
    fun `Skal hente ut riktig sats for ordinær barnetrygd i 2020`() {
        val dato = LocalDate.of(2020, 1, 1)
        val sats = SatsService.hentGyldigSatsFor(SatsType.ORBA, YearMonth.from(dato), YearMonth.from(dato)).first()

        Assertions.assertEquals(1054, sats.sats)
    }

    @Test
    fun `Skal hente ut riktig sats for tillegg til barnetrygd i september 2020`() {
        val dato = LocalDate.of(2020, 9, 1)
        val sats = SatsService.hentGyldigSatsFor(SatsType.TILLEGG_ORBA, YearMonth.from(dato), YearMonth.from(dato)).first()

        Assertions.assertEquals(1354, sats.sats)
    }

    @Test
    fun `Skal hente ut sats for ordinær barnetrygd ved tillegg før september 2020`() {
        val dato = LocalDate.of(2020, 1, 1)
        val sats = SatsService.hentGyldigSatsFor(SatsType.TILLEGG_ORBA, YearMonth.from(dato), YearMonth.from(dato)).first()
        val satsOrdinær = SatsService.hentGyldigSatsFor(SatsType.ORBA, YearMonth.from(dato), YearMonth.from(dato)).first()

        Assertions.assertEquals(satsOrdinær.sats, sats.sats)
    }

    @Test
    fun `Skal hente ut riktig sats for småbarnstillegg`() {
        val dato = LocalDate.of(2020, 1, 1)
        val sats = SatsService.hentGyldigSatsFor(SatsType.SMA, YearMonth.from(dato), YearMonth.from(dato)).first()

        Assertions.assertEquals(660, sats.sats)
    }

    @Test
    fun `Skal hente ut riktig sats for ordinær barnetrygd i 2018`() {
        val dato = LocalDate.of(2018, 1, 1)
        val sats = SatsService.hentGyldigSatsFor(SatsType.ORBA, YearMonth.from(dato), YearMonth.from(dato)).first()

        Assertions.assertEquals(970, sats.sats)
    }
}
