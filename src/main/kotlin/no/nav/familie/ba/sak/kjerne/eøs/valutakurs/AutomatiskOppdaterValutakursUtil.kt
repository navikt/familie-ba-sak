package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.outerJoin
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.mapIkkeNull
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.ValutakursForVedtaksperiode
import java.time.YearMonth

fun finnFørsteEndringIValutakurs(
    valutakurserDenneBehandling: Collection<Valutakurs>,
    valutakurserForrigeBehandling: Collection<Valutakurs>,
): YearMonth {
    val valutakurserDenneBehandlingTidslinje =
        valutakurserDenneBehandling
            .filtrerUtfylteValutakurser()
            .groupBy { it.barnAktører }
            .mapValues { (_, valutakurser) -> valutakurser.tilTidslinje().mapIkkeNull { ValutakursForVedtaksperiode(it) } }
    val valutakurserForrigeBehandlingTidslinje =
        valutakurserForrigeBehandling
            .filtrerUtfylteValutakurser()
            .groupBy { it.barnAktører }
            .mapValues { (_, valutakurser) -> valutakurser.tilTidslinje().mapIkkeNull { ValutakursForVedtaksperiode(it) } }

    val erEndringIValutakursTidslinje =
        valutakurserDenneBehandlingTidslinje
            .outerJoin(valutakurserForrigeBehandlingTidslinje) { valutakursDenneBehandling, valutakursForrigeBehandling ->
                valutakursDenneBehandling != valutakursForrigeBehandling
            }.values
            .kombiner { erValutakursForPersonLikIPeriode ->
                erValutakursForPersonLikIPeriode.any { it }
            }

    return erEndringIValutakursTidslinje
        .perioder()
        .firstOrNull { it.innhold == true }
        ?.fraOgMed
        ?.tilYearMonth() ?: TIDENES_ENDE.toYearMonth()
}
