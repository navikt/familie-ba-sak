package no.nav.familie.ba.sak.kjerne.eøs.felles.beregning

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjema
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.AktørSkjemaTidslinje
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerForAlleNøklerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned

fun <S : PeriodeOgBarnSkjema<S>, I> tilpassSkjemaerTilTidslinjer(
    gjeldendeSkjemaer: Collection<S>,
    barnasTidslinjer: Map<Aktør, Tidslinje<I, Måned>>,
    tomtSkjemForBarnFactory: (Aktør) -> S
): Collection<S> {
    val skjemaerUtenOverflødige = fjernOverflødigeSkjemaerRekursivt(gjeldendeSkjemaer, barnasTidslinjer)

    val barnTilSkjemaTidslinje = skjemaerUtenOverflødige.tilTidslinjerForBarna()

    val manglendeSkjemaer = barnTilSkjemaTidslinje
        .kombinerForAlleNøklerMed(barnasTidslinjer) { barn: Aktør ->
            { skjema: S?, innhold: I? ->
                if (innhold != null && skjema == null) tomtSkjemForBarnFactory(barn) else null
            }
        }
        .slåSammen()

    return (skjemaerUtenOverflødige + manglendeSkjemaer).slåSammen()
}

fun <S : PeriodeOgBarnSkjema<S>, I> fjernOverflødigeSkjemaerRekursivt(
    skjemaer: Collection<S>,
    barnasTidslinjer: Map<Aktør, Tidslinje<I, Måned>>,
): Collection<S> {
    val barnTilSkjemaTidslinje = skjemaer.tilTidslinjerForBarna()

    val overflødigeSkjemaer = barnTilSkjemaTidslinje
        .kombinerForAlleNøklerMed(barnasTidslinjer) {
            { skjema: S?, innhold: I? -> if (innhold == null && skjema != null) skjema else null }
        }
        .slåSammen()

    return if (overflødigeSkjemaer.isNotEmpty()) {
        val skjemaerFratrukketOverflødige = skjemaer.trekkFra(overflødigeSkjemaer.first())
        fjernOverflødigeSkjemaerRekursivt(
            skjemaerFratrukketOverflødige,
            barnasTidslinjer
        )
    } else {
        skjemaer
    }
}

fun <S : PeriodeOgBarnSkjema<S>> Iterable<S>.tilTidslinjerForBarna(): Map<Aktør, Tidslinje<S, Måned>> {
    if (this.toList().isEmpty()) return emptyMap()

    val alleBarnAktørIder = this.map { it.barnAktører }.reduce { akk, neste -> akk + neste }

    return alleBarnAktørIder.associateWith { aktør ->
        AktørSkjemaTidslinje(aktør, this.filter { it.barnAktører.contains(aktør) })
    }
}

private fun <S : PeriodeOgBarnSkjema<S>> Map<Aktør, Tidslinje<S, Måned>>.slåSammen() =
    this.flatMap { (_, tidslinjer) -> tidslinjer.tilSkjemaer() }
        .slåSammen()

private fun <S : PeriodeOgBarnSkjema<S>> Tidslinje<S, Måned>.tilSkjemaer() =
    this.perioder().mapNotNull { periode ->
        periode.innhold?.settFomOgTom(periode)
    }
