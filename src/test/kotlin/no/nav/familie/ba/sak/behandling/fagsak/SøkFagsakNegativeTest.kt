package no.nav.familie.ba.sak.behandling.fagsak

import io.mockk.every
import junit.framework.Assert.assertEquals
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsakDeltager
import no.nav.familie.ba.sak.behandling.restDomene.RestSøkParam
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonException
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.client.HttpClientErrorException
import java.util.*

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev", "test-søk")
@Tag("integration")
class SøkFagsakNegativeTest {

    @Autowired
    lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var integrasjonClient: IntegrasjonClient

    @Autowired
    lateinit var fagsakController: FagsakController

    @Test
    fun `test å søke fagsak deltager med ugyldig fnr`() {
        val ugyldigFnr1 = UUID.randomUUID().toString()
        val ugyldigFnr2 = UUID.randomUUID().toString()

        every {
            integrasjonClient.hentPersoninfoFor(eq(ugyldigFnr1))
        } throws (HttpClientErrorException(HttpStatus.NOT_FOUND, "person not found"))

        every {
            integrasjonClient.hentPersoninfoFor(eq(ugyldigFnr2))
        } throws (IntegrasjonException("illegal state"))

        assertThrows<HttpClientErrorException> {
            fagsakService.hentFagsakDeltager(ugyldigFnr1)
        }

        assertThrows<IllegalStateException>{
            fagsakService.hentFagsakDeltager(ugyldigFnr2)
        }
    }

    @Test
    fun `test generer riktig ressur ved feil`(){
        val ukjentId= "43125678910"
        val feilId= "41235678910"

        val resEntity1= fagsakController.søkFagsak(RestSøkParam(ukjentId))
        assertEquals(HttpStatus.OK, resEntity1.statusCode)
        val ress= resEntity1.body as Ressurs<List<RestFagsakDeltager>>
        assertEquals(Ressurs.Status.FEILET, ress.status)

        val resEntity2= fagsakController.søkFagsak(RestSøkParam(feilId))
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resEntity2.statusCode)
    }
}