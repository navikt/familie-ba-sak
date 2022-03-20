package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.tidslinje.util.apr
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jun
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mai
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TidspunktTest {
    @Test
    fun testStørsteAv() {
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
}
