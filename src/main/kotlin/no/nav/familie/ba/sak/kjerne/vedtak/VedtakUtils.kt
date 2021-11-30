package no.nav.familie.ba.sak.kjerne.vedtak

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.overlapperHeltEllerDelvisMed
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toPeriode
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
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
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>
    ): Set<Person> {
        return triggesAv.vilkår.fold(mutableSetOf()) { acc, vilkår ->
            acc.addAll(
                hentPersonerMedUtgjørendeVilkår(
                    vilkårsvurdering = vilkårsvurdering,
                    vedtaksperiode = vedtaksperiode,
                    oppdatertBegrunnelseType = oppdatertBegrunnelseType,
                    utgjørendeVilkår = vilkår,
                    aktuellePersonerForVedtaksperiode = aktuellePersonerForVedtaksperiode,
                    triggesAv = triggesAv,
                    andelerTilkjentYtelse = andelerTilkjentYtelse,
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
        triggesAv: TriggesAv,
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
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

                        oppdatertBegrunnelseType == VedtakBegrunnelseType.OPPHØR && triggesAv.gjelderFørstePeriode
                        -> erFørstePeriodeOgVilkårIkkeOppfylt(
                            andelerTilkjentYtelse = andelerTilkjentYtelse,
                            vedtaksperiode = vedtaksperiode,
                            triggesAv = triggesAv,
                            vilkårResultat = vilkårResultat
                        )

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
                    person.aktør == personResultat.aktør
                }
                if (utgjørendeVilkårResultat != null && person != null) {
                    acc.add(person)
                }
                acc
            }
    }
}

fun erFørstePeriodeOgVilkårIkkeOppfylt(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    vedtaksperiode: Periode,
    triggesAv: TriggesAv,
    vilkårResultat: VilkårResultat
) = !andelerTilkjentYtelse.any { it.stønadFom.isBefore(vedtaksperiode.fom.toYearMonth()) } &&
    triggereErOppfylt(triggesAv, vilkårResultat) &&
    vilkårResultat.resultat == Resultat.IKKE_OPPFYLT &&
    vilkårResultat.toPeriode().overlapperHeltEllerDelvisMed(vedtaksperiode)

private fun triggereErOppfylt(
    triggesAv: TriggesAv,
    vilkårResultat: VilkårResultat
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
