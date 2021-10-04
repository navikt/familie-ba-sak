package no.nav.familie.ba.sak.integrasjoner.sanity

import no.nav.familie.ba.sak.kjerne.dokument.SanityKlient
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityBegrunnelse
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
class CachedSanityKlient(
        private val sanityKlient: SanityKlient
) {

    @Cacheable("sanitybegrunnelser")
    fun hentSanityBegrunnelserCached(): List<SanityBegrunnelse> {
        return sanityKlient.hentSanityBegrunnelser()
    }
}