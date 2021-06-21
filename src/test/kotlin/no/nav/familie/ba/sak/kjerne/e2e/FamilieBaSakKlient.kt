package no.nav.familie.ba.sak.kjerne.e2e

import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestJournalføring
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRequest
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestOperations
import java.net.URI

class FamilieBaSakKlient(
        private val baSakUrl: String,
        private val restOperations: RestOperations,
        private val headers: HttpHeaders
) : AbstractRestClient(restOperations, "familie-ba-sak") {

    fun hentFagsak(fagsakId: Long): Ressurs<RestFagsak> {
        return getForEntity(
                URI.create("$baSakUrl/api/fagsaker/$fagsakId"),
                headers,
        )
    }

    fun journalfør(journalpostId: String,
                   oppgaveId: String,
                   journalførendeEnhet: String,
                   restJournalføring: RestJournalføring): Ressurs<String> {
        return postForEntity(
                URI.create("$baSakUrl/api/journalpost/$journalpostId/journalfør/$oppgaveId?journalfoerendeEnhet=$journalførendeEnhet"),
                restJournalføring,
                headers
        )
    }

    fun opprettBehandling(søkersIdent: String,
                          behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                          behandlingÅrsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD): Ressurs<RestFagsak> {
        return postForEntity(
                URI.create("$baSakUrl/api/behandlinger"),
                NyBehandling(
                        kategori = BehandlingKategori.NASJONAL,
                        underkategori = BehandlingUnderkategori.ORDINÆR,
                        søkersIdent = søkersIdent,
                        behandlingType = behandlingType,
                        behandlingÅrsak = behandlingÅrsak
                ),
                headers
        )
    }

    fun registrererSøknad(behandlingId: Long, restRegistrerSøknad: RestRegistrerSøknad): Ressurs<RestFagsak> {
        return postForEntity(URI.create("$baSakUrl/api/behandlinger/$behandlingId/registrere-søknad-og-hent-persongrunnlag"),
                             restRegistrerSøknad, headers)
    }
}