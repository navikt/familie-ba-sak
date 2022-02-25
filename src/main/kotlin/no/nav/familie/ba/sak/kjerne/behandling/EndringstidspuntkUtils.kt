package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.PeriodeResultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.PeriodeVilkår
import no.nav.familie.ba.sak.kjerne.beregning.domene.lagVertikaleSegmenter
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.fpsak.tidsserie.LocalDateSegment
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

    val eldsteEndringVilkår = finnEldsteEndringIVilkår(nyeVilkårsperioder, gamleVilkårsperioder)

    val eldsteEndringAndeler = finnEldsteEndringIAndelTilkjentYtelse(nyeAndelSegmenter, gamleAndelSegmenter)

    return minsteNullableDato(eldsteEndringAndeler, eldsteEndringVilkår)
}

private fun finnEldsteEndringIAndelTilkjentYtelse(
    nyeAndelSegmenter: Map<LocalDateSegment<Int>, List<AndelTilkjentYtelse>>,
    gamleAndelSegmenter: Map<LocalDateSegment<Int>, List<AndelTilkjentYtelse>>
): LocalDate? {
    val fomDatoerIkkeINyeVilkår =
        gamleAndelSegmenter.keys
            .map { it.fom ?: TIDENES_MORGEN }
            .filter { gammelAndelsperiode ->
                !nyeAndelSegmenter.keys.any { it.fom == gammelAndelsperiode }
            }
    val minsteFomdatoIkkeINyeAndeler = fomDatoerIkkeINyeVilkår.minOfOrNull { it }

    val førsteDiffFraNyeAndeler = nyeAndelSegmenter.keys
        .sortedBy { it.fom }
        .firstOrNull { nyAndelPeriode ->
            val nyeAndelerIPeriode = nyeAndelSegmenter[nyAndelPeriode]!!
            val gamleAndelerIPeriode = gamleAndelSegmenter[nyAndelPeriode] ?: return nyAndelPeriode.fom

            !nyeAndelerIPeriode.erFunksjoneltLik(gamleAndelerIPeriode)
        }?.fom

    return minsteNullableDato(førsteDiffFraNyeAndeler, minsteFomdatoIkkeINyeAndeler)
}

private fun finnEldsteEndringIVilkår(
    nyeVilkårsperioder: List<PeriodeResultat>,
    gamleVilkårsperioder: List<PeriodeResultat>
): LocalDate? {
    val fomDatoerIkkeINyeVilkår =
        gamleVilkårsperioder
            .map { it.periodeFom ?: TIDENES_MORGEN }
            .filter { gammelVilkårsperiode ->
                !nyeVilkårsperioder.any { it.periodeFom == gammelVilkårsperiode }
            }
    val minsteFomdatoIkkeINyeVilkår = fomDatoerIkkeINyeVilkår.minOfOrNull { it }

    val førsteDiffFraNyeVilkår = nyeVilkårsperioder
        .sortedBy { it.periodeFom }
        .filter { (it.periodeFom ?: TIDENES_MORGEN).isBefore(minsteFomdatoIkkeINyeVilkår ?: TIDENES_ENDE) }
        .firstOrNull { nyeVilkårresultaterIPeriode ->
            !gamleVilkårsperioder.any { it.erFunksjoneltLik(nyeVilkårresultaterIPeriode) }
        }?.periodeFom

    return minsteNullableDato(førsteDiffFraNyeVilkår, minsteFomdatoIkkeINyeVilkår)
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

private fun minsteNullableDato(
    dato1: LocalDate?,
    dato2: LocalDate?
) = when {
    dato1 == null && dato2 == null -> null
    dato1 == null -> dato2
    dato2 == null -> dato1
    else -> minOf(dato1, dato2)
}
