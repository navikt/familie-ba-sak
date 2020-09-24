package no.nav.familie.ba.sak.integrasjoner

import org.springframework.http.HttpHeaders

fun HttpHeaders.medContentTypeJsonUTF8(): HttpHeaders {
    this.add("Content-Type", "application/json;charset=UTF-8")
    this.acceptCharset = listOf(Charsets.UTF_8)
    return this
}

fun HttpHeaders.medAktørId(aktørId: String): HttpHeaders {
    this.add("Nav-Aktorid", aktørId)
    return this
}