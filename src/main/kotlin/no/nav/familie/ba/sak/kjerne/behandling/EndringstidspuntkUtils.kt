package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.PeriodeResultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.PeriodeVilkår
import no.nav.familie.ba.sak.kjerne.beregning.domene.lagVertikaleSegmenter
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate

fun finnEndringstidspunkt(
    nyVilkårsvurdering: Vilkårsvurdering,
    gammelVilkårsvurdering: Vilkårsvurdering,
    nyeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    gamleAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    nyttPersonopplysningGrunnlag: PersonopplysningGrunnlag,
    gammeltPersonopplysningGrunnlag: PersonopplysningGrunnlag
): LocalDate? {
    val nyeAndelSegmenter = nyeAndelerTilkjentYtelse.lagVertikaleSegmenter()
    val gamleAndelSegmenter = gamleAndelerTilkjentYtelse.lagVertikaleSegmenter()

    val nyeVilkårsperioder =
        nyVilkårsvurdering.hentInnvilgedePerioder(nyttPersonopplysningGrunnlag).let { it.first + it.second }
    val gamleVilkårsperioder =
        gammelVilkårsvurdering.hentInnvilgedePerioder(gammeltPersonopplysningGrunnlag).let { it.first + it.second }

    val eldsteEndringVilkår: LocalDate? = nyeVilkårsperioder
        .sortedBy { it.periodeFom }
        .firstOrNull { nyeVilkårresultaterIPeriode ->
            !gamleVilkårsperioder.any { it.erFunksjoneltLik(nyeVilkårresultaterIPeriode) }
        }?.periodeFom

    val eldsteEndringAndeler = nyeAndelSegmenter.keys.sortedBy { it.fom }.firstOrNull { nyAndelPeriode ->
        val nyeAndelerIPeriode = nyeAndelSegmenter[nyAndelPeriode]!!
        val gamleAndelerIPeriode = gamleAndelSegmenter[nyAndelPeriode] ?: return nyAndelPeriode.fom

        nyeAndelerIPeriode.erFunksjoneltLik(gamleAndelerIPeriode)
    }?.fom

    return when {
        eldsteEndringAndeler == null && eldsteEndringVilkår == null -> null
        eldsteEndringAndeler == null -> eldsteEndringVilkår
        eldsteEndringVilkår == null -> eldsteEndringAndeler
        else -> minOf(eldsteEndringAndeler, eldsteEndringVilkår)
    }
}

private fun PeriodeResultat.erFunksjoneltLik(annenPeriode: PeriodeResultat): Boolean {
    return this.periodeFom == annenPeriode.periodeFom &&
        this.periodeTom == annenPeriode.periodeTom &&
        this.aktør.aktørId == annenPeriode.aktør.aktørId &&
        this.vilkårResultater.erFunksjoneltLik(annenPeriode.vilkårResultater)
}

private fun Set<PeriodeVilkår>.erFunksjoneltLik(andrePeriodeVilkårResultater: Set<PeriodeVilkår>): Boolean {
    return this.size == andrePeriodeVilkårResultater.size &&
        this.all { periodeVilkårResultat ->
            andrePeriodeVilkårResultater.any { it.erFunksjoneltLik(periodeVilkårResultat) }
        }
}

private fun PeriodeVilkår.erFunksjoneltLik(annenPeriodeVilkår: PeriodeVilkår): Boolean {
    return this.vilkårType == annenPeriodeVilkår.vilkårType &&
        this.resultat == annenPeriodeVilkår.resultat &&
        this.utdypendeVilkårsvurderinger.all { annenPeriodeVilkår.utdypendeVilkårsvurderinger.contains(it) } &&
        this.periodeFom == annenPeriodeVilkår.periodeFom &&
        this.periodeTom == annenPeriodeVilkår.periodeTom
}

private fun List<AndelTilkjentYtelse>.erFunksjoneltLik(andreAndelerTilkjentYtelse: List<AndelTilkjentYtelse>): Boolean {
    return this.size == andreAndelerTilkjentYtelse.size &&
        this.all { andelTilkjentYtelse ->
            andreAndelerTilkjentYtelse.any { it.erFunksjoneltLik(andelTilkjentYtelse) }
        }
}
