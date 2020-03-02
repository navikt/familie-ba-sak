package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.sisteDagIForrigeMåned
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class TidTest {

    @Test
    fun `skal finne siste dag i måneden før 2020-03-01`() {
        assertEquals(dato("2020-02-29"), dato("2020-03-01").sisteDagIForrigeMåned())
    }

    @Test
    fun `skal finne siste dag i måneden før 2021-03-01`() {
        assertEquals(dato("2021-02-28"), dato("2021-03-01").sisteDagIForrigeMåned())
    }

    @Test
    fun `skal finne siste dag i måneden forrige år`() {
        assertEquals(dato("2019-12-31"), dato("2020-01-15").sisteDagIForrigeMåned())
    }

    @Test
    fun `skal finne første dag neste år`() {
        assertEquals(dato("2020-01-01"), dato("2019-12-03").førsteDagINesteMåned())
    }

    @Test
    fun `skal finne første dag i måneden etter skuddårsdagen`() {
        assertEquals(dato("2020-03-01"), dato("2020-02-29").førsteDagINesteMåned())
    }

    private fun dato(s: String): LocalDate {
        return LocalDate.parse(s)
    }
}

