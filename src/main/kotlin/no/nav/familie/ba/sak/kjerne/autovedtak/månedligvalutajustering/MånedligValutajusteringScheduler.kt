package no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering

import no.nav.familie.ba.sak.config.LeaderClientService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.task.MånedligValutajusteringFinnFagsakerTask
import no.nav.familie.util.VirkedagerProvider
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@Component
class MånedligValutajusteringScheduler(
    val behandlingService: BehandlingService,
    val fagsakService: FagsakService,
    val leaderClientService: LeaderClientService,
    val taskRepository: TaskRepositoryWrapper,
) {
    private val logger = LoggerFactory.getLogger(MånedligValutajusteringScheduler::class.java)

    /**
     * Denne funksjonen kjøres kl.6 den første dagen i måneden og setter triggertid på tasken til kl.7 den første virkedagen i måneden.
     * For testformål kan funksjonen opprettTask også kalles direkte via et restendepunkt.
     */
    @Scheduled(cron = "0 0 $KLOKKETIME_SCHEDULER_TRIGGES 1 * *")
    @Transactional
    fun lagScheduledMånedligValutajusteringTask() {
        if (leaderClientService.isLeader()) {
            lagMånedligValutajusteringTask(triggerTid = hentNesteVirkedag())
        }
    }

    fun lagMånedligValutajusteringTask(triggerTid: LocalDateTime) {
        val inneværendeMåned = YearMonth.now()
        logger.info("Kjører scheduled månedlig valutajustering for $inneværendeMåned")
        taskRepository.save(
            MånedligValutajusteringFinnFagsakerTask.lagTask(
                inneværendeMåned = inneværendeMåned,
                triggerTid = triggerTid,
            ),
        )
    }

    private fun hentNesteVirkedag(): LocalDateTime =
        VirkedagerProvider
            .nesteVirkedag(
                LocalDate.now().minusDays(1),
            ).atTime(KLOKKETIME_SCHEDULER_TRIGGES.inc(), 0)

    companion object {
        const val KLOKKETIME_SCHEDULER_TRIGGES = 2
    }
}
