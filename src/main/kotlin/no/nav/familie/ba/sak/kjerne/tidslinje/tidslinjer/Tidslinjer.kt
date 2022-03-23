package no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate

class Tidslinjer(
    vilkårsvurdering: Vilkårsvurdering,
    søkersFødselsdato: LocalDate,
    yngsteBarnSin18årsdag: LocalDate,
    barnOgFødselsdatoer: Map<Aktør, LocalDate>
) {
    private val barna = vilkårsvurdering.personResultater.filter { !it.erSøkersResultater() }.map { it.aktør }
    private val søker = vilkårsvurdering.personResultater.single { it.erSøkersResultater() }.aktør
    private val aktørTilPersonResultater = vilkårsvurdering.personResultater.associateBy { it.aktør }

    private val barnasVilkårsresultatTidslinjeMap = aktørTilPersonResultater
        .filter { !it.value.erSøkersResultater() }
        .entries.associate { (aktør, personResultat) ->
            aktør to personResultat.vilkårResultater.groupBy { it.vilkårType }
                .map {
                    val barnFødselsdato = barnOgFødselsdatoer[aktør] ?: throw Feil("Finner ikke fødselsdato på barn")
                    VilkårResultatTidslinje(
                        vilkårsresultater = it.value,
                        praktiskTidligsteDato = barnFødselsdato,
                        praktiskSenesteDato = barnFødselsdato.til18ÅrsVilkårsdato()
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
            personResultat.vilkårResultater
                .groupBy { it.vilkårType }
                .map {
                    VilkårResultatTidslinje(
                        vilkårsresultater = it.value,
                        praktiskTidligsteDato = søkersFødselsdato,
                        praktiskSenesteDato = yngsteBarnSin18årsdag.til18ÅrsVilkårsdato()
                    )
                }
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
        val regelverkTidslinje: Tidslinje<Regelverk>
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

        override val regelverkTidslinje = barnetsVilkårsresultatTidslinjer.kombiner(RegelverkPeriodeKombinator())
    }
}
