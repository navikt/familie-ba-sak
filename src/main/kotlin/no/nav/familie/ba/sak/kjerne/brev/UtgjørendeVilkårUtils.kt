package no.nav.familie.ba.sak.kjerne.vedtak

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
                oppdatertBegrunnelseType = oppdatertBegrunnelseType,
                utgjørendeVilkår = vilkår,
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
    oppdatertBegrunnelseType: VedtakBegrunnelseType,
    utgjørendeVilkår: Vilkår?,
    aktuellePersonerForVedtaksperiode: List<MinimertRestPerson>,
    triggesAv: TriggesAv,
    erFørsteVedtaksperiodePåFagsak: Boolean
): List<MinimertRestPerson> {

    return minimertRestPersonResultater
        .fold(mutableListOf()) { acc, personResultat ->
            val utgjørendeVilkårResultat =
                personResultat.minimerteVilkårResultater.firstOrNull { minimertVilkårResultat ->

                    val oppfyltTomMånedEtter =
                        if (minimertVilkårResultat.vilkårType == Vilkår.UNDER_18_ÅR &&
                            minimertVilkårResultat.periodeTom != minimertVilkårResultat.periodeTom?.sisteDagIMåned()
                        ) 0L
                        else 1L
                    when {
                        minimertVilkårResultat.vilkårType != utgjørendeVilkår -> false

                        minimertVilkårResultat.periodeFom == null
                            && oppdatertBegrunnelseType != VedtakBegrunnelseType.AVSLAG -> false

                        oppdatertBegrunnelseType == VedtakBegrunnelseType.INNVILGET -> {
                            triggereErOppfylt(triggesAv, minimertVilkårResultat) &&
                                minimertVilkårResultat.periodeFom!!.toYearMonth() == vedtaksperiode.fom.minusMonths(
                                1
                            )
                                .toYearMonth() &&
                                minimertVilkårResultat.resultat == Resultat.OPPFYLT
                        }

                        oppdatertBegrunnelseType == VedtakBegrunnelseType.OPPHØR && triggesAv.gjelderFørstePeriode
                        -> erFørstePeriodeOgVilkårIkkeOppfylt(
                            erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
                            vedtaksperiode = vedtaksperiode,
                            triggesAv = triggesAv,
                            vilkårResultat = minimertVilkårResultat
                        )

                        oppdatertBegrunnelseType == VedtakBegrunnelseType.REDUKSJON ||
                            oppdatertBegrunnelseType == VedtakBegrunnelseType.OPPHØR -> {
                            triggereErOppfylt(triggesAv, minimertVilkårResultat) &&
                                minimertVilkårResultat.periodeTom != null &&
                                minimertVilkårResultat.resultat == Resultat.OPPFYLT &&
                                minimertVilkårResultat.periodeTom.toYearMonth() ==
                                vedtaksperiode.fom.minusMonths(oppfyltTomMånedEtter).toYearMonth()
                        }

                        oppdatertBegrunnelseType == VedtakBegrunnelseType.AVSLAG ->
                            vilkårResultatPasserForAvslagsperiode(minimertVilkårResultat, vedtaksperiode)

                        else -> throw Feil("Henting av personer med utgjørende vilkår when: Ikke implementert")
                    }
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
        triggereErOppfylt(triggesAv, vilkårResultat) &&
        (vilkårIkkeOppfyltForPeriode || vilkårOppfyltRettEtterPeriode)
}

private fun triggereErOppfylt(
    triggesAv: TriggesAv,
    vilkårResultat: MinimertVilkårResultat
): Boolean {

    val erDeltBostedOppfylt =
        triggesAv.deltbosted &&
            vilkårResultat.utdypendeVilkårsvurderinger.contains(
                UtdypendeVilkårsvurdering.DELT_BOSTED
            ) || !triggesAv.deltbosted

    val erSkjønnsmessigVurderingOppfylt =
        triggesAv.vurderingAnnetGrunnlag &&
            vilkårResultat.utdypendeVilkårsvurderinger.contains(
                UtdypendeVilkårsvurdering.VURDERING_ANNET_GRUNNLAG
            ) || !triggesAv.vurderingAnnetGrunnlag

    val erMedlemskapOppfylt =
        triggesAv.medlemskap ==
            vilkårResultat.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP)

    return erDeltBostedOppfylt && erSkjønnsmessigVurderingOppfylt && erMedlemskapOppfylt
}
