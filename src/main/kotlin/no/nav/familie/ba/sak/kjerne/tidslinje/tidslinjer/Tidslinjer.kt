package no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.komprimer
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate

class Tidslinjer(
    vilkårsvurdering: Vilkårsvurdering,
    søkersFødselsdato: LocalDate,
    yngsteBarnFødselsdato: LocalDate,
    barnOgFødselsdatoer: Map<Aktør, LocalDate>
) {
    private val barna = vilkårsvurdering.personResultater.filter { !it.erSøkersResultater() }.map { it.aktør }
    private val søker = vilkårsvurdering.personResultater.single { it.erSøkersResultater() }.aktør
    private val aktørTilPersonResultater =
        vilkårsvurdering.personResultater.associateBy { it.aktør }

    private val vilkårsresultaterTidslinjeMap = aktørTilPersonResultater
        .entries.associate { (aktør, personResultat) ->
            aktør to personResultat.vilkårResultater.groupBy { it.vilkårType }
                .map {
                    if (personResultat.erSøkersResultater()) {
                        VilkårsresultatDagTidslinje(
                            vilkårsresultater = it.value,
                            praktiskTidligsteDato = søkersFødselsdato,
                            praktiskSenesteDato = yngsteBarnFødselsdato.til18ÅrsVilkårsdato()
                        )
                    } else {
                        val barnFødselsdato =
                            barnOgFødselsdatoer[aktør] ?: throw Feil("Finner ikke fødselsdato på barn")
                        VilkårsresultatDagTidslinje(
                            vilkårsresultater = it.value,
                            praktiskTidligsteDato = barnFødselsdato,
                            praktiskSenesteDato = barnFødselsdato.til18ÅrsVilkårsdato()
                        )
                    }
                }
        }

    private val søkersTidslinje: SøkersTidslinjerTimeline =
        SøkersTidslinjerTimeline(this, søker)

    fun søkersTidslinjer(): SøkersTidslinjerTimeline = søkersTidslinje

    private val barnasTidslinjer: Map<Aktør, BarnetsTidslinjerTimeline> =
        barna.associateWith { BarnetsTidslinjerTimeline(this, it) }

    fun forBarn(barn: Person) = barnasTidslinjer[barn.aktør]!!

    fun barnasTidslinjer(): Map<Aktør, BarnetsTidslinjerTimeline> =
        barnasTidslinjer.entries.associate { it.key to it.value }

    interface SøkersTidslinjer {
        val vilkårsresultatTidslinjer: Collection<Tidslinje<VilkårRegelverkResultat, Dag>>
        val oppfyllerVilkårTidslinje: Tidslinje<Resultat, Måned>
    }

    interface BarnetsTidslinjer {
        val vilkårsresultatTidslinjer: Collection<Tidslinje<VilkårRegelverkResultat, Dag>>
        val oppfyllerVilkårTidslinje: Tidslinje<Resultat, Måned>
        val barnetIKombinasjonMedSøkerOppfyllerVilkårTidslinje: Tidslinje<Resultat, Måned>
        val regelverkTidslinje: Tidslinje<Regelverk, Måned>
    }

    class SøkersTidslinjerTimeline(
        tidslinjer: Tidslinjer,
        aktør: Aktør,
    ) : SøkersTidslinjer {
        override val vilkårsresultatTidslinjer = tidslinjer.vilkårsresultaterTidslinjeMap[aktør]!!

        val vilkårsresultatMånedTidslinjer =
            vilkårsresultatTidslinjer.map {
                VilkårsresultatMånedTidslinje(it)
            }

        override val oppfyllerVilkårTidslinje: Tidslinje<Resultat, Måned> =
            vilkårsresultatMånedTidslinjer.kombiner(SøkerOppfyllerVilkårKombinator()::kombiner)
    }

    class BarnetsTidslinjerTimeline(
        tidslinjer: Tidslinjer,
        aktør: Aktør,
    ) : BarnetsTidslinjer {
        override val vilkårsresultatTidslinjer = tidslinjer.vilkårsresultaterTidslinjeMap[aktør]!!

        val vilkårsresultatMånedTidslinjer: List<Tidslinje<VilkårRegelverkResultat, Måned>> =
            vilkårsresultatTidslinjer.map {
                VilkårsresultatMånedTidslinje(it)
            }

        override val oppfyllerVilkårTidslinje: Tidslinje<Resultat, Måned> =
            vilkårsresultatMånedTidslinjer.kombiner(BarnOppfyllerVilkårKombinator()::kombiner)

        override val barnetIKombinasjonMedSøkerOppfyllerVilkårTidslinje: Tidslinje<Resultat, Måned> =
            oppfyllerVilkårTidslinje.kombinerMed(
                tidslinjer.søkersTidslinje.oppfyllerVilkårTidslinje,
                BarnIKombinasjonMedSøkerOppfyllerVilkårKombinator()::kombiner
            )

        val regelverkMidlertidigTidslinje: Tidslinje<Regelverk, Måned> =
            RegelverkPeriodeTidslinje(vilkårsresultatMånedTidslinjer.kombiner(RegelverkPeriodeKombinator()::kombiner)).komprimer()

        override val regelverkTidslinje = barnetIKombinasjonMedSøkerOppfyllerVilkårTidslinje.kombinerMed(
            regelverkMidlertidigTidslinje,
            RegelverkOgOppfyltePerioderKombinator()::kombiner
        )
    }
}
