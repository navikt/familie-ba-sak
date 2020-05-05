package no.nav.familie.ba.sak.dokument

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.dokument.domene.MalMedData
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
class MalerServiceMock {

    @Bean
    @Profile("mock-maler-service")
    @Primary
    fun mockMalerService(): MalerService {
        val mockMalerService = mockk<MalerService>()

        every { mockMalerService.mapTilBrevfelter(any(), any(), any()) } returns MalMedData(mal = "mock", fletteFelter = "")

        return mockMalerService
    }
}