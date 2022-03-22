package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.VilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.tidslinje.VilkårResultatTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.kombinerMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate
import java.time.YearMonth

class Tidslinjer(
    vilkårsvurdering: Vilkårsvurdering,
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    private val kompetanser: Collection<Kompetanse>
) {
    private val aktørIdTilVilkårsresultater = vilkårsvurdering.personResultater
        .flatMap { pr -> pr.vilkårResultater }
        .groupBy { it.personResultat!!.aktør.aktørId }

    private val fødseldato: (String) -> LocalDate =
        { aktørId ->
            personopplysningGrunnlag.personer
                .filter { it.aktør.aktørId == aktørId }
                .first().fødselsdato
        }

    private val barnasVilkårsresultatTidslinjeMap = aktørIdTilVilkårsresultater
        .filter { (aktørId, _) -> personopplysningGrunnlag.søker.aktør.aktørId != aktørId }
        .mapValues { (_, resultater) ->
            resultater
                .groupBy { it.vilkårType }
                .mapValues { VilkårResultatTidslinje(it.value) }
                .values
        }

    val søkersVilkårsresultatTidslinjer = aktørIdTilVilkårsresultater
        .filter { (aktørId, _) -> personopplysningGrunnlag.søker.aktør.aktørId == aktørId }
        .mapValues { (_, resultater) ->
            resultater.groupBy { it.vilkårType }
                .mapValues { VilkårResultatTidslinje(it.value) }
                .values
        }.values.flatten()

    val søkerOppfyllerVilkårTidslinje = søkersVilkårsresultatTidslinjer.kombiner(SøkerOppfyllerVilkårKombinator())

    private val barnasTidslinjer = barnasVilkårsresultatTidslinjeMap.mapValues { (barnAktørId, _) ->
        BarnetsTidslinjerTimeline(this, barnAktørId)
    }

    fun forBarn(barn: Person) = barnasTidslinjer[barn.aktør.aktørId]!!

    interface BarnetsTidslinjer {
        val barnetsVilkårsresultatTidslinjer: Collection<Tidslinje<VilkårRegelverkResultat, YearMonth>>
        val barnetOppfyllerVilkårTidslinje: Tidslinje<Boolean, YearMonth>
        val erEøsTidslinje: Tidslinje<Boolean, YearMonth>
        val kompetanseTidslinje: Tidslinje<Kompetanse, YearMonth>
        val kompetanseValideringTidslinje: Tidslinje<KompetanseValidering, YearMonth>
        val erSekundærlandTidslinje: Tidslinje<Boolean, YearMonth>
    }

    class BarnetsTidslinjerUtsnitt(
        tidslinjer: Tidslinjer,
        barnAktørId: String,
    ) : BarnetsTidslinjer {
        override val barnetsVilkårsresultatTidslinjer = tidslinjer.barnasVilkårsresultatTidslinjeMap[barnAktørId]!!

        override val barnetOppfyllerVilkårTidslinje =
            AktørOppfyllerVilkårTidslinje(barnetsVilkårsresultatTidslinjer, BarnOppfyllerVilkårKombinator())

        override val erEøsTidslinje = ErEøsPeriodeTidslinje(barnetsVilkårsresultatTidslinjer)

        override val kompetanseTidslinje = tidslinjer.kompetanser.tilTidslinjeforBarn(barnAktørId)

        override val kompetanseValideringTidslinje = KompetanseValideringTidslinje(erEøsTidslinje, kompetanseTidslinje)

        override val erSekundærlandTidslinje =
            ErSekundærlandTidslinje(kompetanseTidslinje, kompetanseValideringTidslinje)
    }

    class BarnetsTidslinjerTimeline(
        tidslinjer: Tidslinjer,
        barnIdent: String,
    ) : BarnetsTidslinjer {
        override val barnetsVilkårsresultatTidslinjer = tidslinjer.barnasVilkårsresultatTidslinjeMap[barnIdent]!!

        override val barnetOppfyllerVilkårTidslinje: Tidslinje<Boolean, YearMonth> =
            barnetsVilkårsresultatTidslinjer.kombiner(BarnOppfyllerVilkårKombinator())

        override val erEøsTidslinje = barnetsVilkårsresultatTidslinjer.kombiner(EøsPeriodeKombinator())

        override val kompetanseTidslinje = tidslinjer.kompetanser.tilTidslinjeforBarn(barnIdent)

        override val kompetanseValideringTidslinje =
            kompetanseTidslinje.kombinerMed(erEøsTidslinje, KompetanseValideringKombinator())

        override val erSekundærlandTidslinje =
            kompetanseTidslinje.kombinerMed(kompetanseValideringTidslinje, ErSekundærlandKombinator())
    }
}

fun Iterable<Kompetanse>.tilTidslinjeforBarn(barnAktørId: String) =
    this.filter { it.barnAktørIder.contains(barnAktørId) }
        .let { KompetanseTidslinje(it) }
