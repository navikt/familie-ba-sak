package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import org.springframework.stereotype.Service

@Service
class AvgjørAutomatiskEllerManuell
    : BehandlingSteg<String> {

    override fun stegType(): StegType {
        return StegType.AVGJØR_AUTOMATISK_ELLER_MANUELL
    }

    override fun utførStegOgAngiNeste(behandling: Behandling, data: String): StegType {
        // TODO: Kjør filtreringsregler som avgjør om fødselshendelsen skal behandles automatisk eller manuelt

        return hentNesteStegForNormalFlyt(behandling)
    }
}