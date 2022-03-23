package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.http.client.RessursException
import org.springframework.web.client.RestClientResponseException

fun RessursException.hentStatuskodeFraOriginalFeil(): Int {
    val cause = this.cause
    return if (cause is RestClientResponseException) {
        cause.rawStatusCode
    } else {
        throw this
    }
}
