package no.nav.familie.ba.sak.config

import io.mockk.unmockkAll
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollClient
import no.nav.familie.ba.sak.mock.FamilieIntegrasjonerTilgangskontrollMock
import org.junit.jupiter.api.BeforeEach
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

abstract class AbstractMockkSpringRunner {
    @Autowired
    private lateinit var mockFamilieIntegrasjonerTilgangskontrollClient: FamilieIntegrasjonerTilgangskontrollClient

    @BeforeEach
    fun reset() {
        clearMocks()
    }

    private fun clearMocks() {
        unmockkAll()

        FamilieIntegrasjonerTilgangskontrollMock.clearMockFamilieIntegrasjonerTilgangskontrollClient(
            mockFamilieIntegrasjonerTilgangskontrollClient,
        )

        MDC.put("callId", "${this::class.java.simpleName}-${UUID.randomUUID()}")
    }
}
