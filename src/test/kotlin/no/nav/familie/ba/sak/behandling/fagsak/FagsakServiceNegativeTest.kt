package no.nav.familie.ba.sak.behandling.fagsak

import io.mockk.every
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonException
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev")
@Tag("integration")
class FagsakServiceNegativeTest {

    @Autowired
    lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var integrasjonClient: IntegrasjonClient

    @Test
    fun `test å søke fagsak deltager med ugyldig fnr`() {
        val ugyldigFnr = UUID.randomUUID().toString()

        every {
            integrasjonClient.hentPersoninfoFor(eq(ugyldigFnr))
        } throws (IntegrasjonException("Kall mot integrasjon feilet ved uthenting av personinfo"))

        assertThrows<IllegalStateException> {
            fagsakService.hentFagsakDeltager(ugyldigFnr)
        }
    }
}