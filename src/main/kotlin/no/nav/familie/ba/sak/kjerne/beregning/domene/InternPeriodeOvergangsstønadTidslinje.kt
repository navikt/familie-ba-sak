package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.DagTidspunkt.Companion.tilTidspunktEllerUendeligSent
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.DagTidspunkt.Companion.tilTidspunktEllerUendeligTidlig
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.time.LocalDate
import no.nav.familie.tidslinje.Periode as FamilieFellesPeriode
import no.nav.familie.tidslinje.Tidslinje as FamilieFellesTidslinje

open class InternPeriodeOvergangsstønadTidslinje(
    private val internePeriodeOvergangsstønader: List<InternPeriodeOvergangsstønad>,
) : Tidslinje<InternPeriodeOvergangsstønad, Dag>() {
    override fun lagPerioder(): List<Periode<InternPeriodeOvergangsstønad, Dag>> =
        internePeriodeOvergangsstønader.map {
            Periode(
                fraOgMed = it.fomDato.tilTidspunktEllerUendeligTidlig(it.tomDato),
                tilOgMed = it.tomDato.tilTidspunktEllerUendeligSent(it.fomDato),
                innhold = it,
            )
        }
}

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
