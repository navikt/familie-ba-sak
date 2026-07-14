package no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs

import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.eøs.sats.EøsSatserRegister
import no.nav.familie.ba.sak.task.OpprettTaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class StartSatsendringEøs(
    private val behandlingRepository: BehandlingRepository,
    private val opprettTaskService: OpprettTaskService,
) {
    fun opprettSatsendringEøsTaskerForRelevanteFagsaker(
        utbetalingsland: String,
        satsTidspunkt: YearMonth,
    ): List<Long> {
        val nySats = EøsSatserRegister.hentSatsForLandIMåned(utbetalingsland, satsTidspunkt)
        val fagsakIder =
            behandlingRepository.finnLøpendeEøsFagsakerMedUtenlandskPeriodebeløpSomOverlapperSats(
                utbetalingsland = utbetalingsland,
                satsFom = nySats.fom.toLocalDate(),
                satsTom = nySats.tom?.toLocalDate(),
            )

        logger.info("Oppretter EØS-satsendringstasker for ${fagsakIder.size} fagsaker for utbetalingsland $utbetalingsland og satstidspunkt $satsTidspunkt")

        fagsakIder.forEach { fagsakId ->
            opprettTaskService.opprettSatsendringEøsTask(
                fagsakId = fagsakId,
                utbetalingsland = utbetalingsland,
                satsTidspunkt = satsTidspunkt,
            )
        }

        return fagsakIder
    }

    fun opprettSatsendringEøsTaskForFagsak(
        fagsakId: Long,
        utbetalingsland: String,
        satsTidspunkt: YearMonth,
    ) {
        EøsSatserRegister.hentSatsForLandIMåned(utbetalingsland, satsTidspunkt)
        opprettTaskService.opprettSatsendringEøsTask(
            fagsakId = fagsakId,
            utbetalingsland = utbetalingsland,
            satsTidspunkt = satsTidspunkt,
        )
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(StartSatsendringEøs::class.java)
    }
}
