package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.erUnder18ÅrVilkårTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.tilMånedFraMånedsskifte
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.tilMånedFraMånedsskifteIkkeNull
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.tilTidslinje
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.beskjærEtter
import no.nav.familie.tidslinje.tomTidslinje
import no.nav.familie.tidslinje.utvidelser.filtrerIkkeNull
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import java.time.LocalDate

object VilkårsvurderingForskyvningUtils {
    fun PersonResultat.tilTidslinjeForSplittForPerson(
        person: Person,
        fagsakType: FagsakType,
    ): Tidslinje<List<VilkårResultat>> {
        val tidslinjer = this.vilkårResultater.tilForskjøvetTidslinjerForHvertOppfylteVilkår(person.fødselsdato)

        return tidslinjer
            .kombiner {
                alleOrdinæreVilkårErOppfyltEllerNull(
                    vilkårResultater = it,
                    personType = person.type,
                    fagsakType = fagsakType,
                )
            }.filtrerIkkeNull()
            .slåSammenLikePerioder()
    }

    /**
     * Extention-funksjon som tar inn et sett med vilkårResultater og returnerer en forskjøvet måned-basert tidslinje for hvert vilkår
     * Se readme-fil for utdypende forklaring av logikken for hvert vilkår
     * */
    fun Collection<VilkårResultat>.tilForskjøvetTidslinjerForHvertOppfylteVilkår(fødselsdato: LocalDate): List<Tidslinje<VilkårResultat>> =
        this.groupBy { it.vilkårType }.map { (vilkår, vilkårResultater) ->
            vilkårResultater.tilForskjøvetTidslinjeForOppfyltVilkår(vilkår, fødselsdato)
        }

    fun Collection<VilkårResultat>.tilForskjøvedeVilkårTidslinjer(fødselsdato: LocalDate): List<Tidslinje<VilkårResultat>> =
        this.groupBy { it.vilkårType }.map { (vilkår, vilkårResultater) ->
            vilkårResultater.tilForskjøvetTidslinje(vilkår, fødselsdato)
        }

    fun Collection<VilkårResultat>.tilForskjøvetTidslinjeForOppfyltVilkår(
        vilkår: Vilkår,
        fødselsdato: LocalDate?,
    ): Tidslinje<VilkårResultat> {
        if (this.isEmpty()) return tomTidslinje()

        val tidslinje = this.lagForskjøvetTidslinjeForOppfylteVilkår(vilkår)

        return tidslinje.beskjærPå18ÅrHvisUnder18ÅrVilkår(vilkår = vilkår, fødselsdato = fødselsdato)
    }

    fun Collection<VilkårResultat>.tilForskjøvetTidslinjeForOppfyltVilkårForVoksenPerson(vilkår: Vilkår): Tidslinje<VilkårResultat> {
        if (vilkår == Vilkår.UNDER_18_ÅR) throw Feil("Funksjonen skal ikke brukes for under 18 vilkåret")

        return this.lagForskjøvetTidslinjeForOppfylteVilkår(vilkår)
    }

    fun Collection<VilkårResultat>.lagForskjøvetTidslinjeForOppfylteVilkår(vilkår: Vilkår): Tidslinje<VilkårResultat> =
        this
            .filter { it.vilkårType == vilkår && it.erOppfylt() }
            .tilTidslinje()
            .tilMånedFraMånedsskifteIkkeNull { innholdSisteDagForrigeMåned, innholdFørsteDagDenneMåned ->
                when {
                    !innholdSisteDagForrigeMåned.erOppfylt() || !innholdFørsteDagDenneMåned.erOppfylt() -> null
                    vilkår == Vilkår.BOR_MED_SØKER && innholdFørsteDagDenneMåned.erDeltBosted() -> innholdSisteDagForrigeMåned
                    else -> innholdFørsteDagDenneMåned
                }
            }

