package no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.rest

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.Tidslinjer
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.VilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærEtter
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import java.time.LocalDate

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
                    .tilRestRegelverkTidslinje()
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
    val tilOgMed: LocalDate?,
    val innhold: T
)
