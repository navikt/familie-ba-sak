package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.hentBegrunnelser

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.brev.domene.ISanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityPeriodeResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.Valgbarhet
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.IBegrunnelseGrunnlagForPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.begrunnelseGjelderOpphørFraForrigeBehandling
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.begrunnelseGjelderReduksjonFraForrigeBehandling
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.erGjeldendeForBrevPeriodeType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.erGjeldendeForUtgjørendeVilkår
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.erLikKompetanseIPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.filtrerPåHendelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.filtrerPåSkalVisesSelvOmIkkeEndring
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.matcherErAutomatisk
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
): Map<IVedtakBegrunnelse, ISanityBegrunnelse> {
    val begrunnelserFiltrertPåPeriodetype =
        sanityEØSBegrunnelser.filterValues {
            it.periodeResultat in relevantePeriodeResultater
        }

    val begrunnelserFiltrertPåPerioderesultatOgBrevPeriodeType =
        begrunnelserFiltrertPåPeriodetype
            .filterValues { it.erGjeldendeForBrevPeriodeType(vedtaksperiode, erUtbetalingEllerDeltBostedIPeriode) }

    val filtrertPåManuelleBegrunnelser =
        begrunnelserFiltrertPåPerioderesultatOgBrevPeriodeType
            .filterValues { it.matcherErAutomatisk(behandling.skalBehandlesAutomatisk) }

    val relevanteBegrunnelser =
        filtrertPåManuelleBegrunnelser
            .filterValues { !it.begrunnelseGjelderReduksjonFraForrigeBehandling() && !it.begrunnelseGjelderOpphørFraForrigeBehandling() }

    val filtrertPåEndretVilkår =
        filtrertPåManuelleBegrunnelser.filterValues {
            it.erGjeldendeForUtgjørendeVilkår(
                begrunnelseGrunnlag,
                utvidetVilkårPåSøkerIPeriode,
                utvidetVilkårPåSøkerIForrigePeriode,
            )
        }

    val filtrertPåEndretKompetanseValutakursOgUtenlandskperiodeBeløp =
        begrunnelserFiltrertPåPerioderesultatOgBrevPeriodeType.filterValues { begrunnelse ->
            val endringIKompetanseValutakursEllerUtenlandskPeriodebeløp =
                erEndringIKompetanse(begrunnelseGrunnlag) || erEndringIValutakurs(begrunnelseGrunnlag) ||
                    erEndringIUtenlandskPeriodebeløp(
                        begrunnelseGrunnlag,
                    )

            endringIKompetanseValutakursEllerUtenlandskPeriodebeløp && begrunnelse.erLikKompetanseIPeriode(begrunnelseGrunnlag)
        }

    val filtrertPåIngenEndringMedLikKompetanse =
        begrunnelserFiltrertPåPerioderesultatOgBrevPeriodeType.filterValues {
            SanityPeriodeResultat.INGEN_ENDRING in relevantePeriodeResultater &&
                it.erLikKompetanseIPeriode(
                    begrunnelseGrunnlag,
                )
        }

    val filtrertPåTilleggstekstMedLikKompetanse =
        begrunnelserFiltrertPåPerioderesultatOgBrevPeriodeType.filterValues {
            it.valgbarhet == Valgbarhet.TILLEGGSTEKST &&
                it.erLikKompetanseIPeriode(
                    begrunnelseGrunnlag,
                )
        }

    @Suppress("UNCHECKED_CAST")
    val filtrertPåHendelser =
        (relevanteBegrunnelser as Map<IVedtakBegrunnelse, ISanityBegrunnelse>).filtrerPåHendelser(
            begrunnelseGrunnlag,
            vedtaksperiode.fom,
        )

    @Suppress("UNCHECKED_CAST")
    val filtrertPåSkalVisesSelvOmIkkeEndring =
        (relevanteBegrunnelser as Map<IVedtakBegrunnelse, ISanityBegrunnelse>).filtrerPåSkalVisesSelvOmIkkeEndring(begrunnelseGrunnlag.dennePerioden)

    return filtrertPåEndretVilkår +
        filtrertPåEndretKompetanseValutakursOgUtenlandskperiodeBeløp +
        filtrertPåIngenEndringMedLikKompetanse +
        filtrertPåTilleggstekstMedLikKompetanse +
        filtrertPåSkalVisesSelvOmIkkeEndring +
        filtrertPåHendelser
}

private fun erEndringIKompetanse(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode) =
    begrunnelseGrunnlag.dennePerioden.kompetanse != begrunnelseGrunnlag.forrigePeriode?.kompetanse

private fun erEndringIValutakurs(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode) =
    begrunnelseGrunnlag.dennePerioden.valutakurs != begrunnelseGrunnlag.forrigePeriode?.valutakurs

private fun erEndringIUtenlandskPeriodebeløp(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode) =
    begrunnelseGrunnlag.dennePerioden.utenlandskPeriodebeløp != begrunnelseGrunnlag.forrigePeriode?.utenlandskPeriodebeløp
