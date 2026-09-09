package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.domene.erOppfylt
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.FiltreringsreglerFødselshendelseService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FiltreringAutomatiskBehandlingSteg(
    private val filtreringsreglerFødselshendelseService: FiltreringsreglerFødselshendelseService,
) : BehandlingSteg<NyBehandlingHendelse> {
    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: NyBehandlingHendelse,
    ): StegType {
        logger.info("Kjører filtreringsregler for behandling ${behandling.id}")

        return when (behandling.opprettetÅrsak) {
            BehandlingÅrsak.FØDSELSHENDELSE -> {
                val fødselshendelsefiltreringResultat =
                    filtreringsreglerFødselshendelseService.kjørFiltreringsregler(
                        data,
                        behandling,
                    )

                if (!fødselshendelsefiltreringResultat.erOppfylt()) {
                    StegType.HENLEGG_BEHANDLING
                } else {
                    hentNesteStegForNormalFlyt(behandling)
                }
            }

            else -> {
                throw Feil("Behandling ${behandling.id} har en årsak som ikke skal filtreres: ${behandling.opprettetÅrsak} ")
            }
        }
    }

    override fun stegType(): StegType = StegType.FILTRERING_AUTOMATISK_BEHANDLING

    companion object {
        private val logger = LoggerFactory.getLogger(FiltreringAutomatiskBehandlingSteg::class.java)
    }
}
