package no.nav.familie.ba.sak.kjerne.eøs.tidslinjer

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.DagTidspunkt.Companion.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.DagTidspunkt.Companion.tilTidspunktEllerUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat

enum class RegelverkResultat(val regelverk: Regelverk?, val resultat: Resultat?) {
    OPPFYLT_EØS_FORORDNINGEN(Regelverk.EØS_FORORDNINGEN, Resultat.OPPFYLT),
    OPPFYLT_NASJONALE_REGLER(Regelverk.NASJONALE_REGLER, Resultat.OPPFYLT),
    OPPFYLT_REGELVERK_IKKE_SATT(null, Resultat.OPPFYLT),
    OPPFYLT_TO_REGELVERK(null, Resultat.OPPFYLT),
    IKKE_OPPFYLT(null, Resultat.IKKE_OPPFYLT),
    IKKE_VURDERT(null, Resultat.IKKE_VURDERT),
}

data class VilkårRegelverkResultat(
    val vilkår: Vilkår,
    val regelverkResultat: RegelverkResultat
) {
    val resultat get() = regelverkResultat.resultat
    val regelverk get() = regelverkResultat.regelverk
}

fun VilkårResultat.tilRegelverkResultat() = when (this.resultat) {
    Resultat.OPPFYLT -> when (this.vurderesEtter) {
        Regelverk.EØS_FORORDNINGEN -> RegelverkResultat.OPPFYLT_EØS_FORORDNINGEN
        Regelverk.NASJONALE_REGLER -> RegelverkResultat.OPPFYLT_NASJONALE_REGLER
        else -> RegelverkResultat.OPPFYLT_REGELVERK_IKKE_SATT
    }
    Resultat.IKKE_OPPFYLT -> RegelverkResultat.IKKE_OPPFYLT
    Resultat.IKKE_VURDERT -> RegelverkResultat.IKKE_VURDERT
}

class VilkårsresultatDagTidslinje(
    private val vilkårsresultater: List<VilkårResultat>
) : Tidslinje<VilkårRegelverkResultat, Dag>() {

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
            regelverkResultat = this.tilRegelverkResultat()
        )
    )
}
