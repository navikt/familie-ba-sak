package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.beregnUtbetalingsperioder
import no.nav.familie.ba.sak.behandling.domene.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.domene.vedtak.VedtakPerson
import no.nav.familie.ba.sak.behandling.domene.vedtak.VedtakResultat.OPPHØRT
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag.KodeEndring.*
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode.SatsType.MND
import no.nav.fpsak.tidsserie.LocalDateSegment
import java.math.BigDecimal

// Må forsikre oss om at tidslinjesegmentene er i samme rekkefølge for å få konsekvent periodeId
// Sorter etter fraDato, sats, og evt til dato
// Denne måten å sette periodeId på krever at vedtak.id inkrementeres i store nok steg, f.eks 50 og 50
// Og at måten segmentene bygges opp på ikke endrer seg, dvs det kommer ALLTID i samme rekkefølge
// Beholder bare siste utbetalingsperiode hvis det er opphør.

fun lagUtbetalingsoppdrag(saksbehandlerId: String,
                          vedtak: Vedtak,
                          personberegninger: List<VedtakPerson>): Utbetalingsoppdrag {

    val erOpphør = vedtak.resultat == OPPHØRT

    val utbetalingsperiodeMal =
            when (erOpphør) {
                true -> UtbetalingsperiodeMal(vedtak, true, vedtak.forrigeVedtakId!!)
                false -> UtbetalingsperiodeMal(vedtak)
            }

    val tidslinjeMap = beregnUtbetalingsperioder(personberegninger)

    val utbetalingsperioder = tidslinjeMap.flatMap { (klassifisering, tidslinje) ->
        tidslinje.toSegments()
                .sortedWith(compareBy<LocalDateSegment<Int>>({ it.fom }, { it.value }, { it.tom }))
                .mapIndexed { indeks, segment ->
                    utbetalingsperiodeMal.lagPeriode(klassifisering, segment, indeks)
                }.kunSisteHvis(erOpphør)
    }

    return Utbetalingsoppdrag(
            saksbehandlerId = saksbehandlerId,
            kodeEndring = if (!erOpphør) NY else UEND,
            fagSystem = FAGSYSTEM,
            saksnummer = vedtak.behandling.fagsak.id.toString(),
            aktoer = vedtak.behandling.fagsak.personIdent.ident,
            utbetalingsperiode = utbetalingsperioder
    )
}

fun <T> List<T>.kunSisteHvis(kunSiste: Boolean): List<T> {
    return this.foldRight(mutableListOf()) { element, resultat ->
        if (resultat.size == 0 || !kunSiste) resultat.add(0, element);resultat
    }
}

data class UtbetalingsperiodeMal(
        val vedtak: Vedtak,
        val erEndringPåEksisterendePeriode: Boolean = false,
        val periodeIdStart: Long = vedtak.id!!
) {

    fun lagPeriode(klassifisering: String, segment: LocalDateSegment<Int>, periodeIdOffset: Int): Utbetalingsperiode =
            Utbetalingsperiode(
                    erEndringPåEksisterendePeriode,
                    vedtak.opphørsdato?.let { Opphør(it) },
                    periodeIdStart + periodeIdOffset,
                    if (periodeIdOffset > 0) periodeIdStart + (periodeIdOffset - 1).toLong() else null,
                    vedtak.vedtaksdato,
                    klassifisering,
                    segment.fom,
                    segment.tom,
                    BigDecimal(segment.value),
                    MND,
                    vedtak.behandling.fagsak.personIdent.ident,
                    vedtak.behandling.id
            )

}
