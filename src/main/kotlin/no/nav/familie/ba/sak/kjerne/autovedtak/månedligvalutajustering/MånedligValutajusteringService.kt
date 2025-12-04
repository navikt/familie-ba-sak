package no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.UtfyltValutakurs
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Vurderingsform
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.tilIValutakurs
import no.nav.familie.util.VirkedagerProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class MånedligValutajusteringService(
    private val ecbService: ECBService,
    private val valutakursService: ValutakursService,
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun oppdaterValutakurserFraOgMedMåned(
        behandlingId: BehandlingId,
        valutajusteringMåned: YearMonth,
    ) {
        logger.info("Oppdaterer valutakurser fra og med $valutajusteringMåned for behandlingId=${behandlingId.id}")

        val valutakurser = valutakursService.hentValutakurser(BehandlingId(behandlingId.id))
        val valutakurserSomMåOppdateres =
            valutakurser
                .map { it.tilIValutakurs() }
                .filterIsInstance<UtfyltValutakurs>()
                .filter { valutakurs -> valutakurs.tom == null || valutakurs.tom.isSameOrAfter(valutajusteringMåned) }

        val sisteVirkedagForrigeMåned = valutajusteringMåned.minusMonths(1).tilSisteVirkedag()

        val nyeValutaKurser =
            valutakurserSomMåOppdateres.map { valutakurs ->
                val nyKurs = ecbService.hentValutakurs(valutakurs.valutakode, sisteVirkedagForrigeMåned)

                Valutakurs(
                    fom = maxOf(valutajusteringMåned, valutakurs.fom),
                    tom = valutakurs.tom,
                    barnAktører = valutakurs.barnAktører,
                    valutakursdato = sisteVirkedagForrigeMåned,
                    valutakode = valutakurs.valutakode,
                    kurs = nyKurs,
                    vurderingsform = Vurderingsform.AUTOMATISK,
                )
            }

        nyeValutaKurser.forEach { valutakursService.oppdaterValutakurs(BehandlingId(behandlingId.id), it) }
    }
}

fun YearMonth.tilSisteVirkedag() = VirkedagerProvider.senesteVirkedagFørEllerMed(this.atEndOfMonth())

private fun UtfyltValutakurs.periodeInneholder(
    valutajusteringMåned: YearMonth,
) = fom.isSameOrBefore(valutajusteringMåned) &&
    (tom ?: TIDENES_ENDE.toYearMonth()).isSameOrAfter(valutajusteringMåned)
