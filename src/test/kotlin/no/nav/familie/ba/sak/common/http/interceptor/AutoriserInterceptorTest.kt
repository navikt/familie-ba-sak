package no.nav.familie.ba.sak.common.http.interceptor

import io.mockk.mockk
import junit.framework.Assert.assertTrue
import no.nav.familie.ba.sak.config.RolleConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@SpringBootTest(classes = [RolleConfig::class])
@ActiveProfiles("dev")
internal class AutoriserInterceptorTest {

    @Autowired
    lateinit var rolleConfig: RolleConfig

    val request = mockk<HttpServletRequest>()
    val response = mockk<HttpServletResponse>()
    val handler = mockk<Any>()

    @Test
    fun `Verifiser at systembruker har tilgang`() {
        assertTrue(AutoriserInterceptor(rolleConfig).preHandle(request, response, handler))
    }
}