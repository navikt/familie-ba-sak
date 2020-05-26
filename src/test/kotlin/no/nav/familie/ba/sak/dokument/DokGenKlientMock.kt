package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.behandling.restDomene.DocFormat
import no.nav.familie.ba.sak.dokument.domene.DokumentRequest
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate


@Service
@Profile("mock-dokgen-java")
@Primary
class DokGenKlientMock : DokGenKlient(
        dokgenServiceUri = "dokgen_uri_mock",
        restTemplate = RestTemplate()
) {

    override fun <T : Any> utførRequest(request: RequestEntity<Any>, responseType: Class<T>): ResponseEntity<T> {
        if (request.url.path.matches(Regex(".+create-markdown"))) {
            return ResponseEntity.ok(responseType.cast("# Vedtaksbrev Markdown (Mock)"))
        } else if (request.url.path.matches(Regex(".+create-doc"))) {
            return when ((request.body!! as DokumentRequest).docFormat) {
                DocFormat.HTML -> ResponseEntity.ok(responseType.cast("<HTML><H1>Vedtaksbrev HTML (Mock)</H1></HTML>"))
                DocFormat.PDF -> ResponseEntity.ok(responseType.cast("Vedtaksbrev PDF".toByteArray()))
                else -> ResponseEntity.ok(responseType.cast(""))
            }
        }

        return ResponseEntity.ok(responseType.cast(""))
    }
}
