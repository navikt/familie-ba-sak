package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.overlapperHeltEllerDelvisMed
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertRestPersonResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertVilkårResultat
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertRestPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

/**
 * Funksjonen henter personer som trigger den gitte vedtaksperioden ved å hente vilkårResultater
 * basert på de attributter som definerer om en vedtaksbegrunnelse er trigget for en periode.
 *
 * @param minimertePersonResultater - Resultatene fra vilkårsvurderingen for hver person
 * @param vedtaksperiode - Perioden det skal sjekkes for
 * @param oppdatertBegrunnelseType - Begrunnelsestype det skal sjekkes for
 * @param aktuellePersonerForVedtaksperiode - Personer på behandlingen som er aktuelle for vedtaksperioden
 * @param triggesAv -  Hva som trigger en vedtaksbegrynnelse.
 * @param erFørsteVedtaksperiodePåFagsak - Om vedtaksperioden er første periode på fagsak.
 *        Brukes for opphør som har egen logikk dersom det er første periode.
 * @return List med personene det trigges endring på
 */
fun hentPersonerForAlleUtgjørendeVilkår(
    minimertePersonResultater: List<MinimertRestPersonResultat>,
    vedtaksperiode: Periode,
    oppdatertBegrunnelseType: VedtakBegrunnelseType,
    aktuellePersonerForVedtaksperiode: List<MinimertRestPerson>,
    triggesAv: TriggesAv,
    erFørsteVedtaksperiodePåFagsak: Boolean
): Set<MinimertRestPerson> {
    return triggesAv.vilkår.fold(mutableSetOf()) { acc, vilkår ->
        acc.addAll(
            hentPersonerMedUtgjørendeVilkår(
                minimertRestPersonResultater = minimertePersonResultater,
                vedtaksperiode = vedtaksperiode,
                begrunnelseType = oppdatertBegrunnelseType,
                vilkårGjeldendeForBegrunnelse = vilkår,
                aktuellePersonerForVedtaksperiode = aktuellePersonerForVedtaksperiode,
                triggesAv = triggesAv,
                erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak
            )
        )

        acc
    }
}

private fun hentPersonerMedUtgjørendeVilkår(
    minimertRestPersonResultater: List<MinimertRestPersonResultat>,
    vedtaksperiode: Periode,
    begrunnelseType: VedtakBegrunnelseType,
    vilkårGjeldendeForBegrunnelse: Vilkår,
    aktuellePersonerForVedtaksperiode: List<MinimertRestPerson>,
    triggesAv: TriggesAv,
    erFørsteVedtaksperiodePåFagsak: Boolean
): List<MinimertRestPerson> {

    val aktuellePersonidenter = aktuellePersonerForVedtaksperiode.map { it.personIdent }

    return minimertRestPersonResultater
        .filter { aktuellePersonidenter.contains(it.personIdent) }
        .fold(mutableListOf()) { acc, personResultat ->
            val utgjørendeVilkårResultat =
                personResultat.minimerteVilkårResultater
                    .filter { it.vilkårType == vilkårGjeldendeForBegrunnelse }
                    .firstOrNull { minimertVilkårResultat ->
                        erVilkårResultatUtgjørende(
                            minimertVilkårResultat = minimertVilkårResultat,
                            begrunnelseType = begrunnelseType,
                            triggesAv = triggesAv,
                            vedtaksperiode = vedtaksperiode,
                            erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak
                        )
                    }

            val person = aktuellePersonerForVedtaksperiode.firstOrNull { person ->
                person.personIdent == personResultat.personIdent
            }

            if (utgjørendeVilkårResultat != null && person != null) {
                acc.add(person)
            }
            acc
        }
}

private fun erVilkårResultatUtgjørende(
    minimertVilkårResultat: MinimertVilkårResultat,
    begrunnelseType: VedtakBegrunnelseType,
    triggesAv: TriggesAv,
    vedtaksperiode: Periode,
    erFørsteVedtaksperiodePåFagsak: Boolean,
): Boolean {
    if (minimertVilkårResultat.periodeFom == null && begrunnelseType != VedtakBegrunnelseType.AVSLAG) {
        return false
    }

    return when (begrunnelseType) {
        VedtakBegrunnelseType.INNVILGET ->
            erInnvilgetVilkårResultatUtgjørende(triggesAv, minimertVilkårResultat, vedtaksperiode)

        VedtakBegrunnelseType.OPPHØR -> if (triggesAv.gjelderFørstePeriode) {
            erFørstePeriodeOgVilkårIkkeOppfylt(
                erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
                vedtaksperiode = vedtaksperiode,
                triggesAv = triggesAv,
                vilkårResultat = minimertVilkårResultat
            )
        } else {
            erOpphørResultatUtgjøreneForPeriode(
                minimertVilkårResultat = minimertVilkårResultat,
                triggesAv = triggesAv,
                vedtaksperiode = vedtaksperiode
            )
        }

        VedtakBegrunnelseType.REDUKSJON -> {
            erReduksjonResultatUtgjøreneForPeriode(
                minimertVilkårResultat = minimertVilkårResultat,
                triggesAv = triggesAv,
                vedtaksperiode = vedtaksperiode
            )
        }

        VedtakBegrunnelseType.AVSLAG ->
            vilkårResultatPasserForAvslagsperiode(minimertVilkårResultat, vedtaksperiode)

        else -> throw Feil("Henting av personer med utgjørende vilkår when: Ikke implementert")
    }
}

