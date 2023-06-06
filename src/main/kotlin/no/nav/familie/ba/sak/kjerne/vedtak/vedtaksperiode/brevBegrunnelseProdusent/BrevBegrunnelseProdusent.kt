package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.brevBegrunnelseProdusent

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.tilPerson
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.periodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.GrunnlagForPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.GrunnlagForPersonInnvilget
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.GrunnlagForVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import java.time.YearMonth

fun UtvidetVedtaksperiodeMedBegrunnelser.hentGyldigeBegrunnelserForPeriode(
    grunnlagForVedtaksperioder: GrunnlagForVedtaksperioder,
    grunnlagForVedtaksperioderForrigeBehandling: GrunnlagForVedtaksperioder?,
    sanityBegrunnelser: Map<Standardbegrunnelse, SanityBegrunnelse>,
    behandlingUnderkategori: BehandlingUnderkategori,
    fagsakType: FagsakType,
): Set<Standardbegrunnelse> {
    val gyldigeBegrunnelserPerPerson = hentGyldigeBegrunnelserPerPerson(
        grunnlagForVedtaksperioder = grunnlagForVedtaksperioder,
        grunnlagForVedtaksperioderForrigeBehandling = grunnlagForVedtaksperioderForrigeBehandling,
        fagsakType = fagsakType,
        behandlingUnderkategori = behandlingUnderkategori,
        sanityBegrunnelser = sanityBegrunnelser,
    )

    return gyldigeBegrunnelserPerPerson.values.flatten().toSet()
}

private fun UtvidetVedtaksperiodeMedBegrunnelser.hentGyldigeBegrunnelserPerPerson(
    grunnlagForVedtaksperioder: GrunnlagForVedtaksperioder,
    grunnlagForVedtaksperioderForrigeBehandling: GrunnlagForVedtaksperioder?,
    fagsakType: FagsakType,
    behandlingUnderkategori: BehandlingUnderkategori,
    sanityBegrunnelser: Map<Standardbegrunnelse, SanityBegrunnelse>,
): Map<Aktør, Set<Standardbegrunnelse>> {
    val begrunnelseGrunnlagPerPerson =
        finnBegrunnelseGrunnlagPerPerson(
            grunnlagForVedtaksperioder,
            grunnlagForVedtaksperioderForrigeBehandling,
        )

    return begrunnelseGrunnlagPerPerson.mapValues { (aktør, begrunnelseGrunnlag) ->
        val utgjørendeVilkår = hentUtgjørendeVilkårForPerson(
            begrunnelseGrunnlag,
            aktør,
            grunnlagForVedtaksperioder,
            fagsakType,
            behandlingUnderkategori,
        )

        val filtrerteBegrunnelser =
            sanityBegrunnelser.filter { (_, sanityBegrunnelse) -> sanityBegrunnelse.vilkår == utgjørendeVilkår }

        filtrerteBegrunnelser.keys
    }
}

private fun hentUtgjørendeVilkårForPerson(
    begrunnelseGrunnlag: BegrunnelseGrunnlag,
    aktør: Aktør,
    grunnlagForVedtaksperioder: GrunnlagForVedtaksperioder,
    fagsakType: FagsakType,
    behandlingUnderkategori: BehandlingUnderkategori,
) = when (begrunnelseGrunnlag) {
    is BegrunnelseGrunnlagMedVerdiIDennePerioden -> finnUtgjørendeVilkår(begrunnelseGrunnlag)
    is BegrunnelseGrunnlagIngenVerdiIDenneBehandlingen -> Vilkår.hentVilkårFor(
        personType = aktør.tilPerson(grunnlagForVedtaksperioder.persongrunnlag)?.type
            ?: error("Har ikke persongrunnlag for person"),
        fagsakType = fagsakType,
        behandlingUnderkategori = behandlingUnderkategori,
    )
}

private fun finnUtgjørendeVilkår(begrunnelseGrunnlag: BegrunnelseGrunnlagMedVerdiIDennePerioden): Set<Vilkår> =
    if (begrunnelseGrunnlag.grunnlagForVedtaksperiode is GrunnlagForPersonInnvilget) {
        hentVilkårTjent(
            begrunnelseGrunnlag.grunnlagForVedtaksperiode,
            begrunnelseGrunnlag.grunnlagForForrigeVedtaksperiode,
        )
    } else {
        hentVilkårTapt(
            begrunnelseGrunnlag.grunnlagForVedtaksperiode,
            begrunnelseGrunnlag.grunnlagForForrigeVedtaksperiode,
        )
    }

