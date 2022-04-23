package no.nav.familie.ba.sak.kjerne.eøs.tidslinjer

import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.komprimer
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate

class Tidslinjer(
    vilkårsvurdering: Vilkårsvurdering,
    personopplysningGrunnlag: PersonopplysningGrunnlag
) {
    private val barna: List<Aktør> = personopplysningGrunnlag.barna.map { it.aktør }
    private val søker: Aktør = personopplysningGrunnlag.søker.aktør

    internal val barnOgFødselsdatoer = personopplysningGrunnlag.barna.associate { it.aktør to it.fødselsdato }

    private val aktørTilPersonResultater =
        vilkårsvurdering.personResultater.associateBy { it.aktør }

    private val vilkårsresultaterTidslinjeMap = aktørTilPersonResultater
        .entries.associate { (aktør, personResultat) ->
            aktør to personResultat.vilkårResultater.groupBy { it.vilkårType }
                .map {
                    VilkårsresultatDagTidslinje(vilkårsresultater = it.value)
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
            vilkårsresultatTidslinjer.map { it.tilVilkårsresultaterMånedTidslinje() }

        val oppfyllerVilkårTidslinje: Tidslinje<Resultat, Måned> =
            vilkårsresultatMånedTidslinjer.kombiner(SøkerOppfyllerVilkårKombinator()::kombiner)
    }

    class BarnetsTidslinjer(
        tidslinjer: Tidslinjer,
        aktør: Aktør,
    ) {
        val vilkårsresultatTidslinjer: List<Tidslinje<VilkårRegelverkResultat, Dag>> =
            tidslinjer.vilkårsresultaterTidslinjeMap[aktør] ?: listOf(TomTidslinje())

        private val vilkårsresultatMånedTidslinjer: List<Tidslinje<VilkårRegelverkResultat, Måned>> =
            vilkårsresultatTidslinjer.map { it.tilVilkårsresultaterMånedTidslinje() }

        val oppfyllerVilkårTidslinje: Tidslinje<Resultat, Måned> =
            vilkårsresultatMånedTidslinjer
                .kombiner(BarnOppfyllerVilkårKombinator()::kombiner)
                .filtrerMed(erUnder18ÅrVilkårTidslinje(tidslinjer.barnOgFødselsdatoer.getValue(aktør)))

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

fun erUnder18ÅrVilkårTidslinje(fødselsdato: LocalDate): Tidslinje<Boolean, Måned> {

    return object : Tidslinje<Boolean, Måned>() {
        override fun fraOgMed() = Tidspunkt.med(fødselsdato.toYearMonth()).neste()
        override fun tilOgMed() = Tidspunkt.med(fødselsdato.til18ÅrsVilkårsdato().toYearMonth()).forrige()

        override fun lagPerioder(): Collection<Periode<Boolean, Måned>> = listOf(
            Periode(fraOgMed(), tilOgMed(), true)
        )
    }
}
