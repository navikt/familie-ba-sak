package no.nav.familie.ba.sak.kjerne.vedtak

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.overlapperHeltEllerDelvisMed
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.MinimertPersonResultat
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.MinimertVilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

object VedtakUtils {

    /**
     * TODO fix this
     * Funksjonen henter personer som trigger den gitte vedtaksperioden ved å hente vilkårResultater
     * basert på de attributter som definerer om en vedtaksbegrunnelse er trigget for en periode.
     *
     * @param minimertePersonResultater - Vilkårsvurderingen man ser på for å sammenligne vilkår
     * @param vedtaksperiode - Perioden det skal sjekkes for
     * @param oppdatertBegrunnelseType - Begrunnelsestype det skal sjekkes for
     * @param triggesAv -  Hva som trigger en vedtaksbegrynnelse.
     * @return List med personene det trigges endring på
     */
    fun hentPersonerForAlleUtgjørendeVilkår(
        minimertePersonResultater: List<MinimertPersonResultat>,
        vedtaksperiode: Periode,
        oppdatertBegrunnelseType: VedtakBegrunnelseType,
        aktuellePersonerForVedtaksperiode: List<MinimertPerson>,
        triggesAv: TriggesAv,
        erFørsteVedtaksperiodePåFagsak: Boolean
    ): Set<MinimertPerson> {
        return triggesAv.vilkår.fold(mutableSetOf()) { acc, vilkår ->
            acc.addAll(
                hentPersonerMedUtgjørendeVilkår(
                    minimertPersonResultater = minimertePersonResultater,
                    vedtaksperiode = vedtaksperiode,
                    oppdatertBegrunnelseType = oppdatertBegrunnelseType,
                    utgjørendeVilkår = vilkår,
                    aktuellePersonerForVedtaksperiode = aktuellePersonerForVedtaksperiode,
                    triggesAv = triggesAv,
                    erAndelerMedFomFørPeriode = erFørsteVedtaksperiodePåFagsak
                )
            )

            acc
        }
    }

    private fun hentPersonerMedUtgjørendeVilkår(
        minimertPersonResultater: List<MinimertPersonResultat>,
        vedtaksperiode: Periode,
        oppdatertBegrunnelseType: VedtakBegrunnelseType,
        utgjørendeVilkår: Vilkår?,
        aktuellePersonerForVedtaksperiode: List<MinimertPerson>,
        triggesAv: TriggesAv,
        erAndelerMedFomFørPeriode: Boolean
    ): List<MinimertPerson> {

        return minimertPersonResultater
            .fold(mutableListOf()) { acc, personResultat ->
                val utgjørendeVilkårResultat =
                    personResultat.minimerteVilkårResultater.firstOrNull { minimertVilkårResultat ->
                        if (minimertVilkårResultat.resultat == Resultat.IKKE_OPPFYLT) {
                            print("Hei")
                        }

                        val oppfyltTomMånedEtter =
                            if (minimertVilkårResultat.vilkårType == Vilkår.UNDER_18_ÅR &&
                                minimertVilkårResultat.periodeTom != minimertVilkårResultat.periodeTom?.sisteDagIMåned()
                            ) 0L
                            else 1L
                        when {
                            minimertVilkårResultat.vilkårType != utgjørendeVilkår -> false
                            minimertVilkårResultat.periodeFom == null -> false
                            oppdatertBegrunnelseType == VedtakBegrunnelseType.INNVILGET -> {
                                triggereErOppfylt(triggesAv, minimertVilkårResultat) &&
                                    minimertVilkårResultat.periodeFom.toYearMonth() == vedtaksperiode.fom.minusMonths(
                                    1
                                )
                                    .toYearMonth() &&
                                    minimertVilkårResultat.resultat == Resultat.OPPFYLT
                            }

                            oppdatertBegrunnelseType == VedtakBegrunnelseType.OPPHØR && triggesAv.gjelderFørstePeriode
                            -> erFørstePeriodeOgVilkårIkkeOppfylt(
                                erFørsteVedtaksperiodePåFagsak = erAndelerMedFomFørPeriode,
                                vedtaksperiode = vedtaksperiode,
                                triggesAv = triggesAv,
                                vilkårResultat = minimertVilkårResultat
                            )

                            oppdatertBegrunnelseType == VedtakBegrunnelseType.REDUKSJON ||
                                oppdatertBegrunnelseType == VedtakBegrunnelseType.OPPHØR -> {
                                triggereErOppfylt(triggesAv, minimertVilkårResultat) &&
                                    minimertVilkårResultat.periodeTom != null &&
                                    minimertVilkårResultat.resultat == Resultat.OPPFYLT &&
                                    minimertVilkårResultat.periodeTom!!.toYearMonth() ==
                                    vedtaksperiode.fom.minusMonths(oppfyltTomMånedEtter).toYearMonth()
                            }
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
