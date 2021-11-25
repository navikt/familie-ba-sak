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
import java.time.LocalDate
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

    @Scheduled(initialDelay = INITIAL_DELAY, fixedDelay = OPPDATERINGSFREKVENS)
    fun utbetalinger() {
        val månederMedTotalUtbetaling =
            listOf<LocalDate>(LocalDate.now(), LocalDate.now().plusMonths(1)).associateWith {
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
                    "aar-maaned", it.key.year.toString() + "-" + it.key.month.toString()
                ),
                it.value
            )
        }

        utbetalingerPerMånedGauge.register(rows)
    }

    @Scheduled(initialDelay = INITIAL_DELAY, fixedDelay = OPPDATERINGSFREKVENS)
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
                    "aar-maaned", YearMonth.now().year.toString() + "-" + YearMonth.now().month.toString()
                ),
                antallFagsaker
            )
        )

        antallFagsakerPerMånedGauge.register(rows)
    }

    @Scheduled(initialDelay = INITIAL_DELAY, fixedDelay = OPPDATERINGSFREKVENS)
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
                    "aar-maaned", YearMonth.now().year.toString() + "-" + YearMonth.now().month.toString()
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
                    "aar-maaned", YearMonth.now().year.toString() + "-" + YearMonth.now().month.toString()
                ),
                åpneBehandlinger
            )
        )

        åpneBehandlingerPerMånedGauge.register(rows)
    }

    @Scheduled(initialDelay = INITIAL_DELAY, fixedDelay = OPPDATERINGSFREKVENS)
    fun tidFraOpprettelsePåÅpneBehandlinger() {
        val opprettelsestidspunktPååpneBehandlinger = behandlingRepository.finnOpprettelsestidspunktPåÅpneBehandlinger()
        val diffPåÅpneBehandlinger =
            opprettelsestidspunktPååpneBehandlinger.map { Duration.between(it, LocalDate.now()).seconds }

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
                    "aar-maaned-snitt", YearMonth.now().year.toString() + "-" + YearMonth.now().month.toString()
                ),
                snitt
            ),
            MultiGauge.Row.of(
                Tags.of(
                    "aar-maaned-max", YearMonth.now().year.toString() + "-" + YearMonth.now().month.toString()
                ),
                max
            ),
            MultiGauge.Row.of(
                Tags.of(
                    "aar-maaned-min", YearMonth.now().year.toString() + "-" + YearMonth.now().month.toString()
                ),
                min
            )
        )

        tidSidenOpprettelseåpneBehandlingerPerMånedGauge.register(rows)
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
        const val INITIAL_DELAY = 120000L
        val logger = LoggerFactory.getLogger(TeamStatistikkService::class.java)
    }
}
