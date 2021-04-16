package no.nav.familie.ba.sak.simulering

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class TilbakeRestClient(
        @Value("\${FAMILIE_TILBAKE_API_URL}") private val familieTilbakeUri: URI,
        @Qualifier("jwtBearer") restOperations: RestOperations,
        private val environment: Environment
) : AbstractRestClient(restOperations, "Tilbakekreving") {

    fun opprettTilbakekrevingBehandling(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): String {
        if (environment.activeProfiles.contains("e2e")) {
            return ""
        }

        val response: Ressurs<String> =
                postForEntity(URI.create("$familieTilbakeUri/behandling/v1"), opprettTilbakekrevingRequest)

        return response.data ?: throw Feil("Klarte ikke opprette tilbakekrevingsbehandling mot familie-tilbake")
    }
}
z