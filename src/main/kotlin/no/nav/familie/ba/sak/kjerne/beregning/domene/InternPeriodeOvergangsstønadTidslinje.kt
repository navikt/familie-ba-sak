package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.time.LocalDate
import no.nav.familie.tidslinje.Periode as FamilieFellesPeriode
import no.nav.familie.tidslinje.Tidslinje as FamilieFellesTidslinje

fun Collection<InternPeriodeOvergangsstønad>.tilTidslinje(): FamilieFellesTidslinje<InternPeriodeOvergangsstønad> =
    this
        .map {
            FamilieFellesPeriode(
                verdi = it,
                fom = it.fomDato,
                tom = it.tomDato,
            )
        }.tilTidslinje()

fun FamilieFellesTidslinje<InternPeriodeOvergangsstønad>.lagInternePerioderOvergangsstønad(): List<InternPeriodeOvergangsstønad> =
    this.tilPerioderIkkeNull().map {
        it.verdi.copy(
            fomDato = it.fom?.førsteDagIInneværendeMåned() ?: LocalDate.MIN,
            tomDato = it.tom?.sisteDagIMåned() ?: LocalDate.MAX,
        )
    }
