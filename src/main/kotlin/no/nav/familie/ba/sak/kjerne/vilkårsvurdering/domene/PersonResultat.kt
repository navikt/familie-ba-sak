package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.erUnder18ÅrVilkårTidslinje
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærEtter
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.tilMånedFraMånedsskifteIkkeNull
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat.Companion.VilkårResultatComparator
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDate
import java.util.SortedSet
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "PersonResultat")
@Table(name = "PERSON_RESULTAT")
class PersonResultat(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "periode_resultat_seq_generator")
    @SequenceGenerator(
        name = "periode_resultat_seq_generator",
        sequenceName = "periode_resultat_seq",
        allocationSize = 50
    )
    val id: Long = 0,

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "fk_vilkaarsvurdering_id", nullable = false, updatable = false)
    var vilkårsvurdering: Vilkårsvurdering,

    @OneToOne(optional = false)
    @JoinColumn(name = "fk_aktoer_id", nullable = false, updatable = false)
    val aktør: Aktør,

    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "personResultat",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val vilkårResultater: MutableSet<VilkårResultat> = sortedSetOf(VilkårResultatComparator),

    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "personResultat",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val andreVurderinger: MutableSet<AnnenVurdering> = mutableSetOf()

) : BaseEntitet() {

    fun setSortedVilkårResultater(nyeVilkårResultater: Set<VilkårResultat>) {
        vilkårResultater.clear()
        vilkårResultater.addAll(nyeVilkårResultater.toSortedSet(VilkårResultatComparator))
    }

    fun setAndreVurderinger(nyeAndreVurderinger: Set<AnnenVurdering>) {
        andreVurderinger.clear()
        andreVurderinger.addAll(nyeAndreVurderinger)
    }

    fun getSortedVilkårResultat(index: Int): VilkårResultat? {
        return vilkårResultater.toSortedSet(VilkårResultatComparator).elementAtOrNull(index)
    }

    fun addVilkårResultat(vilkårResultat: VilkårResultat) {
        vilkårResultater.add(vilkårResultat)
        setSortedVilkårResultater(vilkårResultater.toSet())
        vilkårResultat.personResultat = this
    }

    fun removeVilkårResultat(vilkårResultatId: Long) {
        vilkårResultater.find { vilkårResultatId == it.id }?.personResultat = null
        setSortedVilkårResultater(vilkårResultater.filter { vilkårResultatId != it.id }.toSet())
    }

    fun slettEllerNullstill(vilkårResultatId: Long) {
        val vilkårResultat = vilkårResultater.find { it.id == vilkårResultatId }
            ?: throw Feil(
                message = "Prøver å slette et vilkår som ikke finnes",
                frontendFeilmelding = "Vilkåret du prøver å slette finnes ikke i systemet."
            )

        val perioderMedSammeVilkårType = vilkårResultater
            .filter { it.vilkårType == vilkårResultat.vilkårType && it.id != vilkårResultat.id }

        if (perioderMedSammeVilkårType.isEmpty()) {
            vilkårResultat.nullstill()
        } else {
            removeVilkårResultat(vilkårResultatId)
        }
    }

    fun kopierMedParent(
        vilkårsvurdering: Vilkårsvurdering,
        inkluderAndreVurderinger: Boolean = false
    ): PersonResultat {
        val nyttPersonResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = aktør
        )
        val kopierteVilkårResultater: SortedSet<VilkårResultat> =
            vilkårResultater.map { it.kopierMedParent(nyttPersonResultat) }.toSortedSet(VilkårResultatComparator)
        nyttPersonResultat.setSortedVilkårResultater(kopierteVilkårResultater)

        if (inkluderAndreVurderinger) {
            val kopierteAndreVurderinger: MutableSet<AnnenVurdering> =
                andreVurderinger.map { it.kopierMedParent(nyttPersonResultat) }.toMutableSet()

            nyttPersonResultat.setAndreVurderinger(kopierteAndreVurderinger)
        }
        return nyttPersonResultat
    }

    fun erSøkersResultater() = vilkårResultater.none { it.vilkårType == Vilkår.UNDER_18_ÅR } ||
        vilkårsvurdering.behandling.fagsak.type in listOf(FagsakType.BARN_ENSLIG_MINDREÅRIG, FagsakType.INSTITUSJON)

    fun erDeltBosted(segmentFom: LocalDate): Boolean =
        vilkårResultater
            .filter { it.vilkårType == Vilkår.BOR_MED_SØKER }
            .filter {
                (it.periodeFom == null || it.periodeFom!!.isSameOrBefore(segmentFom)) &&
                    (it.periodeTom == null || it.periodeTom!!.isSameOrAfter(segmentFom))
            }.any { it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED) }

    fun harEksplisittAvslag() = vilkårResultater.any { it.erEksplisittAvslagPåSøknad == true }
}

fun Set<PersonResultat>.tilTidslinjeForSplitt(personerIPersongrunnlag: List<Person>): Tidslinje<List<VilkårResultat>, Måned> {
    val tidslinjerPerPerson = this.map { personResultat ->
        val person = personerIPersongrunnlag.find { it.aktør == personResultat.aktør }
            ?: throw Feil("Finner ikke person med aktørId=${personResultat.aktør.aktørId} i persongrunnlaget ved generering av tidslinje for splitt")
        personResultat.tilTidslinjeForSplittForPerson(fødselsdato = person.fødselsdato, personType = person.type)
    }

    return tidslinjerPerPerson.kombiner { it.filterNotNull().flatten() }.filtrerIkkeNull().slåSammenLike()
}

fun PersonResultat.tilTidslinjeForSplittForPerson(
    fødselsdato: LocalDate,
    personType: PersonType
): Tidslinje<List<VilkårResultat>, Måned> {
    val tidslinjer = this.vilkårResultater.tilForskjøvetTidslinjerForHvertOppfylteVilkår(fødselsdato)

    return tidslinjer.kombiner { alleVilkårOppfyltEllerNull(vilkårResultater = it, personType = personType) }
        .filtrerIkkeNull().slåSammenLike()
}

/**
 * Extention-funksjon som tar inn et sett med vilkårResultater og returnerer en forskjøvet måned-basert tidslinje for hvert vilkår
 * Se readme-fil for utdypende forklaring av logikken for hvert vilkår
 * */
fun Collection<VilkårResultat>.tilForskjøvetTidslinjerForHvertOppfylteVilkår(fødselsdato: LocalDate): List<Tidslinje<VilkårResultat, Måned>> {
    return this.groupBy { it.vilkårType }.map { (vilkår, vilkårResultater) ->
        val tidslinje = vilkårResultater.tilForskjøvetTidslinjeForOppfyltVilkår(vilkår)
        if (vilkår == Vilkår.UNDER_18_ÅR) tidslinje.beskjærPå18År(fødselsdato) else tidslinje
    }
}

fun Collection<VilkårResultat>.tilForskjøvetTidslinjeForOppfyltVilkår(vilkår: Vilkår): Tidslinje<VilkårResultat, Måned> {
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

fun Tidslinje<VilkårResultat, Måned>.beskjærPå18År(fødselsdato: LocalDate): Tidslinje<VilkårResultat, Måned> {
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
