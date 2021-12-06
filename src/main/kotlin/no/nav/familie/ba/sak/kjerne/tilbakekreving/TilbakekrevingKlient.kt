package no.nav.familie.ba.sak.kjerne.tilbakekreving

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.assertGenerelleSuksessKriterier
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandling
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.KanBehandlingOpprettesManueltRespons
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettManueltTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

typealias TilbakekrevingId = String

data class FinnesBehandlingsresponsDto(val finnesÅpenBehandling: Boolean)

@Component
class TilbakekrevingKlient(
    @Value("\${FAMILIE_TILBAKE_API_URL}") private val familieTilbakeUri: URI,
    @Qualifier("jwtBearer") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "Tilbakekreving") {

    fun hentForhåndsvisningVarselbrev(forhåndsvisVarselbrevRequest: ForhåndsvisVarselbrevRequest): ByteArray {
        return postForEntity(
            uri = URI.create("$familieTilbakeUri/dokument/forhandsvis-varselbrev"),
            payload = forhåndsvisVarselbrevRequest,
            httpHeaders = HttpHeaders().apply {
                accept = listOf(MediaType.APPLICATION_PDF)
            }
        )
    }

    fun opprettTilbakekrevingBehandling(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): TilbakekrevingId {
        val response: Ressurs<String> =
            postForEntity(URI.create("$familieTilbakeUri/behandling/v1"), opprettTilbakekrevingRequest)

        assertGenerelleSuksessKriterier(response)

        return response.data ?: throw Feil("Klarte ikke opprette tilbakekrevingsbehandling mot familie-tilbake")
    }

    fun harÅpenTilbakekrevingsbehandling(fagsakId: Long): Boolean {
        val uri = URI.create("$familieTilbakeUri/fagsystem/${Fagsystem.BA}/fagsak/$fagsakId/finnesApenBehandling/v1")

        val response: Ressurs<FinnesBehandlingsresponsDto> = getForEntity(uri)

        assertGenerelleSuksessKriterier(response)

        return response.data?.finnesÅpenBehandling
            ?: throw Feil("Finner ikke om tilbakekrevingsbehandling allerede er opprettet")
    }

    fun hentTilbakekrevingsbehandlinger(fagsakId: Long): List<Behandling> {
        try {
            val uri = URI.create("$familieTilbakeUri/fagsystem/${Fagsystem.BA}/fagsak/$fagsakId/behandlinger/v1")

            val response: Ressurs<List<Behandling>> = getForEntity(uri)

            assertGenerelleSuksessKriterier(response)

            return if (response.status == Ressurs.Status.SUKSESS) {
                response.data!!
            } else {
                log.error(
                    "Kallet for å hente tilbakekrevingsbehandlinger feilet! Feilmelding: ",
                    response.frontendFeilmelding
                )
                emptyList()
            }
        } catch (e: Exception) {
            secureLogger.error("Trøbbel mot tilbakekreving", e)
            log.error("Exception når kallet for å hente tilbakekrevingsbehandlinger vart kjørt.")
            return emptyList()
        }
    }

    fun kanTilbakekrevingsbehandlingOpprettesManuelt(fagsakId: Long): KanBehandlingOpprettesManueltRespons {
        try {
            val uri =
                URI.create("$familieTilbakeUri/ytelsestype/${Ytelsestype.BARNETRYGD}/fagsak/$fagsakId/kanBehandlingOpprettesManuelt/v1")

            val response: Ressurs<KanBehandlingOpprettesManueltRespons> = getForEntity(uri)

            assertGenerelleSuksessKriterier(response)

            return if (response.status == Ressurs.Status.SUKSESS) {
                response.data!!
            } else {
                log.error(
                    "Kallet for å sjekke om tilbakekrevingsbehandling kan opprettes feilet! Feilmelding: ",
                    response.frontendFeilmelding
                )
                KanBehandlingOpprettesManueltRespons(
                    kanBehandlingOpprettes = false,
                    melding = "Teknisk feil. Feilen er logget"
                )
            }
        } catch (e: Exception) {
            secureLogger.error("Trøbbel mot tilbakekreving", e)
            log.error("Exception når kallet for  å sjekke om tilbakekrevingsbehandling kan opprettes vart kjørt.")
            return KanBehandlingOpprettesManueltRespons(
                kanBehandlingOpprettes = false,
                melding = "Tilbakekreving kan ikke opprettes på nåverende tidspunkt. Prøv igjen senere. Kontakt brukerstøtte dersom feilen vedvarer"
            )
        }
    }

    fun opprettTilbakekrevingsbehandlingManuelt(request: OpprettManueltTilbakekrevingRequest): Boolean {
        try {
            val uri = URI.create("$familieTilbakeUri/behandling/manuelt/task/v1")

            val response: Ressurs<String> = postForEntity(uri, request)

            assertGenerelleSuksessKriterier(response)

            return if (response.status == Ressurs.Status.SUKSESS) {
                log.debug("Respons fra familie-tilbake: ${response.data}")
                true
            } else {
                log.error(
                    "Kallet for å opprette tilbakekrevingsbehandling feilet! Feilmelding: ",
                    response.frontendFeilmelding
                )
                false
            }
        } catch (e: Exception) {
            secureLogger.error("Trøbbel mot tilbakekreving", e)
            log.error("Exception når kallet for å opprette tilbakekrevingsbehandling vart kjørt.")
            return false
        }
    }
}
