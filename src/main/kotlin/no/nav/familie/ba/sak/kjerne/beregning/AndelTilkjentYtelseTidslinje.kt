package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.AndelForBrevperiode
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

class AndelTilkjentYtelseForBrevperioderTidslinje(
    private val andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
) : Tidslinje<AndelForBrevperiode, Måned>() {
    override fun lagPerioder(): List<Periode<AndelForBrevperiode, Måned>> =
        andelerTilkjentYtelse.map {
            Periode(
                fraOgMed = it.stønadFom.tilTidspunkt(),
                tilOgMed = it.stønadTom.tilTidspunkt(),
                innhold = AndelForBrevperiode(it),
            )
        }
}
