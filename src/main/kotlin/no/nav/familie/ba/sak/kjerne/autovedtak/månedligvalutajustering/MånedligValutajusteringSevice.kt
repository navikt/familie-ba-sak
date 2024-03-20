package no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.UtfyltValutakurs
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.tilIValutakurs
import no.nav.familie.valutakurs.exception.IngenValutakursException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Service
class MånedligValutajusteringSevice(
    private val ecbService: ECBService,
    private val valutakursService: ValutakursService,
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    fun oppdaterValutakurserForMåned(
        behandlingId: BehandlingId,
        valutajusteringMåned: YearMonth,
    ) {
        logger.info("Oppdaterer valutakurser for behandlingId=${behandlingId.id} og valutajusteringMåned=$valutajusteringMåned")

        val valutakurser = valutakursService.hentValutakurser(BehandlingId(behandlingId.id))
        val valutakurserSomMåOppdateres =
            valutakurser
                .map { it.tilIValutakurs() }.filterIsInstance<UtfyltValutakurs>()
                .filter { valutakurs -> valutakurs.periodeInneholder(valutajusteringMåned) }

        val sisteDagForrigeMåned = valutajusteringMåned.minusMonths(1).atEndOfMonth()

        val nyeValutaKurser =
            valutakurserSomMåOppdateres.map { valutakurs ->
                val (nyKurs, kursDato) =
                    hentKursPåDagEllerTidligereVedRødDag(
                        valutakode = valutakurs.valutakode,
                        dag = sisteDagForrigeMåned,
                    )

                Valutakurs(
                    fom = valutajusteringMåned,
                    tom = valutakurs.tom,
                    barnAktører = valutakurs.barnAktører,
                    valutakursdato = kursDato,
                    valutakode = valutakurs.valutakode,
                    kurs = nyKurs,
                )
            }

        nyeValutaKurser.forEach { valutakursService.oppdaterValutakurs(BehandlingId(behandlingId.id), it) }
    }

    data class KursOgKursdato(
        val kurs: BigDecimal,
        val kursdato: LocalDate,
    )

    fun hentKursPåDagEllerTidligereVedRødDag(
        valutakode: String,
        dag: LocalDate,
        forsøk: Int = 0,
    ): KursOgKursdato {
        if (forsøk == 10) {
            throw Feil("Klarte ikke å hente valutakurs for $valutakode etter å ha prøvd 10 forskjellige dager")
        }

        return try {
            KursOgKursdato(ecbService.hentValutakurs(valutakode, dag), dag)
        } catch (e: IngenValutakursException) {
            val dagenFør = dag.minusDays(1)
            hentKursPåDagEllerTidligereVedRødDag(
                valutakode = valutakode,
                dag = dagenFør,
                forsøk = forsøk + 1,
            )
        }
    }
}

private fun UtfyltValutakurs.periodeInneholder(
    valutajusteringMåned: YearMonth,
) = fom.isSameOrBefore(valutajusteringMåned) &&
    (tom ?: TIDENES_ENDE.toYearMonth()).isSameOrAfter(valutajusteringMåned)
