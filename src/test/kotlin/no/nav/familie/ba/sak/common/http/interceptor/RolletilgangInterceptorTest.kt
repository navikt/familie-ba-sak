package no.nav.familie.ba.sak.common.http.interceptor

import io.mockk.mockk
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.RolleConfig
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

internal class RolletilgangInterceptorTest : AbstractSpringIntegrationTest() {

    @Autowired
    lateinit var rolleConfig: RolleConfig

    val request = mockk<HttpServletRequest>()
    val response = mockk<HttpServletResponse>()
    val handler = mockk<Any>()

    @Test
    fun `Verifiser at systembruker har tilgang`() {
        assertTrue(RolletilgangInterceptor(rolleConfig).preHandle(request, response, handler))
    }
}