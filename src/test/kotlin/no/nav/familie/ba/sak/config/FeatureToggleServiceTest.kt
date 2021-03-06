package no.nav.familie.ba.sak.config

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev", "mock-pdl")
@Tag("integration")
class FeatureToggleServiceTest(
        @Autowired
        private val featureToggleService: FeatureToggleService
) {

    @Test
    fun `skal svare true ved dummy impl`() {
        Assertions.assertEquals(true, featureToggleService.isEnabled("sull-bala-tull"))
    }

}