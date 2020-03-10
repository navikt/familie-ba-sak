import no.nav.familie.log.NavHttpHeaders
import org.springframework.http.HttpHeaders

fun HttpHeaders.medContentTypeJsonUTF8(): HttpHeaders {
    this.add("Content-Type", "application/json;charset=UTF-8")
    this.acceptCharset = listOf(Charsets.UTF_8)
    return this
}

fun HttpHeaders.medPersonident(personident: String): HttpHeaders {
    this.add(NavHttpHeaders.NAV_PERSONIDENT.asString(), personident)
    return this
}
