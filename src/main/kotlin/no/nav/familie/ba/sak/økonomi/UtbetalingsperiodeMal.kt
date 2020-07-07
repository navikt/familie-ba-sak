package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.common.senesteDatoAv
import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import java.math.BigDecimal
import java.time.LocalDate

data class UtbetalingsperiodeMal(
        val vedtak: Vedtak,
        val andeler: List<AndelTilkjentYtelse>? = null,
        val erEndringPåEksisterendePeriode: Boolean = false,
        val periodeIdStart: Long = vedtak.id
) {

    private val MAX_PERIODEID_OFFSET = 1_000

    fun lagPeriodeFraAndel(andel: AndelTilkjentYtelse, periodeIdOffset: Int, forrigePeriodeIdOffset: Int?): Utbetalingsperiode {

        // Vedtak-id øker med 50, så vi kan ikke risikere overflow
        if (periodeIdOffset >= MAX_PERIODEID_OFFSET) {
            throw IllegalArgumentException("periodeIdOffset forsøkt satt høyere enn ${MAX_PERIODEID_OFFSET}. " +
                                           "Det ville ført til duplisert periodeId")
        }

        // Skaper "plass" til offset
        val utvidetPeriodeIdStart = periodeIdStart * MAX_PERIODEID_OFFSET

        return Utbetalingsperiode(
                erEndringPåEksisterendePeriode = erEndringPåEksisterendePeriode,
                opphør = if (erEndringPåEksisterendePeriode) utledOpphørPåLinje(opphørForVedtak = vedtak.opphørsdato,
                                                                                linje = andel) else null,
                forrigePeriodeId = forrigePeriodeIdOffset?.let { utvidetPeriodeIdStart + forrigePeriodeIdOffset.toLong() },
                periodeId = utvidetPeriodeIdStart + periodeIdOffset,
                datoForVedtak = vedtak.vedtaksdato,
                klassifisering = andel.type.klassifisering,
                vedtakdatoFom = andel.stønadFom,
                vedtakdatoTom = andel.stønadTom,
                sats = BigDecimal(andel.beløp),
                satsType = Utbetalingsperiode.SatsType.MND,
                utbetalesTil = vedtak.behandling.fagsak.hentAktivIdent().ident,
                behandlingId = vedtak.behandling.id
        )
    }

    fun tidligsteFomDatoIKjede(andel: AndelTilkjentYtelse): LocalDate {
        return andeler!!.filter { it.type == andel.type && it.personIdent == andel.personIdent }
                .minBy { it.stønadFom }!!.stønadFom
    }

    fun utledOpphørPåLinje(opphørForVedtak: LocalDate?, linje: AndelTilkjentYtelse): Opphør? {
        return if (opphørForVedtak != null) {
            Opphør(senesteDatoAv(opphørForVedtak, tidligsteFomDatoIKjede(linje)))
        } else {
            Opphør(linje.stønadFom)
        }
    }
}
