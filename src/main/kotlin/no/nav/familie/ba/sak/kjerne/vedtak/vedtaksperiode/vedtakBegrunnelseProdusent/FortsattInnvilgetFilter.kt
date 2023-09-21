package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent

import erLikVilkårOgUtdypendeVilkårIPeriode
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.GrunnlagForBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityPeriodeResultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse

fun hentFortsattInnvilgetBegrunnelserPerPerson(
    begrunnelseGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>,
    grunnlag: GrunnlagForBegrunnelse,
): Map<Person, Set<IVedtakBegrunnelse>> {
    val fagsakType = grunnlag.behandlingsGrunnlagForVedtaksperioder.fagsakType

    val standardbegrunnelserIngenEndring = grunnlag.sanityBegrunnelser
        .filterValues { it.erGjeldendeForFagsakType(fagsakType) }
        .filterValues { it.periodeResultat == SanityPeriodeResultat.INGEN_ENDRING }

    val eøsBegrunnelserIngenEndring = grunnlag.sanityEØSBegrunnelser
        .filterValues { it.erGjeldendeForFagsakType(fagsakType) }
        .filterValues { it.periodeResultat == SanityPeriodeResultat.INGEN_ENDRING }

    return begrunnelseGrunnlagPerPerson.mapValues { (person, begrunnelseGrunnlag) ->
        val begrunnelseGrunnlagForPerson = begrunnelseGrunnlag.dennePerioden

        val oppfylteVilkårresultater =
            begrunnelseGrunnlagForPerson.vilkårResultater.filter { it.resultat == Resultat.OPPFYLT }.toList()

        val standardbegrunnelseSomMatcherVilkår = standardbegrunnelserIngenEndring
            .filterValues { it.erGjeldendeForRolle(person, fagsakType) }
            .filterValues { it.erLikVilkårOgUtdypendeVilkårIPeriode(oppfylteVilkårresultater) }

        val eøsBegrunnelserSomMatcherKompetanse = eøsBegrunnelserIngenEndring
            .filterValues { it.erLikKompetanseIPeriode(begrunnelseGrunnlag) }

        standardbegrunnelseSomMatcherVilkår.keys.toSet() +
            eøsBegrunnelserSomMatcherKompetanse.keys.toSet()
    }
}
