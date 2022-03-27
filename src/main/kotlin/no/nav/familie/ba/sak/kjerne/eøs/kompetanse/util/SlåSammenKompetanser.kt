package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.util

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.tilTidspunktEllerUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TidslinjeSomStykkerOppTiden
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.hentUtsnitt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import java.time.YearMonth

fun slåSammenKompetanser(kompetanser: Collection<Kompetanse>): Collection<Kompetanse> {
    return kompetanser.map { EnkeltKompetanseTidslinje(it) }
        .let { SlåSammenKompetanserTidslinje(it) }
        .perioder().flatMap { periode -> periode.innhold?.settFomOgTom(periode) ?: emptyList() }
}

fun Iterable<Kompetanse>.slåSammen() =
    slåSammenKompetanser(this.toList())

fun Iterable<Kompetanse>?.settFomOgTom(periode: Periode<*, Måned>) =
    this?.map { kompetanse -> kompetanse.settFomOgTom(periode) }

fun Kompetanse.settFomOgTom(periode: Periode<*, Måned>) =
    this.copy(
        fom = periode.fraOgMed.tilYearMonthEllerNull(),
        tom = periode.tilOgMed.tilYearMonthEllerNull()
    ).also { it.id = this.id }

internal class EnkeltKompetanseTidslinje(
    val kompetanse: Kompetanse
) : Tidslinje<Kompetanse, Måned>() {
    override fun fraOgMed() = kompetanse.fom.tilTidspunktEllerUendeligLengeSiden { kompetanse.tom ?: YearMonth.now() }

    override fun tilOgMed() = kompetanse.tom.tilTidspunktEllerUendeligLengeTil { kompetanse.fom ?: YearMonth.now() }

    override fun lagPerioder(): Collection<Periode<Kompetanse, Måned>> {
        return listOf(Periode(fraOgMed(), tilOgMed(), kompetanse.copy(fom = null, tom = null)))
    }
}

internal class SlåSammenKompetanserTidslinje(
    val kompetanseTidslinjer: Collection<EnkeltKompetanseTidslinje>
) : TidslinjeSomStykkerOppTiden<Set<Kompetanse>, Måned>(kompetanseTidslinjer) {
    override fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt<Måned>): Set<Kompetanse>? {
        return kompetanseTidslinjer
            .map { it.hentUtsnitt(tidspunkt) }
            .filterNotNull()
            .fold(mutableSetOf()) { kompetanser, kompetanse ->
                val matchendeKompetanse = kompetanser.plukkUtHvis { it.erLikUtenBarn(kompetanse) }
                val oppdatertKompetanse = matchendeKompetanse?.leggSammenBarn(kompetanse) ?: kompetanse
                kompetanser.add(oppdatertKompetanse)
                kompetanser
            }
    }

    private fun <T> MutableSet<T>.plukkUtHvis(predicate: (T) -> Boolean): T? =
        this.find { predicate(it) }?.also { this.remove(it) }

    private fun Kompetanse.erLikUtenBarn(kompetanse: Kompetanse) =
        this.copy(barnAktørIder = emptySet()) == kompetanse.copy(barnAktørIder = emptySet())

    private fun Kompetanse.leggSammenBarn(kompetanse: Kompetanse) =
        this.copy(barnAktørIder = this.barnAktørIder.plus(kompetanse.barnAktørIder))
}
