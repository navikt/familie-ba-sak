package no.nav.familie.ba.sak.kjerne.vedtak

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.util.*

object VedtakUtils {

    fun hentHjemlerBruktIVedtak(vedtak: Vedtak): SortedSet<Int> {
        val hjemler = mutableSetOf<Int>()
        vedtak.vedtakBegrunnelser.forEach {
            hjemler.addAll(it.begrunnelse.hentHjemler().toSet())
        }
        return hjemler.toSortedSet()
    }

    /**
     * Funksjonen henter personer som trigger den gitte vedtaksperioden ved å hente vilkårResultater
     * basert på de attributter som definerer om en vedtaksbegrunnelse er trigget for en periode.
     *
     * @param vilkårsvurdering - Vilkårsvurderingen man ser på for å sammenligne vilkår
     * @param vedtaksperiode - Perioden det skal sjekkes for
     * @param oppdatertBegrunnelseType - Begrunnelsestype det skal sjekkes for
     * @param utgjørendeVilkår -  Alle de vilkår som trigger en vedtaksbegrynnelse.
     * @param deltBosted -  Om delt bosted må være valgt for å trigger en vedtaksbegrynnelse.
     * @param skjønnsmessigVurdert -  Om skjønnsmessig vurdering må være valgt for å trigger en vedtaksbegrynnelse.
     * @return List med personene det trigges endring på
     */
    fun hentPersonerForAlleUtgjørendeVilkår(vilkårsvurdering: Vilkårsvurdering,
            vedtaksperiode: Periode,
            oppdatertBegrunnelseType: VedtakBegrunnelseType,
            utgjørendeVilkår: Set<Vilkår>?,
            aktuellePersonerForVedtaksperiode: List<Person>,
            deltBosted: Boolean = false,
            vurderingAnnetGrunnlag: Boolean = false): Set<Person> {

        var personerSomOppfyllerTriggerVilkår: MutableSet<Person>? = null

        utgjørendeVilkår?.forEach { vilkår ->
            val personerMedVilkårForPeriode = hentPersonerMedUtgjørendeVilkår(
                    vilkårsvurdering = vilkårsvurdering,
                    vedtaksperiode = vedtaksperiode,
                    oppdatertBegrunnelseType = oppdatertBegrunnelseType,
                    utgjørendeVilkår = vilkår,
                    aktuellePersonerForVedtaksperiode = aktuellePersonerForVedtaksperiode,
                    deltBosted = deltBosted,
                    vurderingAnnetGrunnlag = vurderingAnnetGrunnlag
            )
            personerSomOppfyllerTriggerVilkår = if (personerSomOppfyllerTriggerVilkår == null) {
                personerMedVilkårForPeriode.toMutableSet()
            } else {
                personerSomOppfyllerTriggerVilkår!!.intersect(personerMedVilkårForPeriode).toMutableSet()
            }
        }
        return personerSomOppfyllerTriggerVilkår ?: emptySet()
    }

    private fun hentPersonerMedUtgjørendeVilkår(vilkårsvurdering: Vilkårsvurdering,
                                        vedtaksperiode: Periode,
                                        oppdatertBegrunnelseType: VedtakBegrunnelseType,
                                        utgjørendeVilkår: Vilkår?,
                                        aktuellePersonerForVedtaksperiode: List<Person>,
                                        deltBosted: Boolean = false,
                                        vurderingAnnetGrunnlag: Boolean = false): List<Person> {

        return vilkårsvurdering.personResultater.fold(mutableListOf()) { acc, personResultat ->
            val utgjørendeVilkårResultat = personResultat.vilkårResultater.firstOrNull { vilkårResultat ->

                val oppfyltTomMånedEtter =
                        if (vilkårResultat.vilkårType == Vilkår.UNDER_18_ÅR
                            && vilkårResultat.periodeTom != vilkårResultat.periodeTom?.sisteDagIMåned()) 0L
                        else 1L
                when {
                    vilkårResultat.vilkårType != utgjørendeVilkår -> false
                    vilkårResultat.periodeFom == null -> false
                    oppdatertBegrunnelseType == VedtakBegrunnelseType.INNVILGELSE -> {
                        (!deltBosted || vilkårResultat.erDeltBosted) &&
                        (!vurderingAnnetGrunnlag || vilkårResultat.erSkjønnsmessigVurdert) &&
                        vilkårResultat.periodeFom!!.toYearMonth() == vedtaksperiode.fom.minusMonths(1).toYearMonth() &&
                        vilkårResultat.resultat == Resultat.OPPFYLT
                    }

                    oppdatertBegrunnelseType == VedtakBegrunnelseType.REDUKSJON ||
                    oppdatertBegrunnelseType == VedtakBegrunnelseType.OPPHØR -> {
                        (!deltBosted || vilkårResultat.erDeltBosted) &&
                        (!vurderingAnnetGrunnlag || vilkårResultat.erSkjønnsmessigVurdert) &&
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
}

fun validerAvslagsbegrunnelse(triggesAv: TriggesAv,
                              vilkårResultat: VilkårResultat) {
    if (triggesAv.vilkår?.contains(vilkårResultat.vilkårType) != true) {
        error("Avslagbegrunnelser som oppdateres må tilhøre samme vilkår")
    }
}