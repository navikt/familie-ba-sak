package no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
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
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.forlengFremtidTilUendelig
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.mapVerdi
import no.nav.familie.tidslinje.tomTidslinje
import no.nav.familie.tidslinje.utvidelser.filtrer
import no.nav.familie.tidslinje.utvidelser.filtrerIkkeNull
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.leftJoin
import no.nav.familie.tidslinje.utvidelser.outerJoin
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

        val endretUtbetalingAndeler = endretUtbetalingAndelHentOgPersisterService.hentForBehandling(behandlingId.id)

        val endredeUtbetalingPerioderSomKreverKompetanse =
            utbetalingTidslinjeService.hentEndredeUtbetalingsPerioderSomKreverKompetanseTidslinjer(behandlingId = behandlingId, endretUtbetalingAndeler = endretUtbetalingAndeler)

        val oppdaterteKompetanser =
            tilpassKompetanserTilRegelverk(
                gjeldendeKompetanser = gjeldendeKompetanser,
                barnaRegelverkTidslinjer = barnasRegelverkResultatTidslinjer,
                endredeUtbetalingPerioderSomKreverKompetanseTidlinjer = endredeUtbetalingPerioderSomKreverKompetanse,
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

        val endredeUtbetalingPerioderSomKreverKompetanse =
            utbetalingTidslinjeService.hentEndredeUtbetalingsPerioderSomKreverKompetanseTidslinjer(behandlingId = behandlingId, endretUtbetalingAndeler = endretUtbetalingAndeler)

        val oppdaterteKompetanser =
            tilpassKompetanserTilRegelverk(
                gjeldendeKompetanser = gjeldendeKompetanser,
                barnaRegelverkTidslinjer = barnasRegelverkResultatTidslinjer,
                endredeUtbetalingPerioderSomKreverKompetanseTidlinjer = endredeUtbetalingPerioderSomKreverKompetanse,
                annenForelderOmfattetAvNorskLovgivningTidslinje = annenForelderOmfattetAvNorskLovgivningTidslinje,
                inneværendeMåned = YearMonth.now(clockProvider.get()),
            ).medBehandlingId(behandlingId)

        skjemaService.lagreDifferanseOgVarsleAbonnenter(behandlingId, gjeldendeKompetanser, oppdaterteKompetanser)
    }
}

fun tilpassKompetanserTilRegelverk(
    gjeldendeKompetanser: Collection<Kompetanse>,
    barnaRegelverkTidslinjer: Map<Aktør, Tidslinje<RegelverkResultat>>,
    endredeUtbetalingPerioderSomKreverKompetanseTidlinjer: Map<Aktør, Tidslinje<Boolean>>,
    annenForelderOmfattetAvNorskLovgivningTidslinje: Tidslinje<Boolean> = tomTidslinje(),
    inneværendeMåned: YearMonth,
): Collection<Kompetanse> {
    val barnasEøsRegelverkTidslinjer =
        barnaRegelverkTidslinjer
            .tilBarnasEøsRegelverkTidslinjer()
            .leftJoin(endredeUtbetalingPerioderSomKreverKompetanseTidlinjer) { regelverk, endretUtbetalingPeriodeSomKreverKompetanse ->
                when (endretUtbetalingPeriodeSomKreverKompetanse) {
                    false -> null

                    // Endrede utbetalingsperioder fører til at vi ikke krever kompetanse
                    else -> regelverk // Ingen endrede utbetalingsperioder eller de endrede utbetalingsperiodene fører til at vi krever kompetanse
                }
            }

    return gjeldendeKompetanser
        .tilSeparateTidslinjerForBarna()
        .outerJoin(barnasEøsRegelverkTidslinjer) { kompetanse, eøsRegelverk ->
            eøsRegelverk?.let { kompetanse ?: Kompetanse.NULL }
        }.mapValues { (_, value) ->
            value.kombinerMed(annenForelderOmfattetAvNorskLovgivningTidslinje) { kompetanse, annenForelderOmfattet ->
                kompetanse?.copy(erAnnenForelderOmfattetAvNorskLovgivning = annenForelderOmfattet ?: false)
            }
        }.mapValues { (_, tidslinje) ->
            tidslinje.forlengFremtidTilUendelig(tidspunktForUendelighet = inneværendeMåned.sisteDagIInneværendeMåned())
        }.tilSkjemaer()
}

fun VilkårsvurderingTidslinjeService.hentBarnasRegelverkResultatTidslinjer(behandlingId: BehandlingId) =
    this
        .hentTidslinjerThrows(behandlingId)
        .barnasTidslinjer()
        .mapValues { (_, tidslinjer) ->
            tidslinjer.regelverkResultatTidslinje
        }

private fun Map<Aktør, Tidslinje<RegelverkResultat>>.tilBarnasEøsRegelverkTidslinjer(): Map<Aktør, Tidslinje<Regelverk>> =
    this.mapValues { (_, regelverkResultatTidslinje) ->
        regelverkResultatTidslinje
            .mapVerdi { it?.regelverk }
            .filtrer { it == Regelverk.EØS_FORORDNINGEN }
            .filtrerIkkeNull()
    }
