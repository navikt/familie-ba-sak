package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.AndelForVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.AndelForVedtaksperiode
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje

fun List<AndelTilkjentYtelse>.tilTidslinje() = this.map { it.tilPeriode() }.tilTidslinje()

fun List<AndelTilkjentYtelse>.tilAndelForVedtaksperiodeTidslinje() =
    this
        .map {
            Periode(
                verdi = AndelForVedtaksperiode(it),
                fom = it.stønadFom.førsteDagIInneværendeMåned(),
                tom = it.stønadTom.sisteDagIInneværendeMåned(),
            )
        }.tilTidslinje()

fun List<AndelTilkjentYtelse>.tilAndelForVedtaksbegrunnelseTidslinje() =
    this
        .map {
            Periode(
                verdi = AndelForVedtaksbegrunnelse(it),
                fom = it.stønadFom.førsteDagIInneværendeMåned(),
                tom = it.stønadTom.sisteDagIInneværendeMåned(),
            )
        }.tilTidslinje()
