package no.nav.familie.ba.sak.integrasjoner.sanity

import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityBegrunnelse
import org.springframework.stereotype.Service

@Service
class SanityService(private val cachedSanityKlient: CachedSanityKlient) {

    fun hentSanityBegrunnelser(): List<SanityBegrunnelse> {
        return cachedSanityKlient.hentSanityBegrunnelserCached()
    }
}