package no.nav.familie.ba.sak.kjerne.autovedtak.oppdaterutvidetklassekode

import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.LeaderClientService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.kjerne.autovedtak.oppdaterutvidetklassekode.domene.OppdaterUtvidetKlassekodeKjøringRepository
import no.nav.familie.ba.sak.kjerne.autovedtak.oppdaterutvidetklassekode.domene.Status
import no.nav.familie.ba.sak.task.OpprettTaskService.Companion.overstyrTaskMedNyCallId
import no.nav.familie.prosessering.util.IdUtils
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class OppdaterUtvidetKlassekodeScheduler(
    private val oppdaterUtvidetKlassekodeKjøringRepository: OppdaterUtvidetKlassekodeKjøringRepository,
    private val taskRepository: TaskRepositoryWrapper,
    private val leaderClientService: LeaderClientService,
    private val unleashService: UnleashNextMedContextService,
) {
    @Scheduled(cron = CRON_HVERT_10_MIN_UKEDAG)
    fun triggAutovedtakOppdaterUtvidetKlassekode() {
        if (leaderClientService.isLeader() && unleashService.isEnabled(FeatureToggle.OPPRETT_AUTOVEDTAK_OPPDATER_KLASSEKODE_FOR_UTVIDET_BARNETRYGD_AUTOMATISK)) {
            if (unleashService.isEnabled(FeatureToggle.AUTOVEDTAK_OPPDATER_KLASSEKODE_FOR_UTVIDET_BARNETRYGD_HØYT_VOLUM)) {
                startAutovedtakOppdaterUtvidetKlassekode(1200)
            } else {
                startAutovedtakOppdaterUtvidetKlassekode(100)
            }
        }
    }

    private fun startAutovedtakOppdaterUtvidetKlassekode(antallFagsaker: Int) {
        oppdaterUtvidetKlassekodeKjøringRepository
            .finnRelevanteOppdaterUtvidetKlassekodeKjøringer(PageRequest.of(0, antallFagsaker))
            .also {
                logger.info("Oppretter tasker for å migrere fagsak til ny utvidet klassekode på ${it.size} fagsaker.")
            }.forEach { fagsak ->
                taskRepository.save(overstyrTaskMedNyCallId(IdUtils.generateId()) { OppdaterUtvidetKlassekodeTask.lagTask(fagsak.fagsakId) })
                oppdaterUtvidetKlassekodeKjøringRepository.oppdaterStatus(fagsakId = fagsak.fagsakId, status = Status.UTFØRES)
            }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OppdaterUtvidetKlassekodeScheduler::class.java)
        const val CRON_HVERT_10_MIN_UKEDAG = "0 */10 7-20 * * MON-FRI"
    }
}
