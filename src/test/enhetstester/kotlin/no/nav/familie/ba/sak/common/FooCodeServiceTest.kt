package no.nav.familie.ba.sak.common

import org.junit.jupiter.api.Test

class FooCodeServiceTest {
    private val fooCodeService = FooCodeService()

    @Test
    fun `generateNumericCode should throw exception when length is not positive`() {
        fooCodeService.getJallaIfOdd(4)
    }
}
