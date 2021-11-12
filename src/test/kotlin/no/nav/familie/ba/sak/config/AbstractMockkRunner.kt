package no.nav.familie.ba.sak.config

import io.mockk.junit5.MockKExtension
import io.mockk.unmockkAll
import no.nav.familie.ba.sak.integrasjoner.`ef-sak`.EfSakRestClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.annotation.DirtiesContext

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractMockkRunner(
    private val personopplysningerService: PersonopplysningerService? = null,
    private val integrasjonClient: IntegrasjonClient? = null,
    private val efSakRestClient: EfSakRestClient? = null
) {

    @BeforeAll
    fun beforeAll() {
        clearMocks()
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