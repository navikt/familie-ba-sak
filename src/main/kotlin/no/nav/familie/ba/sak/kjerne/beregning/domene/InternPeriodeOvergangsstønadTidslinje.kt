package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull

fun Collection<InternPeriodeOvergangsstønad>.tilTidslinje(): Tidslinje<InternPeriodeOvergangsstønad> =
    this
        .map {
            Periode(
                verdi = it,
                fom = it.fomDato,
                tom = it.tomDato,
            )
        }.tilTidslinje()

fun Tidslinje<InternPeriodeOvergangsstønad>.lagInternePerioderOvergangsstønad(): List<InternPeriodeOvergangsstønad> =
    this.tilPerioderIkkeNull().map {
        it.verdi.copy(
            fomDato = it.fom?.førsteDagIInneværendeMåned() ?: throw Feil("Fra og med-dato kan ikke være null"),
            tomDato = it.tom?.sisteDagIMåned() ?: throw Feil("Til og med-dato kan ikke være null"),
        )
    }