private fun erOpphørResultatUtgjøreneForPeriode(
    minimertVilkårResultat: MinimertVilkårResultat,
    triggesAv: TriggesAv,
    vedtaksperiode: Periode
): Boolean {
    val oppfyltTomMånedEtter =
        if (minimertVilkårResultat.vilkårType == Vilkår.UNDER_18_ÅR &&
            minimertVilkårResultat.periodeTom != minimertVilkårResultat.periodeTom?.sisteDagIMåned()
        ) 0L
        else 1L

    return triggereForUtdypendeVilkårsvurderingErOppfylt(triggesAv, minimertVilkårResultat) &&
        minimertVilkårResultat.periodeTom != null &&
        minimertVilkårResultat.resultat == Resultat.OPPFYLT &&
        minimertVilkårResultat.periodeTom.toYearMonth() == vedtaksperiode.fom.minusMonths(
        oppfyltTomMånedEtter
    ).toYearMonth()
}

private fun erReduksjonResultatUtgjøreneForPeriode(
    minimertVilkårResultat: MinimertVilkårResultat,
    triggesAv: TriggesAv,
    vedtaksperiode: Periode
): Boolean {
    val oppfyltTomMånedEtter =
        if (minimertVilkårResultat.vilkårType == Vilkår.UNDER_18_ÅR &&
            minimertVilkårResultat.periodeTom != minimertVilkårResultat.periodeTom?.sisteDagIMåned()
        ) 0L
        else 1L

    val erStartPåDeltBosted =
        minimertVilkårResultat.vilkårType == Vilkår.BOR_MED_SØKER &&
            !minimertVilkårResultat.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED) &&
            triggesAv.deltbosted

    return triggereForUtdypendeVilkårsvurderingErOppfylt(
        triggesAv = triggesAv,
        vilkårResultat = minimertVilkårResultat,
        erReduksjonStartPåDeltBosted = erStartPåDeltBosted
    ) &&
        minimertVilkårResultat.periodeTom != null &&
        minimertVilkårResultat.resultat == Resultat.OPPFYLT &&
        minimertVilkårResultat.periodeTom.plusDays(if (erStartPåDeltBosted) 1 else 0)
        .toYearMonth() == vedtaksperiode.fom.minusMonths(
        oppfyltTomMånedEtter
    ).toYearMonth()
}

private fun erInnvilgetVilkårResultatUtgjørende(
    triggesAv: TriggesAv,
    minimertVilkårResultat: MinimertVilkårResultat,
    vedtaksperiode: Periode
): Boolean {
    val vilkårResultatFomMåned = minimertVilkårResultat.periodeFom!!.toYearMonth()
    val vedtaksperiodeFomMåned = vedtaksperiode.fom.toYearMonth()

    return triggereForUtdypendeVilkårsvurderingErOppfylt(triggesAv, minimertVilkårResultat) &&
        vilkårResultatFomMåned == vedtaksperiodeFomMåned.minusMonths(1) &&
        minimertVilkårResultat.resultat == Resultat.OPPFYLT
}

private fun vilkårResultatPasserForAvslagsperiode(
    minimertVilkårResultat: MinimertVilkårResultat,
    vedtaksperiode: Periode
): Boolean {
    val erAvslagUtenFomDato = minimertVilkårResultat.periodeFom == null

    val fomVilkår =
        if (erAvslagUtenFomDato) TIDENES_MORGEN.toYearMonth()
        else minimertVilkårResultat.periodeFom!!.toYearMonth()

    return fomVilkår == vedtaksperiode.fom.toYearMonth() &&
        minimertVilkårResultat.resultat == Resultat.IKKE_OPPFYLT
}

fun erFørstePeriodeOgVilkårIkkeOppfylt(
    erFørsteVedtaksperiodePåFagsak: Boolean,
    vedtaksperiode: Periode,
    triggesAv: TriggesAv,
    vilkårResultat: MinimertVilkårResultat
): Boolean {
    val vilkårIkkeOppfyltForPeriode =
        vilkårResultat.resultat == Resultat.IKKE_OPPFYLT &&
            vilkårResultat.toPeriode().overlapperHeltEllerDelvisMed(vedtaksperiode)

    val vilkårOppfyltRettEtterPeriode =
        vilkårResultat.resultat == Resultat.OPPFYLT &&
            vedtaksperiode.tom.toYearMonth() == vilkårResultat.periodeFom!!.toYearMonth()

    return erFørsteVedtaksperiodePåFagsak &&
        triggereForUtdypendeVilkårsvurderingErOppfylt(triggesAv, vilkårResultat) &&
        (vilkårIkkeOppfyltForPeriode || vilkårOppfyltRettEtterPeriode)
}

private fun triggereForUtdypendeVilkårsvurderingErOppfylt(
    triggesAv: TriggesAv,
    vilkårResultat: MinimertVilkårResultat,
    erReduksjonStartPåDeltBosted: Boolean = false,
): Boolean {

    val resultatInneholderDeltBosted =
        vilkårResultat.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED)
    val erDeltBostedOppfylt =
        triggesAv.deltbosted &&
            erReduksjonStartPåDeltBosted != resultatInneholderDeltBosted ||
            !triggesAv.deltbosted

    val resultatInneholderVurderingAnnetGrunnlag =
        vilkårResultat.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.VURDERING_ANNET_GRUNNLAG)
    val erSkjønnsmessigVurderingOppfylt =
        triggesAv.vurderingAnnetGrunnlag &&
            resultatInneholderVurderingAnnetGrunnlag ||
            !triggesAv.vurderingAnnetGrunnlag

    val resultatInneholderMedlemsskap =
        vilkårResultat.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP)
    val erMedlemskapOppfylt =
        triggesAv.medlemskap == resultatInneholderMedlemsskap

    return erDeltBostedOppfylt && erSkjønnsmessigVurderingOppfylt && erMedlemskapOppfylt
}
