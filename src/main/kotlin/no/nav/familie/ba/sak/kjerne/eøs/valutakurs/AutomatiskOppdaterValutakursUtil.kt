package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.transformasjon.mapIkkeNull
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.ValutakursForVedtaksperiode
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.outerJoin
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import java.time.YearMonth

fun finnFørsteEndringIValutakurs(
    valutakurserDenneBehandling: Collection<Valutakurs>,
    valutakurserForrigeBehandling: Collection<Valutakurs>,
): YearMonth {
    val valutakurserDenneBehandlingTidslinje =
        valutakurserDenneBehandling
            .filtrerUtfylteValutakurser()
            .groupBy { it.barnAktører }
            .mapValues { (_, valutakurser) -> valutakurser.tilFamilieFellesTidslinje().mapIkkeNull { ValutakursForVedtaksperiode(it) } }
    val valutakurserForrigeBehandlingTidslinje =
        valutakurserForrigeBehandling
            .filtrerUtfylteValutakurser()
            .groupBy { it.barnAktører }
            .mapValues { (_, valutakurser) -> valutakurser.tilFamilieFellesTidslinje().mapIkkeNull { ValutakursForVedtaksperiode(it) } }

    val erEndringIValutakursTidslinje =
        valutakurserDenneBehandlingTidslinje
            .outerJoin(valutakurserForrigeBehandlingTidslinje) { valutakursDenneBehandling, valutakursForrigeBehandling ->
                valutakursDenneBehandling != valutakursForrigeBehandling
            }.values
            .kombiner { erValutakursForPersonEndretIPeriode ->
                erValutakursForPersonEndretIPeriode.any { it }
            }

    return erEndringIValutakursTidslinje
        .tilPerioder()
        .firstOrNull { it.verdi == true }
        ?.fom
        ?.toYearMonth() ?: TIDENES_ENDE.toYearMonth()
}
