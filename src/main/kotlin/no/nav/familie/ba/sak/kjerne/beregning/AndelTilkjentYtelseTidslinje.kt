package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.AndelForVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.AndelForVedtaksperiode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.Periode as FamilieFellesPeriode

fun List<AndelTilkjentYtelse>.tilTidslinje() = this.map { it.tilPeriode() }.tilTidslinje()

class AndelTilkjentYtelseForVedtaksperioderTidslinje(
    private val andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
) : Tidslinje<AndelForVedtaksperiode, Måned>() {
    override fun lagPerioder(): List<Periode<AndelForVedtaksperiode, Måned>> =
        andelerTilkjentYtelse.map {
            Periode(
                fraOgMed = it.stønadFom.tilTidspunkt(),
                tilOgMed = it.stønadTom.tilTidspunkt(),
                innhold = AndelForVedtaksperiode(it),
            )
        }
}

fun List<AndelTilkjentYtelse>.tilAndelForVedtaksperiodeTidslinje() =
    this
        .map {
            FamilieFellesPeriode(
                verdi = AndelForVedtaksperiode(it),
                fom = it.stønadFom.førsteDagIInneværendeMåned(),
                tom = it.stønadTom.sisteDagIInneværendeMåned(),
            )
        }.tilTidslinje()

class AndelTilkjentYtelseForVedtaksbegrunnelserTidslinje(
    private val andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
) : Tidslinje<AndelForVedtaksbegrunnelse, Måned>() {
    override fun lagPerioder(): List<Periode<AndelForVedtaksbegrunnelse, Måned>> =
        andelerTilkjentYtelse.map {
            Periode(
                fraOgMed = it.stønadFom.tilTidspunkt(),
                tilOgMed = it.stønadTom.tilTidspunkt(),
                innhold = AndelForVedtaksbegrunnelse(it),
            )
        }
}

fun List<AndelTilkjentYtelse>.tilAndelForVedtaksbegrunnelseTidslinje() =
    this
        .map {
            FamilieFellesPeriode(
                verdi = AndelForVedtaksbegrunnelse(it),
                fom = it.stønadFom.førsteDagIInneværendeMåned(),
                tom = it.stønadTom.sisteDagIInneværendeMåned(),
            )
        }.tilTidslinje()
