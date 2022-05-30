package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.DagTidspunkt.Companion.tilTidspunktEllerSenereEnn
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.DagTidspunkt.Companion.tilTidspunktEllerTidligereEnn
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat

fun Iterable<VilkårResultat>.tilVilkårRegelverkResultatTidslinje(): Tidslinje<VilkårRegelverkResultat, Dag> {
    val vilkårResultater = this
    return object : Tidslinje<VilkårRegelverkResultat, Dag>() {
        override fun lagPerioder() = vilkårResultater.map { it.tilPeriode() }
    }
}

fun VilkårResultat.tilPeriode(): Periode<VilkårRegelverkResultat, Dag> {
    val fom = periodeFom.tilTidspunktEllerTidligereEnn(periodeTom)
    val tom = periodeTom.tilTidspunktEllerSenereEnn(periodeFom)
    return Periode(
        fom, tom,
        VilkårRegelverkResultat(
            vilkår = vilkårType,
            regelverkResultat = this.tilRegelverkResultat()
        )
    )
}
