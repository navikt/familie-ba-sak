package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.VilkårResultatTidslinje
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

    val søkersVilkårsresultater = identTilVilkårsresultater
        .filter { (ident, _) -> personopplysningGrunnlag.søker.aktør.harIdent(ident) }
        .mapValues { (_, resultater) ->
            resultater.groupBy { it.vilkårType }
                .mapValues { VilkårResultatTidslinje(it.value) }
                .values
        }.values.flatten()

    val søkerOppfyllerVilkår = SøkerOppfyllerVilkårTidslinje(søkersVilkårsresultater)

    private val barnasTidslinjer = barnasVilkårsresultatTidslinjeMap.mapValues { (barnIdent, _) ->
        BarnetsTidslinjer(this, barnIdent)
    }

    fun forBarn(barn: Aktør) = barnasTidslinjer[barn.aktivFødselsnummer()]!!

    class BarnetsTidslinjer(
        tidslinjer: Tidslinjer,
        barnIdent: String,
    ) {
        val vilkårsresultater = tidslinjer.barnasVilkårsresultatTidslinjeMap[barnIdent]!!
        val barnetsYtelse = BarnetsYtelseTidslinje(tidslinjer.søkerOppfyllerVilkår, vilkårsresultater)
        val erUnder6År = ErBarnetUnder6ÅrTidslinje(barnetsYtelse, tidslinjer.fødseldato(barnIdent))
        val utbetalinger = BarnetsUtbetalingerTidslinje(barnetsYtelse, erUnder6År)
        val erEøs = ErEøsPeriodeTidslinje(vilkårsresultater)
        val kompetanser = KompetanseTidslinje(tidslinjer.kompetanser.forBarn(barnIdent))
        val kompetanseValidering = KompetanseValideringTidslinje(erEøs, kompetanser)
        val erSekundærland = ErSekundærlandTidslinje(kompetanser, kompetanseValidering)
        val differanseBeregning = DifferanseBeregningTidslinje(utbetalinger, erSekundærland)
    }
}

fun Collection<Kompetanse>.forBarn(ident: String) =
    this.filter { it.barn.contains(ident) }
