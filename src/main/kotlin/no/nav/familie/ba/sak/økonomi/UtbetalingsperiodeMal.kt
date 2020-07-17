package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.common.senesteDatoAv
import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Lager mal for generering av utbetalingsperioder med tilpasset setting av verdier basert på parametre
 *
 * @param[vedtak] for vedtakdato og opphørsdato hvis satt
 * @param[erEndringPåEksisterendePeriode] ved true vil oppdrag sette asksjonskode ENDR på linje og ikke referere bakover
 * @return mal med tilpasset lagPeriodeFraAndel
 */
data class UtbetalingsperiodeMal(
        val vedtak: Vedtak,
        val erEndringPåEksisterendePeriode: Boolean = false
) {

    // TODO: Dobbeltsjekk at vi ikke trenger 1_000-sjekk
    /**
     * Lager utbetalingsperioder som legges på utbetalingsoppdrag. En utbetalingsperiode tilsvarer linjer hos økonomi
     *
     * @param[andel] andel som skal mappes til periode
     * @param[periodeIdOffset] brukes til å synce våre linjer med det som ligger hos økonomi
     * @param[forrigePeriodeIdOffset] peker til forrige i kjeden. Kun relevant når IKKE erEndringPåEksisterendePeriode
     * @param[opphørKjedeFom] fom-dato fra tidligste periode i kjede med endring
     * @return Periode til utbetalingsoppdrag
     */
    fun lagPeriodeFraAndel(andel: AndelTilkjentYtelse,
                           periodeIdOffset: Int,
                           forrigePeriodeIdOffset: Int?,
                           opphørKjedeFom: LocalDate? = null): Utbetalingsperiode =
            Utbetalingsperiode(
                    erEndringPåEksisterendePeriode = erEndringPåEksisterendePeriode,
                    opphør = if (erEndringPåEksisterendePeriode) utledOpphørPåLinje(opphørForVedtak = vedtak.opphørsdato,
                                                                                    opphørForLinje = opphørKjedeFom!!) else null,
                    forrigePeriodeId = forrigePeriodeIdOffset?.let { forrigePeriodeIdOffset.toLong() },
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


    fun utledOpphørPåLinje(opphørForVedtak: LocalDate?, opphørForLinje: LocalDate): Opphør? {
        return if (opphørForVedtak != null) {
            Opphør(senesteDatoAv(opphørForVedtak, opphørForLinje))
        } else {
            Opphør(opphørForLinje)
        }
    }
}
