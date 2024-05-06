package no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering

import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.LeaderClientService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.task.MånedligValutajusteringFinnFagsakerTask
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Component
class MånedligValutajusteringScheduler(
    val behandlingService: BehandlingService,
    val fagsakService: FagsakService,
    val leaderClientService: LeaderClientService,
    val taskRepository: TaskRepositoryWrapper,
    private val unleashService: UnleashNextMedContextService,
) {
    private val logger = LoggerFactory.getLogger(MånedligValutajusteringScheduler::class.java)

    @Scheduled(cron = "0 0 12 1-7 * MON")
    @Transactional
    fun lagMånedligValutajusteringTask() {
        val inneværendeMåned = YearMonth.now()
        if (!unleashService.isEnabled(FeatureToggleConfig.KAN_KJØRE_AUTOMATISK_VALUTAJUSTERING_FOR_ALLE_SAKER)) {
            logger.info("FeatureToggle ${FeatureToggleConfig.KAN_KJØRE_AUTOMATISK_VALUTAJUSTERING_FOR_ALLE_SAKER} er skrudd av. Avbryter månedlig valutajustering.")
            return
        }

        if (leaderClientService.isLeader()) {
            logger.info("Kjører scheduled månedlig valutajustering for $inneværendeMåned")
            taskRepository.save(
                MånedligValutajusteringFinnFagsakerTask.lagTask(inneværendeMåned),
            )
        }
    }
}
