package no.nav.familie.ba.sak.kjerne.metrikker

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.leader.LeaderClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.YearMonth

@Service
class TeamStatistikkService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository
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

    @Scheduled(cron = "*/$OPPDATERINGSFREKVENS_MINUTTER * * * *")
    fun utbetalinger() {
        val månederMedTotalUtbetaling =
            listOf<LocalDateTime>(
                LocalDateTime.now(),
                LocalDateTime.now().plusMonths(1)
            ).associateWith {
                behandlingRepository.hentTotalUtbetalingForMåned(it)
            }

        if (!erLeaderOgLoggResultat(
                beskrivelse = "Total utbetalinger",
                resultat = månederMedTotalUtbetaling.toString()
            )
        ) return

        val rows = månederMedTotalUtbetaling.map {
            MultiGauge.Row.of(
                Tags.of(
                    ÅR_MÅNED_TAG, "${it.key.year}-${it.key.month}"
                ),
                it.value
            )
        }

        utbetalingerPerMånedGauge.register(rows)
    }

    @Scheduled(cron = "*/$OPPDATERINGSFREKVENS_MINUTTER * * * *")
    fun antallFagsaker() {
        val antallFagsaker = fagsakRepository.finnAntallFagsakerTotalt()

        if (!erLeaderOgLoggResultat(
                beskrivelse = "Antall fagsaker",
                resultat = antallFagsaker.toString()
            )
        ) return

        val rows = listOf(
            MultiGauge.Row.of(
                Tags.of(
                    ÅR_MÅNED_TAG, "${YearMonth.now().year}-${YearMonth.now().month}"
                ),
                antallFagsaker
            )
        )

        antallFagsakerPerMånedGauge.register(rows)
    }

    @Scheduled(cron = "*/$OPPDATERINGSFREKVENS_MINUTTER * * * *")
    fun løpendeFagsaker() {
        val løpendeFagsaker = fagsakRepository.finnAntallFagsakerLøpende()

        if (!erLeaderOgLoggResultat(
                beskrivelse = "Løpende fagsaker",
                resultat = løpendeFagsaker.toString()
            )
        ) return

        val rows = listOf(
            MultiGauge.Row.of(
                Tags.of(
                    ÅR_MÅNED_TAG, "${YearMonth.now().year}-${YearMonth.now().month}"
                ),
                løpendeFagsaker
            )
        )

        løpendeFagsakerPerMånedGauge.register(rows)
    }

    @Scheduled(initialDelay = INITIAL_DELAY, fixedDelay = OPPDATERINGSFREKVENS)
    fun åpneBehandlinger() {
        val åpneBehandlinger = behandlingRepository.finnAntallBehandlingerIkkeAvsluttet()

        if (!erLeaderOgLoggResultat(
                beskrivelse = "Åpne behandlinger",
                resultat = åpneBehandlinger.toString()
            )
        ) return

        val rows = listOf(
            MultiGauge.Row.of(
                Tags.of(
                    ÅR_MÅNED_TAG, "${YearMonth.now().year}-${YearMonth.now().month}"
                ),
                åpneBehandlinger
            )
        )

        åpneBehandlingerPerMånedGauge.register(rows)
    }

    @Scheduled(cron = "*/$OPPDATERINGSFREKVENS_MINUTTER * * * *")
    fun tidFraOpprettelsePåÅpneBehandlinger() {
        val opprettelsestidspunktPååpneBehandlinger = behandlingRepository.finnOpprettelsestidspunktPåÅpneBehandlinger()
        val diffPåÅpneBehandlinger =
            opprettelsestidspunktPååpneBehandlinger.map { Duration.between(it, LocalDateTime.now()).seconds }

        val snitt = diffPåÅpneBehandlinger.average()
        val max = diffPåÅpneBehandlinger.maxOf { it }
        val min = diffPåÅpneBehandlinger.minOf { it }

        if (!erLeaderOgLoggResultat(
                beskrivelse = "Gjennomsnitt siden åpne behandlinger ble opprettet",
                resultat = "snitt: $snitt, max: $max, min, $min"
            )
        ) return

        val rows = listOf(
            MultiGauge.Row.of(
                Tags.of(
                    ÅR_MÅNED_TAG, "${YearMonth.now().year}-${YearMonth.now().month}-snitt"
                ),
                snitt
            ),
            MultiGauge.Row.of(
                Tags.of(
                    ÅR_MÅNED_TAG, "${YearMonth.now().year}-${YearMonth.now().month}-max"
                ),
                max
            ),
            MultiGauge.Row.of(
                Tags.of(
                    ÅR_MÅNED_TAG, "${YearMonth.now().year}-${YearMonth.now().month}-min"
                ),
                min
            )
        )

        tidSidenOpprettelseåpneBehandlingerPerMånedGauge.register(rows)
    }

    @Scheduled(cron = "0 0 14 * * *")
    fun loggÅpneBehandlingerSomHarLiggetLenge() {
        listOf(180, 150, 120, 90, 60).fold(mutableSetOf<Long>()) { acc, dagerSiden ->
            val åpneBehandlinger = behandlingRepository.finnÅpneBehandlinger(
                opprettetFør = LocalDateTime.now().minusDays(dagerSiden.toLong())
            ).filter { !acc.contains(it.id) }

            if (åpneBehandlinger.isNotEmpty()) {
                logger.warn(
                    "${åpneBehandlinger.size} åpne behandlinger har ligget i over $dagerSiden dager: \n" +
                        "${åpneBehandlinger.map { behandling -> "$behandling\n" }}"
                )
                acc.addAll(åpneBehandlinger.map { it.id })
            }

            acc
        }
    }

    private fun erLeaderOgLoggResultat(beskrivelse: String, resultat: String): Boolean {
        return if (LeaderClient.isLeader() != true) {
            logger.info("Node er ikke leader, teller ikke metrikk. $beskrivelse: $resultat")
            false
        } else {
            logger.info("Node er leader, teller metrikk. $beskrivelse: $resultat")
            true
        }
    }

    companion object {
        const val OPPDATERINGSFREKVENS = 30 * 60 * 1000L
        const val OPPDATERINGSFREKVENS_MINUTTER = 30
        const val INITIAL_DELAY = 120000L
        const val ÅR_MÅNED_TAG = "aar-maaned"
        val logger = LoggerFactory.getLogger(TeamStatistikkService::class.java)
    }
}
