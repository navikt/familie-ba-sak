package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.FagsakRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BehandlingslagerService @Autowired constructor(private val fagsakRepository: FagsakRepository,
                                                     private val behandlingRepository: BehandlingRepository) {
    fun nyBehandling(fødselsnummer: String?,
                     fødselsnummerBarn: String?,
                     journalpostID: String?): Behandling? {
        //final var søkerAktørId = oppslagTjeneste.hentAktørId(fødselsnummer);
        //final var fagsak = Fagsak.opprettNy(søkerAktørId, new PersonIdent(fødselsnummer), journalpostID);
        //fagsakRepository.save(fagsak);
        //final var behandling = Behandling.forFørstegangssøknad(fagsak, journalpostID).build();
        //behandlingRepository.save(behandling);
        //return behandling;
        return null
    }

}