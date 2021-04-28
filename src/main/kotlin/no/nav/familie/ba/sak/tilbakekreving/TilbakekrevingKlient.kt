package no.nav.familie.ba.sak.tilbakekreving

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class TilbakekrevingKlient(
        @Value("\${FAMILIE_TILBAKE_API_URL}") private val familieTilbakeUri: URI,
        @Qualifier("jwtBearer") restOperations: RestOperations
) : AbstractRestClient(restOperations, "Tilbakekreving") {

    fun hentForhåndsvisningVarselbrev(forhåndsvisVarselbrevRequest: ForhåndsvisVarselbrevRequest): ByteArray {
        return postForEntity(URI.create("$familieTilbakeUri/api/dokument/forhandsvis-varselbrev"), forhåndsvisVarselbrevRequest)
    }

}