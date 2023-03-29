package no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingId
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelerOppdatertAbonnent
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEndringAbonnent
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaService
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSkjemaer
import no.nav.familie.ba.sak.kjerne.eøs.felles.medBehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.replaceLast
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.EndretUtbetalingAndelTidslinjeService
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjeService
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.tilBarnasHarEtterbetaling3ÅrTidslinjer
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrer
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.fraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.leftJoin
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.outerJoin
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.tilOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilpassKompetanserTilRegelverkService(
    private val vilkårsvurderingTidslinjeService: VilkårsvurderingTidslinjeService,
    private val endretUtbetalingAndelTidslinjeService: EndretUtbetalingAndelTidslinjeService,
    kompetanseRepository: PeriodeOgBarnSkjemaRepository<Kompetanse>,
    endringsabonnenter: Collection<PeriodeOgBarnSkjemaEndringAbonnent<Kompetanse>>
) {
    val skjemaService = PeriodeOgBarnSkjemaService(
        kompetanseRepository,
        endringsabonnenter
    )

    @Transactional
    fun tilpassKompetanserTilRegelverk(behandlingId: BehandlingId) {
        val gjeldendeKompetanser = skjemaService.hentMedBehandlingId(behandlingId)
        val barnasRegelverkResultatTidslinjer =
            vilkårsvurderingTidslinjeService.hentBarnasRegelverkResultatTidslinjer(behandlingId)

        val barnasHarEtterbetaling3ÅrTidslinjer =
            endretUtbetalingAndelTidslinjeService.hentBarnasHarEtterbetaling3ÅrTidslinjer(behandlingId)

        val oppdaterteKompetanser = tilpassKompetanserTilRegelverk(
            gjeldendeKompetanser,
            barnasRegelverkResultatTidslinjer,
            barnasHarEtterbetaling3ÅrTidslinjer
        ).medBehandlingId(behandlingId)

        skjemaService.lagreDifferanseOgVarsleAbonnenter(behandlingId, gjeldendeKompetanser, oppdaterteKompetanser)
    }
}

@Service
class TilpassKompetanserTilEndretUtebetalingAndelerService(
    private val vilkårsvurderingTidslinjeService: VilkårsvurderingTidslinjeService,
    kompetanseRepository: PeriodeOgBarnSkjemaRepository<Kompetanse>,
    endringsabonnenter: Collection<PeriodeOgBarnSkjemaEndringAbonnent<Kompetanse>>
) : EndretUtbetalingAndelerOppdatertAbonnent {
    val skjemaService = PeriodeOgBarnSkjemaService(
        kompetanseRepository,
        endringsabonnenter
    )

    @Transactional
    override fun endretUtbetalingAndelerOppdatert(
        behandlingId: BehandlingId,
        endretUtbetalingAndeler: List<EndretUtbetalingAndel>
    ) {
        val behandlingId = behandlingId
        val gjeldendeKompetanser = skjemaService.hentMedBehandlingId(behandlingId)
        val barnasRegelverkResultatTidslinjer =
            vilkårsvurderingTidslinjeService.hentBarnasRegelverkResultatTidslinjer(behandlingId)

        val barnasHarEtterbetaling3ÅrTidslinjer = endretUtbetalingAndeler
            .tilBarnasHarEtterbetaling3ÅrTidslinjer()

        val oppdaterteKompetanser = tilpassKompetanserTilRegelverk(
            gjeldendeKompetanser,
            barnasRegelverkResultatTidslinjer,
            barnasHarEtterbetaling3ÅrTidslinjer
        ).medBehandlingId(behandlingId)

        skjemaService.lagreDifferanseOgVarsleAbonnenter(behandlingId, gjeldendeKompetanser, oppdaterteKompetanser)
    }
}

fun tilpassKompetanserTilRegelverk(
    gjeldendeKompetanser: Collection<Kompetanse>,
    barnaRegelverkTidslinjer: Map<Aktør, Tidslinje<RegelverkResultat, Måned>>,
    barnasHarEtterbetaling3ÅrTidslinjer: Map<Aktør, Tidslinje<Boolean, Måned>>
): Collection<Kompetanse> {
    val barnasEøsRegelverkTidslinjer = barnaRegelverkTidslinjer.tilBarnasEøsRegelverkTidslinjer()
        .leftJoin(barnasHarEtterbetaling3ÅrTidslinjer) { regelverk, harEtterbetaling3År ->
            when (harEtterbetaling3År) {
                true -> null // ta bort regelverk hvis barnet har etterbetaling 3 år
                else -> regelverk
            }
        }

    return gjeldendeKompetanser.tilSeparateTidslinjerForBarna()
        .outerJoin(barnasEøsRegelverkTidslinjer) { kompetanse, regelverk ->
            regelverk?.let { kompetanse ?: Kompetanse.NULL }
        }
        .tilSkjemaer()
}

fun VilkårsvurderingTidslinjeService.hentBarnasRegelverkResultatTidslinjer(behandlingId: BehandlingId): Map<Aktør, Tidslinje<RegelverkResultat, Måned>> =
    this.hentTidslinjerThrows(behandlingId).barnasTidslinjer()
        .mapValues { (_, tidslinjer) ->
            tidslinjer.regelverkResultatTidslinje
        }

private fun Map<Aktør, Tidslinje<RegelverkResultat, Måned>>.tilBarnasEøsRegelverkTidslinjer() =
    this.mapValues { (_, tidslinjer) ->
        tidslinjer.map { it?.regelverk }
            .filtrer { it == Regelverk.EØS_FORORDNINGEN }
            .filtrerIkkeNull()
            .forlengFremtidTilUendelig(MånedTidspunkt.nå())
    }

private fun <I, T : Tidsenhet> Tidslinje<I, T>.forlengFremtidTilUendelig(nå: Tidspunkt<T>): Tidslinje<I, T> {
    val tilOgMed = this.tilOgMed()
    return if (tilOgMed != null && tilOgMed > nå) {
        this.flyttTilOgMed(tilOgMed.somUendeligLengeTil())
    } else {
        this
    }
}

private fun <I, T : Tidsenhet> Tidslinje<I, T>.flyttTilOgMed(tilTidspunkt: Tidspunkt<T>): Tidslinje<I, T> {
    val tidslinje = this
    val fraOgMed = tidslinje.fraOgMed()

    return if (fraOgMed == null || tilTidspunkt < fraOgMed) {
        TomTidslinje()
    } else {
        object : Tidslinje<I, T>() {
            override fun lagPerioder(): Collection<Periode<I, T>> = tidslinje.perioder()
                .filter { it.fraOgMed <= tilTidspunkt }
                .replaceLast { Periode(it.fraOgMed, tilTidspunkt, it.innhold) }
        }
    }
}
