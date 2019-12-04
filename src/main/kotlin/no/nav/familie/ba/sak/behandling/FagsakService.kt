package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.Fagsak
import no.nav.familie.ba.sak.behandling.domene.FagsakRepository
import no.nav.familie.ba.sak.behandling.restDomene.RestBehandling
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.restDomene.toRestFagsak
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakt.Ressurs
import org.springframework.stereotype.Service

@Service
class FagsakService(private val behandlingRepository: BehandlingRepository,
                    private val fagsakRepository: FagsakRepository) {

    fun hentFagsak(fagsakId: Long): Ressurs<Fagsak> {
        return when(val it = fagsakRepository.finnFagsak(fagsakId)) {
            null -> Ressurs.failure("Fant ikke fagsak med fagsakId: $fagsakId")
            else -> Ressurs.success( data = it )
        }
    }

    fun hentRestFagsak(fagsakId: Long): Ressurs<RestFagsak> {
        val fagsak = fagsakRepository.finnFagsak(fagsakId) ?: return Ressurs.failure("Fant ikke fagsak med fagsakId: $fagsakId")

        val behandlinger = behandlingRepository.finnBehandlinger(fagsak.id)

        val restBehandlinger: List<RestBehandling> = behandlinger.map {
            RestBehandling(it?.id, it?.barnasFødselsnummer)
        }

        return Ressurs.success( data = fagsak.toRestFagsak(restBehandlinger) )
    }

    fun hentFagsakForPersonident(personIdent: PersonIdent): Fagsak? {
        return fagsakRepository.finnFagsakForPersonIdent(personIdent)
    }

    fun lagreFagsak(fagsak: Fagsak) {
        fagsakRepository.save(fagsak)
    }
}