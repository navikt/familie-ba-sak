package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer.VilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer.VilkårResultatTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate

class Tidslinjer(
    vilkårsvurdering: Vilkårsvurdering,
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    private val kompetanser: Collection<Kompetanse>
) {
    private val identTilVilkårsresultater = vilkårsvurdering.personResultater
        .flatMap { pr -> pr.vilkårResultater }
        .groupBy { it.personResultat!!.aktør.aktivFødselsnummer() }

    private val fødseldato: (String) -> LocalDate =
        { ident ->
            personopplysningGrunnlag.personer
                .filter { it.aktør.harIdent(ident) }
                .first().fødselsdato
        }

    private val barnasVilkårsresultatTidslinjeMap = identTilVilkårsresultater
        .filter { (ident, _) -> !personopplysningGrunnlag.søker.aktør.harIdent(ident) }
        .mapValues { (_, resultater) ->
            resultater
                .groupBy { it.vilkårType }
                .mapValues { VilkårResultatTidslinje(it.value) }
                .values
        }

    val søkersVilkårsresultatTidslinjer = identTilVilkårsresultater
        .filter { (ident, _) -> personopplysningGrunnlag.søker.aktør.harIdent(ident) }
        .mapValues { (_, resultater) ->
            resultater.groupBy { it.vilkårType }
                .mapValues { VilkårResultatTidslinje(it.value) }
                .values
        }.values.flatten()

    val søkerOppfyllerVilkårTidslinje = søkersVilkårsresultatTidslinjer.kombiner(SøkerOppfyllerVilkårKombinator())

    private val barnasTidslinjer = barnasVilkårsresultatTidslinjeMap.mapValues { (barnIdent, _) ->
        BarnetsTidslinjerTimeline(this, barnIdent)
    }

    fun forBarn(barn: Person) = barnasTidslinjer[barn.aktør.aktivFødselsnummer()]!!

    interface BarnetsTidslinjer {
        val barnetsVilkårsresultatTidslinjer: Collection<Tidslinje<VilkårRegelverkResultat>>
        val barnetOppfyllerVilkårTidslinje: Tidslinje<Boolean>
        val erEøsTidslinje: Tidslinje<Boolean>
        val kompetanseTidslinje: Tidslinje<Kompetanse>
        val kompetanseValideringTidslinje: Tidslinje<KompetanseValidering>
        val erSekundærlandTidslinje: Tidslinje<Boolean>
    }

    class BarnetsTidslinjerUtsnitt(
        tidslinjer: Tidslinjer,
        barnIdent: String,
    ) : BarnetsTidslinjer {
        override val barnetsVilkårsresultatTidslinjer = tidslinjer.barnasVilkårsresultatTidslinjeMap[barnIdent]!!

        override val barnetOppfyllerVilkårTidslinje =
            AktørOppfyllerVilkårTidslinje(barnetsVilkårsresultatTidslinjer, BarnOppfyllerVilkårKombinator())

        override val erEøsTidslinje = ErEøsPeriodeTidslinje(barnetsVilkårsresultatTidslinjer)

        override val kompetanseTidslinje = tidslinjer.kompetanser.tilTidslinjeforBarn(barnIdent)

        override val kompetanseValideringTidslinje = KompetanseValideringTidslinje(erEøsTidslinje, kompetanseTidslinje)

        override val erSekundærlandTidslinje =
            ErSekundærlandTidslinje(kompetanseTidslinje, kompetanseValideringTidslinje)
    }

    class BarnetsTidslinjerTimeline(
        tidslinjer: Tidslinjer,
        barnIdent: String,
    ) : BarnetsTidslinjer {
        override val barnetsVilkårsresultatTidslinjer = tidslinjer.barnasVilkårsresultatTidslinjeMap[barnIdent]!!

        override val barnetOppfyllerVilkårTidslinje: Tidslinje<Boolean> =
            barnetsVilkårsresultatTidslinjer.kombiner(BarnOppfyllerVilkårKombinator())

        override val erEøsTidslinje = barnetsVilkårsresultatTidslinjer.kombiner(EøsPeriodeKombinator())

        override val kompetanseTidslinje = tidslinjer.kompetanser.tilTidslinjeforBarn(barnIdent)

        override val kompetanseValideringTidslinje =
            kompetanseTidslinje.kombinerMed(erEøsTidslinje, KompetanseValideringKombinator())

        override val erSekundærlandTidslinje =
            kompetanseTidslinje.kombinerMed(kompetanseValideringTidslinje, ErSekundærlandKombinator())
    }
}

fun Iterable<Kompetanse>.tilTidslinjeforBarn(barnIdent: String) =
    this.filter { it.barn.contains(barnIdent) }
        .let { KompetanseTidslinje(it) }
