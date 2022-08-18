package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.lagOgValiderPeriodeFraVilkår
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import java.time.LocalDate

data class MinimertVilkårResultat(
    val vilkårType: Vilkår,
    val periodeFom: LocalDate?,
    val periodeTom: LocalDate?,
    val resultat: Resultat,
    val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering>,
    val erEksplisittAvslagPåSøknad: Boolean?,
) {

    fun toPeriode(): Periode = lagOgValiderPeriodeFraVilkår(
        this.periodeFom,
        this.periodeTom,
        this.erEksplisittAvslagPåSøknad
    )

    fun erAvsluttetOgSkalBegrunnesIdennePerioden(
        fomVedtaksperiode: LocalDate?,
        etterfølgendeVilkårResultatAvSammeType: MinimertVilkårResultat?
    ): Boolean {
        if (fomVedtaksperiode == null) return false

        val erOppfyltTomMånedEtter = this.vilkårType != Vilkår.UNDER_18_ÅR ||
            this.periodeTom == this.periodeTom?.sisteDagIMåned()

        val erStartPåDeltBosted = erStartPåDeltBosted(etterfølgendeVilkårResultatAvSammeType)

        val startNestePeriodeEtterVilkår = this.periodeTom
            ?.plusDays(if (erStartPåDeltBosted) 1 else 0)
            ?.plusMonths(if (erOppfyltTomMånedEtter) 1 else 0)

        return this.periodeTom != null &&
            this.resultat == Resultat.OPPFYLT &&
            startNestePeriodeEtterVilkår?.toYearMonth() == fomVedtaksperiode.toYearMonth()
    }

    private fun erStartPåDeltBosted(nesteBack2BackVilkårResultatAvSammeType: MinimertVilkårResultat?) =
        this.vilkårType == Vilkår.BOR_MED_SØKER &&
            nesteBack2BackVilkårResultatAvSammeType?.vilkårType == Vilkår.BOR_MED_SØKER &&
            this.resultat == Resultat.OPPFYLT &&
            nesteBack2BackVilkårResultatAvSammeType.resultat == Resultat.OPPFYLT &&
            !this.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED) &&
            nesteBack2BackVilkårResultatAvSammeType.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED)
}

fun VilkårResultat.tilMinimertVilkårResultat() =
    MinimertVilkårResultat(
        vilkårType = this.vilkårType,
        periodeFom = this.periodeFom,
        periodeTom = this.periodeTom,
        resultat = this.resultat,
        utdypendeVilkårsvurderinger = this.utdypendeVilkårsvurderinger,
        erEksplisittAvslagPåSøknad = this.erEksplisittAvslagPåSøknad,
    )
