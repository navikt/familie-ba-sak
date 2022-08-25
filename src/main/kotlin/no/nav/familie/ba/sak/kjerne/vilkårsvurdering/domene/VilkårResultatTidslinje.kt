package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.DagTidspunkt.Companion.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.DagTidspunkt.Companion.tilTidspunktEllerUendeligLengeTil

class VilkårResultatTidslinje(
    private val vilkårResultater: Collection<VilkårResultat>,
) : Tidslinje<VilkårResultat, Dag>() {

    override fun lagPerioder(): List<Periode<VilkårResultat, Dag>> =
        vilkårResultater.map {
            Periode(
                fraOgMed = it.periodeFom.tilTidspunktEllerUendeligLengeSiden(),
                tilOgMed = it.periodeTom.tilTidspunktEllerUendeligLengeTil(),
                innhold = it
            )
        }
}
