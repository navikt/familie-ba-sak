package no.nav.familie.ba.sak.integrasjoner.infotrygd

import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.SEND_START_BEHANDLING_TIL_INFOTRYGD
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.task.OpprettTaskService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class InfotrygdFeedService(
    val opprettTaskService: OpprettTaskService,
    val featureToggleService: FeatureToggleService
) {

    @Transactional
    fun sendTilInfotrygdFeed(barnsIdenter: List<String>) {
        logger.info("Send ${barnsIdenter.size} av fødselsmeldinger til Infotrygd.")
        opprettTaskService.opprettSendFeedTilInfotrygdTask(barnsIdenter)
    }

    @Transactional
    fun sendStartBehandlingTilInfotrygdFeed(fnrStoenadsmottaker: String) {
        if (!featureToggleService.isEnabled(SEND_START_BEHANDLING_TIL_INFOTRYGD)) {
            logger.info("send ikke startBehandling til infotrygd fordi feature er togglet av")
            return
        }
        logger.info("Send startBehandling til Infotrygd.")
        opprettTaskService.opprettSendStartBehandlingTilInfotrygdTask(fnrStoenadsmottaker)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(InfotrygdFeedService::class.java)
    }
}
