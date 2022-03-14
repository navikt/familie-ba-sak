package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.VilkårResultatTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate

class Tidslinjer(
    behandlingId: Long,
    vilkårsvurdering: Vilkårsvurdering,
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    kompetanser: Collection<Kompetanse>,
    perideRepository: MockPerideRepository
) {
    private val aktørTilVilkårsresultater = vilkårsvurdering.personResultater
        .flatMap { pr -> pr.vilkårResultater }
        .groupBy { it.personResultat!!.aktør }

    private val fødseldato: (Aktør) -> LocalDate =
        { aktør -> personopplysningGrunnlag.personer.filter { it.aktør == aktør }.first().fødselsdato }

    val søkersVilkårsresultatTidslinje = aktørTilVilkårsresultater
        .filter { (aktør, _) -> personopplysningGrunnlag.søker.aktør == aktør }
        .mapValues { (_, resultater) ->
            resultater.groupBy { it.vilkårType }
                .mapValues { VilkårResultatTidslinje(it.value, MockPerideRepository()) }
                .values
        }.values.flatten()

    val barnasVilkårsresultatTidslinjeMap = aktørTilVilkårsresultater
        .filter { (aktør, _) -> personopplysningGrunnlag.søker.aktør != aktør }
        .mapValues { (_, resultater) ->
            resultater.groupBy { it.vilkårType }
                .mapValues { VilkårResultatTidslinje(it.value, MockPerideRepository()) }
                .values
        }

    val søkerOppfyllerVilkårTidslinje = SøkerOppfyllerVilkårTidslinje(søkersVilkårsresultatTidslinje)

    val barnsUtebetalingTidslinjeMap =
        barnasVilkårsresultatTidslinjeMap.mapValues { (barn, vilkårsresultatTidslinjer) ->
            val id = barn.aktørId + behandlingId
            val barnetsYtelseTidslinje =
                BarnetsYtelseTidslinje(søkerOppfyllerVilkårTidslinje, vilkårsresultatTidslinjer)
            val erUnder6ÅrTidslinje = ErBarnetUnder6ÅrTidslinje(barnetsYtelseTidslinje, fødseldato(barn))
            BarnetsUtbetalingerTidslinje(id, barnetsYtelseTidslinje, erUnder6ÅrTidslinje, perideRepository)
        }

    val barnasDifferanseBeregningTidslinjeMap =
        barnasVilkårsresultatTidslinjeMap.mapValues { (barn, vilkårsresultatTidslinje) ->
            val erEøsPeriodeTidslinje = ErEøsPeriodeTidslinje(vilkårsresultatTidslinje)
            val kompetanseTidslinje =
                KompetanseTidslinje(kompetanser.forBarn(barn), barn, perideRepository)
            val validerKompetanseTidslinje =
                KompetanseValideringTidslinje(erEøsPeriodeTidslinje, kompetanseTidslinje)
            val erSekundærlandTidslinje = ErSekundærlandTidslinje(kompetanseTidslinje, validerKompetanseTidslinje)
            DifferanseBeregningTidslinje(barnsUtebetalingTidslinjeMap[barn]!!, erSekundærlandTidslinje)
        }
}

fun Collection<Kompetanse>.forBarn(barn: Aktør) =
    this.filter { it.barn.contains(barn.aktivFødselsnummer()) }
