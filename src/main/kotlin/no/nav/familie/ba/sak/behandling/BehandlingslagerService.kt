package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class BehandlingslagerService @Autowired constructor(private val fagsakRepository: FagsakRepository,
                                                     private val behandlingRepository: BehandlingRepository) {
    fun nyBehandling(fødselsnummer: String,
                     barnasFødselsnummer: Array<String>,
                     behandlingType: BehandlingType,
                     journalpostID: String,
                     saksnummer: String): Behandling {
        //final var søkerAktørId = oppslagTjeneste.hentAktørId(fødselsnummer);

        val fagsak = Fagsak(null, AktørId("1"), PersonIdent(fødselsnummer))
        fagsakRepository.save(fagsak)
        val behandling = Behandling(null, fagsak, journalpostID, behandlingType, saksnummer)
        behandlingRepository.save(behandling)

        return behandling
    }

    fun hentAlleBehandlinger(): MutableList<Behandling?> {
        return this.behandlingRepository.findAll();
    }

    fun lagreBehandling(behandling: Behandling) {
        behandlingRepository.save(behandling)
    }
}