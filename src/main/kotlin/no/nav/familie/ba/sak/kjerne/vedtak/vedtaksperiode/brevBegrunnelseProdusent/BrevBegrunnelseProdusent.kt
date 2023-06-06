package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.brevBegrunnelseProdusent

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.periodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.AktørId
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.GrunnlagForPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.GrunnlagForVedtaksperioder
import java.time.YearMonth

fun UtvidetVedtaksperiodeMedBegrunnelser.hentGyldigeBegrunnelserForPeriode(
    grunnlagForVedtaksperioder: GrunnlagForVedtaksperioder,
    grunnlagForVedtaksperioderForrigeBehandling: GrunnlagForVedtaksperioder?,
    sanityBegrunnelser: Map<Standardbegrunnelse, SanityBegrunnelse>,
): List<IVedtakBegrunnelse> {
    val begrunnelseGrunnlagPerPerson: Map<AktørId, Periode<BegrunnelseGrunnlag, Måned>> =
        finnBegrunnelseGrunnlagPerPerson(
            grunnlagForVedtaksperioder,
            grunnlagForVedtaksperioderForrigeBehandling,
        )

    return emptyList()
}

private fun UtvidetVedtaksperiodeMedBegrunnelser.finnBegrunnelseGrunnlagPerPerson(
    grunnlagForVedtaksperioder: GrunnlagForVedtaksperioder,
    grunnlagForVedtaksperioderForrigeBehandling: GrunnlagForVedtaksperioder?,
): Map<AktørId, Periode<BegrunnelseGrunnlag, Måned>> {
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

        grunnlagMedForrigePeriodeOgBehandlingTidslinje.perioder().single { it.innhold != null }
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
