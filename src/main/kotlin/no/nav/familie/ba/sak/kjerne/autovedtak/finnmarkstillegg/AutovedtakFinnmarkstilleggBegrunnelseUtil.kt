package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.tilAndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.tilTidslinje
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import java.time.YearMonth

fun finnInnvilgedeOgReduserteFinnmarkstilleggPerioder(
    forrigeAndeler: List<AndelTilkjentYtelse>,
    nåværendeAndeler: List<AndelTilkjentYtelse>,
): Pair<Set<YearMonth>, Set<YearMonth>> {
    val forrigeFinnmarkstilleggAndeler = forrigeAndeler.filter { it.erFinnmarkstillegg() }
    val nåværendeFinnmarkstilleggAndeler = nåværendeAndeler.filter { it.erFinnmarkstillegg() }

    val relevanteBarn =
        (forrigeFinnmarkstilleggAndeler + nåværendeFinnmarkstilleggAndeler)
            .map { it.aktør }
            .toSet()

    val innvilgedeOgReduserteFinnmarkstilleggPerioder =
        relevanteBarn.fold<Aktør, Pair<Set<YearMonth>, Set<YearMonth>>>(emptySet<YearMonth>() to emptySet()) { (nyePerioder, reduksjonsPerioder), barn ->
            val forrigeFinnmarkstilleggAndelerTidslinje = forrigeFinnmarkstilleggAndeler.filter { it.aktør == barn }.tilTidslinje()
            val nåværendeFinnmarkstilleggAndelerTidslinje = nåværendeFinnmarkstilleggAndeler.filter { it.aktør == barn }.tilTidslinje()

            val nyeAndeler = forrigeFinnmarkstilleggAndelerTidslinje.kombinerMed(nåværendeFinnmarkstilleggAndelerTidslinje) { gammel, ny -> ny.takeIf { gammel == null } }
            val fjernetAndeler = forrigeFinnmarkstilleggAndelerTidslinje.kombinerMed(nåværendeFinnmarkstilleggAndelerTidslinje) { gammel, ny -> gammel.takeIf { ny == null } }

            (nyePerioder + nyeAndeler.tilAndelTilkjentYtelse().map { it.stønadFom } to reduksjonsPerioder + fjernetAndeler.tilAndelTilkjentYtelse().map { it.stønadFom })
        }

    return innvilgedeOgReduserteFinnmarkstilleggPerioder
}
