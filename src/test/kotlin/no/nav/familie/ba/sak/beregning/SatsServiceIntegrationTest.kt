package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.beregning.domene.SatsType
import no.nav.familie.ba.sak.common.DbContainerInitializer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.time.YearMonth

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres")
@Tag("integration")
class SatsServiceIntegrationTest {

    @Test
    fun `Skal hente ut riktig sats for ordinær barnetrygd i 2020`() {
        val dato = LocalDate.of(2020, 1, 1)
        val sats = SatsService.hentGyldigSatsFor(SatsType.ORBA, YearMonth.from(dato), YearMonth.from(dato)).first()

        Assertions.assertEquals(1054, sats.beløp)
    }

    @Test
    fun `Skal hente ut riktig sats for tillegg til barnetrygd i september 2020`() {
        val dato = LocalDate.of(2020, 9, 1)
        val sats = SatsService.hentGyldigSatsFor(SatsType.TILLEGG_ORBA, YearMonth.from(dato), YearMonth.from(dato)).first()

        Assertions.assertEquals(1354, sats.beløp)
    }

    @Test
    fun `Skal hente ut sats for ordinær barnetrygd ved tillegg før september 2020`() {
        val dato = LocalDate.of(2020, 1, 1)
        val sats = SatsService.hentGyldigSatsFor(SatsType.TILLEGG_ORBA, YearMonth.from(dato), YearMonth.from(dato)).first()
        val satsOrdinær = SatsService.hentGyldigSatsFor(SatsType.ORBA, YearMonth.from(dato), YearMonth.from(dato)).first()

        Assertions.assertEquals(satsOrdinær.beløp, sats.beløp)
    }

    @Test
    fun `Skal hente ut riktig sats for småbarnstillegg`() {
        val dato = LocalDate.of(2020, 1, 1)
        val sats = SatsService.hentGyldigSatsFor(SatsType.SMA, YearMonth.from(dato), YearMonth.from(dato)).first()

        Assertions.assertEquals(660, sats.beløp)
    }

    @Test
    fun `Skal hente ut riktig sats for ordinær barnetrygd i 2018`() {
        val dato = LocalDate.of(2018, 1, 1)
        val sats = SatsService.hentGyldigSatsFor(SatsType.ORBA, YearMonth.from(dato), YearMonth.from(dato)).first()

        Assertions.assertEquals(970, sats.beløp)
    }
}