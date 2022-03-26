package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.util

import no.nav.familie.ba.sak.kjerne.beregning.AktørId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.AktørKompetanseTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseUtil
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.settFomOgTom
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.snittKombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer.TomTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk

class TilpassKompetanserTilEøsPerioder(
    val kompetanser: Collection<Kompetanse>,
    val barnTilRegelverkPerioder: Map<AktørId, Tidslinje<Regelverk, Måned>>
) {
    val alleBarnAktørIder = kompetanser.alleBarnasAktørIder() + barnTilRegelverkPerioder.keys

    fun tilpassKompetanserTilEøs(): Collection<Kompetanse> {
        val overflødige = finnOverflødigKompetanse()
        val kompetanserUtenOverflødige = kompetanser.flatMap { kompetanse ->
            overflødige.find { it.id == kompetanse.id }?.let {
                KompetanseUtil.finnRestKompetanser(kompetanse, it)
            } ?: listOf(kompetanse)
        }

        return (kompetanserUtenOverflødige + finnManglendeKompetanse())
            .slåSammen()
    }

    fun finnOverflødigKompetanse(): Collection<Kompetanse> {
        val skalSlettes = alleBarnAktørIder.flatMap { aktørId ->
            val kompetanseTidslinje = kompetanser.tilTidslinjeforBarn(aktørId)
            val regelverkTidslinje = barnTilRegelverkPerioder[aktørId] ?: TomTidslinje { MånedTidspunkt.nå() }

            kompetanseTidslinje.snittKombinerMed(regelverkTidslinje) { kompetanse: Kompetanse?, regelverk: Regelverk? ->
                if (regelverk != Regelverk.EØS_FORORDNINGEN && kompetanse != null) kompetanse else null
            }.tilKompetanser(aktørId)
        }.slåSammen()

        return skalSlettes.map { kompetanseTilSletting ->
            val id = kompetanser.find { kompetanse -> kompetanse.inneholder(kompetanseTilSletting) }?.id!!
            kompetanseTilSletting.also { it.id = id }
        }
    }

    fun finnManglendeKompetanse(): Collection<Kompetanse> {
        return alleBarnAktørIder.flatMap { aktørId ->
            val kompetanseTidslinje = kompetanser.tilTidslinjeforBarn(aktørId)
            val regelverkTidslinje = barnTilRegelverkPerioder[aktørId] ?: TomTidslinje { MånedTidspunkt.nå() }

            kompetanseTidslinje.kombinerMed(regelverkTidslinje) { kompetanse: Kompetanse?, regelverk: Regelverk? ->
                if (regelverk == Regelverk.EØS_FORORDNINGEN && kompetanse == null)
                    Kompetanse(fom = null, tom = null, barnAktørIder = setOf(aktørId))
                else null
            }.tilKompetanser(aktørId)
        }
    }
}

fun Iterable<Kompetanse>.alleBarnasAktørIder() =
    this.map { it.barnAktørIder }.reduce { akk, neste -> akk + neste }

fun Iterable<Kompetanse>.tilTidslinjeforBarn(barnAktørId: AktørId) =
    this.filter { it.barnAktørIder.contains(barnAktørId) }
        .let { AktørKompetanseTidslinje(barnAktørId, it) }

fun Kompetanse.inneholder(kompetanse: Kompetanse): Boolean {
    return this.bareSkjema() == kompetanse.bareSkjema() &&
        (this.fom == null || this.fom <= kompetanse.fom) &&
        (this.tom == null || this.tom >= kompetanse.tom) &&
        this.barnAktørIder.containsAll(kompetanse.barnAktørIder)
}

fun Kompetanse.bareSkjema() =
    this.copy(fom = null, tom = null, barnAktørIder = emptySet())

fun Tidslinje<Kompetanse, Måned>.tilKompetanser(aktørId: AktørId) =
    this.perioder().map { periode ->
        periode.innhold?.settFomOgTom(periode)
    }.filterNotNull()

fun Iterable<Kompetanse>.slåSammen() =
    slåSammenKompetanser(this.toList())
