package no.nav.familie.ba.sak.integrasjoner.sanity

import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.SanityEØSBegrunnelse
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
class CachedSanityKlient(
    @Value("\${SANITY_DATASET}") private val sanityDatasett: String,
) {

    @Cacheable("sanityBegrunnelser", cacheManager = "shortCache")
    fun hentSanityBegrunnelserCached(): List<SanityBegrunnelse> {
        return hentBegrunnelser(datasett = sanityDatasett)
    }

    @Cacheable("sanityEØSBegrunnelser", cacheManager = "shortCache")
    fun hentEØSBegrunnelserCached(): List<SanityEØSBegrunnelse> {
        return hentEØSBegrunnelser(datasett = sanityDatasett)
    }
}
