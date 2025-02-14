package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.rest

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingFamilieFellesTidslinjer
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.komposisjon.kombinerUtenNull
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.komposisjon.mapVerdiNullable
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.transformasjon.beskjærTilOgMedEtter
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.transformasjon.tilDag
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.tidslinje.PRAKTISK_TIDLIGSTE_DAG
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.beskjærEtter
import no.nav.familie.tidslinje.filtrerIkkeNull
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import java.time.LocalDate

fun VilkårsvurderingFamilieFellesTidslinjer.tilRestTidslinjer(): RestTidslinjer {
    val barnasTidslinjer = this.barnasTidslinjer()
    val søkersTidslinje = this.søkersTidslinje()

    val erNoenAvBarnaMellom0Og18ÅrTidslinje: Tidslinje<Boolean> =
        barnasTidslinjer.values
            .map { it.erUnder18ÅrVilkårTidslinje }
            .kombinerUtenNull { barnaEr0Til18ÅrListe -> barnaEr0Til18ÅrListe.any { it } }

    return RestTidslinjer(
        barnasTidslinjer =
            barnasTidslinjer.entries.associate {
                val erUnder18årTidslinje = it.value.erUnder18ÅrVilkårTidslinje
                it.key.aktivFødselsnummer() to
                    RestTidslinjerForBarn(
                        vilkårTidslinjer =
                            it.value.vilkårsresultatTidslinjer.map {
                                it
                                    .beskjærEtter(erUnder18årTidslinje.tilDag())
                                    .tilRestTidslinje()
                            },
                        oppfyllerEgneVilkårIKombinasjonMedSøkerTidslinje =
                            it.value
                                .regelverkResultatTidslinje
                                .mapVerdiNullable { it?.resultat }
                                .beskjærEtter(erUnder18årTidslinje)
                                .tilRestTidslinje(),
                        regelverkTidslinje =
                            it.value.regelverkResultatTidslinje
                                .mapVerdiNullable { it?.regelverk }
                                .beskjærEtter(erUnder18årTidslinje)
                                .tilRestTidslinje(),
                    )
            },
        søkersTidslinjer =
            RestTidslinjerForSøker(
                vilkårTidslinjer =
                    søkersTidslinje.vilkårsresultatTidslinjer.map {
                        it
                            .beskjærTilOgMedEtter(erNoenAvBarnaMellom0Og18ÅrTidslinje.tilDag())
                            .tilRestTidslinje()
                    },
                oppfyllerEgneVilkårTidslinje =
                    søkersTidslinje
                        .regelverkResultatTidslinje
                        .mapVerdiNullable { it?.resultat }
                        .beskjærTilOgMedEtter(erNoenAvBarnaMellom0Og18ÅrTidslinje)
                        .tilRestTidslinje(),
            ),
    )
}

fun <V> Tidslinje<V>.tilRestTidslinje(): List<RestTidslinjePeriode<V>> =
    this.tilPerioder().filtrerIkkeNull().map { periode ->
        RestTidslinjePeriode(
            fraOgMed = periode.fom?.førsteDagIInneværendeMåned() ?: PRAKTISK_TIDLIGSTE_DAG,
            tilOgMed = periode.tom?.sisteDagIMåned(),
            innhold = periode.verdi,
        )
    }

data class RestTidslinjer(
    val barnasTidslinjer: Map<String, RestTidslinjerForBarn>,
    val søkersTidslinjer: RestTidslinjerForSøker,
)

data class RestTidslinjerForBarn(
    val vilkårTidslinjer: List<List<RestTidslinjePeriode<VilkårRegelverkResultat>>>,
    val oppfyllerEgneVilkårIKombinasjonMedSøkerTidslinje: List<RestTidslinjePeriode<Resultat>>,
    val regelverkTidslinje: List<RestTidslinjePeriode<Regelverk>>,
)

data class RestTidslinjerForSøker(
    val vilkårTidslinjer: List<List<RestTidslinjePeriode<VilkårRegelverkResultat>>>,
    val oppfyllerEgneVilkårTidslinje: List<RestTidslinjePeriode<Resultat>>,
)

data class RestTidslinjePeriode<T>(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
    val innhold: T,
)
