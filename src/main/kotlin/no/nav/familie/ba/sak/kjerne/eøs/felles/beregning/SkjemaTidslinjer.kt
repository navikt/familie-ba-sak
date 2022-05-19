package no.nav.familie.ba.sak.kjerne.eøs.felles.beregning

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjema
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEntitet
import no.nav.familie.ba.sak.kjerne.eøs.felles.medBehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.utenPeriode
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerSenereEnn
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerTidligereEnn
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.tilpassTil

fun <S : PeriodeOgBarnSkjema<S>> S.tilTidslinje() = listOf(this).tilTidslinje()

fun <S : PeriodeOgBarnSkjema<S>> Iterable<S>.tilTidslinje() =
    tidslinje {
        this.map {
            Periode(
                it.fom.tilTidspunktEllerTidligereEnn(it.tom),
                it.tom.tilTidspunktEllerSenereEnn(it.fom),
                it.utenPeriode()
            )
        }
    }

fun <S : PeriodeOgBarnSkjema<S>> Iterable<S>.tilSeparateTidslinjerForBarna(): Map<Aktør, Tidslinje<S, Måned>> {
    val skjemaer = this
    if (skjemaer.toList().isEmpty()) return emptyMap()

    val alleBarnAktørIder = skjemaer.map { it.barnAktører }.reduce { akk, neste -> akk + neste }

    return alleBarnAktørIder.associateWith { aktør ->
        tidslinje {
            skjemaer
                .filter { it.barnAktører.contains(aktør) }
                .map {
                    Periode(
                        fraOgMed = it.fom.tilTidspunktEllerTidligereEnn(it.tom),
                        tilOgMed = it.tom.tilTidspunktEllerSenereEnn(it.fom),
                        innhold = it.kopier(fom = null, tom = null, barnAktører = setOf(aktør))
                    )
                }
        }
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
    aktørTilMønsterTidslinje: Map<Aktør, Tidslinje<I, Måned>>,
    nyttSkjemaFactory: () -> S
): Map<Aktør, Tidslinje<S, Måned>> {
    val alleBarnAktørIder = this.keys + aktørTilMønsterTidslinje.keys

    return alleBarnAktørIder.associateWith { aktør ->
        val skjemaTidslinje = this.getOrDefault(aktør, TomTidslinje())
        val mønsterTidslinje = aktørTilMønsterTidslinje.getOrDefault(aktør, TomTidslinje())

        skjemaTidslinje.tilpassTil(mønsterTidslinje) { nyttSkjemaFactory() }
    }
}
