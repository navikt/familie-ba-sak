package no.nav.familie.ba.sak.kjerne.eøs.tidslinjer

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.DagTidspunkt.Companion.dagForUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.DagTidspunkt.Companion.dagMedUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.DagTidspunkt.Companion.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.DagTidspunkt.Companion.tilTidspunktEllerUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.minsteEllerNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.størsteEllerNull
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat

data class VilkårRegelverkResultat(
    val vilkår: Vilkår,
    val regelverk: Regelverk?,
    val resultat: Resultat?,
)

class VilkårsresultatDagTidslinje(
    private val vilkårsresultater: List<VilkårResultat>
) : Tidslinje<VilkårRegelverkResultat, Dag>() {

    override fun fraOgMed() = vilkårsresultater.map {
        it.periodeFom.tilTidspunktEllerUendeligLengeSiden { it.periodeTom }
    }.minsteEllerNull() ?: dagForUendeligLengeSiden()

    override fun tilOgMed() = vilkårsresultater.map {
        it.periodeTom.tilTidspunktEllerUendeligLengeTil { it.periodeFom }
    }.størsteEllerNull() ?: dagMedUendeligLengeTil()

    override fun lagPerioder(): Collection<Periode<VilkårRegelverkResultat, Dag>> {
        return vilkårsresultater.map { it.tilPeriode() }
    }
}

fun VilkårResultat.tilPeriode(): Periode<VilkårRegelverkResultat, Dag> {
    val fom = periodeFom.tilTidspunktEllerUendeligLengeSiden { periodeTom }
    val tom = periodeTom.tilTidspunktEllerUendeligLengeTil { periodeFom }
    return Periode(
        fom, tom,
        VilkårRegelverkResultat(
            vilkår = vilkårType,
            regelverk = vurderesEtter,
            resultat = resultat,
        )
    )
}
