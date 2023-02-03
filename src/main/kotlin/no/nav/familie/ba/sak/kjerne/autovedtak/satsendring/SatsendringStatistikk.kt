package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.leader.LeaderClient
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SatsendringStatistikk(
    private val fagsakRepository: FagsakRepository,
    private val satskjøringRepository: SatskjøringRepository
) {

    val satsendringGauge =
        MultiGauge.builder("satsendring").register(Metrics.globalRegistry)

    @Scheduled(
        fixedRate = OPPDATERING_HVER_HALV_TIME
    )
    fun antallSatsendringerKjørt() {
        if (LeaderClient.isLeader() == true) {
            val antallKjørt = satskjøringRepository.countByFerdigTidspunktIsNotNull()
            val antallTriggetTotalt = satskjøringRepository.count()
            val antallLøpendeFagsakerTotalt = fagsakRepository.finnAntallFagsakerLøpende()

            val rows = listOf(
                MultiGauge.Row.of(
                    Tags.of(
                        "satsendring",
                        "totalt"
                    ),
                    antallTriggetTotalt
                ),
                MultiGauge.Row.of(
                    Tags.of(
                        "satsendring",
                        "antallkjort"
                    ),
                    antallKjørt
                ),
                MultiGauge.Row.of(
                    Tags.of(
                        "satsendring",
                        "antallfagsaker"
                    ),
                    antallLøpendeFagsakerTotalt
                ),
                MultiGauge.Row.of(
                    Tags.of(
                        "satsendring",
                        "antallgjenstaaende"
                    ),
                    antallLøpendeFagsakerTotalt - antallKjørt
                )
            )

            satsendringGauge.register(rows)
        }
    }

    companion object {
        const val OPPDATERING_HVER_HALV_TIME: Long = 1000 * 30
    }
}
