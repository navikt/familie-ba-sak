package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.beregning

import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSkjemaer
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilpassTil
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.replaceLast
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjeService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrer
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.fraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tilOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk

fun tilpassKompetanserTilRegelverk(
    gjeldendeKompetanser: Collection<Kompetanse>,
    barnaRegelverkTidslinjer: Map<Aktør, Tidslinje<RegelverkResultat, Måned>>
): Collection<Kompetanse> {
    val barnasEøsRegelverkTidslinjer = barnaRegelverkTidslinjer.tilBarnasEøsRegelverkTidslinjer()
    return gjeldendeKompetanser.tilSeparateTidslinjerForBarna()
        .tilpassTil(barnasEøsRegelverkTidslinjer) { kompetanse, _ -> kompetanse ?: Kompetanse.NULL }
        .tilSkjemaer()
}

fun VilkårsvurderingTidslinjeService.hentBarnasRegelverkResultatTidslinjer(behandlingId: Long): Map<Aktør, Tidslinje<RegelverkResultat, Måned>> =
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
    return if (this.tilOgMed() > nå)
        this.flyttTilOgMed(this.tilOgMed().somUendeligLengeTil())
    else
        this
}

private fun <I, T : Tidsenhet> Tidslinje<I, T>.flyttTilOgMed(tilTidspunkt: Tidspunkt<T>): Tidslinje<I, T> {
    val tidslinje = this

    return if (tilTidspunkt < tidslinje.fraOgMed())
        TomTidslinje()
    else
        object : Tidslinje<I, T>() {
            override fun lagPerioder(): Collection<Periode<I, T>> = tidslinje.perioder()
                .filter { it.fraOgMed <= tilTidspunkt }
                .replaceLast { Periode(it.fraOgMed, tilTidspunkt, it.innhold) }
        }
}
