package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.simulering.TilbakeRestClient
import no.nav.familie.ba.sak.simulering.TilbakekrevingId
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.net.URI

@Service
@Profile("mock-tilbake-klient")
@Primary
class TilbakeRestClientMock(private val environment: Environment) : TilbakeRestClient(
        familieTilbakeUri = URI.create("tilbake_uri"),
        restOperations = RestTemplate(),
        environment = environment
) {

    override fun opprettTilbakekrevingBehandling(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): TilbakekrevingId {
        return "id1"
    }
}