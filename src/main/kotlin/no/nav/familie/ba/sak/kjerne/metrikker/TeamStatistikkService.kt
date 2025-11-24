package no.nav.familie.ba.sak.kjerne.metrikker

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.config.LeaderClientService
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.YearMonth

@Service
class TeamStatistikkService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val leaderClientService: LeaderClientService,
    private val featureToggleService: FeatureToggleService,
) {
    val utbetalingerPerMånedGauge =
        MultiGauge.builder("UtbetalingerPerMaanedGauge").register(Metrics.globalRegistry)
    val antallFagsakerPerMånedGauge =
        MultiGauge.builder("AntallFagsakerPerMaanedGauge").register(Metrics.globalRegistry)
    val løpendeFagsakerPerMånedGauge =
        MultiGauge.builder("LopendeFagsakerPerMaanedGauge").register(Metrics.globalRegistry)
    val åpneBehandlingerPerMånedGauge =
        MultiGauge.builder("AapneBehandlingerPerMaanedGauge").register(Metrics.globalRegistry)
    val tidSidenOpprettelseåpneBehandlingerPerMånedGauge =
        MultiGauge.builder("TidSidenOpprettelseAapneBehandlingerPerMaanedGauge").register(Metrics.globalRegistry)

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(cron = "0 0 4 * * *")
    fun utbetalinger() {
        if (!featureToggleService.isEnabled(FeatureToggle.TUNGTKJØRENDE_GRAFANA_STATISTIKK, false)) return
        if (!erLeader()) return
        logger.info("Genererer statistikk for utbetalinger")

        val månederMedTotalUtbetaling =
            listOf<YearMonth>(
                YearMonth.now(),
                YearMonth.now().plusMonths(1),
            ).associateWith {
                behandlingRepository.hentTotalUtbetalingForMåned(it.førsteDagIInneværendeMåned().atStartOfDay())
            }

        val rows =
            månederMedTotalUtbetaling.map {
                MultiGauge.Row.of(
                    Tags.of(
                        ÅR_MÅNED_TAG,
                        "${it.key.year}-${it.key.month}",
                    ),
                    it.value,
                )
            }

        utbetalingerPerMånedGauge.register(rows)
        logger.info("Genererer statistikk for utbetalinger er ferdig")
    }

    @Scheduled(initialDelay = FEM_MINUTTER_VENTETID_FØR_OPPDATERING_FØRSTE_GANG, fixedRate = OPPDATERING_HVER_DAG)
    fun antallFagsaker() {
        if (!erLeader()) return
        logger.info("Genererer statistikk for antall fagsaker")

        val antallFagsaker = fagsakRepository.finnAntallFagsakerTotalt()

        val rows =
            listOf(
                MultiGauge.Row.of(
                    Tags.of(
                        ÅR_MÅNED_TAG,
                        "${YearMonth.now().year}-${YearMonth.now().month}",
                    ),
                    antallFagsaker,
                ),
            )

        antallFagsakerPerMånedGauge.register(rows)
        logger.info("Genererer statistikk for antall fagsaker er ferdig")
    }

    @Scheduled(initialDelay = FEM_MINUTTER_VENTETID_FØR_OPPDATERING_FØRSTE_GANG, fixedRate = OPPDATERING_HVER_DAG)
    fun løpendeFagsaker() {
        if (!erLeader()) return
        logger.info("Genererer statistikk for løpende fagsaker")

        val løpendeFagsaker = fagsakRepository.finnAntallFagsakerLøpende()

        val rows =
            listOf(
                MultiGauge.Row.of(
                    Tags.of(
                        ÅR_MÅNED_TAG,
                        "${YearMonth.now().year}-${YearMonth.now().month}",
                    ),
                    løpendeFagsaker,
                ),
            )

        løpendeFagsakerPerMånedGauge.register(rows)
        logger.info("Genererer statistikk for løpende fagsaker er ferdig")
    }

    @Scheduled(initialDelay = FEM_MINUTTER_VENTETID_FØR_OPPDATERING_FØRSTE_GANG, fixedRate = OPPDATERING_HVER_DAG)
    fun åpneBehandlinger() {
        if (!erLeader()) return

        val åpneBehandlinger = behandlingRepository.finnAntallBehandlingerIkkeAvsluttet()

        val rows =
            listOf(
                MultiGauge.Row.of(
                    Tags.of(
                        ÅR_MÅNED_TAG,
                        "${YearMonth.now().year}-${YearMonth.now().month}",
                    ),
                    åpneBehandlinger,
                ),
            )

        åpneBehandlingerPerMånedGauge.register(rows)
    }

    @Scheduled(initialDelay = FEM_MINUTTER_VENTETID_FØR_OPPDATERING_FØRSTE_GANG, fixedRate = OPPDATERING_HVER_DAG)
    fun tidFraOpprettelsePåÅpneBehandlinger() {
        if (!featureToggleService.isEnabled(FeatureToggle.TUNGTKJØRENDE_GRAFANA_STATISTIKK, false)) return
        if (!erLeader()) return

        logger.info("Genererer statistikk for tid siden opprettelse på åpne behandlinger")

        val opprettelsestidspunktPååpneBehandlinger = behandlingRepository.finnOpprettelsestidspunktPåÅpneBehandlinger()
        val diffPåÅpneBehandlinger =
            opprettelsestidspunktPååpneBehandlinger.map { Duration.between(it, LocalDateTime.now()).seconds }

        val snitt = diffPåÅpneBehandlinger.average()
        val max = diffPåÅpneBehandlinger.maxOf { it }
        val min = diffPåÅpneBehandlinger.minOf { it }

        val rows =
            listOf(
                MultiGauge.Row.of(
                    Tags.of(
                        ÅR_MÅNED_TAG,
                        "${YearMonth.now().year}-${YearMonth.now().month}-snitt",
                    ),
                    snitt,
                ),
                MultiGauge.Row.of(
                    Tags.of(
                        ÅR_MÅNED_TAG,
                        "${YearMonth.now().year}-${YearMonth.now().month}-max",
                    ),
                    max,
                ),
                MultiGauge.Row.of(
                    Tags.of(
                        ÅR_MÅNED_TAG,
                        "${YearMonth.now().year}-${YearMonth.now().month}-min",
                    ),
                    min,
                ),
            )

        tidSidenOpprettelseåpneBehandlingerPerMånedGauge.register(rows)
        logger.info("Genererer statistikk for tid siden opprettelse på åpne behandlinger er ferdig")
    }

    @Scheduled(cron = "0 0 14 * * *")
    fun loggÅpneBehandlingerSomHarLiggetLenge() {
        if (!erLeader()) return

        listOf(180, 150, 120, 90, 60).fold(mutableSetOf<Long>()) { acc, dagerSiden ->
            val åpneBehandlinger =
                behandlingRepository
                    .finnÅpneBehandlinger(
                        opprettetFør = LocalDateTime.now().minusDays(dagerSiden.toLong()),
                    ).filter { !acc.contains(it.id) }

            if (åpneBehandlinger.isNotEmpty()) {
                logger.warn(
                    "${åpneBehandlinger.size} åpne behandlinger har ligget i over $dagerSiden dager: \n" +
                        "${åpneBehandlinger.map { behandling -> "$behandling\n" }}",
                )
                acc.addAll(åpneBehandlinger.map { it.id })
            }

            acc
        }
    }

    private fun erLeader(): Boolean = leaderClientService.isLeader()

    companion object {
        const val OPPDATERING_HVER_DAG: Long = 1000 * 60 * 60 * 24
        const val FEM_MINUTTER_VENTETID_FØR_OPPDATERING_FØRSTE_GANG: Long = 1000 * 60 * 5
        const val ÅR_MÅNED_TAG = "aar-maaned"
        val logger = LoggerFactory.getLogger(TeamStatistikkService::class.java)
    }
}
