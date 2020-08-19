package no.nav.familie.ba.sak.integrasjoner

import no.nav.familie.log.NavHttpHeaders
import org.springframework.http.HttpHeaders

fun HttpHeaders.medPersonident(personident: String): HttpHeaders {
    this.add(NavHttpHeaders.NAV_PERSONIDENT.asString(), personident)
    return this
}

fun HttpHeaders.medAktørId(aktørId: String): HttpHeaders {
    this.add("Nav-Aktorid", aktørId)
    return this
}