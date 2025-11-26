package no.nav.familie.ba.sak.kjerne.klage

import no.nav.familie.ba.sak.common.kallEksternTjenesteRessurs
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.UUID

@Component
class KlageKlient(
    @Qualifier("jwtBearer") restOperations: RestOperations,
    @Value("\${FAMILIE_KLAGE_URL}") private val familieKlageUri: URI,
) : AbstractRestClient(restOperations, "integrasjon") {
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
            postForEntity(uri, opprettKlagebehandlingRequest)
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
            getForEntity(uri)
        }
    }

    fun oppdaterEnhetPåÅpenBehandling(
        oppgaveId: Long,
        nyEnhetId: String,
    ): String {
        val uri =
            UriComponentsBuilder
                .fromUri(familieKlageUri)
                .pathSegment("api/portefoljejustering/oppdater-behandlende-enhet")
                .build()
                .toUri()

        val request =
            OppdaterBehandlendeEnhetIKlageRequest(
                oppgaveId = oppgaveId,
                nyEnhet = nyEnhetId,
                fagsystem = Fagsystem.BA,
            )

        return kallEksternTjenesteRessurs(
            tjeneste = "klage",
            uri = uri,
            formål = "Oppdater enhet på åpen klagebehandling",
        ) {
            putForEntity(uri, request)
        }
    }

    data class OppdaterBehandlendeEnhetIKlageRequest(
        val oppgaveId: Long,
        val nyEnhet: String,
        val fagsystem: Fagsystem,
    )
}
