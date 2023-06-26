package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.brevBegrunnelseProdusent

import no.nav.familie.ba.sak.common.Feil
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
import no.nav.familie.ba.sak.kjerne.tidslinje.månedPeriodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.periodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt
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
): Set<Vilkår> {
    val vilkårForPerson = Vilkår.hentVilkårFor(
        personType = aktør.tilPerson(grunnlagForVedtaksperioder.persongrunnlag)?.type
            ?: error("Har ikke persongrunnlag for person"),
        fagsakType = fagsakType,
        behandlingUnderkategori = behandlingUnderkategori,
    )

    return when (begrunnelseGrunnlag) {
        is BegrunnelseGrunnlagMedVerdiIDennePerioden -> finnUtgjørendeVilkår(
            begrunnelseGrunnlag,
            vilkårForPerson,
        )

        is BegrunnelseGrunnlagIngenVerdiIDenneBehandlingen -> {
            vilkårForPerson
        }
    }
}

private fun finnUtgjørendeVilkår(
    begrunnelseGrunnlag: BegrunnelseGrunnlagMedVerdiIDennePerioden,
    vilkårForPerson: Set<Vilkår>,
): Set<Vilkår> =
    if (begrunnelseGrunnlag.grunnlagForVedtaksperiode is GrunnlagForPersonInnvilget) {
        hentVilkårTjent(
            begrunnelseGrunnlag.grunnlagForVedtaksperiode,
            begrunnelseGrunnlag.grunnlagForForrigeVedtaksperiode,
        )
    } else {
        hentVilkårTapt(
            begrunnelseGrunnlag.grunnlagForVedtaksperiode,
            begrunnelseGrunnlag.grunnlagForForrigeVedtaksperiode,
            vilkårForPerson,
        )
    }

private fun hentVilkårTjent(denne: GrunnlagForPerson, forrige: GrunnlagForPerson?): Set<Vilkår> {
    val innvilgedeVilkårDennePerioden =
        denne.hentOppfylteVilkår()

    val innvilgedeVilkårForrigePerioden =
        forrige?.hentOppfylteVilkår() ?: emptySet()

    return innvilgedeVilkårDennePerioden - innvilgedeVilkårForrigePerioden
}

private fun hentVilkårTapt(
    denne: GrunnlagForPerson,
    forrige: GrunnlagForPerson?,
    vilkårForPerson: Set<Vilkår>,
): Set<Vilkår> {
    val manglendeVilkårDennePerioden =
        vilkårForPerson - denne.hentOppfylteVilkår()

    val manglendeVilkårForrigePerioden =
        vilkårForPerson - (forrige?.hentOppfylteVilkår() ?: emptySet())

    return manglendeVilkårDennePerioden - manglendeVilkårForrigePerioden
}

private fun GrunnlagForPerson.hentOppfylteVilkår(): Set<Vilkår> =
    vilkårResultaterForVedtaksperiode.filter { it.resultat == Resultat.OPPFYLT }
        .map { it.vilkårType }
        .toSet()

private fun UtvidetVedtaksperiodeMedBegrunnelser.finnBegrunnelseGrunnlagPerPerson(
    grunnlagForVedtaksperioder: GrunnlagForVedtaksperioder,
    grunnlagForVedtaksperioderForrigeBehandling: GrunnlagForVedtaksperioder?,
): Map<Aktør, BegrunnelseGrunnlag> {
    val tidslinjeMedVedtaksperioden = this.tilTidslinjeForAktuellPeriode()

    val grunnlagTidslinjePerPerson = grunnlagForVedtaksperioder.utledGrunnlagTidslinjePerPerson()
        .mapValues { it.value.copy(grunnlagForPerson = it.value.grunnlagForPerson.fjernOverflødigePerioderPåSlutten()) }

    val grunnlagTidslinjePerPersonForrigeBehandling =
        grunnlagForVedtaksperioderForrigeBehandling?.utledGrunnlagTidslinjePerPerson()

    return grunnlagTidslinjePerPerson.mapValues { (aktørId, grunnlagTidslinje) ->
        val grunnlagMedForrigePeriodeTidslinje =
            grunnlagTidslinje.grunnlagForPerson.tilForrigeOgNåværendePeriodeTidslinje()

        val grunnlagForrigeBehandlingTidslinje =
            grunnlagTidslinjePerPersonForrigeBehandling?.get(aktørId)?.grunnlagForPerson ?: TomTidslinje()

        val grunnlagMedForrigePeriodeOgBehandlingTidslinje = tidslinjeMedVedtaksperioden.kombinerMed(
            grunnlagMedForrigePeriodeTidslinje,
            grunnlagForrigeBehandlingTidslinje,
        ) { vedtaksPerioden, forrigeOgDennePerioden, forrigeBehandling ->
            if (vedtaksPerioden == null) {
                null
            } else {
                lagBegrunnelseGrunnlag(
                    dennePerioden = forrigeOgDennePerioden?.denne,
                    forrigePeriode = forrigeOgDennePerioden?.forrige,
                    sammePeriodeForrigeBehandling = forrigeBehandling,
                )
            }
        }

        grunnlagMedForrigePeriodeOgBehandlingTidslinje.perioder().mapNotNull { it.innhold }.single()
    }
}

private fun Tidslinje<GrunnlagForPerson, Måned>.fjernOverflødigePerioderPåSlutten(): Tidslinje<GrunnlagForPerson, Måned> {
    val sortedByFom = this.perioder()
        .sortedWith(compareBy({ it.fraOgMed }, { it.tilOgMed }))

    val indexForSisteInnvilgetePeriode = sortedByFom.indexOfLast { periode ->
        periode.innhold ?: throw Feil("Innhold er null for periode ${periode.fraOgMed} - ${periode.tilOgMed}")
        periode.innhold is GrunnlagForPersonInnvilget
    }

    return when (indexForSisteInnvilgetePeriode) {
        -1 -> {
            // Har ingen innvilgete perioder
            sortedByFom
        }

        sortedByFom.size - 1 -> {
            // Har kun innvilgete perioder
            sortedByFom
        }

        else -> {
            val sisteIkkeInnvilgetePeriodeSomSkalBegrunnes =
                sortedByFom[indexForSisteInnvilgetePeriode + 1].copy(tilOgMed = MånedTidspunkt.uendeligLengeTil())
            sortedByFom.subList(0, indexForSisteInnvilgetePeriode + 1) + sisteIkkeInnvilgetePeriodeSomSkalBegrunnes
        }
    }.tilTidslinje()
}

private fun UtvidetVedtaksperiodeMedBegrunnelser.tilTidslinjeForAktuellPeriode(): Tidslinje<UtvidetVedtaksperiodeMedBegrunnelser, Måned> {
    return listOf(
        månedPeriodeAv(
            fraOgMed = this.fom?.toYearMonth(),
            tilOgMed = this.tom?.toYearMonth(),
            innhold = this,
        ),
    ).tilTidslinje()
}

data class ForrigeOgDennePerioden(val forrige: GrunnlagForPerson?, val denne: GrunnlagForPerson?)

private fun Tidslinje<GrunnlagForPerson, Måned>.tilForrigeOgNåværendePeriodeTidslinje(): Tidslinje<ForrigeOgDennePerioden, Måned> {
    return (
        listOf(
            månedPeriodeAv(YearMonth.now(), YearMonth.now(), null),
        ) + this.perioder()
        ).zipWithNext { forrige, denne ->
        periodeAv(denne.fraOgMed, denne.tilOgMed, ForrigeOgDennePerioden(forrige.innhold, denne.innhold))
    }.tilTidslinje()
}
