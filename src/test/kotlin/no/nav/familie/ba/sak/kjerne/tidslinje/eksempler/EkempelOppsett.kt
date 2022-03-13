package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.VilkårResultatTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.VilkårsresultatTidslinjeSerialiserer
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate

fun byggSøkerOgBarn(
    behandlingId: Long,
    vilkårsvurdering: Vilkårsvurdering,
    personopplysningGrunnlag: PersonopplysningGrunnlag
) {

    val vilkårResultatTidslinjer = vilkårsvurdering.personResultater
        .flatMap { pr -> pr.vilkårResultater.map { Pair(pr.aktør, it) } }
        .groupBy({ Pair(it.first, it.second.vilkårType) }, { it.second })
        .mapValues {
            VilkårResultatTidslinje(
                it.key.first, it.key.second, it.value,
                VilkårsresultatTidslinjeSerialiserer(behandlingId, it.key.first, it.key.second)
            )
        }
        .values

    val fødseldato: (Aktør) -> LocalDate =
        { aktør -> personopplysningGrunnlag.personer.filter { it.aktør == aktør }.first().fødselsdato }

    val søkersVilkårsresultatTidslinje = vilkårResultatTidslinjer
        .filter { personopplysningGrunnlag.søker.aktør == it.aktør }

    val barnasVilkårsresultatTidslinjeMap = vilkårResultatTidslinjer
        .filter { personopplysningGrunnlag.søker.aktør != it.aktør }
        .groupBy { it.aktør }

    val søkerOppfyllerVilkårTidslinje = SøkerOppfyllerVilkårTidslinje(søkersVilkårsresultatTidslinje)

    val barnsUtebetalingTidslinjeMap =
        barnasVilkårsresultatTidslinjeMap.mapValues { (barn, vilkårsresultatTidslinjer) ->
            val barnetsYtelseTidslinje =
                BarnetsYtelseTidslinje(søkerOppfyllerVilkårTidslinje, vilkårsresultatTidslinjer)
            val erUnder6ÅrTidslinje = ErBarnetUnder6ÅrTidslinje(barnetsYtelseTidslinje, fødseldato(barn))
            BarnetsUtbetalingerTidslinje(barnetsYtelseTidslinje, erUnder6ÅrTidslinje)
        }

    val barnasDifferanseBeregningTidslinjeMap =
        barnasVilkårsresultatTidslinjeMap.mapValues { (barn, vilkårsresultatTidslinje) ->
            val erEøsPeriodeTidslinje = ErEøsPeriodeTidslinje(vilkårsresultatTidslinje)
            val kompetanseTidslinje =
                KompetanseTidslinje(KompetanseService(), erEøsPeriodeTidslinje, behandlingId, barn.aktivFødselsnummer())
            val erSekundærlandTidslinje = ErSekundærlandTidslinje(kompetanseTidslinje)
            DifferanseBeregningTidslinje(barnsUtebetalingTidslinjeMap[barn]!!, erSekundærlandTidslinje)
        }
}
