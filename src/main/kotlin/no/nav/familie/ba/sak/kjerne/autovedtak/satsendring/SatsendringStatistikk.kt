package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.leader.LeaderClient
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.YearMonth

@Component
class SatsendringStatistikk(
    private val fagsakRepository: FagsakRepository,
    private val satskjøringRepository: SatskjøringRepository
) {

    val satsendringGauge =
        MultiGauge.builder("satsendring").register(Metrics.globalRegistry)

    @Scheduled(
        fixedRate = OPPDATERING_HVER_TIME
    )
    fun antallSatsendringerKjørt() {
        if (LeaderClient.isLeader() == true) {
            val antallKjørt = satskjøringRepository.countByFerdigTidspunktIsNotNull()
            val antallTriggetTotalt = satskjøringRepository.count()
            val antallFagsakerTotalt = fagsakRepository.finnAntallFagsakerTotalt()

            val rows = listOf(
                MultiGauge.Row.of(
                    Tags.of(
                        "totalt",
                        "${YearMonth.now().year}-${YearMonth.now().month}"
                    ),
                    antallTriggetTotalt
                ),
                MultiGauge.Row.of(
                    Tags.of(
                        "antall-kjort",
                        "${YearMonth.now().year}-${YearMonth.now().month}"
                    ),
                    antallKjørt
                ),
                MultiGauge.Row.of(
                    Tags.of(
                        "antall-fagsaker-totalt",
                        "${YearMonth.now().year}-${YearMonth.now().month}"
                    ),
                    antallFagsakerTotalt
                ),
                MultiGauge.Row.of(
                    Tags.of(
                        "antall-gjenstaaende",
                        "${YearMonth.now().year}-${YearMonth.now().month}"
                    ),
                    antallFagsakerTotalt - antallKjørt
                )
            )

            satsendringGauge.register(rows)
        }
    }

    companion object {
        const val OPPDATERING_HVER_TIME: Long = 1000 * 60
    }
}
