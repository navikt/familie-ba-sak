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
        val erEndringPåEksisterendePeriode: Boolean = false
) {
    // TODO: Dobbeltsjekk at vi ikke trenger 1_000-sjekk
    fun lagPeriodeFraAndel(andel: AndelTilkjentYtelse,
                           periodeIdOffset: Int,
                           forrigePeriodeIdOffset: Int?): Utbetalingsperiode =
            Utbetalingsperiode(
                    erEndringPåEksisterendePeriode = erEndringPåEksisterendePeriode,
                    opphør = if (erEndringPåEksisterendePeriode) utledOpphørPåLinje(opphørForVedtak = vedtak.opphørsdato,
                                                                                    linje = andel) else null,
                    forrigePeriodeId = forrigePeriodeIdOffset?.let { forrigePeriodeIdOffset.toLong() }, //TODO: Husk å skrive migreringsscript for gamle periodeIder / spesialhåndtere
                    periodeId = periodeIdOffset.toLong(),
                    datoForVedtak = vedtak.vedtaksdato,
                    klassifisering = andel.type.klassifisering,
                    vedtakdatoFom = andel.stønadFom,
                    vedtakdatoTom = andel.stønadTom,
                    sats = BigDecimal(andel.beløp),
                    satsType = Utbetalingsperiode.SatsType.MND,
                    utbetalesTil = vedtak.behandling.fagsak.hentAktivIdent().ident,
                    behandlingId = vedtak.behandling.id
            )

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
