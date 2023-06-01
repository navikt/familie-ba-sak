package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.integrasjoner.sanity.SanityKlient
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.SanityEØSBegrunnelse
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@TestConfiguration
class SanityKlientMock {
    @Bean
    @Profile("mock-sanity-client")
    @Primary
    fun mockSanityClient(): SanityKlient {
        return TestSanityKlient
    }
}

object TestSanityKlient : SanityKlient(restTemplate) {
    private val begrunnelser = mutableMapOf<String, List<SanityBegrunnelse>>()
    private val eøsBegrunnelser = mutableMapOf<String, List<SanityEØSBegrunnelse>>()

    override fun hentBegrunnelser(datasett: String): List<SanityBegrunnelse> {
        return begrunnelser.computeIfAbsent(datasett) {
            super.hentBegrunnelser(datasett)
        }
    }

    override fun hentEØSBegrunnelser(datasett: String): List<SanityEØSBegrunnelse> {
        return eøsBegrunnelser.computeIfAbsent(datasett) {
            super.hentEØSBegrunnelser(datasett)
        }
    }
}
