package no.nav.familie.ba.sak.integrasjoner.sanity

import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.hentSanityBegrunnelser
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
class CachedSanityKlient {

    @Cacheable("sanitybegrunnelser")
    fun hentSanityBegrunnelserCached(): List<SanityBegrunnelse> {
        return hentSanityBegrunnelser()
    }
}
