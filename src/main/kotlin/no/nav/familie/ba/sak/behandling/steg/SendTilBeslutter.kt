package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.logg.LoggService
import org.springframework.stereotype.Service

@Service
class SendTilBeslutter(
        private val behandlingService: BehandlingService,
        private val loggService: LoggService
) : BehandlingSteg<String> {

    override fun utførStegOgAngiNeste(behandling: Behandling, data: String): StegType {
        loggService.opprettSendTilBeslutterLogg(behandling)
        behandlingService.oppdaterStatusPåBehandling(behandlingId = behandling.id,
                                                     status = BehandlingStatus.SENDT_TIL_BESLUTTER)
        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.SEND_TIL_BESLUTTER
    }
}