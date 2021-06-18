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
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.client.postForEntity

class FamilieBaSakKlient(
        private val baSakUrl: String,
        private val restTemplate: RestTemplate,
        private val headers: HttpHeaders
) {

    fun hentFagsak(fagsakId: Long): Ressurs<RestFagsak> {
        return restTemplate.exchange<Ressurs<RestFagsak>>("$baSakUrl/api/fagsaker/$fagsakId",
                                                          HttpMethod.GET,
                                                          HttpEntity(null, headers)).body!!
    }

    fun opprettFagsak(søkersIdent: String): Ressurs<RestFagsak> {
        return restTemplate.postForEntity<Ressurs<RestFagsak>>(
                "$baSakUrl/api/fagsaker",
                HttpEntity(FagsakRequest(
                        personIdent = søkersIdent
                ), headers)).body!!
    }

    fun journalfør(journalpostId: String,
                   oppgaveId: String,
                   journalførendeEnhet: String,
                   restJournalføring: RestJournalføring): Ressurs<String> {
        return restTemplate.postForEntity<Ressurs<String>>(
                "$baSakUrl/api/journalpost/$journalpostId/journalfør/$oppgaveId?journalfoerendeEnhet=$journalførendeEnhet",
                HttpEntity<RestJournalføring>(restJournalføring, headers)
        ).body!!
    }

    fun opprettBehandling(søkersIdent: String,
                          behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                          behandlingÅrsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD): Ressurs<RestFagsak> {
        return restTemplate.postForEntity<Ressurs<RestFagsak>>(
                "$baSakUrl/api/behandlinger",
                HttpEntity(NyBehandling(
                        kategori = BehandlingKategori.NASJONAL,
                        underkategori = BehandlingUnderkategori.ORDINÆR,
                        søkersIdent = søkersIdent,
                        behandlingType = behandlingType,
                        behandlingÅrsak = behandlingÅrsak
                ), headers)).body!!
    }

    fun registrererSøknad(behandlingId: Long, restRegistrerSøknad: RestRegistrerSøknad): Ressurs<RestFagsak> {
        return restTemplate.postForEntity<Ressurs<RestFagsak>>("$baSakUrl/api/behandlinger/$behandlingId/registrere-søknad-og-hent-persongrunnlag",
                                                               HttpEntity(restRegistrerSøknad, headers)).body!!
    }
}