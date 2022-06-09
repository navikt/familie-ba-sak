package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.rest

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjer
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.fraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.rangeTo
import no.nav.familie.ba.sak.kjerne.tidslinje.tilOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærEtter
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærTilOgMedEtter
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.tilDag
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import java.time.LocalDate
import java.util.Random

fun VilkårsvurderingTidslinjer.tilRestTidslinjer(): RestTidslinjer {
    val barnasTidslinjer = this.barnasTidslinjer()
    val søkersTidslinjer = this.søkersTidslinjer()

    val erNoenAvBarnaMellom0Og18ÅrTidslinje: Tidslinje<Boolean, Måned> = barnasTidslinjer.values
        .map { it.erUnder18ÅrVilkårTidslinje }
        .kombinerUtenNull { barnaEr0Til18ÅrListe -> barnaEr0Til18ÅrListe.any { it } }

    return RestTidslinjer(
        barnasTidslinjer = barnasTidslinjer.entries.associate {
            val erUnder18årTidslinje = it.value.erUnder18ÅrVilkårTidslinje
            it.key.aktivFødselsnummer() to RestTidslinjerForBarn(
                vilkårTidslinjer = it.value.vilkårsresultatTidslinjer.map {
                    it.beskjærEtter(erUnder18årTidslinje.tilDag())
                        .tilRestTidslinje()
                },
                oppfyllerEgneVilkårIKombinasjonMedSøkerTidslinje = it.value
                    .regelverkResultatTidslinje
                    .map { it?.resultat }
                    .beskjærEtter(erUnder18årTidslinje)
                    .tilRestTidslinje(),
                regelverkTidslinje = it.value.regelverkResultatTidslinje
                    .map { it?.regelverk }
                    .beskjærEtter(erUnder18årTidslinje)
                    .tilRestTidslinje(),
                oppsummeringTidslinje = tilfeldigOppsummering(
                    it.value.regelverkResultatTidslinje
                        .beskjærEtter(erUnder18årTidslinje)
                )
            )
        },
        søkersTidslinjer = RestTidslinjerForSøker(
            vilkårTidslinjer = søkersTidslinjer.vilkårsresultatTidslinjer.map {
                it.beskjærTilOgMedEtter(erNoenAvBarnaMellom0Og18ÅrTidslinje.tilDag())
                    .tilRestTidslinje()
            },
            oppfyllerEgneVilkårTidslinje = søkersTidslinjer
                .regelverkResultatTidslinje.map { it?.resultat }
                .beskjærTilOgMedEtter(erNoenAvBarnaMellom0Og18ÅrTidslinje)
                .tilRestTidslinje()
        )
    )
}

fun <I, T : Tidsenhet> Tidslinje<I, T>.tilRestTidslinje(): List<RestTidslinjePeriode<I>> =
    this.filtrerIkkeNull().perioder().map { periode ->
        RestTidslinjePeriode(
            fraOgMed = periode.fraOgMed.tilFørsteDagIMåneden().tilLocalDate(),
            tilOgMed = periode.tilOgMed.tilSisteDagIMåneden().tilLocalDate(),
            innhold = periode.innhold!!
        )
    }

data class RestTidslinjer(
    val barnasTidslinjer: Map<String, RestTidslinjerForBarn>,
    val søkersTidslinjer: RestTidslinjerForSøker
)

data class RestTidslinjerForBarn(
    val vilkårTidslinjer: List<List<RestTidslinjePeriode<VilkårRegelverkResultat>>>,
    val oppfyllerEgneVilkårIKombinasjonMedSøkerTidslinje: List<RestTidslinjePeriode<Resultat>>,
    val regelverkTidslinje: List<RestTidslinjePeriode<Regelverk>>,
    // / TODO: Er kun for å teste ut visualisering.
    val oppsummeringTidslinje: List<RestTidslinjePeriode<BeregningOppsummering>>
)

data class RestTidslinjerForSøker(
    val vilkårTidslinjer: List<List<RestTidslinjePeriode<VilkårRegelverkResultat>>>,
    val oppfyllerEgneVilkårTidslinje: List<RestTidslinjePeriode<Resultat>>,
)

data class RestTidslinjePeriode<T>(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
    val innhold: T
)

data class BeregningOppsummering(
    val regelverk: Regelverk?,
    val status: BeregningOppsummeringStatus?,
    val kompetentLand: KompetanseResultat?
)

enum class BeregningOppsummeringStatus {
    VURDERT,
    IKKE_VURDERT
}

fun tilfeldigOppsummering(regelverkResultatTidslinje: Tidslinje<RegelverkResultat, Måned>):
    List<RestTidslinjePeriode<BeregningOppsummering>> {

    val tilfeldigTidslinje = tilfeldigIntTidslinje(
        regelverkResultatTidslinje.fraOgMed(),
        regelverkResultatTidslinje.tilOgMed()
    )

    return regelverkResultatTidslinje
        .kombinerMed(tilfeldigTidslinje) { regelverkResultat, rnd ->
            when (regelverkResultat) {
                RegelverkResultat.OPPFYLT_EØS_FORORDNINGEN ->
                    BeregningOppsummering(
                        Regelverk.EØS_FORORDNINGEN,
                        status = finnSikkert<BeregningOppsummeringStatus>(rnd!!),
                        kompetentLand = finnSikkert<KompetanseResultat>(rnd)
                    )
                else ->
                    BeregningOppsummering(
                        regelverk = regelverkResultat?.regelverk,
                        status = null,
                        kompetentLand = null
                    )
            }
        }.tilRestTidslinje()
}

fun <T : Tidsenhet> tilfeldigIntTidslinje(
    fraOgMed: Tidspunkt<T>,
    tilOgMed: Tidspunkt<T>,
): Tidslinje<Int, T> {

    val random = Random()

    return object : Tidslinje<Int, T>() {
        override fun lagPerioder(): Collection<Periode<Int, T>> {
            return (fraOgMed..tilOgMed).map { Periode(it, it, random.nextInt()) }
        }
    }
}

inline fun <reified T> finnSikkert(indeks: Int): T? where T : Enum<T> {
    return enumValues<T>().find { it.ordinal == indeks.mod(enumValues<T>().size) }
}
