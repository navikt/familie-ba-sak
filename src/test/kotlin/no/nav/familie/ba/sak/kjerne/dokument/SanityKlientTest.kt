package no.nav.familie.ba.sak.kjerne.dokument

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SanityKlientTest(@Autowired private val sanityKlient: SanityKlient) : AbstractSpringIntegrationTest() {

    @Test
    fun `Skal teste at vi klarer å hente begrunnelser fra sanity-apiet`() {
        Assertions.assertTrue(sanityKlient.hentSanityBegrunnelser().isNotEmpty())
    }
}