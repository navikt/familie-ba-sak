package no.nav.familie.ba.sak.dokument

import io.mockk.mockk
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

val mockMalerService = mockk<MalerService>(relaxed = true)

@Service
@Profile("mock-dokgen-java")
class DokGenKlientMock : DokGenKlient(
        dokgenServiceUri = "dokgen_uri_mock",
        restTemplate = RestTemplate(),
        malerService = mockMalerService
) {

    override fun <T : Any> utførRequest(request: RequestEntity<String>, responseType: Class<T>): ResponseEntity<T> {
        if (request.url.path.matches(Regex(".+create-markdown"))) {
            return ResponseEntity.ok(responseType.cast("# Vedtaksbrev Markdown (Mock)"))
        } else if (request.url.path.matches(Regex(".+create-doc"))) {
            if (request.body!!.matches(Regex(".+HTML.+"))) {
                return ResponseEntity.ok(responseType.cast("<HTML><H1>Vedtaksbrev HTML (Mock)</H1></HTML>"))
            } else if (request.body!!.matches(Regex(".+PDF.+"))) {
                return ResponseEntity.ok(responseType.cast("Vedtaksbrev PDF".toByteArray()))
            }
        }

        return ResponseEntity.ok(responseType.cast(""))
    }
}
