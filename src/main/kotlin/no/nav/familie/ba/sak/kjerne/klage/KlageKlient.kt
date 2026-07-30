package no.nav.familie.ba.sak.kjerne.klage

import no.nav.familie.ba.sak.common.kallEksternTjenesteRessurs
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.UUID

@Component
class KlageKlient(
    @Qualifier("klageRestClient") private val restClient: RestClient,
    @Value("\${FAMILIE_KLAGE_URL}") private val familieKlageUri: URI,
) {
    fun opprettKlage(opprettKlagebehandlingRequest: OpprettKlagebehandlingRequest): UUID {
        val uri =
            UriComponentsBuilder
                .fromUri(familieKlageUri)
                .pathSegment("api/ekstern/behandling/v2/opprett")
                .build()
                .toUri()

        return kallEksternTjenesteRessurs(
            tjeneste = "klage",
            uri = uri,
            formål = "Opprett klagebehandling",
        ) {
            restClient
                .post()
                .uri(uri)
                .body(opprettKlagebehandlingRequest)
                .retrieve()
                .body()!!
        }
    }

    fun hentKlagebehandlinger(fagsakId: Long): List<KlagebehandlingDto> {
        val uri =
            UriComponentsBuilder
                .fromUri(familieKlageUri)
                .pathSegment("api/ekstern/behandling/baks/${Fagsystem.BA}")
                .queryParam("eksternFagsakId", fagsakId)
                .build()
                .toUri()

        return kallEksternTjenesteRessurs(
            tjeneste = "klage",
            uri = uri,
            formål = "Hent klagebehandlinger",
        ) {
            restClient
                .get()
                .uri(uri)
                .retrieve()
                .body()!!
        }
    }
}
