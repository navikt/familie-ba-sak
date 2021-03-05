package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.simulering.SimuleringService
import org.springframework.stereotype.Service

@Service
class Simulering(
        private val simuleringService: SimuleringService,
        private val vedtakService: VedtakService
) : BehandlingSteg<String> {

    override fun utførStegOgAngiNeste(behandling: Behandling, data: String): StegType {
        val vedtak = vedtakService.hentAktivForBehandling(behandling.id)
                     ?: throw Feil("Fant ikke vedtak på behandling ${behandling.id}")

        val detaljertSimuleringResultat = simuleringService.hentSimulering(vedtak = vedtak)


        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType = StegType.SIMULERING
}