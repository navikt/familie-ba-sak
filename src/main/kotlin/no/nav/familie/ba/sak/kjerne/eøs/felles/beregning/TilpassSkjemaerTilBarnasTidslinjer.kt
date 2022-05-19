package no.nav.familie.ba.sak.kjerne.eøs.felles.beregning

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjema
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEntitet
import no.nav.familie.ba.sak.kjerne.eøs.felles.medBehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.AktørSkjemaTidslinje
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerForAlleNøklerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned

fun <S : PeriodeOgBarnSkjema<S>> Iterable<S>.tilTidslinjerForBarna(): Map<Aktør, Tidslinje<S, Måned>> {
    if (this.toList().isEmpty()) return emptyMap()

    val alleBarnAktørIder = this.map { it.barnAktører }.reduce { akk, neste -> akk + neste }

    return alleBarnAktørIder.associateWith { aktør ->
        AktørSkjemaTidslinje(aktør, this.filter { it.barnAktører.contains(aktør) })
    }
}

fun <S : PeriodeOgBarnSkjemaEntitet<S>> Map<Aktør, Tidslinje<S, Måned>>.tilSkjemaer(behandlingId: Long) =
    this.flatMap { (aktør, tidslinjer) -> tidslinjer.tilSkjemaer(aktør) }
        .slåSammen().medBehandlingId(behandlingId)

private fun <S : PeriodeOgBarnSkjema<S>> Tidslinje<S, Måned>.tilSkjemaer(aktør: Aktør) =
    this.perioder().mapNotNull { periode ->
        periode.innhold?.kopier(
            fom = periode.fraOgMed.tilYearMonthEllerNull(),
            tom = periode.tilOgMed.tilYearMonthEllerNull(),
            barnAktører = setOf(aktør)
        )
    }

fun <S : PeriodeOgBarnSkjema<S>, I> Map<Aktør, Tidslinje<S, Måned>>.tilpassTil(
    aktørTilTidslinjeMap: Map<Aktør, Tidslinje<I, Måned>>,
    nyttSkjemaFactory: () -> S
): Map<Aktør, Tidslinje<S, Måned>> {
    return this.kombinerForAlleNøklerMed(aktørTilTidslinjeMap) {
        { skjema: S?, innhold: I? ->
            when {
                innhold == null -> null
                else -> skjema ?: nyttSkjemaFactory()
            }
        }
    }
}
