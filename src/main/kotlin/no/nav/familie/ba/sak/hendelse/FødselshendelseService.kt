package no.nav.familie.ba.sak.hendelse

import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.infotrygd.InfotrygdFeedService
import org.springframework.stereotype.Service

@Service
class FødselshendelseService(private val infotrygdFeedService: InfotrygdFeedService,
                             private val featureToggleService: FeatureToggleService) {

    fun fødselshendelseSkalBehandlesHosInfotrygd(): Boolean {
        // TODO: Avgjør om fødsel skal behandles i BA-sak eller infotrygd basert på data fra replikatjenesten og BA-sak

        return featureToggleService.isEnabled("familie-ba-sak.rollback-automatisk-regelkjoring")
    }

    fun sendTilInfotrygdFeed(barnIdenter: List<String>) {
        infotrygdFeedService.sendTilInfotrygdFeed(barnIdenter)
    }
}