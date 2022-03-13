package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Periode
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.PeriodeRepository
import no.nav.familie.ba.sak.kjerne.tidslinje.VilkårResultatTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate

class MockPerideRepository : PeriodeRepository {
    override fun hentPerioder(
        tidslinjeId: String,
        akseptertInnhold: List<String>?
    ): Iterable<Periode<String>> {
        TODO("Not yet implemented")
    }

    override fun lagrePerioder(
        tidslinjeId: String,
        perioder: Iterable<Periode<String>>
    ): Iterable<Periode<String>> {
        TODO("Not yet implemented")
    }
}

fun byggSøkerOgBarn(
    behandlingId: Long,
    vilkårsvurdering: Vilkårsvurdering,
    personopplysningGrunnlag: PersonopplysningGrunnlag
) {

    val aktørTilVilkårsresultater = vilkårsvurdering.personResultater
        .flatMap { pr -> pr.vilkårResultater }
        .groupBy { it.personResultat!!.aktør }

    val fødseldato: (Aktør) -> LocalDate =
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
            val barnetsYtelseTidslinje =
                BarnetsYtelseTidslinje(søkerOppfyllerVilkårTidslinje, vilkårsresultatTidslinjer)
            val erUnder6ÅrTidslinje = ErBarnetUnder6ÅrTidslinje(barnetsYtelseTidslinje, fødseldato(barn))
            BarnetsUtbetalingerTidslinje(barnetsYtelseTidslinje, erUnder6ÅrTidslinje)
        }

    val kompetanser = KompetanseService().hentKompetanser(behandlingId)

    val barnasDifferanseBeregningTidslinjeMap =
        barnasVilkårsresultatTidslinjeMap.mapValues { (barn, vilkårsresultatTidslinje) ->
            val erEøsPeriodeTidslinje = ErEøsPeriodeTidslinje(vilkårsresultatTidslinje)
            val kompetanseTidslinje =
                KompetanseTidslinje(kompetanser.forBarn(barn), barn)
            val validerKompetanseTidslinje =
                KompetanseValideringTidslinje(erEøsPeriodeTidslinje, kompetanseTidslinje)
            val erSekundærlandTidslinje = ErSekundærlandTidslinje(kompetanseTidslinje, validerKompetanseTidslinje)
            DifferanseBeregningTidslinje(barnsUtebetalingTidslinjeMap[barn]!!, erSekundærlandTidslinje)
        }
}

fun Collection<Kompetanse>.forBarn(barn: Aktør) =
    this.filter { it.barn.contains(barn.aktivFødselsnummer()) }
