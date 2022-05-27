package no.nav.familie.ba.sak.integrasjoner.sanity

import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
class CachedSanityKlient(
    @Value("\${SANITY_DATASET}") private val sanityDatasett: String,
) {

    @Cacheable("sanityNasjonaleBegrunnelser", cacheManager = "shortCache")
    fun hentSanityBegrunnelserCached(): List<SanityBegrunnelse> {
        return hentNasjonaleBegrunnelser(datasett = sanityDatasett)
    }

    @Cacheable("sanityEØSBegrunnelser", cacheManager = "shortCache")
    fun hentEØSBegrunnelserCached(): List<SanityBegrunnelse> {
        return hentNasjonaleBegrunnelser(datasett = sanityDatasett)
    }
}
