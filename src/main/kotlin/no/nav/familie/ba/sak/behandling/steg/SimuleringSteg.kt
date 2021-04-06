package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SimuleringSteg(
) : BehandlingSteg<String> {

    @Transactional
    override fun utførStegOgAngiNeste(behandling: Behandling, data: String): StegType {
        if (behandling.skalBehandlesAutomatisk) {
            return when (behandling.resultat) {
                BehandlingResultat.FORTSATT_INNVILGET -> StegType.JOURNALFØR_VEDTAKSBREV
                else -> StegType.IVERKSETT_MOT_OPPDRAG
            }
        }
        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType = StegType.SIMULERING
}