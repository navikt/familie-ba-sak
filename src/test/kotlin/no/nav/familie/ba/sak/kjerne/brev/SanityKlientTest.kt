package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.integrasjoner.sanity.hentSanityBegrunnelser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SanityKlientTest() : AbstractSpringIntegrationTest() {

    @Test
    fun `Skal teste at vi klarer å hente begrunnelser fra sanity-apiet`() {
        Assertions.assertTrue(hentSanityBegrunnelser().isNotEmpty())
    }
}
