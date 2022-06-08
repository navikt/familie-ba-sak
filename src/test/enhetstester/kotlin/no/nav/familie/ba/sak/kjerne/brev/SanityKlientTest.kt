package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.integrasjoner.sanity.hentBegrunnelser
import no.nav.familie.ba.sak.integrasjoner.sanity.hentEØSBegrunnelser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SanityKlientTest() {

    @Test
    fun `Skal teste at vi klarer å hente begrunnelser fra sanity-apiet`() {
        Assertions.assertTrue(hentBegrunnelser().isNotEmpty())
    }

    @Test
    fun `Skal teste at vi klarer å hente eøs-begrunnelser fra sanity-apiet`() {
        Assertions.assertTrue(hentEØSBegrunnelser().isNotEmpty())
    }
}
