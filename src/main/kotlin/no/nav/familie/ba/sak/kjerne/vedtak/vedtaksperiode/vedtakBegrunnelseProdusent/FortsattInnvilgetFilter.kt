package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.GrunnlagForBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityPeriodeResultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser

fun hentFortsattInnvilgetBegrunnelserPerPerson(
    begrunnelseGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>,
    grunnlag: GrunnlagForBegrunnelse,
    vedtaksperiode: VedtaksperiodeMedBegrunnelser,
): Map<Person, Set<IVedtakBegrunnelse>> {
    val fagsakType = grunnlag.behandlingsGrunnlagForVedtaksperioder.behandling.fagsak.type

    val erUtbetalingEllerDeltBostedIPeriode = erUtbetalingEllerDeltBostedIPeriode(begrunnelseGrunnlagPerPerson)

    val relevanteStandardbegrunnelser =
        grunnlag.sanityBegrunnelser
            .filterValues { it.erGjeldendeForFagsakType(fagsakType) }
            .filterValues { it.erGjeldendeForBrevPeriodeType(vedtaksperiode, erUtbetalingEllerDeltBostedIPeriode) }
            .filterValues { it.periodeResultat == SanityPeriodeResultat.INGEN_ENDRING || it.periodeResultat == SanityPeriodeResultat.IKKE_RELEVANT }

    val relevanteEøsBegrunnelser =
        grunnlag.sanityEØSBegrunnelser
            .filterValues { it.erGjeldendeForFagsakType(fagsakType) }
            .filterValues { it.erGjeldendeForBrevPeriodeType(vedtaksperiode, erUtbetalingEllerDeltBostedIPeriode) }
            .filterValues { it.periodeResultat == SanityPeriodeResultat.INGEN_ENDRING || it.periodeResultat == SanityPeriodeResultat.IKKE_RELEVANT }

    return begrunnelseGrunnlagPerPerson.mapValues { (person, begrunnelseGrunnlag) ->
        val begrunnelseGrunnlagForPerson = begrunnelseGrunnlag.dennePerioden

        val endretUtbetalingAndelDennePeriodenForPerson = begrunnelseGrunnlagForPerson.endretUtbetalingAndel

        val oppfylteVilkårresultater =
            begrunnelseGrunnlagForPerson.vilkårResultater.filter { it.resultat == Resultat.OPPFYLT }.toList()

        val standardbegrunnelseSomMatcherVilkår =
            relevanteStandardbegrunnelser
                .filterValues { it.erGjeldendeForRolle(person, fagsakType) }
                .filterValues { it.erLikVilkårOgUtdypendeVilkårIPeriode(oppfylteVilkårresultater) }

        val standardbegrunnelseSomMatcherEndretUtbetalingAndel =
            relevanteStandardbegrunnelser
                .filterValues { it.erGjeldendeForRolle(person, fagsakType) }
                .filterValues { it.erLikEndretUtbetalingIPeriode(endretUtbetalingAndelDennePeriodenForPerson) }

        val eøsBegrunnelserSomMatcherKompetanse =
            relevanteEøsBegrunnelser
                .filterValues { it.erLikKompetanseIPeriode(begrunnelseGrunnlag) }

        standardbegrunnelseSomMatcherVilkår.keys.toSet() +
            standardbegrunnelseSomMatcherEndretUtbetalingAndel.keys.toSet() +
            eøsBegrunnelserSomMatcherKompetanse.keys.toSet()
    }
}
