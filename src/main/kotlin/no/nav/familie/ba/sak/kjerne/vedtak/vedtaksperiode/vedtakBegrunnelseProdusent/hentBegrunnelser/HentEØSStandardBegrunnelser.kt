package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.hentBegrunnelser

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityPeriodeResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.Valgbarhet
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.IBegrunnelseGrunnlagForPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.TemaerForBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.erGjeldendeForBrevPeriodeType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.erGjeldendeForReduksjonFraForrigeBehandling
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.erGjeldendeForUtgjørendeVilkår
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.erLikKompetanseIPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.erLikVilkårOgUtdypendeVilkårIPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.erSammeTemaSomPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.filtrerPåEndretUtbetaling
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.matcherErAutomatisk
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.skalFiltreresPåHendelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.skalVisesSelvOmIkkeEndring
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.VilkårResultatForVedtaksperiode

internal fun hentEØSStandardBegrunnelser(
    vedtaksperiode: VedtaksperiodeMedBegrunnelser,
    sanityEØSBegrunnelser: Map<EØSStandardbegrunnelse, SanityEØSBegrunnelse>,
    behandling: Behandling,
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
    relevantePeriodeResultater: List<SanityPeriodeResultat>,
    erUtbetalingEllerDeltBostedIPeriode: Boolean,
    utvidetVilkårPåSøkerIPeriode: VilkårResultatForVedtaksperiode?,
    utvidetVilkårPåSøkerIForrigePeriode: VilkårResultatForVedtaksperiode?,
    temaSomPeriodeErVurdertEtter: TemaerForBegrunnelser,
): Set<IVedtakBegrunnelse> {
    val endretUtbetalingDennePerioden = hentEndretUtbetalingDennePerioden(begrunnelseGrunnlag)

    val filtrertPåManuelleBegrunnelser =
        sanityEØSBegrunnelser
            .filterValues { it.periodeResultat in relevantePeriodeResultater || it.periodeResultat == SanityPeriodeResultat.IKKE_RELEVANT }
            .filterValues { it.erGjeldendeForBrevPeriodeType(vedtaksperiode, erUtbetalingEllerDeltBostedIPeriode) }
            .filterValues { it.matcherErAutomatisk(behandling.skalBehandlesAutomatisk) }
            .filterValues { it.erSammeTemaSomPeriode(temaSomPeriodeErVurdertEtter) }

    val filtrertPåEndretVilkår =
        filtrertPåManuelleBegrunnelser.filterValues {
            it.erGjeldendeForUtgjørendeVilkår(
                begrunnelseGrunnlag,
                utvidetVilkårPåSøkerIPeriode,
                utvidetVilkårPåSøkerIForrigePeriode,
                vedtaksperiode,
            )
        }

    val filtrertPåEndretKompetanseValutakursOgUtenlandskperiodeBeløp =
        filtrertPåManuelleBegrunnelser.filterValues { begrunnelse ->
            val endringIKompetanseValutakursEllerUtenlandskPeriodebeløp =
                erEndringIKompetanse(begrunnelseGrunnlag) ||
                    erEndringIValutakurs(begrunnelseGrunnlag) ||
                    erEndringIUtenlandskPeriodebeløp(
                        begrunnelseGrunnlag,
                    )

            endringIKompetanseValutakursEllerUtenlandskPeriodebeløp && begrunnelse.erLikKompetanseIPeriode(begrunnelseGrunnlag)
        }

    val filtrertPåIngenEndringMedLikKompetanse =
        filtrertPåManuelleBegrunnelser.filterValues {
            SanityPeriodeResultat.INGEN_ENDRING in relevantePeriodeResultater &&
                it.erLikKompetanseIPeriode(
                    begrunnelseGrunnlag,
                )
        }

    val filtrertPåUendretKompetanseEtterEndretUtbetalingAndel =
        filtrertPåManuelleBegrunnelser.filterValues {
            it.erLikKompetanseIPeriode(
                begrunnelseGrunnlag,
            ) &&
                begrunnelseGrunnlag.dennePerioden.endretUtbetalingAndel == null &&
                begrunnelseGrunnlag.forrigePeriode?.endretUtbetalingAndel != null
        }

    val oppfylteVilkårDennePerioden =
        begrunnelseGrunnlag.dennePerioden.vilkårResultater.filter { it.resultat == Resultat.OPPFYLT }

    val filtrertPåTilleggstekstMedLikKompetanseEllerVilkår =
        filtrertPåManuelleBegrunnelser.filterValues {
            it.valgbarhet == Valgbarhet.TILLEGGSTEKST &&
                (
                    it.erLikKompetanseIPeriode(begrunnelseGrunnlag) ||
                        it.erLikVilkårOgUtdypendeVilkårIPeriode(oppfylteVilkårDennePerioden)
                )
        }

    val filtrertPåReduksjonFraForrigeBehandling =
        filtrertPåManuelleBegrunnelser.filterValues {
            it.erGjeldendeForReduksjonFraForrigeBehandling(begrunnelseGrunnlag)
        }

    val filtrertPåOpphørFraForrigeBehandling =
        filtrertPåManuelleBegrunnelser.filterValues {
            it.erGjeldendeForOpphørFraForrigeBehandling(begrunnelseGrunnlag)
        }

    val filtrertPåHendelser =
        filtrertPåEndretVilkår.filterValues {
            it.skalFiltreresPåHendelser(begrunnelseGrunnlag, vedtaksperiode.fom, vedtaksperiode.tom)
        }

    val filtrertPåSkalVisesSelvOmIkkeEndring =
        filtrertPåEndretVilkår.filterValues {
            it.skalVisesSelvOmIkkeEndring(begrunnelseGrunnlag.dennePerioden)
        }

    val filtrertPåEndretUtbetaling =
        filtrertPåManuelleBegrunnelser.filterValues {
            filtrerPåEndretUtbetaling(it, endretUtbetalingDennePerioden)
        }

    return filtrertPåEndretVilkår.keys +
        filtrertPåEndretKompetanseValutakursOgUtenlandskperiodeBeløp.keys +
        filtrertPåIngenEndringMedLikKompetanse.keys +
        filtrertPåUendretKompetanseEtterEndretUtbetalingAndel.keys +
        filtrertPåTilleggstekstMedLikKompetanseEllerVilkår.keys +
        filtrertPåSkalVisesSelvOmIkkeEndring.keys +
        filtrertPåHendelser.keys +
        filtrertPåOpphørFraForrigeBehandling.keys +
        filtrertPåReduksjonFraForrigeBehandling.keys +
        filtrertPåEndretUtbetaling.keys
}

private fun erEndringIKompetanse(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode) = begrunnelseGrunnlag.dennePerioden.kompetanse != begrunnelseGrunnlag.forrigePeriode?.kompetanse

private fun erEndringIValutakurs(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode) = begrunnelseGrunnlag.dennePerioden.valutakurs != begrunnelseGrunnlag.forrigePeriode?.valutakurs

private fun erEndringIUtenlandskPeriodebeløp(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode) = begrunnelseGrunnlag.dennePerioden.utenlandskPeriodebeløp != begrunnelseGrunnlag.forrigePeriode?.utenlandskPeriodebeløp

private fun hentEndretUtbetalingDennePerioden(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode) = begrunnelseGrunnlag.dennePerioden.endretUtbetalingAndel.takeIf { begrunnelseGrunnlag.dennePerioden.erOrdinæreVilkårInnvilget() }
