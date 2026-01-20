package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.rest

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjer
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNull
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærTilOgMedEtter
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.tidslinje.PRAKTISK_TIDLIGSTE_DAG
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.beskjærEtter
import no.nav.familie.tidslinje.filtrerIkkeNull
import no.nav.familie.tidslinje.mapVerdi
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import java.time.LocalDate

fun VilkårsvurderingTidslinjer.tilTidslinjerDto(): TidslinjerDto {
    val barnasTidslinjer = this.barnasTidslinjer()
    val søkersTidslinje = this.søkersTidslinje()

    val erNoenAvBarnaMellom0Og18ÅrTidslinje: Tidslinje<Boolean> =
        barnasTidslinjer.values
            .map { it.erUnder18ÅrVilkårTidslinje }
            .kombinerUtenNull { barnaEr0Til18ÅrListe -> barnaEr0Til18ÅrListe.any { it } }

    return TidslinjerDto(
        barnasTidslinjer =
            barnasTidslinjer.entries.associate {
                val erUnder18årTidslinje = it.value.erUnder18ÅrVilkårTidslinje
                it.key.aktivFødselsnummer() to
                    TidslinjerForBarnDto(
                        vilkårTidslinjer =
                            it.value.vilkårsresultatTidslinjer.map {
                                it
                                    .beskjærEtter(erUnder18årTidslinje)
                                    .tilTidslinjeDto()
                            },
                        oppfyllerEgneVilkårIKombinasjonMedSøkerTidslinje =
                            it.value
                                .regelverkResultatTidslinje
                                .mapVerdi { it?.resultat }
                                .beskjærEtter(erUnder18årTidslinje)
                                .tilTidslinjeDto(),
                        regelverkTidslinje =
                            it.value.regelverkResultatTidslinje
                                .mapVerdi { it?.regelverk }
                                .beskjærEtter(erUnder18årTidslinje)
                                .tilTidslinjeDto(),
                    )
            },
        søkersTidslinjer =
            TidslinjerForSøkerDto(
                vilkårTidslinjer =
                    søkersTidslinje.vilkårsresultatTidslinjer.map {
                        it
                            .beskjærTilOgMedEtter(erNoenAvBarnaMellom0Og18ÅrTidslinje)
                            .tilTidslinjeDto()
                    },
                oppfyllerEgneVilkårTidslinje =
                    søkersTidslinje
                        .regelverkResultatTidslinje
                        .mapVerdi { it?.resultat }
                        .beskjærTilOgMedEtter(erNoenAvBarnaMellom0Og18ÅrTidslinje)
                        .tilTidslinjeDto(),
            ),
    )
}

fun <V> Tidslinje<V>.tilTidslinjeDto(): List<TidslinjePeriodeDto<V>> =
    this.tilPerioder().filtrerIkkeNull().map { periode ->
        TidslinjePeriodeDto(
            fraOgMed = periode.fom?.førsteDagIInneværendeMåned() ?: PRAKTISK_TIDLIGSTE_DAG,
            tilOgMed = periode.tom?.sisteDagIMåned(),
            innhold = periode.verdi,
        )
    }

data class TidslinjerDto(
    val barnasTidslinjer: Map<String, TidslinjerForBarnDto>,
    val søkersTidslinjer: TidslinjerForSøkerDto,
)

data class TidslinjerForBarnDto(
    val vilkårTidslinjer: List<List<TidslinjePeriodeDto<VilkårRegelverkResultat>>>,
    val oppfyllerEgneVilkårIKombinasjonMedSøkerTidslinje: List<TidslinjePeriodeDto<Resultat>>,
    val regelverkTidslinje: List<TidslinjePeriodeDto<Regelverk>>,
)

data class TidslinjerForSøkerDto(
    val vilkårTidslinjer: List<List<TidslinjePeriodeDto<VilkårRegelverkResultat>>>,
    val oppfyllerEgneVilkårTidslinje: List<TidslinjePeriodeDto<Resultat>>,
)

data class TidslinjePeriodeDto<T>(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
    val innhold: T,
)
