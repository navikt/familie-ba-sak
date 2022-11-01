package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.erUnder18ÅrVilkårTidslinje
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.tilMånedEtterVilkårsregler
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrer
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærEtter
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.forskyv
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.mapNotNull
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.tilMånedFraSisteDagIMåneden
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

fun Set<PersonResultat>.tilFørskjøvetVilkårResultatTidslinjeMap(): Map<Aktør, Tidslinje<List<VilkårResultat>, Måned>> =
    this.associate { personResultat ->
        val vilkårResultaterForAktørMap = personResultat.vilkårResultater
            .groupByTo(mutableMapOf()) { it.vilkårType }
            .mapValues { if (it.key != Vilkår.BOR_MED_SØKER) it.value else it.value.fjernAvslagUtenPeriodeHvisDetFinsAndreVilkårResultat() }

        val vilkårResultaterKombinert = vilkårResultaterForAktørMap
            .tilVilkårResultatTidslinjer()
            .kombiner { alleVilkårOppfyltEllerNull(it) }
            .filtrerIkkeNull()

        val vilkårResultaterForMåned = vilkårResultaterKombinert
            .tilMånedFraSisteDagIMåneden()
            .filtrer { it != null && it.toList().isNotEmpty() }
            .forskyv(1)

        val vilkårResultaterBeskåret = vilkårResultaterForMåned
            .beskjærPå18årVilkåretOmDetFinnes(vilkårResultaterForAktørMap[Vilkår.UNDER_18_ÅR])
            .filtrerIkkeNull()
            .slåSammenLike()

        Pair(
            personResultat.aktør,
            vilkårResultaterBeskåret
        )
    }

fun Set<PersonResultat>.tilSplittkriterierTidslinje(): Tidslinje<Map<Aktør, Splittkriterie>, Måned> {
    val splittkriterierTidslinjerForAktører = this.map { it.tilSplittkriterie() }

    return splittkriterierTidslinjerForAktører.kombinerUtenNull { it.toMap() }.slåSammenLike()
}

data class Splittkriterie(
    val vanligeVilkårResultater: List<VilkårResultat>?,
    val deltBostedVilkårResultater: VilkårResultat?
)

private fun PersonResultat.tilSplittkriterie(): Tidslinje<Pair<Aktør, Splittkriterie>, Måned> {
    val vilkårResultaterUtenDeltBostedTidslinjer = this.vilkårResultater.tilOppfyltVilkårsresultatUtenDeltBostedTidslinjer()
    val vilkårResultaterKombinert = vilkårResultaterUtenDeltBostedTidslinjer.kombiner { alleVilkårOppfyltEllerNull(it) }.filtrerIkkeNull()
    val vilkårResultaterUtenDeltBostedForMåned = vilkårResultaterKombinert.tilMånedEtterVilkårsregler { it.alleVilkårErOppfylt() }

    val deltBostedTidslinje = this.vilkårResultater.tilDeltBostedTidslinje()
    val deltBostedForMåned = deltBostedTidslinje.tilMånedEtterVilkårsregler { it?.resultat == Resultat.OPPFYLT }

    val alleKombinert = vilkårResultaterUtenDeltBostedForMåned.kombinerMed(deltBostedForMåned) { vr, db ->
        if (vr == null && db == null) null
        else Pair(vr, db)
    }.filtrerIkkeNull()

    return alleKombinert.mapNotNull { resultat -> Pair(this.aktør, Splittkriterie(resultat.first, resultat.second)) }
}

private fun Iterable<VilkårResultat>.tilOppfyltVilkårsresultatUtenDeltBostedTidslinjer(): List<VilkårResultatTidslinje> {
    val vilkårSomErOppfylt = this.filter { it.erOppfylt() }

    return vilkårSomErOppfylt.groupBy { it.vilkårType }.map { (key, value) ->
        if (key == Vilkår.BOR_MED_SØKER) {
            val vilkårUtenDeltBosted = value.filter { !it.erDeltBosted() }
            VilkårResultatTidslinje(vilkårUtenDeltBosted)
        }
        VilkårResultatTidslinje(value)
    }
}

private fun Iterable<VilkårResultat>.tilDeltBostedTidslinje(): VilkårResultatTidslinje {
    val vilkårMedDeltBosted = this.filter { it.vilkårType == Vilkår.BOR_MED_SØKER && it.erDeltBosted() }
    return VilkårResultatTidslinje(vilkårMedDeltBosted)
}

private fun VilkårResultat.erDeltBosted() = this.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED) || this.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED_SKAL_IKKE_DELES)

private fun MutableList<VilkårResultat>.fjernAvslagUtenPeriodeHvisDetFinsAndreVilkårResultat(): List<VilkårResultat> =
    if (this.any { !it.erAvslagUtenPeriode() }) this.filterNot { it.erAvslagUtenPeriode() } else this

private fun Tidslinje<List<VilkårResultat>, Måned>.beskjærPå18årVilkåretOmDetFinnes(
    under18VilkårResultater: List<VilkårResultat>?
) = if (under18VilkårResultater == null) {
    this
} else {
    val fødselsdag = under18VilkårResultater
        .minOf { it.periodeFom ?: throw Feil("Under 18 år vilkår resultat uten fra og med dato") }
    val erUnder18ÅrVilkårTidslinje = erUnder18ÅrVilkårTidslinje(fødselsdag)
    beskjærEtter(erUnder18ÅrVilkårTidslinje)
}

private fun Map<Vilkår, List<VilkårResultat>>.tilVilkårResultatTidslinjer() =
    this.map { (_, vilkårResultater) -> VilkårResultatTidslinje(vilkårResultater) }

private fun alleVilkårOppfyltEllerNull(vilkårResultater: Iterable<VilkårResultat?>): List<VilkårResultat>? {
    return if (vilkårResultater.alleVilkårErOppfylt()) vilkårResultater.filterNotNull() else null
}

private fun Iterable<VilkårResultat?>.alleVilkårErOppfylt() = all { it?.resultat == Resultat.OPPFYLT }

private fun List<VilkårResultat>?.alleVilkårErOppfylt() = !this.isNullOrEmpty()
