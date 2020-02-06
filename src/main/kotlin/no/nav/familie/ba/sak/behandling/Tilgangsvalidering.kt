package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.FagsakRepository
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonOnBehalfClient
import org.springframework.stereotype.Component

@Component("tilgang")
class Tilgangsvalidering(private val behandlingRepository: BehandlingRepository,
                         private val fagsakRepository: FagsakRepository,
                         private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
                         private val integrasjonOnBehalfClient: IntegrasjonOnBehalfClient) {


    fun tilFagsak(fagsakId: Long): Boolean {
        val fagsak = fagsakRepository.finnFagsak(fagsakId) ?: return true

        val personer = behandlingRepository.finnBehandlinger(fagsak.id)
                .filterNotNull()
                .mapNotNull { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(it.id)?.personer }
                .flatten()

        integrasjonOnBehalfClient.sjekkTilgangTilPersoner(personer).forEach { if (!it.harTilgang) return false }

        return true
    }

    fun tilBehandling(behandlingId: Long): Boolean {

        val personer = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)?.personer
        if (personer != null) {
            integrasjonOnBehalfClient.sjekkTilgangTilPersoner(personer).forEach { if (!it.harTilgang) return false }
        }

        return true
    }


}
