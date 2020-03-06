package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.beregning.domene.SatsType
import no.nav.familie.ba.sak.util.DbContainerInitializer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres")
@Tag("integration")
class SatsServiceTest {

    @Autowired
    private lateinit var satsService: SatsService

    @Test
    fun `Skal hente ut riktig sats for ordinær barnetrygd i 2020`() {

        val sats = satsService.hentGyldigSatsFor(SatsType.ORBA, LocalDate.of(2020, 1, 1))

        Assertions.assertEquals(1054, sats.beløp)
    }

    @Test
    fun `Skal hente ut riktig sats for tillegg til barnetrygd i september 2020`() {
        val sats = satsService.hentGyldigSatsFor(SatsType.TILLEGG_ORBA, LocalDate.of(2020, 9, 1))

        Assertions.assertEquals(1354, sats.beløp)
    }

    @Test
    fun `Skal ikke finne noen sats for tillegg til barnetrygd før september 2020`() {

        Assertions.assertThrows(NoSuchElementException::class.java) {
            satsService.hentGyldigSatsFor(SatsType.TILLEGG_ORBA, LocalDate.of(2020, 1, 1))
        }
    }

    @Test
    fun `Skal hente ut riktig sats for småbarnstillegg`() {
        val sats = satsService.hentGyldigSatsFor(SatsType.SMA, LocalDate.of(2020, 1,1))

        Assertions.assertEquals(660, sats.beløp)
    }

    @Test
    fun `Skal hente ut riktig sats for ordinær barnetrygd i 2018`() {
        val sats = satsService.hentGyldigSatsFor(SatsType.ORBA, LocalDate.of(2018, 1,1))

        Assertions.assertEquals(970, sats.beløp)
    }
}