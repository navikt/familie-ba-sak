package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.beregnUtbetalingsperioder
import no.nav.familie.ba.sak.behandling.domene.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.domene.vedtak.VedtakPerson
import no.nav.familie.ba.sak.behandling.domene.vedtak.VedtakResultat.OPPHØRT
import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag.KodeEndring.NY
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag.KodeEndring.UENDR
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode.SatsType.MND
import no.nav.fpsak.tidsserie.LocalDateSegment
import java.math.BigDecimal

fun lagUtbetalingsoppdrag(saksbehandlerId: String,
                          vedtak: Vedtak,
                          personberegninger: List<VedtakPerson>): Utbetalingsoppdrag {

    val erOpphør = vedtak.resultat == OPPHØRT
    val aktør = vedtak.behandling.fagsak.personIdent.ident
    val saksnummer = vedtak.behandling.fagsak.id.toString()

    val tidslinjeMap = beregnUtbetalingsperioder(personberegninger)
    val utbetalingsperioder = tidslinjeMap.flatMap { (klassifisering, tidslinje) ->
        tidslinje.toSegments()
                // Må forsikre oss om at tidslinjesegmentene er i samme rekkefølge for å få konsekvent periodeId
                // . Sorter etter fraDato, sats, og evt til dato
                .sortedWith(compareBy<LocalDateSegment<Int>>({ it.fom }, { it.value }, { it.tom }))
                .mapIndexed { indeks, segment ->
                    Utbetalingsperiode(
                            erEndringPåEksisterendePeriode = erOpphør,
                            opphør = vedtak.opphørsdato?.let { Opphør(it) },
                            datoForVedtak = vedtak.vedtaksdato,
                            klassifisering = klassifisering,
                            vedtakdatoFom = segment.fom,
                            vedtakdatoTom = segment.tom,
                            sats = BigDecimal(segment.value),
                            satsType = MND,
                            utbetalesTil = aktør,
                            behandlingId = vedtak.behandling.id!!,
                            // Denne måten å sette periodeId på krever at vedtak.id inkrementeres i store nok steg, f.eks 50 og 50
                            // Og at måten segmentene bygges opp på ikke endrer seg, dvs det kommer ALLTID i samme rekkefølge
                            periodeId = (if (!erOpphør) vedtak.id else vedtak.forrigeVedtakId)!! + indeks.toLong()
                    )
                }
    }

    val utbetalingsoppdrag = Utbetalingsoppdrag(
            saksbehandlerId = saksbehandlerId,
            kodeEndring = if (!erOpphør) NY else UENDR,
            fagSystem = FAGSYSTEM,
            saksnummer = saksnummer,
            aktoer = aktør,
            utbetalingsperiode = utbetalingsperioder
    )
    return utbetalingsoppdrag
}
