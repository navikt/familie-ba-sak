package no.nav.familie.ba.sak.kjerne.vedtak

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurderingType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering

object VedtakUtils {

    /**
     * Funksjonen henter personer som trigger den gitte vedtaksperioden ved å hente vilkårResultater
     * basert på de attributter som definerer om en vedtaksbegrunnelse er trigget for en periode.
     *
     * @param vilkårsvurdering - Vilkårsvurderingen man ser på for å sammenligne vilkår
     * @param vedtaksperiode - Perioden det skal sjekkes for
     * @param oppdatertBegrunnelseType - Begrunnelsestype det skal sjekkes for
     * @param triggesAv -  Hva som trigger en vedtaksbegrynnelse.
     * @return List med personene det trigges endring på
     */
    fun hentPersonerForAlleUtgjørendeVilkår(
        vilkårsvurdering: Vilkårsvurdering,
        vedtaksperiode: Periode,
        oppdatertBegrunnelseType: VedtakBegrunnelseType,
        aktuellePersonerForVedtaksperiode: List<Person>,
        triggesAv: TriggesAv,
    ): Set<Person> {
        return triggesAv.vilkår.fold(mutableSetOf()) { acc, vilkår ->
            acc.addAll(
                hentPersonerMedUtgjørendeVilkår(
                    vilkårsvurdering = vilkårsvurdering,
                    vedtaksperiode = vedtaksperiode,
                    oppdatertBegrunnelseType = oppdatertBegrunnelseType,
                    utgjørendeVilkår = vilkår,
                    aktuellePersonerForVedtaksperiode = aktuellePersonerForVedtaksperiode,
                    triggesAv = triggesAv
                )
            )

            acc
        }
    }

    private fun hentPersonerMedUtgjørendeVilkår(
        vilkårsvurdering: Vilkårsvurdering,
        vedtaksperiode: Periode,
        oppdatertBegrunnelseType: VedtakBegrunnelseType,
        utgjørendeVilkår: Vilkår?,
        aktuellePersonerForVedtaksperiode: List<Person>,
        triggesAv: TriggesAv
    ): List<Person> {

        return vilkårsvurdering.personResultater
            .fold(mutableListOf()) { acc, personResultat ->
                val utgjørendeVilkårResultat = personResultat.vilkårResultater.firstOrNull { vilkårResultat ->

                    val oppfyltTomMånedEtter =
                        if (vilkårResultat.vilkårType == Vilkår.UNDER_18_ÅR &&
                            vilkårResultat.periodeTom != vilkårResultat.periodeTom?.sisteDagIMåned()
                        ) 0L
                        else 1L
                    when {
                        vilkårResultat.vilkårType != utgjørendeVilkår -> false
                        vilkårResultat.periodeFom == null -> false
                        oppdatertBegrunnelseType == VedtakBegrunnelseType.INNVILGET -> {
                            triggereErOppfylt(triggesAv, vilkårResultat) &&
                                vilkårResultat.periodeFom!!.toYearMonth() == vedtaksperiode.fom.minusMonths(1)
                                .toYearMonth() &&
                                vilkårResultat.resultat == Resultat.OPPFYLT
                        }

                        oppdatertBegrunnelseType == VedtakBegrunnelseType.REDUKSJON ||
                            oppdatertBegrunnelseType == VedtakBegrunnelseType.OPPHØR -> {
                            triggereErOppfylt(triggesAv, vilkårResultat) &&
                                vilkårResultat.periodeTom != null &&
                                vilkårResultat.resultat == Resultat.OPPFYLT &&
                                vilkårResultat.periodeTom!!.toYearMonth() ==
                                vedtaksperiode.fom.minusMonths(oppfyltTomMånedEtter).toYearMonth()
                        }
                        else -> throw Feil("Henting av personer med utgjørende vilkår when: Ikke implementert")
                    }
                }

                val person = aktuellePersonerForVedtaksperiode.firstOrNull { person ->
                    person.personIdent.ident == personResultat.personIdent
                }
                if (utgjørendeVilkårResultat != null && person != null) {
                    acc.add(person)
                }
                acc
            }
    }

    private fun triggereErOppfylt(
        triggesAv: TriggesAv,
        vilkårResultat: VilkårResultat
    ): Boolean {
        val erDeltBostedOppfylt =
            (
                !triggesAv.deltbosted || vilkårResultat.erDeltBosted || vilkårResultat.utdypendeVilkårsvurderinger.contains(
                    UtdypendeVilkårsvurderingType.DELT_BOSTED
                )
                )
        val erSkjønnsmessigVurderingOppfylt =
            (
                !triggesAv.vurderingAnnetGrunnlag || vilkårResultat.erSkjønnsmessigVurdert || vilkårResultat.utdypendeVilkårsvurderinger.contains(
                    UtdypendeVilkårsvurderingType.VURDERING_ANNET_GRUNNLAG
                )
                )
        val erMedlemskapOppfylt =
            vilkårResultat.erMedlemskapVurdert == triggesAv.medlemskap || (
                vilkårResultat.utdypendeVilkårsvurderinger.contains(
                    UtdypendeVilkårsvurderingType.VURDERT_MEDLEMSKAP
                ) && triggesAv.medlemskap
                ) || (!vilkårResultat.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurderingType.VURDERT_MEDLEMSKAP) && !triggesAv.medlemskap)

        return erDeltBostedOppfylt && erSkjønnsmessigVurderingOppfylt && erMedlemskapOppfylt
    }
}

fun validerAvslagsbegrunnelse(
    triggesAv: TriggesAv,
    vilkårResultat: VilkårResultat
) {
    if (triggesAv.vilkår.contains(vilkårResultat.vilkårType) != true) {
        error("Avslagbegrunnelser som oppdateres må tilhøre samme vilkår")
    }
}
