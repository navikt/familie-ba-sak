package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.oppgave.OppgaveService
import no.nav.familie.ba.sak.task.dto.FerdigstillOppgaveDTO
import org.springframework.stereotype.Service

@Service
class FerdigstillOppgave(private val oppgaveService: OppgaveService) : BehandlingSteg<FerdigstillOppgaveDTO> {

    override fun utførStegOgAngiNeste(behandling: Behandling, data: FerdigstillOppgaveDTO): StegType {
        oppgaveService.ferdigstillOppgave(
                behandlingId = data.behandlingId, oppgavetype = data.oppgavetype
        )

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.FERDIGSTILLE_OPPGAVE
    }
}