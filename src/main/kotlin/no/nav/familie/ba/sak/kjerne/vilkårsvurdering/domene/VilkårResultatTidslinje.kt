package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.DagTidspunkt.Companion.tilTidspunktEllerSenereEnn
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.DagTidspunkt.Companion.tilTidspunktEllerTidligereEnn

class VilkårResultatTidslinje(
    private val vilkårResultater: Collection<VilkårResultat>
) : Tidslinje<VilkårResultat, Dag>() {

    override fun lagPerioder(): List<Periode<VilkårResultat, Dag>> =
        vilkårResultater.map {
            Periode(
                fraOgMed = it.periodeFom.tilTidspunktEllerTidligereEnn(it.periodeTom),
                tilOgMed = it.periodeTom.tilTidspunktEllerSenereEnn(it.periodeFom),
                innhold = it
            )
        }
}

fun List<VilkårResultat>.tilTidslinje() = VilkårResultatTidslinje(this)
