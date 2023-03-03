package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.erUnder18ÅrVilkårTidslinje
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærEtter
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.tilMånedFraMånedsskifteIkkeNull
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.beskjærPå18År
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvetTidslinjeForOppfyltVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.tilTidslinje
import java.time.LocalDate

object VilkårsvurderingForskyvningUtils {
    fun Set<PersonResultat>.tilTidslinjeForSplitt(personerIPersongrunnlag: List<Person>): Tidslinje<List<VilkårResultat>, Måned> {
        val tidslinjerPerPerson = this.map { personResultat ->
            val person = personerIPersongrunnlag.find { it.aktør == personResultat.aktør }
                ?: throw Feil("Finner ikke person med aktørId=${personResultat.aktør.aktørId} i persongrunnlaget ved generering av tidslinje for splitt")
            personResultat.tilTidslinjeForSplittForPerson(personType = person.type)
        }

        return tidslinjerPerPerson.kombiner { it.filterNotNull().flatten() }.filtrerIkkeNull().slåSammenLike()
    }

    fun PersonResultat.tilTidslinjeForSplittForPerson(
        personType: PersonType
    ): Tidslinje<List<VilkårResultat>, Måned> {
        val tidslinjer = this.vilkårResultater.tilForskjøvetTidslinjerForHvertOppfylteVilkår()

        return tidslinjer.kombiner { alleVilkårOppfyltEllerNull(vilkårResultater = it, personType = personType) }
            .filtrerIkkeNull().slåSammenLike()
    }

    /**
     * Extention-funksjon som tar inn et sett med vilkårResultater og returnerer en forskjøvet måned-basert tidslinje for hvert vilkår
     * Se readme-fil for utdypende forklaring av logikken for hvert vilkår
     * */
    fun Collection<VilkårResultat>.tilForskjøvetTidslinjerForHvertOppfylteVilkår(): List<Tidslinje<VilkårResultat, Måned>> {
        return this.groupBy { it.vilkårType }.map { (vilkår, vilkårResultater) ->
            vilkårResultater.tilForskjøvetTidslinjeForOppfyltVilkår(vilkår)
        }
    }

    fun Collection<VilkårResultat>.tilForskjøvetTidslinjeForOppfyltVilkår(vilkår: Vilkår): Tidslinje<VilkårResultat, Måned> {
        if (this.isEmpty()) return tidslinje { emptyList() }

        val tidslinje = this.lagForskjøvetTidslinjeForOppfylteVilkår(vilkår)

        return tidslinje.beskjærPå18ÅrHvisUnder18ÅrVilkår(vilkår = vilkår, vilkårResultater = this)
    }

    private fun Collection<VilkårResultat>.lagForskjøvetTidslinjeForOppfylteVilkår(vilkår: Vilkår): Tidslinje<VilkårResultat, Måned> {
        return this
            .filter { it.vilkårType == vilkår && it.erOppfylt() }
            .tilTidslinje()
            .tilMånedFraMånedsskifteIkkeNull { innholdSisteDagForrigeMåned, innholdFørsteDagDenneMåned ->
                when {
                    !innholdSisteDagForrigeMåned.erOppfylt() || !innholdFørsteDagDenneMåned.erOppfylt() -> null
                    vilkår == Vilkår.BOR_MED_SØKER && innholdFørsteDagDenneMåned.erDeltBosted() -> innholdSisteDagForrigeMåned
                    else -> innholdFørsteDagDenneMåned
                }
            }
    }

    private fun Tidslinje<VilkårResultat, Måned>.beskjærPå18ÅrHvisUnder18ÅrVilkår(vilkår: Vilkår, vilkårResultater: Iterable<VilkårResultat>): Tidslinje<VilkårResultat, Måned> {
        return if (vilkår == Vilkår.UNDER_18_ÅR) {
            val minstePeriodeFom = vilkårResultater.minOf { it.periodeFom ?: throw Feil("Finner ikke fra og med dato på 'under 18 år'-vilkåret") } // Fra og med dato skal være lik fødselsdato for under 18-vilkåret
            this.beskjærPå18År(fødselsdato = minstePeriodeFom)
        } else {
            this
        }
    }

    internal fun Tidslinje<VilkårResultat, Måned>.beskjærPå18År(fødselsdato: LocalDate): Tidslinje<VilkårResultat, Måned> {
        val under18Tidslinje = erUnder18ÅrVilkårTidslinje(fødselsdato)
        return this.beskjærEtter(under18Tidslinje)
    }

    private fun VilkårResultat.erDeltBosted() =
        this.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED) || this.utdypendeVilkårsvurderinger.contains(
            UtdypendeVilkårsvurdering.DELT_BOSTED_SKAL_IKKE_DELES
        )

    private fun alleVilkårOppfyltEllerNull(
        vilkårResultater: Iterable<VilkårResultat>,
        personType: PersonType
    ): List<VilkårResultat>? {
        return if (vilkårResultater.alleVilkårErOppfylt(personType)) vilkårResultater.filterNotNull() else null
    }

    fun Iterable<VilkårResultat>.alleVilkårErOppfylt(personType: PersonType): Boolean {
        val alleVilkårForPersonType = Vilkår.hentVilkårFor(personType)
        return this.map { it.vilkårType }
            .containsAll(alleVilkårForPersonType) && this.all { it.resultat == Resultat.OPPFYLT }
    }
}
