package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.common.Utils.hentPropertyFraMaven
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UtilsTest {

    @Test
    fun `hent property fra maven skal ikke være blank`() {
        val result = hentPropertyFraMaven("java.version")
        Assertions.assertThat(result).isNotBlank()
    }

    @Test
    fun `hent property som mangler skal returnere null`() {
        val result = hentPropertyFraMaven("skalikkefinnes")
        Assertions.assertThat(result).isNullOrEmpty()
    }

    private fun dato(s: String): LocalDate {
        return LocalDate.parse(s)
    }
}

