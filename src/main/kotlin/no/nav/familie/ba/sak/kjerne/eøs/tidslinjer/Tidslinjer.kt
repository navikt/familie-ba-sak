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
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.snittKombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.snittKombinerUtenNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærEtter
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
            vilkårsresultatTidslinjer.map { it.tilMånedsbasertTidslinjeForVilkårRegelverkResultat() }

        val oppfyllerVilkårTidslinje: Tidslinje<Resultat, Måned> =
            vilkårsresultatMånedTidslinjer.snittKombinerUtenNull(SøkerOppfyllerVilkårKombinator()::kombiner)
    }

    class BarnetsTidslinjer(
        tidslinjer: Tidslinjer,
        aktør: Aktør,
    ) {
        val vilkårsresultatTidslinjer: List<Tidslinje<VilkårRegelverkResultat, Dag>> =
            tidslinjer.vilkårsresultaterTidslinjeMap[aktør] ?: listOf(TomTidslinje())

        private val vilkårsresultatMånedTidslinjer: List<Tidslinje<VilkårRegelverkResultat, Måned>> =
            vilkårsresultatTidslinjer.map { it.tilMånedsbasertTidslinjeForVilkårRegelverkResultat() }

        val erUnder18ÅrVilkårTidslinje = erUnder18ÅrVilkårTidslinje(tidslinjer.barnOgFødselsdatoer.getValue(aktør))

        val oppfyllerVilkårTidslinje: Tidslinje<Resultat, Måned> =
            vilkårsresultatMånedTidslinjer
                .snittKombinerUtenNull(BarnOppfyllerVilkårKombinator()::kombiner)
                .filtrerMed(erUnder18ÅrVilkårTidslinje)

        val barnetIKombinasjonMedSøkerOppfyllerVilkårTidslinje: Tidslinje<Resultat, Måned> =
            oppfyllerVilkårTidslinje.snittKombinerMed(
                tidslinjer.søkersTidslinje.oppfyllerVilkårTidslinje,
                BarnIKombinasjonMedSøkerOppfyllerVilkårKombinator()::kombiner
            )

        val regelverkResultatTidslinje =
            vilkårsresultatMånedTidslinjer
                .snittKombinerUtenNull {
                    kombinerVilkårResultaterTilRegelverkResultat(it)
                }.snittKombinerMed(barnetIKombinasjonMedSøkerOppfyllerVilkårTidslinje) { regelverkResultat, oppfylt ->
                    kombinerVilkårResultatMedRegelverkResultat(oppfylt, regelverkResultat)
                }
                // Hvis barnet har uendelige vilkårsvurderinger, vil også tidslinjen hertil være uendelig,
                // selv om søker har endelige vilkårsvurderinger. Berskjærer mot søker for å forhindre det
                .beskjærEtter(tidslinjer.søkersTidslinje.oppfyllerVilkårTidslinje)
    }
}

fun Tidslinjer.hentRegelverkTidslinjer(): Map<Aktør, Tidslinje<Regelverk, Måned>> = this.barnasTidslinjer()
    .mapValues { (_, tidslinjer): Map.Entry<Aktør, Tidslinjer.BarnetsTidslinjer> -> tidslinjer.regelverkTidslinje }

fun erUnder18ÅrVilkårTidslinje(fødselsdato: LocalDate): Tidslinje<Boolean, Måned> {

    return object : Tidslinje<Boolean, Måned>() {
        private val fraOgMed = Tidspunkt.med(fødselsdato.toYearMonth()).neste()
        private val tilOgMed = Tidspunkt.med(fødselsdato.til18ÅrsVilkårsdato().toYearMonth()).forrige()

        override fun lagPerioder(): Collection<Periode<Boolean, Måned>> = listOf(
            Periode(fraOgMed, tilOgMed, true)
        )
    }
}
