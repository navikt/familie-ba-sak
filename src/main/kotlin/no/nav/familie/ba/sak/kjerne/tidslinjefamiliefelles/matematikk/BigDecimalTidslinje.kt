package no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.matematikk

import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.komposisjon.join
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.komposisjon.kombinerUtenNullOgIkkeTom
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.transformasjon.mapIkkeNull
import no.nav.familie.tidslinje.Tidslinje
import java.math.BigDecimal
import java.math.RoundingMode

fun <K> Map<K, Tidslinje<BigDecimal>>.minus(
    bTidslinjer: Map<K, Tidslinje<BigDecimal>>,
) = this.join(bTidslinjer) { a, b ->
    when {
        a != null && b != null -> a - b
        else -> a
    }
}

fun Map<Aktør, Tidslinje<BigDecimal>>.sum() = values.kombinerUtenNullOgIkkeTom { it.reduce { sum, verdi -> sum.plus(verdi) } }

fun Tidslinje<BigDecimal>.rundAvTilHeltall() = this.mapIkkeNull { it.setScale(0, RoundingMode.HALF_UP) }