private fun hentVilkårTjent(denne: GrunnlagForPerson, forrige: GrunnlagForPerson?): Set<Vilkår> {
    val innvilgedeVilkårDennePerioden =
        denne.vilkårResultaterForVedtaksperiode.filter { it.resultat == Resultat.OPPFYLT }.map { it.vilkårType }.toSet()

    val innvilgedeVilkårForrigePerioden =
        forrige?.vilkårResultaterForVedtaksperiode?.filter { it.resultat == Resultat.OPPFYLT }?.map { it.vilkårType }
            ?.toSet() ?: emptySet()

    return innvilgedeVilkårDennePerioden - innvilgedeVilkårForrigePerioden
}

private fun hentVilkårTapt(denne: GrunnlagForPerson, forrige: GrunnlagForPerson?): Set<Vilkår> {
    val innvilgedeVilkårDennePerioden =
        denne.vilkårResultaterForVedtaksperiode.filter { it.resultat == Resultat.IKKE_OPPFYLT }.map { it.vilkårType }
            .toSet()

    val innvilgedeVilkårForrigePerioden =
        forrige?.vilkårResultaterForVedtaksperiode?.filter { it.resultat == Resultat.IKKE_OPPFYLT }
            ?.map { it.vilkårType }
            ?.toSet() ?: emptySet()

    return innvilgedeVilkårDennePerioden - innvilgedeVilkårForrigePerioden
}

private fun UtvidetVedtaksperiodeMedBegrunnelser.finnBegrunnelseGrunnlagPerPerson(
    grunnlagForVedtaksperioder: GrunnlagForVedtaksperioder,
    grunnlagForVedtaksperioderForrigeBehandling: GrunnlagForVedtaksperioder?,
): Map<Aktør, BegrunnelseGrunnlag> {
    val tidslinjeMedVedtaksperioden = this.tilTidslinjeForAktuellPeriode()

    val grunnlagTidslinjePerPerson = grunnlagForVedtaksperioder.utledGrunnlagTidslinjePerPerson()

    val grunnlagTidslinjePerPersonForrigeBehandling =
        grunnlagForVedtaksperioderForrigeBehandling?.utledGrunnlagTidslinjePerPerson()

    return grunnlagTidslinjePerPerson.mapValues { (aktørId, grunnlagTidslinje) ->
        val grunnlagMedForrigePeriodeTidslinje = grunnlagTidslinje.tilForrigeOgNåværendePeriodeTidslinje()

        val grunnlagForrigeBehandlingTidslinje =
            grunnlagTidslinjePerPersonForrigeBehandling?.get(aktørId) ?: TomTidslinje()

        val grunnlagMedForrigePeriodeOgBehandlingTidslinje = tidslinjeMedVedtaksperioden.kombinerMed(
            grunnlagMedForrigePeriodeTidslinje,
            grunnlagForrigeBehandlingTidslinje,
        ) { vedtaksPerioden, forrigeOgDennePerioden, forrigeBehandling ->
            if (vedtaksPerioden == null) {
                null
            } else {
                val forrigePeriode = forrigeOgDennePerioden?.first
                val dennePerioden = forrigeOgDennePerioden?.second

                lagBegrunnelseGrunnlag(
                    dennePerioden = dennePerioden,
                    forrigePeriode = forrigeBehandling,
                    sammePeriodeForrigeBehandling = forrigePeriode,
                )
            }
        }

        grunnlagMedForrigePeriodeOgBehandlingTidslinje.perioder().mapNotNull { it.innhold }.single()
    }
}

private fun UtvidetVedtaksperiodeMedBegrunnelser.tilTidslinjeForAktuellPeriode(): Tidslinje<UtvidetVedtaksperiodeMedBegrunnelser, Måned> {
    return listOf(
        periodeAv(
            fraOgMed = this.fom?.toYearMonth(),
            tilOgMed = this.tom?.toYearMonth(),
            innhold = this,
        ),
    ).tilTidslinje()
}

private fun Tidslinje<GrunnlagForPerson, Måned>.tilForrigeOgNåværendePeriodeTidslinje(): Tidslinje<Pair<GrunnlagForPerson?, GrunnlagForPerson?>, Måned> {
    return (
        listOf(
            periodeAv(YearMonth.now(), YearMonth.now(), null),
        ) + this.perioder()
        ).zipWithNext { forrige, denne ->
            periodeAv(denne.fraOgMed, denne.tilOgMed, Pair(forrige.innhold, denne.innhold))
        }.tilTidslinje()
}
