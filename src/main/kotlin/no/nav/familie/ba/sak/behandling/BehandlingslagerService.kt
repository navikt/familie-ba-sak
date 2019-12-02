package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.Fagsak
import no.nav.familie.ba.sak.behandling.domene.FagsakRepository
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class BehandlingslagerService @Autowired constructor(private val fagsakRepository: FagsakRepository,
                                                     private val behandlingRepository: BehandlingRepository) {
    fun nyBehandling(fødselsnummer: String,
                     fødselsnummerBarn: String,
                     journalpostID: String): Behandling? {
        //final var søkerAktørId = oppslagTjeneste.hentAktørId(fødselsnummer);

        var fagsak = Fagsak(null,AktørId("1"), PersonIdent(fødselsnummer))
        fagsakRepository.save(fagsak)
        var behandling = Behandling(null, fagsak, journalpostID, "LagMeg")
        behandlingRepository.save(behandling)

        return behandling
    }

    fun hentAlleBehandlinger(): MutableList<Behandling?> {
        return this.behandlingRepository.findAll();
    }

}