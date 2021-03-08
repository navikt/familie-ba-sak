package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.simulering.SimuleringService
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SimuleringSteg(
        private val simuleringService: SimuleringService,
        private val vedtakService: VedtakService
) : BehandlingSteg<String> {

    @Transactional
    override fun utførStegOgAngiNeste(behandling: Behandling, data: String): StegType {
        val vedtak = vedtakService.hentAktivForBehandling(behandling.id)
                     ?: throw Feil("Fant ikke vedtak på behandling ${behandling.id}")

        val simulering: List<SimuleringMottaker> =
                simuleringService.hentSimulering(vedtak = vedtak).simuleringMottaker
        simuleringService.lagreSimulering(simulering, vedtak)



        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType = StegType.SIMULERING
}