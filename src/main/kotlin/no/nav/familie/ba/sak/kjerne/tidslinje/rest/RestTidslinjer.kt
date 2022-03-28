package no.nav.familie.ba.sak.kjerne.tidslinje.rest

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer.Tidslinjer
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer.VilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer.VilkårsresultatDagTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import java.time.LocalDate

fun Tidslinjer.tilRestTidslinjer(): RestTidslinjer {
    val barnasTidslinjer = this.barnasTidslinjer()
    val søkersTidslinjer = this.søkersTidslinje()

    return RestTidslinjer(
        barnasTidslinjer = barnasTidslinjer.entries.associate {
            it.key.aktivFødselsnummer() to RestTidslinjerForBarn(
                vilkårTidslinjer = it.value.vilkårsresultatTidslinjer.tilRestVilkårTidslinjer(),
                oppfyllerEgneVilkårIKombinasjonMedSøkerTidslinje = it.value.barnetIKombinasjonMedSøkerOppfyllerVilkårTidslinje.tilRestOppfyllerVilkårTidslinje(),
                regelverkTidslinje = it.value.regelverkTidslinje.tilRestRegelverkTidslinje(),
            )
        },
        søkersTidslinjer = RestTidslinjerForSøker(
            vilkårTidslinjer = søkersTidslinjer.vilkårsresultatTidslinjer.tilRestVilkårTidslinjer(),
            oppfyllerEgneVilkårTidslinje = søkersTidslinjer.oppfyllerVilkårTidslinje.tilRestOppfyllerVilkårTidslinje(),
        )
    )
}

fun List<VilkårsresultatDagTidslinje>.tilRestVilkårTidslinjer(): List<List<RestTidslinjePeriode<VilkårRegelverkResultat>>> =
    this.map { vilkårsresultatTidslinje ->
        vilkårsresultatTidslinje.perioder().map { periode ->
            RestTidslinjePeriode(
                fraOgMed = periode.fraOgMed.tilLocalDate(),
                tilOgMed = periode.tilOgMed.tilLocalDate(),
                innhold = periode.innhold!!
            )
        }
    }

fun Tidslinje<Regelverk?, Måned>.tilRestRegelverkTidslinje(): List<RestTidslinjePeriode<Regelverk?>> =
    this.perioder().map { periode ->
        RestTidslinjePeriode(
            fraOgMed = periode.fraOgMed.tilFørsteDagIMåneden().tilLocalDate(),
            tilOgMed = periode.tilOgMed.tilSisteDagIMåneden().tilLocalDate(),
            innhold = periode.innhold
        )
    }

fun Tidslinje<Resultat, Måned>.tilRestOppfyllerVilkårTidslinje(): List<RestTidslinjePeriode<Resultat>> =
    this.perioder().map { periode ->
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
    val regelverkTidslinje: List<RestTidslinjePeriode<Regelverk?>>
)

data class RestTidslinjerForSøker(
    val vilkårTidslinjer: List<List<RestTidslinjePeriode<VilkårRegelverkResultat>>>,
    val oppfyllerEgneVilkårTidslinje: List<RestTidslinjePeriode<Resultat>>,
)

data class RestTidslinjePeriode<T>(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val innhold: T
)
