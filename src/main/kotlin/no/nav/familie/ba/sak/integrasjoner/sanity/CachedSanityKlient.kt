package no.nav.familie.ba.sak.integrasjoner.sanity

import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
class CachedSanityKlient {

    @Cacheable("sanitybegrunnelser", cacheManager = "shortCache")
    fun hentSanityBegrunnelserCached(): List<SanityBegrunnelse> {
        return hentSanityBegrunnelser()
    }
}
