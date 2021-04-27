package no.nav.familie.ba.sak.tilbakekreving

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.assertGenerelleSuksessKriterier
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.tilbakekreving.Fagsystem
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

typealias TilbakekrevingId = String

data class FinnesBehandlingsresponsDto(val finnesÅpenBehandling: Boolean)

@Component
class TilbakeRestClient(
        @Value("\${FAMILIE_TILBAKE_API_URL}") private val familieTilbakeUri: URI,
        @Qualifier("jwtBearer") restOperations: RestOperations,
        private val environment: Environment
) : AbstractRestClient(restOperations, "Tilbakekreving") {

    fun opprettTilbakekrevingBehandling(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): TilbakekrevingId {
        if (environment.activeProfiles.contains("e2e")) {
            return ""
        }

        val response: Ressurs<String> =
                postForEntity(URI.create("$familieTilbakeUri/behandling/v1"), opprettTilbakekrevingRequest)

        assertGenerelleSuksessKriterier(response)

        return response.data ?: throw Feil("Klarte ikke opprette tilbakekrevingsbehandling mot familie-tilbake")
    }

    fun harÅpenTilbakekreingBehandling(fagsakId: Long): Boolean {
        if (environment.activeProfiles.contains("e2e")) {
            return false
        }
        val uri = URI.create("$familieTilbakeUri/fagsystem/${Fagsystem.BA}/fagsak/${fagsakId}/finnesApenBehandling/v1")

        val response: Ressurs<FinnesBehandlingsresponsDto> = getForEntity(uri)

        assertGenerelleSuksessKriterier(response)

        return response.data?.finnesÅpenBehandling
               ?: throw Feil("Finner ikke om tilbakekrevingsbehandling allerede er opprettet")
    }
}