    fun Collection<VilkårResultat>.tilForskjøvetTidslinje(
        vilkår: Vilkår,
        fødselsdato: LocalDate,
    ): Tidslinje<VilkårResultat> {
        val tidslinje = this.lagForskjøvetTidslinje(vilkår)

        return tidslinje.beskjærPå18ÅrHvisUnder18ÅrVilkår(vilkår = vilkår, fødselsdato = fødselsdato)
    }

    private fun Collection<VilkårResultat>.lagForskjøvetTidslinje(vilkår: Vilkår): Tidslinje<VilkårResultat> =
        this
            .filter { it.vilkårType == vilkår }
            .tilTidslinje()
            .tilMånedFraMånedsskifte { innholdSisteDagForrigeMåned, innholdFørsteDagDenneMåned ->
                if (innholdFørsteDagDenneMåned != null && innholdSisteDagForrigeMåned != null) {
                    when {
                        vilkår == Vilkår.BOR_MED_SØKER && innholdFørsteDagDenneMåned.erDeltBosted() && innholdSisteDagForrigeMåned.erOppfylt() && innholdFørsteDagDenneMåned.erOppfylt() -> innholdSisteDagForrigeMåned
                        innholdSisteDagForrigeMåned.erEksplisittAvslagInnenforSammeMåned() && innholdFørsteDagDenneMåned.erOppfylt() -> innholdSisteDagForrigeMåned
                        innholdSisteDagForrigeMåned.erEksplisittAvslagPåSøknad == true && innholdFørsteDagDenneMåned.erOppfylt() -> null
                        else -> innholdFørsteDagDenneMåned
                    }
                } else if (innholdFørsteDagDenneMåned == null && innholdSisteDagForrigeMåned.erEksplisittAvslagInnenforSammeMåned()) {
                    innholdSisteDagForrigeMåned
                } else {
                    null
                }
            }

    private fun VilkårResultat?.erEksplisittAvslagInnenforSammeMåned(): Boolean =
        this?.erEksplisittAvslagPåSøknad == true &&
            this.periodeFom != null &&
            this.periodeFom!!.toYearMonth() == this.periodeTom?.toYearMonth()

    private fun Tidslinje<VilkårResultat>.beskjærPå18ÅrHvisUnder18ÅrVilkår(
        vilkår: Vilkår,
        fødselsdato: LocalDate?,
    ): Tidslinje<VilkårResultat> =
        if (vilkår == Vilkår.UNDER_18_ÅR) {
            this.beskjærPå18År(fødselsdato = fødselsdato ?: throw Feil("Mangler fødselsdato, men prøver å beskjære på 18-år vilkåret"))
        } else {
            this
        }

    internal fun Tidslinje<VilkårResultat>.beskjærPå18År(fødselsdato: LocalDate): Tidslinje<VilkårResultat> {
        val erUnder18Tidslinje = erUnder18ÅrVilkårTidslinje(fødselsdato)
        return this.beskjærEtter(erUnder18Tidslinje)
    }

    private fun VilkårResultat.erDeltBosted() =
        this.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED) ||
            this.utdypendeVilkårsvurderinger.contains(
                UtdypendeVilkårsvurdering.DELT_BOSTED_SKAL_IKKE_DELES,
            )

    private fun alleOrdinæreVilkårErOppfyltEllerNull(
        vilkårResultater: Iterable<VilkårResultat>,
        personType: PersonType,
        fagsakType: FagsakType,
    ): List<VilkårResultat>? =
        if (vilkårResultater.alleOrdinæreVilkårErOppfylt(personType, fagsakType)) {
            vilkårResultater.filterNotNull()
        } else {
            null
        }

    fun Iterable<VilkårResultat>.alleOrdinæreVilkårErOppfylt(
        personType: PersonType,
        fagsakType: FagsakType,
    ): Boolean {
        val alleVilkårForPersonType =
            Vilkår.hentVilkårFor(
                personType = personType,
                fagsakType = fagsakType,
                behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
            )
        return this
            .map { it.vilkårType }
            .containsAll(alleVilkårForPersonType) &&
            this.all { it.resultat == Resultat.OPPFYLT }
    }
}
