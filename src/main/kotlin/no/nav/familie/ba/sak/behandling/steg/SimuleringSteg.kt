package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.simulering.SimuleringService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SimuleringSteg(
        private val vedtakService: VedtakService,
        private val simuleringService: SimuleringService,
        private val toggleService: FeatureToggleService,
) : BehandlingSteg<String> {

    @Transactional
    override fun utførStegOgAngiNeste(behandling: Behandling, data: String): StegType {

        if (toggleService.isEnabled(FeatureToggleConfig.BRUK_SIMULERING)) {
            simuleringService.oppdaterSimuleringPåBehandling(behandling)
        }

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType = StegType.SIMULERING
}