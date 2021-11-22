package no.nav.familie.ba.sak.kjerne.metrikker

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.leader.LeaderClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class Utbetalingsmetrikker(
    val behandlingRepository: BehandlingRepository
) {
    val utbetalingInneværendeMånedGauge =
        MultiGauge.builder("UtbetalingInnevaerendeMaanedGauge").register(Metrics.globalRegistry)

    @Scheduled(initialDelay = 60000, fixedDelay = OPPDATERINGSFREKVENS)
    fun åpneBehandlinger() {
        if (LeaderClient.isLeader() != true) return
        val totalUtbetalingInneværendeMåned = behandlingRepository.hentTotalUtbetalingInneværendeMåned()

        logger.info("Total utbetaling for inneværende måned: $totalUtbetalingInneværendeMåned")
        val rows = listOf(
            MultiGauge.Row.of(
                Tags.of(
                    "uke", YearMonth.now().year.toString() + "-" + YearMonth.now().month.toString()
                ),
                totalUtbetalingInneværendeMåned
            )
        )

        utbetalingInneværendeMånedGauge.register(rows)
    }

    companion object {
        const val OPPDATERINGSFREKVENS = 30 * 60 * 1000L
        val logger = LoggerFactory.getLogger(Utbetalingsmetrikker::class.java)
    }
}
