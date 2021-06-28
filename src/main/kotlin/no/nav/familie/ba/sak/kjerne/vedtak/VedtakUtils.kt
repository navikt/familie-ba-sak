package no.nav.familie.ba.sak.kjerne.vedtak

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.Vilkår
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
     * basert på utgjørendeVilkår og begrunnelseType.
     *
     * @param vilkårsvurdering - Vilkårsvurderingen man ser på for å sammenligne vilkår
     * @param vedtaksperiode - Periode
     * @param oppdatertBegrunnelseType - Brukes til å se om man skal sammenligne fom eller tom-dato
     * @param utgjørendeVilkår -  Brukes til å sammenligne vilkår i vilkårsvurdering
     * @return List med personene det trigges endring på
     */
    fun hentPersonerMedUtgjørendeVilkår(vilkårsvurdering: Vilkårsvurdering,
                                        vedtaksperiode: Periode,
                                        oppdatertBegrunnelseType: VedtakBegrunnelseType,
                                        utgjørendeVilkår: Vilkår?,
                                        aktuellePersonerForVedtaksperiode: List<Person>): List<Person> {

        return vilkårsvurdering.personResultater.fold(mutableListOf()) { acc, personResultat ->
            val utgjørendeVilkårResultat = personResultat.vilkårResultater.firstOrNull { vilkårResultat ->

                val oppfyltTomMånedEtter =
                        if (vilkårResultat.vilkårType == Vilkår.UNDER_18_ÅR && vilkårResultat.periodeTom != vilkårResultat.periodeTom?.sisteDagIMåned()) 0L else 1L
                when {
                    vilkårResultat.vilkårType != utgjørendeVilkår -> false
                    vilkårResultat.periodeFom == null -> {
                        false
                    }
                    oppdatertBegrunnelseType == VedtakBegrunnelseType.INNVILGELSE -> {
                        vilkårResultat.periodeFom!!.toYearMonth() == vedtaksperiode.fom.minusMonths(1)
                                .toYearMonth() && vilkårResultat.resultat == Resultat.OPPFYLT
                    }

                    oppdatertBegrunnelseType == VedtakBegrunnelseType.REDUKSJON ||
                    oppdatertBegrunnelseType == VedtakBegrunnelseType.OPPHØR -> {
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