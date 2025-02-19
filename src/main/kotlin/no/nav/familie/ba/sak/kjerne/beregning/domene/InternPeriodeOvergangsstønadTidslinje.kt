package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.DagTidspunkt.Companion.tilTidspunktEllerUendeligSent
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.DagTidspunkt.Companion.tilTidspunktEllerUendeligTidlig
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilFørsteDagIMåneden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilSisteDagIMåneden
import no.nav.familie.tidslinje.tilTidslinje
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

fun Tidslinje<InternPeriodeOvergangsstønad, Dag>.lagInternePerioderOvergangsstønad(): List<InternPeriodeOvergangsstønad> =
    this.perioder().mapNotNull {
        it.innhold?.copy(
            fomDato = it.fraOgMed.tilFørsteDagIMåneden().tilLocalDate(),
            tomDato = it.tilOgMed.tilSisteDagIMåneden().tilLocalDate(),
        )
    }
