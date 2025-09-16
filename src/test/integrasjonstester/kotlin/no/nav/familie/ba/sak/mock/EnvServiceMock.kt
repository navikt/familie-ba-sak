package no.nav.familie.ba.sak.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.EnvService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class EnvServiceMock {
    @Bean
    @Primary
    fun mockEnvService(): EnvService {
        val mockEnvService = mockk<EnvService>(relaxed = true)

        every {
            mockEnvService.erProd()
        } answers {
            true
        }

        every {
            mockEnvService.erPreprod()
        } answers {
            true
        }

        every {
            mockEnvService.erDev()
        } answers {
            true
        }

        return mockEnvService
    }
}
