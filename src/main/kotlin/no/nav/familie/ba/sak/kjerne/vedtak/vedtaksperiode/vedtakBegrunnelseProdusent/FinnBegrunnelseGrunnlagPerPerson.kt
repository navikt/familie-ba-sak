package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.GrunnlagForBegrunnelse
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MAX_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MIN_MÅNED
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.tomTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import java.time.YearMonth

data class ForrigeOgDennePerioden(
    val forrige: BegrunnelseGrunnlagForPersonIPeriode?,
    val denne: BegrunnelseGrunnlagForPersonIPeriode?,
)

fun VedtaksperiodeMedBegrunnelser.finnBegrunnelseGrunnlagPerPerson(
    grunnlag: GrunnlagForBegrunnelse,
): Map<Person, IBegrunnelseGrunnlagForPeriode> {
    val tidslinjeMedVedtaksperioden = this.tilTidslinjeForAktuellPeriode()

    val begrunnelsegrunnlagTidslinjerPerPerson =
        grunnlag.behandlingsGrunnlagForVedtaksperioder.lagBegrunnelseGrunnlagTidslinjer()

    val grunnlagTidslinjePerPersonForrigeBehandling =
        grunnlag.behandlingsGrunnlagForVedtaksperioderForrigeBehandling?.lagBegrunnelseGrunnlagTidslinjer()

    return begrunnelsegrunnlagTidslinjerPerPerson.mapValues { (person, grunnlagTidslinje) ->
        val grunnlagMedForrigePeriodeOgBehandlingTidslinje =
            tidslinjeMedVedtaksperioden.lagTidslinjeGrunnlagDennePeriodenForrigePeriodeOgPeriodeForrigeBehandling(
                grunnlagTidslinje,
                grunnlagTidslinjePerPersonForrigeBehandling,
                person,
            )

        val begrunnelseperioderIVedtaksperiode =
            grunnlagMedForrigePeriodeOgBehandlingTidslinje.tilPerioder().mapNotNull { it.verdi }

        when (this.type) {
            Vedtaksperiodetype.OPPHØR -> {
                begrunnelseperioderIVedtaksperiode.first()
            }

            Vedtaksperiodetype.FORTSATT_INNVILGET -> {
                if (this.fom == null && this.tom == null) {
                    val perioder = grunnlagMedForrigePeriodeOgBehandlingTidslinje.tilPerioder()
                    perioder.single { grunnlag.nåDato.toYearMonth() in (it.fom?.toYearMonth() ?: MIN_MÅNED)..(it.tom?.toYearMonth() ?: MAX_MÅNED) }.verdi!!
                } else {
                    begrunnelseperioderIVedtaksperiode.first()
                }
            }

            else -> {
                begrunnelseperioderIVedtaksperiode.first()
            }
        }
    }
}

private fun Tidslinje<VedtaksperiodeMedBegrunnelser>.lagTidslinjeGrunnlagDennePeriodenForrigePeriodeOgPeriodeForrigeBehandling(
    grunnlagTidslinje: Tidslinje<BegrunnelseGrunnlagForPersonIPeriode>,
    grunnlagTidslinjePerPersonForrigeBehandling: Map<Person, Tidslinje<BegrunnelseGrunnlagForPersonIPeriode>>?,
    person: Person,
): Tidslinje<IBegrunnelseGrunnlagForPeriode> {
    val grunnlagMedForrigePeriodeTidslinje = grunnlagTidslinje.tilForrigeOgNåværendePeriodeTidslinje(this)

    val grunnlagForrigeBehandlingTidslinje = grunnlagTidslinjePerPersonForrigeBehandling?.get(person) ?: tomTidslinje()

    return this.kombinerMed(
        grunnlagMedForrigePeriodeTidslinje,
        grunnlagForrigeBehandlingTidslinje,
    ) { vedtaksPerioden, forrigeOgDennePerioden, forrigeBehandling ->
        val dennePerioden = forrigeOgDennePerioden?.denne

        if (vedtaksPerioden == null) {
            null
        } else {
            IBegrunnelseGrunnlagForPeriode.opprett(
                dennePerioden = dennePerioden ?: BegrunnelseGrunnlagForPersonIPeriode.tomPeriode(person),
                forrigePeriode = forrigeOgDennePerioden?.forrige,
                sammePeriodeForrigeBehandling = forrigeBehandling,
                periodetype = vedtaksPerioden.type,
            )
        }
    }
}

private fun VedtaksperiodeMedBegrunnelser.tilTidslinjeForAktuellPeriode(): Tidslinje<VedtaksperiodeMedBegrunnelser> =
    listOf(
        Periode(
            verdi = this,
            fom = this.fom?.førsteDagIInneværendeMåned(),
            tom = this.tom?.sisteDagIMåned(),
        ),
    ).tilTidslinje()

private fun Tidslinje<BegrunnelseGrunnlagForPersonIPeriode>.tilForrigeOgNåværendePeriodeTidslinje(
    vedtaksperiodeTidslinje: Tidslinje<VedtaksperiodeMedBegrunnelser>,
): Tidslinje<ForrigeOgDennePerioden> {
    val grunnlagPerioderSplittetPåVedtaksperiode =
        kombinerMed(vedtaksperiodeTidslinje) { grunnlag, periode ->
            Pair(grunnlag, periode)
        }.tilPerioder().map { Periode(it.verdi?.first, it.fom, it.tom) }

    return (
        listOf(
            Periode(
                verdi = null,
                fom = YearMonth.now().førsteDagIInneværendeMåned(),
                tom = YearMonth.now().sisteDagIInneværendeMåned(),
            ),
        ) + grunnlagPerioderSplittetPåVedtaksperiode
    ).zipWithNext { forrige, denne ->
        val innholdForrigePeriode = if (forrige.tom?.nesteMåned() == denne.fom?.toYearMonth()) forrige.verdi else null
        Periode(ForrigeOgDennePerioden(innholdForrigePeriode, denne.verdi), denne.fom, denne.tom)
    }.tilTidslinje()
}
