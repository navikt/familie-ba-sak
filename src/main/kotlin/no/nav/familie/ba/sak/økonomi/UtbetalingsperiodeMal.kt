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
        val erEndringPåEksisterendePeriode: Boolean = false
) {

    // TODO: Dobbeltsjekk at vi ikke trenger 1_000-sjekk
    fun lagPeriodeFraAndel(andel: AndelTilkjentYtelse,
                           periodeIdOffset: Int,
                           forrigePeriodeIdOffset: Int?,
                           opphørIKjede: LocalDate? = null): Utbetalingsperiode =
            Utbetalingsperiode(
                    erEndringPåEksisterendePeriode = erEndringPåEksisterendePeriode, // True gjør at oppdrag setter endringskode ENDR på linje og ikke vil referere bakover
                    opphør = if (erEndringPåEksisterendePeriode) utledOpphørPåLinje(opphørForVedtak = vedtak.opphørsdato,
                                                                                    opphørForLinje = opphørIKjede!!) else null,
                    forrigePeriodeId = forrigePeriodeIdOffset?.let { forrigePeriodeIdOffset.toLong() }, //TODO: Husk å skrive migreringsscript for gamle periodeIder / spesialhåndtere
                    // TODO: forrigePeriodeId kun relevant hvis IKKE erEndringPåEksisterendePeriode? Altså for nye perioder som skal hektes på
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
