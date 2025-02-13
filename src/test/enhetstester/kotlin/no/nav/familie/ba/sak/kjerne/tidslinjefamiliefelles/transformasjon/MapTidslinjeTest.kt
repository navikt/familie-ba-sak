package no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.transformasjon

import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.tilCharTidslinje
import no.nav.familie.tidslinje.mapVerdi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MapTidslinjeTest {
    @Test
    fun `skal mappe innhold og fjerne null`() {
        val tidslinje = "  AAA  BB   CCC ".tilCharTidslinje(1.jan(2020))

        val faktisk = tidslinje.mapIkkeNull { it.lowercase() }

        val forventet = "  aaa  bb   ccc ".tilCharTidslinje(1.jan(2020)).mapVerdi { it?.toString() }

        assertEquals(forventet, faktisk)
        assertEquals(1.jan(2020), faktisk.startsTidspunkt)
        assertEquals(16.jan(2020), faktisk.kalkulerSluttTidspunkt())
    }
}
