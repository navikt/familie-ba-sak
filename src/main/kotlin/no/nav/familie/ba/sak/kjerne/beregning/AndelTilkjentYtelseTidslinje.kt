package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.AndelForVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.AndelForVedtaksperiode

class AndelTilkjentYtelseTidslinje(
    private val andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
) : Tidslinje<AndelTilkjentYtelse, Måned>() {
    override fun lagPerioder(): List<Periode<AndelTilkjentYtelse, Måned>> =
        andelerTilkjentYtelse.map {
            Periode(
                fraOgMed = it.stønadFom.tilTidspunkt(),
                tilOgMed = it.stønadTom.tilTidspunkt(),
                innhold = it,
            )
        }
}

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
