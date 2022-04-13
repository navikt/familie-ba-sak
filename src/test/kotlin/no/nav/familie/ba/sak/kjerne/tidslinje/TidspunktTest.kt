package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.minsteAv
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.størsteAv
import no.nav.familie.ba.sak.kjerne.tidslinje.util.apr
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jun
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mai
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class TidspunktTest {
    @Test
    fun testStørsteAv() {
        assertEquals(
            feb(2020),
            størsteAv(jan(2020), feb(2020))
        )
        assertEquals(
            feb(2020).somUendeligLengeTil(),
            størsteAv(jan(2020).somUendeligLengeTil(), jan(2020))
        )
        assertEquals(
            jun(2020).somUendeligLengeTil(),
            størsteAv(jan(2020).somUendeligLengeTil(), mai(2020))
        )
        assertEquals(
            jan(2020).somUendeligLengeTil(),
            størsteAv(jan(2020).somUendeligLengeTil(), des(2019))
        )
        assertEquals(
            feb(2020).somUendeligLengeTil(),
            størsteAv(jan(2020).somUendeligLengeTil(), feb(2020).somUendeligLengeTil())
        )
    }

    @Test
    fun testMinsteAv() {
        assertEquals(
            des(2019).somUendeligLengeSiden(),
            minsteAv(jan(2020).somUendeligLengeSiden(), jan(2020))
        )
        assertEquals(
            apr(2019).somUendeligLengeSiden(),
            minsteAv(jan(2020).somUendeligLengeSiden(), mai(2019))
        )
        assertEquals(
            jan(2020).somUendeligLengeSiden(),
            minsteAv(jan(2020).somUendeligLengeSiden(), feb(2020))
        )
        assertEquals(
            feb(2020).somUendeligLengeSiden(),
            størsteAv(feb(2020).somUendeligLengeSiden(), mar(2020).somUendeligLengeSiden())
        )
    }

    @Test
    fun `Equals på ulike tidsenheter skal være forskjellig`() {
        assertEquals(feb(2020), feb(2020))
        assertEquals(1.feb(2020), 1.feb(2020))
        assertEquals(31.jan(2020), 31.jan(2020))

        assertNotEquals(1.jan(2020), jan(2020))
        assertNotEquals(31.jan(2020), jan(2020))
    }

    @Test
    fun `Equals på samme uendelig skal være lik`() {
        assertEquals(feb(2020).somUendeligLengeTil(), mar(2020).somUendeligLengeTil())
        assertEquals(feb(2020).somUendeligLengeSiden(), mar(2020).somUendeligLengeSiden())

        assertEquals(1.feb(2020).somUendeligLengeTil(), 2.feb(2020).somUendeligLengeTil())
        assertEquals(1.feb(2020).somUendeligLengeSiden(), 2.feb(2020).somUendeligLengeSiden())

        assertEquals(1.feb(2020).somUendeligLengeSiden(), mar(2020).somUendeligLengeSiden())
        assertEquals(feb(2020).somUendeligLengeTil(), 1.jan(2020).somUendeligLengeTil())

        assertNotEquals(feb(2020).somUendeligLengeSiden(), feb(2020).somUendeligLengeTil())
        assertNotEquals(5.feb(2020).somUendeligLengeTil(), 5.feb(2020).somUendeligLengeSiden())
    }

    @Test
    fun `En høyst teoretisk sjekk av at systemet ikke er blitt så gammelt at det ikke virker`() {
        assertThrows<IllegalArgumentException> {
            // Systemet virker ikke om 500 år
            Tidspunkt.med(LocalDate.now().plusYears(500))
        }
        // Vil kaste exception hvis det er mindre enn 100 år til systemet ikke virker
        Tidspunkt.med(LocalDate.now().plusYears(100))
    }
}
