package no.nav.familie.ba.sak.integrasjoner.sanity

import no.nav.familie.ba.sak.kjerne.brev.BrevKlient
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

@Service
class SanityService(
    private val cachedSanityKlient: CachedSanityKlient,
    private val brevKlient: BrevKlient,
    private val environment: Environment
) {

    fun hentSanityBegrunnelser(): List<SanityBegrunnelse> {
        val erIMiljø = environment.activeProfiles.any {
            listOf("preprod", "prod").contains(it.trim(' '))
        }

        return if (erIMiljø) {
            brevKlient.hentSanityBegrunnelser()
        } else {
            cachedSanityKlient.hentSanityBegrunnelserCached()
        }
    }
}
