package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.beregning.beregnUtbetalingsperioder
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag.KodeEndring.NY
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag.KodeEndring.UEND
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode.SatsType.MND
import no.nav.fpsak.tidsserie.LocalDateSegment
import java.math.BigDecimal

// Må forsikre oss om at tidslinjesegmentene er i samme rekkefølge for å få konsekvent periodeId
// Sorter etter fraDato, sats, og evt til dato
// PeriodeId = Vedtak.id * 1000 + offset
// Beholder bare siste utbetalingsperiode hvis det er opphør.
fun lagUtbetalingsoppdrag(saksbehandlerId: String,
                          vedtak: Vedtak,
                          behandlingResultatType: BehandlingResultatType,
                          andelerTilkjentYtelse: List<AndelTilkjentYtelse>): Utbetalingsoppdrag {

    val erOpphør = behandlingResultatType == BehandlingResultatType.OPPHØRT

    val utbetalingsperiodeMal =
            if (erOpphør)
                UtbetalingsperiodeMal(vedtak, true, vedtak.forrigeVedtakId!!)
            else
                UtbetalingsperiodeMal(vedtak)

    val utbetalingsperioder = andelerTilkjentYtelse
            .mapIndexed { index, andelTilkjentYtelse ->
                utbetalingsperiodeMal.lagPeriodeFraAndel(andelTilkjentYtelse, index)
            }

    return Utbetalingsoppdrag(
            saksbehandlerId = saksbehandlerId,
            kodeEndring = if (!erOpphør) NY else UEND,
            fagSystem = FAGSYSTEM,
            saksnummer = vedtak.behandling.fagsak.id.toString(),
            aktoer = vedtak.behandling.fagsak.hentAktivIdent().ident,
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
        val periodeIdStart: Long = vedtak.id
) {

    private val MAX_PERIODEID_OFFSET = 1_000

    fun lagPeriode(klassifisering: String, segment: LocalDateSegment<Int>, periodeIdOffset: Int): Utbetalingsperiode {

        // Vedtak-id øker med 50, så vi kan ikke risikere overflow
        if (periodeIdOffset >= MAX_PERIODEID_OFFSET) {
            throw IllegalArgumentException("periodeIdOffset forsøkt satt høyere enn ${MAX_PERIODEID_OFFSET}. " +
                                           "Det ville ført til duplisert periodeId")
        }

        // Skaper "plass" til offset
        val utvidetPeriodeIdStart = periodeIdStart * MAX_PERIODEID_OFFSET

        return Utbetalingsperiode(
                erEndringPåEksisterendePeriode,
                vedtak.opphørsdato?.let { Opphør(it) },
                utvidetPeriodeIdStart + periodeIdOffset,
                if (periodeIdOffset > 0) utvidetPeriodeIdStart + (periodeIdOffset - 1).toLong() else null,
                vedtak.vedtaksdato,
                klassifisering,
                segment.fom,
                segment.tom,
                BigDecimal(segment.value),
                MND,
                vedtak.behandling.fagsak.hentAktivIdent().ident,
                vedtak.behandling.id
        )
    }

    fun lagPeriodeFraAndel(andel: AndelTilkjentYtelse, periodeIdOffset: Int): Utbetalingsperiode {

        // Vedtak-id øker med 50, så vi kan ikke risikere overflow
        if (periodeIdOffset >= MAX_PERIODEID_OFFSET) {
            throw IllegalArgumentException("periodeIdOffset forsøkt satt høyere enn ${MAX_PERIODEID_OFFSET}. " +
                    "Det ville ført til duplisert periodeId")
        }

        // Skaper "plass" til offset
        val utvidetPeriodeIdStart = periodeIdStart * MAX_PERIODEID_OFFSET

        return Utbetalingsperiode(
                erEndringPåEksisterendePeriode = erEndringPåEksisterendePeriode,
                opphør = vedtak.opphørsdato?.let { Opphør(it) },
                periodeId = utvidetPeriodeIdStart + periodeIdOffset,
                datoForVedtak = vedtak.vedtaksdato,
                klassifisering = andel.type.klassifisering,
                vedtakdatoFom = andel.stønadFom,
                vedtakdatoTom = andel.stønadTom,
                sats = BigDecimal(andel.beløp),
                satsType = MND,
                utbetalesTil = vedtak.behandling.fagsak.hentAktivIdent().ident,
                behandlingId = vedtak.behandling.id
        )
    }
}
