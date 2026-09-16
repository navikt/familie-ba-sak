package no.nav.familie.ba.sak.kjerne.autovedtak.svalbardstillegg

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.tilAndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.tilTidslinje
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import java.time.YearMonth

fun finnInnvilgedeOgReduserteSvalbardtilleggPerioder(
    forrigeAndeler: List<AndelTilkjentYtelse>,
    nåværendeAndeler: List<AndelTilkjentYtelse>,
): Pair<Set<YearMonth>, Set<YearMonth>> {
    val forrigeSvalbardtilleggAndeler = forrigeAndeler.filter { it.erSvalbardtillegg() }
    val nåværendeSvalbardtilleggAndeler = nåværendeAndeler.filter { it.erSvalbardtillegg() }

    val relevanteBarn =
        (forrigeSvalbardtilleggAndeler + nåværendeSvalbardtilleggAndeler)
            .map { it.aktør }
            .toSet()

    val innvilgedeOgReduserteSvalbardtilleggPerioder =
        relevanteBarn.fold<Aktør, Pair<Set<YearMonth>, Set<YearMonth>>>(emptySet<YearMonth>() to emptySet()) { (nyePerioder, reduksjonsPerioder), barn ->
            val forrigeSvalbardtilleggAndelerTidslinje = forrigeSvalbardtilleggAndeler.filter { it.aktør == barn }.tilTidslinje()
            val nåværendeSvalbardtilleggAndelerTidslinje = nåværendeSvalbardtilleggAndeler.filter { it.aktør == barn }.tilTidslinje()

            val nyeAndeler = forrigeSvalbardtilleggAndelerTidslinje.kombinerMed(nåværendeSvalbardtilleggAndelerTidslinje) { gammel, ny -> ny.takeIf { gammel == null } }
            val fjernetAndeler = forrigeSvalbardtilleggAndelerTidslinje.kombinerMed(nåværendeSvalbardtilleggAndelerTidslinje) { gammel, ny -> gammel.takeIf { ny == null } }

            (nyePerioder + nyeAndeler.tilAndelTilkjentYtelse().map { it.stønadFom } to reduksjonsPerioder + fjernetAndeler.tilAndelTilkjentYtelse().map { it.stønadFom })
        }

    return innvilgedeOgReduserteSvalbardtilleggPerioder
}
