package no.nav.familie.ba.sak.config

import io.mockk.MockKAnnotations
import io.mockk.junit5.MockKExtension
import io.mockk.unmockkAll
import no.nav.familie.ba.sak.integrasjoner.`ef-sak`.EfSakRestClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractMockkRunner(
    private val personopplysningerService: PersonopplysningerService? = null,
    private val integrasjonClient: IntegrasjonClient? = null,
    private val efSakRestClient: EfSakRestClient? = null
) {
    init {
        MockKAnnotations.init()
    }

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
