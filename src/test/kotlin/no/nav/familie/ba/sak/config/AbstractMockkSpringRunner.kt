package no.nav.familie.ba.sak.config

import io.mockk.unmockkAll
import no.nav.familie.ba.sak.integrasjoner.`ef-sak`.EfSakRestClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractMockkSpringRunner(
    private val personopplysningerService: PersonopplysningerService? = null,
    private val integrasjonClient: IntegrasjonClient? = null,
    private val efSakRestClient: EfSakRestClient? = null
) {

    @AfterAll
    fun afterAll() {
        clearMocks()
    }

    private fun clearMocks() {
        unmockkAll()
        if (personopplysningerService != null)
            ClientMocks.clearPdlMocks(personopplysningerService)

        if (integrasjonClient != null)
            ClientMocks.clearIntegrasjonMocks(integrasjonClient)

        if (efSakRestClient != null)
            EfSakRestClientMock.clearEfSakRestMocks(efSakRestClient)
    }
}
