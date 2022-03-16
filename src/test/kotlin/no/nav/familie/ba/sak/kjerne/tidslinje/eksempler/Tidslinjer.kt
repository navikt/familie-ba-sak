package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje2
import no.nav.familie.ba.sak.kjerne.tidslinje.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.tilPeriode
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
                .mapValues { it.value.map { vr -> vr.tilPeriode() }.let { Tidslinje2(it) } }
                .values
        }

    val søkersVilkårsresultatTidslinjer = identTilVilkårsresultater
        .filter { (ident, _) -> personopplysningGrunnlag.søker.aktør.harIdent(ident) }
        .mapValues { (_, resultater) ->
            resultater.groupBy { it.vilkårType }
                .mapValues { it.value.map { vr -> vr.tilPeriode() }.let { Tidslinje2(it) } }
                .values
        }.values.flatten()

    val søkerOppfyllerVilkårTidslinje = søkersVilkårsresultatTidslinjer.kombiner(SøkerOppfyllerVilkårKombinator())

    private val barnasTidslinjer = barnasVilkårsresultatTidslinjeMap.mapValues { (barnIdent, _) ->
        BarnetsTidslinjer(this, barnIdent)
    }

    fun forBarn(barn: Person) = barnasTidslinjer[barn.aktør.aktivFødselsnummer()]!!

    class BarnetsTidslinjer(
        tidslinjer: Tidslinjer,
        barnIdent: String,
    ) {
        val barnetsVilkårsresultatTidslinjer = tidslinjer.barnasVilkårsresultatTidslinjeMap[barnIdent]!!
        val erEøsTidslinje = barnetsVilkårsresultatTidslinjer.kombiner(EøsPeriodeKombinator())
        // val kompetanseTidslinje = tidslinjer.kompetanser.tilTidslinjeforBarn(barnIdent)
        // val kompetanseValideringTidslinje =
        //    kompetanseTidslinje.kombinerMed(erEøsTidslinje, KompetanseValideringKombinator())
        // val erSekundærland = kompetanseTidslinje.kombinerMed(kompetanseValideringTidslinje, ErSekundærlandKombinator())
    }
}

fun Collection<Kompetanse>.tilTidslinjeforBarn(ident: String) =
    this.filter { it.barn.contains(ident) }
        .map { it.tilPeriode() }
        .let { Tidslinje2(it) }
