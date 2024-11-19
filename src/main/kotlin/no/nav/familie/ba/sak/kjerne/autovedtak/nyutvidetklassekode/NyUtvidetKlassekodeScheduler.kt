package no.nav.familie.ba.sak.kjerne.autovedtak.nyutvidetklassekode

import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.KJØR_AUTOVEDTAK_NY_KLASSEKODE_FOR_UTVIDET_BARNETRYGD
import no.nav.familie.ba.sak.config.LeaderClientService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.nyutvidetklassekode.domene.NyUtvidetKlasskodeKjøringRepository
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.AutovedtakSatsendringScheduler.Companion.CRON_HVERT_10_MIN_UKEDAG
import no.nav.familie.unleash.UnleashService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Limit
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class NyUtvidetKlassekodeScheduler(
    private val nyUtvidetKlassekodeKjøringRepository: NyUtvidetKlasskodeKjøringRepository,
    private val taskRepository: TaskRepositoryWrapper,
    private val leaderClientService: LeaderClientService,
    private val unleashunleashService: UnleashService,
) {
    @Scheduled(cron = CRON_HVERT_10_MIN_UKEDAG)
    fun triggAutovedtakNyUtvidetKlassekode() {
        if (leaderClientService.isLeader() && unleashunleashService.isEnabled(KJØR_AUTOVEDTAK_NY_KLASSEKODE_FOR_UTVIDET_BARNETRYGD)) {
            startAutovedtakNyUtvidetKlassekode(1000)
        }
    }

    private fun startAutovedtakNyUtvidetKlassekode(antallFagsaker: Int) {
        nyUtvidetKlassekodeKjøringRepository
            .findByBrukerNyKlassekodeIsFalse(limit = Limit.of(antallFagsaker))
            .also {
                logger.info("Oppretter tasker for å migrere fagsak til ny utvidet klassekode på ${it.size} fagsaker.")
            }.forEach { fagsak ->
                taskRepository.save(NyUtvidetKlassekodeTask.lagTask(fagsak.fagsakId))
            }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NyUtvidetKlassekodeScheduler::class.java)
    }
}
