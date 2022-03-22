package no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.kombinerMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering

class Tidslinjer(
    vilkårsvurdering: Vilkårsvurdering,
) {
    private val barna = vilkårsvurdering.personResultater.filter { !it.erSøkersResultater() }.map { it.aktør }
    private val søker = vilkårsvurdering.personResultater.single { it.erSøkersResultater() }.aktør
    private val aktørTilPersonResultater = vilkårsvurdering.personResultater.associateBy { it.aktør }

    private val barnasVilkårsresultatTidslinjeMap = aktørTilPersonResultater
        .filter { !it.value.erSøkersResultater() }
        .entries.associate { (aktør, personResultat) ->
            aktør to personResultat.vilkårResultater.groupBy { it.vilkårType }
                .map {
                    VilkårResultatTidslinje(
                        it.value
                        /**.filter { vilkårResultat ->

                        // TODO støtt perioder med uendelig fom og tom, kan komme i kombinasjon med perioder med fom eller tom
                        vilkårResultat.periodeFom != null || vilkårResultat.periodeTom != null
                        }*/
                    )
                }
        }

    val søkersVilkårsresultatTidslinjer = aktørTilPersonResultater
        .filter { it.value.erSøkersResultater() }
        .flatMap { (_, personResultat) ->
            personResultat.vilkårResultater.groupBy { it.vilkårType }.map { VilkårResultatTidslinje(it.value) }
        }

    val søkerOppfyllerVilkårTidslinje = søkersVilkårsresultatTidslinjer.kombiner(SøkerOppfyllerVilkårKombinator())

    private val barnasTidslinjer: Map<Aktør, BarnetsTidslinjerTimeline> =
        barna.associateWith { BarnetsTidslinjerTimeline(this, it) }

    fun forBarn(barn: Person) = barnasTidslinjer[barn.aktør]!!

    fun barnasTidslinjer(): Map<Aktør, BarnetsTidslinjerTimeline> =
        barnasTidslinjer.entries.associate { it.key to it.value }

    interface BarnetsTidslinjer {
        val barnetsVilkårsresultatTidslinjer: Collection<Tidslinje<VilkårRegelverkResultat>>
        val barnetOppfyllerVilkårTidslinje: Tidslinje<Resultat>
        val barnetIKombinasjonMedSøkerOppfyllerVilkårTidslinje: Tidslinje<Resultat>
        val erEøsTidslinje: Tidslinje<Boolean>
    }

    class BarnetsTidslinjerTimeline(
        tidslinjer: Tidslinjer,
        barnAktør: Aktør,
    ) : BarnetsTidslinjer {
        override val barnetsVilkårsresultatTidslinjer = tidslinjer.barnasVilkårsresultatTidslinjeMap[barnAktør]!!

        override val barnetOppfyllerVilkårTidslinje: Tidslinje<Resultat> =
            barnetsVilkårsresultatTidslinjer.kombiner(BarnOppfyllerVilkårKombinator())

        override val barnetIKombinasjonMedSøkerOppfyllerVilkårTidslinje: Tidslinje<Resultat> =
            barnetOppfyllerVilkårTidslinje.kombinerMed(
                tidslinjer.søkerOppfyllerVilkårTidslinje,
                BarnIKombinasjonMedSøkerOppfyllerVilkårKombinator()
            )

        override val erEøsTidslinje = barnetsVilkårsresultatTidslinjer.kombiner(EøsPeriodeKombinator())
    }
}
