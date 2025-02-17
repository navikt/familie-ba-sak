package no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelerOppdatertAbonnent
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEndringAbonnent
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaService
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSkjemaer
import no.nav.familie.ba.sak.kjerne.eøs.felles.medBehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.utbetaling.UtbetalingTidslinjeService
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjeService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrer
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.leftJoin
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.outerJoin
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilForrigeMåned
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærTilOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.forlengFremtidTilUendelig
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class TilpassKompetanserTilRegelverkService(
    private val vilkårsvurderingTidslinjeService: VilkårsvurderingTidslinjeService,
    private val utbetalingTidslinjeService: UtbetalingTidslinjeService,
    private val endretUtbetalingAndelHentOgPersisterService: EndretUtbetalingAndelHentOgPersisterService,
    private val clockProvider: ClockProvider,
    kompetanseRepository: PeriodeOgBarnSkjemaRepository<Kompetanse>,
    endringsabonnenter: Collection<PeriodeOgBarnSkjemaEndringAbonnent<Kompetanse>>,
) {
    val skjemaService =
        PeriodeOgBarnSkjemaService(
            kompetanseRepository,
            endringsabonnenter,
        )

    @Transactional
    fun tilpassKompetanserTilRegelverk(behandlingId: BehandlingId) {
        val gjeldendeKompetanser = skjemaService.hentMedBehandlingId(behandlingId)
        val barnasRegelverkResultatTidslinjer =
            vilkårsvurderingTidslinjeService.hentBarnasRegelverkResultatTidslinjer(behandlingId)

        val annenForelderOmfattetAvNorskLovgivningTidslinje =
            vilkårsvurderingTidslinjeService.hentAnnenForelderOmfattetAvNorskLovgivningTidslinje(behandlingId = behandlingId)

        val utbetalesIkkeOrdinærEllerUtvidetTidslinjer =
            utbetalingTidslinjeService.hentUtbetalesIkkeOrdinærEllerUtvidetTidslinjer(
                behandlingId = behandlingId,
                endretUtbetalingAndeler = endretUtbetalingAndelHentOgPersisterService.hentForBehandling(behandlingId.id),
            )

        val oppdaterteKompetanser =
            tilpassKompetanserTilRegelverk(
                gjeldendeKompetanser = gjeldendeKompetanser,
                barnaRegelverkTidslinjer = barnasRegelverkResultatTidslinjer,
                utbetalesIkkeOrdinærEllerUtvidetTidslinjer = utbetalesIkkeOrdinærEllerUtvidetTidslinjer,
                annenForelderOmfattetAvNorskLovgivningTidslinje = annenForelderOmfattetAvNorskLovgivningTidslinje,
                inneværendeMåned = YearMonth.now(clockProvider.get()),
            ).medBehandlingId(behandlingId)

        skjemaService.lagreDifferanseOgVarsleAbonnenter(behandlingId, gjeldendeKompetanser, oppdaterteKompetanser)
    }
}

@Service
class TilpassKompetanserTilEndretUtebetalingAndelerService(
    private val vilkårsvurderingTidslinjeService: VilkårsvurderingTidslinjeService,
    private val utbetalingTidslinjeService: UtbetalingTidslinjeService,
    private val clockProvider: ClockProvider,
    kompetanseRepository: PeriodeOgBarnSkjemaRepository<Kompetanse>,
    endringsabonnenter: Collection<PeriodeOgBarnSkjemaEndringAbonnent<Kompetanse>>,
) : EndretUtbetalingAndelerOppdatertAbonnent {
    val skjemaService =
        PeriodeOgBarnSkjemaService(
            kompetanseRepository,
            endringsabonnenter,
        )

    @Transactional
    override fun endretUtbetalingAndelerOppdatert(
        behandlingId: BehandlingId,
        endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    ) {
        val gjeldendeKompetanser = skjemaService.hentMedBehandlingId(behandlingId)
        val barnasRegelverkResultatTidslinjer =
            vilkårsvurderingTidslinjeService.hentBarnasRegelverkResultatTidslinjer(behandlingId)

        val annenForelderOmfattetAvNorskLovgivningTidslinje =
            vilkårsvurderingTidslinjeService.hentAnnenForelderOmfattetAvNorskLovgivningTidslinje(behandlingId = behandlingId)

        val utbetalesIkkeOrdinærEllerUtvidetTidslinjer =
            utbetalingTidslinjeService.hentUtbetalesIkkeOrdinærEllerUtvidetTidslinjer(behandlingId = behandlingId, endretUtbetalingAndeler = endretUtbetalingAndeler)

        val oppdaterteKompetanser =
            tilpassKompetanserTilRegelverk(
                gjeldendeKompetanser = gjeldendeKompetanser,
                barnaRegelverkTidslinjer = barnasRegelverkResultatTidslinjer,
                utbetalesIkkeOrdinærEllerUtvidetTidslinjer = utbetalesIkkeOrdinærEllerUtvidetTidslinjer,
                annenForelderOmfattetAvNorskLovgivningTidslinje = annenForelderOmfattetAvNorskLovgivningTidslinje,
                inneværendeMåned = YearMonth.now(clockProvider.get()),
            ).medBehandlingId(behandlingId)

        skjemaService.lagreDifferanseOgVarsleAbonnenter(behandlingId, gjeldendeKompetanser, oppdaterteKompetanser)
    }
}

