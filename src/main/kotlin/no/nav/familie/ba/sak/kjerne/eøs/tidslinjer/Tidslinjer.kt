package no.nav.familie.ba.sak.kjerne.eøs.tidslinjer

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.komprimer
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering

class Tidslinjer(
    vilkårsvurdering: Vilkårsvurdering,
    personopplysningGrunnlag: PersonopplysningGrunnlag
) {
    private val barna: List<Aktør> = personopplysningGrunnlag.barna.map { it.aktør }
    private val søker: Aktør = personopplysningGrunnlag.søker.aktør

    val søkersFødselsdato = personopplysningGrunnlag.søker.fødselsdato
    val yngsteBarnFødselsdato = personopplysningGrunnlag.yngsteBarnSinFødselsdato
    val barnOgFødselsdatoer = personopplysningGrunnlag.barna.associate { it.aktør to it.fødselsdato }

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

    private val søkersTidslinje: SøkersTidslinjer =
        SøkersTidslinjer(this, søker)

    fun søkersTidslinjer(): SøkersTidslinjer = søkersTidslinje

    private val barnasTidslinjer: Map<Aktør, BarnetsTidslinjer> =
        barna.associateWith { BarnetsTidslinjer(this, it) }

    fun forBarn(barn: Person) = barnasTidslinjer[barn.aktør]!!

    fun barnasTidslinjer(): Map<Aktør, BarnetsTidslinjer> =
        barnasTidslinjer.entries.associate { it.key to it.value }

    class SøkersTidslinjer(
        tidslinjer: Tidslinjer,
        aktør: Aktør,
    ) {
        val vilkårsresultatTidslinjer = tidslinjer.vilkårsresultaterTidslinjeMap[aktør]!!

        private val vilkårsresultatMånedTidslinjer =
            vilkårsresultatTidslinjer.map {
                VilkårsresultatMånedTidslinje(it)
            }

        val oppfyllerVilkårTidslinje: Tidslinje<Resultat, Måned> =
            vilkårsresultatMånedTidslinjer.kombiner(SøkerOppfyllerVilkårKombinator()::kombiner)
    }

    class BarnetsTidslinjer(
        tidslinjer: Tidslinjer,
        aktør: Aktør,
    ) {
        val vilkårsresultatTidslinjer = tidslinjer.vilkårsresultaterTidslinjeMap[aktør]!!

        private val vilkårsresultatMånedTidslinjer: List<Tidslinje<VilkårRegelverkResultat, Måned>> =
            vilkårsresultatTidslinjer.map {
                VilkårsresultatMånedTidslinje(it)
            }

        val oppfyllerVilkårTidslinje: Tidslinje<Resultat, Måned> =
            vilkårsresultatMånedTidslinjer.kombiner(BarnOppfyllerVilkårKombinator()::kombiner)

        val barnetIKombinasjonMedSøkerOppfyllerVilkårTidslinje: Tidslinje<Resultat, Måned> =
            oppfyllerVilkårTidslinje.kombinerMed(
                tidslinjer.søkersTidslinje.oppfyllerVilkårTidslinje,
                BarnIKombinasjonMedSøkerOppfyllerVilkårKombinator()::kombiner
            )

        val regelverkMidlertidigTidslinje: Tidslinje<Regelverk, Måned> =
            RegelverkPeriodeTidslinje(vilkårsresultatMånedTidslinjer.kombiner(RegelverkPeriodeKombinator()::kombiner)).komprimer()

        val regelverkTidslinje = barnetIKombinasjonMedSøkerOppfyllerVilkårTidslinje.kombinerMed(
            regelverkMidlertidigTidslinje,
            RegelverkOgOppfyltePerioderKombinator()::kombiner
        )
    }
}
