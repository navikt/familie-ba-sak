package no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.rest

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.Tidslinjer
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.VilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.fraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.snittKombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.PRAKTISK_SENESTE_DAG
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.rangeTo
import no.nav.familie.ba.sak.kjerne.tidslinje.tilOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærEtter
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import java.time.LocalDate
import java.util.Random

fun Tidslinjer.tilRestTidslinjer(): RestTidslinjer {
    val barnasTidslinjer = this.barnasTidslinjer()
    val søkersTidslinjer = this.søkersTidslinjer()

    return RestTidslinjer(
        barnasTidslinjer = barnasTidslinjer.entries.associate {
            it.key.aktivFødselsnummer() to RestTidslinjerForBarn(
                vilkårTidslinjer = it.value.vilkårsresultatTidslinjer.tilRestVilkårTidslinjer(),
                oppfyllerEgneVilkårIKombinasjonMedSøkerTidslinje = it.value
                    .barnetIKombinasjonMedSøkerOppfyllerVilkårTidslinje
                    .filtrerIkkeNull()
                    .beskjærEtter(it.value.erUnder18ÅrVilkårTidslinje)
                    .tilRestOppfyllerVilkårTidslinje(),
                regelverkTidslinje = it.value.regelverkTidslinje
                    .filtrerIkkeNull()
                    .beskjærEtter(it.value.erUnder18ÅrVilkårTidslinje)
                    .tilRestRegelverkTidslinje(),
                oppsummeringTidslinje = tilfeldigOppsummering(it.value.regelverkTidslinje)
            )
        },
        søkersTidslinjer = RestTidslinjerForSøker(
            vilkårTidslinjer = søkersTidslinjer.vilkårsresultatTidslinjer.tilRestVilkårTidslinjer(),
            oppfyllerEgneVilkårTidslinje = søkersTidslinjer.oppfyllerVilkårTidslinje.tilRestOppfyllerVilkårTidslinje(),
        )
    )
}

fun List<Tidslinje<VilkårRegelverkResultat, Dag>>.tilRestVilkårTidslinjer(): List<List<RestTidslinjePeriode<VilkårRegelverkResultat>>> =
    this.map { vilkårsresultatTidslinje ->
        vilkårsresultatTidslinje.perioder().map { periode ->
            RestTidslinjePeriode(
                fraOgMed = periode.fraOgMed.tilLocalDate(),
                tilOgMed = periode.tilOgMed.tilLocalDateEllerNull(),
                innhold = periode.innhold!!
            )
        }
    }

fun Tidslinje<Regelverk, Måned>.tilRestRegelverkTidslinje(): List<RestTidslinjePeriode<Regelverk?>> =
    this.perioder().map { periode ->
        RestTidslinjePeriode(
            fraOgMed = periode.fraOgMed.tilFørsteDagIMåneden().tilLocalDate(),
            tilOgMed = periode.tilOgMed.tilSisteDagIMåneden().tilLocalDateEllerNull() ?: PRAKTISK_SENESTE_DAG,
            innhold = periode.innhold
        )
    }

fun Tidslinje<Resultat, Måned>.tilRestOppfyllerVilkårTidslinje(): List<RestTidslinjePeriode<Resultat>> =
    this.perioder().map { periode ->
        RestTidslinjePeriode(
            fraOgMed = periode.fraOgMed.tilFørsteDagIMåneden().tilLocalDate(),
            tilOgMed = periode.tilOgMed.tilSisteDagIMåneden().tilLocalDateEllerNull() ?: PRAKTISK_SENESTE_DAG,
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
    val regelverkTidslinje: List<RestTidslinjePeriode<Regelverk?>>,
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

fun tilfeldigOppsummering(regelverkTidslinje: Tidslinje<Regelverk, Måned>):
    List<RestTidslinjePeriode<BeregningOppsummering>> {

    val tilfeldigTidslinje = tilfeldigIntTidslinje(
        regelverkTidslinje.fraOgMed(),
        regelverkTidslinje.tilOgMed()
    )

    val tilfeldigOppsummeringTidslinje = regelverkTidslinje
        .snittKombinerMed(tilfeldigTidslinje) { regelverk, rnd ->
            when (regelverk) {
                Regelverk.EØS_FORORDNINGEN ->
                    BeregningOppsummering(
                        Regelverk.EØS_FORORDNINGEN,
                        status = finnSikkert<BeregningOppsummeringStatus>(rnd!!),
                        kompetentLand = finnSikkert<KompetanseResultat>(rnd)
                    )
                else ->
                    BeregningOppsummering(
                        regelverk = regelverk,
                        status = null,
                        kompetentLand = null
                    )
            }
        }

    return tilfeldigOppsummeringTidslinje.perioder().map {
        RestTidslinjePeriode(
            fraOgMed = it.fraOgMed.tilFørsteDagIMåneden().tilLocalDate(),
            tilOgMed = it.tilOgMed.tilSisteDagIMåneden().tilLocalDateEllerNull() ?: PRAKTISK_SENESTE_DAG,
            innhold = it.innhold!!
        )
    }
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