fun tilpassKompetanserTilRegelverk(
    gjeldendeKompetanser: Collection<Kompetanse>,
    barnaRegelverkTidslinjer: Map<Aktør, Tidslinje<RegelverkResultat, Måned>>,
    utbetalesIkkeOrdinærEllerUtvidetTidslinjer: Map<Aktør, Tidslinje<Boolean, Måned>>,
    annenForelderOmfattetAvNorskLovgivningTidslinje: Tidslinje<Boolean, Måned> = TomTidslinje<Boolean, Måned>(),
    inneværendeMåned: YearMonth,
): Collection<Kompetanse> {
    val barnasEøsRegelverkTidslinjer =
        barnaRegelverkTidslinjer
            .tilBarnasEøsRegelverkTidslinjer()
            .leftJoin(utbetalesIkkeOrdinærEllerUtvidetTidslinjer) { regelverk, utbetalesIkkeOrdinærEllerUtvidet ->
                when (utbetalesIkkeOrdinærEllerUtvidet) {
                    true -> null // ta bort regelverk dersom det verken utbetales ordinær på barnet eller utvidet for søker
                    else -> regelverk
                }
            }.mapValues { (_, tidslinjer) ->
                tidslinjer.forlengFremtidTilUendelig(MånedTidspunkt.nå())
            }

    return gjeldendeKompetanser
        .tilSeparateTidslinjerForBarna()
        .outerJoin(barnasEøsRegelverkTidslinjer) { kompetanse, eøsRegelverk ->
            eøsRegelverk?.let { kompetanse ?: Kompetanse.NULL }
        }.mapValues { (_, value) ->
            value.kombinerMed(annenForelderOmfattetAvNorskLovgivningTidslinje) { kompetanse, annenForelderOmfattet ->
                kompetanse?.copy(erAnnenForelderOmfattetAvNorskLovgivning = annenForelderOmfattet ?: false)
            }
        }.mapValues { (_, value) ->
            val nåMåned = inneværendeMåned.tilTidspunkt()
            value
                .beskjærTilOgMed(nåMåned)
                .forlengFremtidTilUendelig(senesteEndeligeTidspunkt = nåMåned.tilForrigeMåned())
        }.tilSkjemaer()
}

fun VilkårsvurderingTidslinjeService.hentBarnasRegelverkResultatTidslinjer(behandlingId: BehandlingId) =
    this
        .hentTidslinjerThrows(behandlingId)
        .barnasTidslinjer()
        .mapValues { (_, tidslinjer) ->
            tidslinjer.regelverkResultatTidslinje
        }

private fun Map<Aktør, Tidslinje<RegelverkResultat, Måned>>.tilBarnasEøsRegelverkTidslinjer(): Map<Aktør, Tidslinje<Regelverk, Måned>> =
    this.mapValues { (_, regelverkResultatTidslinje) ->
        regelverkResultatTidslinje
            .map { it?.regelverk }
            .filtrer { it == Regelverk.EØS_FORORDNINGEN }
            .filtrerIkkeNull()
    }
