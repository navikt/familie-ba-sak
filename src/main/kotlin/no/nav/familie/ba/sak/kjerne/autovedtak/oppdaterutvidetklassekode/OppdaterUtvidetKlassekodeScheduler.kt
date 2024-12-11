package no.nav.familie.ba.sak.kjerne.autovedtak.oppdaterutvidetklassekode

import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.AUTOVEDTAK_OPPDATER_KLASSEKODE_FOR_UTVIDET_BARNETRYGD_HØYT_VOLUM
import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.OPPRETT_AUTOVEDTAK_OPPDATER_KLASSEKODE_FOR_UTVIDET_BARNETRYGD_AUTOMATISK
import no.nav.familie.ba.sak.config.LeaderClientService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.oppdaterutvidetklassekode.domene.OppdaterUtvidetKlassekodeKjøringRepository
import no.nav.familie.ba.sak.task.OpprettTaskService.Companion.overstyrTaskMedNyCallId
import no.nav.familie.prosessering.util.IdUtils
import no.nav.familie.unleash.UnleashService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Limit
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class OppdaterUtvidetKlassekodeScheduler(
    private val oppdaterUtvidetKlassekodeKjøringRepository: OppdaterUtvidetKlassekodeKjøringRepository,
    private val taskRepository: TaskRepositoryWrapper,
    private val leaderClientService: LeaderClientService,
    private val unleashService: UnleashService,
) {
    @Scheduled(cron = CRON_HVERT_10_MIN_UKEDAG)
    fun triggAutovedtakOppdaterUtvidetKlassekode() {
        if (leaderClientService.isLeader() && unleashService.isEnabled(OPPRETT_AUTOVEDTAK_OPPDATER_KLASSEKODE_FOR_UTVIDET_BARNETRYGD_AUTOMATISK)) {
            if (unleashService.isEnabled(AUTOVEDTAK_OPPDATER_KLASSEKODE_FOR_UTVIDET_BARNETRYGD_HØYT_VOLUM)) {
                startAutovedtakOppdaterUtvidetKlassekode(1200)
            } else {
                startAutovedtakOppdaterUtvidetKlassekode(100)
            }
        }
    }

    private fun startAutovedtakOppdaterUtvidetKlassekode(antallFagsaker: Int) {
        oppdaterUtvidetKlassekodeKjøringRepository
            .findByBrukerNyKlassekodeIsFalse(limit = Limit.of(antallFagsaker))
            .also {
                logger.info("Oppretter tasker for å migrere fagsak til ny utvidet klassekode på ${it.size} fagsaker.")
            }.forEach { fagsak ->
                taskRepository.save(overstyrTaskMedNyCallId(IdUtils.generateId()) { OppdaterUtvidetKlassekodeTask.lagTask(fagsak.fagsakId) })
            }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OppdaterUtvidetKlassekodeScheduler::class.java)
        const val CRON_HVERT_10_MIN_UKEDAG = "0 */10 7-18 * * MON-FRI"
    }
}
