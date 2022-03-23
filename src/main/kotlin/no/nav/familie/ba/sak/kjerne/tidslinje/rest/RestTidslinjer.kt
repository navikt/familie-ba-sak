package no.nav.familie.ba.sak.kjerne.tidslinje.rest

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer.Tidslinjer
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer.VilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk

fun Tidslinjer.tilRestTidslinjer(): RestTidslinjer {
    val barnasTidslinjer = this.barnasTidslinjer()

    return RestTidslinjer(
        tidslinjer = barnasTidslinjer.entries.associate {
            it.key.aktivFødselsnummer() to RestTidslinjerForBarn(
                vilkårTidslinjer = it.value.barnetsVilkårsresultatTidslinjer.map { vilkårsresultatTidslinje ->
                    vilkårsresultatTidslinje.perioder().map { periode ->
                        RestTidslinjePeriode(
                            fraOgMed = periode.fraOgMed,
                            tilOgMed = periode.tilOgMed,
                            innhold = periode.innhold!!
                        )
                    }
                },
                oppfyllerVilkårTidslinje = it.value.barnetIKombinasjonMedSøkerOppfyllerVilkårTidslinje.perioder()
                    .map { periode ->
                        RestTidslinjePeriode(
                            fraOgMed = periode.fraOgMed,
                            tilOgMed = periode.tilOgMed,
                            innhold = periode.innhold!!
                        )
                    },
                regelverkTidslinje = it.value.regelverkTidslinje.perioder().map { periode ->
                    RestTidslinjePeriode(
                        fraOgMed = periode.fraOgMed,
                        tilOgMed = periode.tilOgMed,
                        innhold = periode.innhold!!
                    )
                },
            )
        }
    )
}

data class RestTidslinjer(
    val tidslinjer: Map<String, RestTidslinjerForBarn>
)

data class RestTidslinjerForBarn(
    val vilkårTidslinjer: List<List<RestTidslinjePeriode<VilkårRegelverkResultat, Dag>>>,
    val oppfyllerVilkårTidslinje: List<RestTidslinjePeriode<Resultat, Måned>>,
    val regelverkTidslinje: List<RestTidslinjePeriode<Regelverk, Måned>>
)

data class RestTidslinjePeriode<T, I : Tidsenhet>(
    val fraOgMed: Tidspunkt<I>,
    val tilOgMed: Tidspunkt<I>,
    val innhold: T
)
