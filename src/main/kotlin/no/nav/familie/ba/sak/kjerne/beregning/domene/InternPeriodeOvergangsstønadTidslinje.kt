package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.DagTidspunkt.Companion.tilTidspunktEllerSenereEnn
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.DagTidspunkt.Companion.tilTidspunktEllerTidligereEnn
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilFørsteDagIMåneden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.tilSisteDagIMåneden

open class InternPeriodeOvergangsstønadTidslinje(
    private val internePeriodeOvergangsstønader: List<InternPeriodeOvergangsstønad>
) : Tidslinje<InternPeriodeOvergangsstønad, Dag>() {

    override fun lagPerioder(): List<Periode<InternPeriodeOvergangsstønad, Dag>> {
        return internePeriodeOvergangsstønader.map {
            Periode(
                fraOgMed = it.fomDato.tilTidspunktEllerTidligereEnn(it.tomDato),
                tilOgMed = it.tomDato.tilTidspunktEllerSenereEnn(it.fomDato),
                innhold = it
            )
        }
    }
}

fun Tidslinje<InternPeriodeOvergangsstønad, Dag>.lagInternePerioderOvergangsstønad(): List<InternPeriodeOvergangsstønad> =
    this.perioder().mapNotNull {
        it.innhold?.copy(
            fomDato = it.fraOgMed.tilFørsteDagIMåneden().tilLocalDate(),
            tomDato = it.tilOgMed.tilSisteDagIMåneden().tilLocalDate()
        )
    }
