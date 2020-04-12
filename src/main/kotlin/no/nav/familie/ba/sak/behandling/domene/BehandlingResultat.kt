package no.nav.familie.ba.sak.behandling.domene

import no.nav.familie.ba.sak.behandling.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.beregning.domene.PeriodeResultat
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators
import no.nav.nare.core.evaluations.Resultat
import java.time.LocalDate
import javax.persistence.*

@Entity(name = "BehandlingResultat")
@Table(name = "BEHANDLING_RESULTAT")
data class BehandlingResultat(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "behandling_resultat_seq_generator")
        @SequenceGenerator(name = "behandling_resultat_seq_generator",
                           sequenceName = "behandling_resultat_seq",
                           allocationSize = 50)
        val id: Long = 0,

        @ManyToOne(optional = false)
        @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
        val behandling: Behandling,

        @Column(name = "aktiv", nullable = false)
        var aktiv: Boolean = true,

        @OneToMany(fetch = FetchType.EAGER,
                   mappedBy = "behandlingResultat",
                   cascade = [CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE]
        )
        var personResultater: Set<PersonResultat> = setOf()

) : BaseEntitet() {

    override fun toString(): String {
        return "BehandlingResultat(id=$id, behandling=${behandling.id})"
    }

    fun hentSamletResultat(): BehandlingResultatType {
        if (personResultater.isEmpty()) {
            return BehandlingResultatType.IKKE_VURDERT
        }

        return when {
            personResultater.all { it.hentSamletResultat() == BehandlingResultatType.INNVILGET } ->
                BehandlingResultatType.INNVILGET
            else ->
                if (behandling.type == BehandlingType.REVURDERING) BehandlingResultatType.OPPHØRT
                else BehandlingResultatType.AVSLÅTT
        }
    }

    var periodeResultater: Set<PeriodeResultat>
        get() = this.tilPeriodeResultater()
        set(value) {} //TODO: INIT https://stackoverflow.com/questions/41440750/kotlin-why-do-i-need-to-initialize-a-var-with-custom-getter
}

enum class BehandlingResultatType(val brevMal: String, val displayName: String) {
    INNVILGET(brevMal = "Innvilget", displayName = "Innvilget"),
    DELVIS_INNVILGET(brevMal = "Ukjent", displayName = "Delvis innvilget"),
    AVSLÅTT(brevMal = "Avslag", displayName = "Avslått"),
    OPPHØRT(brevMal = "Opphor", displayName = "Opphørt"),
    HENLAGT(brevMal = "Ukjent", displayName = "Henlagt"),
    IKKE_VURDERT(brevMal = "Ukjent", displayName = "Ikke vurdert")
}

fun BehandlingResultat.tilPeriodeResultater() : Set<PeriodeResultat> = emptySet()

data class VilkårTmp(
        val personIdent: String,
        val vilkårType: Vilkår, //TODO: String?
        val resultat: Resultat,
        val periodeFom: LocalDate?,
        val periodeTom: LocalDate?,
        val begrunnelse: String
)


fun maptilfptidslinje(restPersonResultater: List<RestPersonResultat>): LocalDateTimeline<List<VilkårTmp>> {
    val flattMedPerson: List<VilkårTmp> = restPersonResultater.flatMap { personResultat ->
        personResultat.vilkårResultater!!.map { //TODO: Fix
            VilkårTmp(personIdent = personResultat.personIdent,
                      vilkårType = it.vilkårType,
                      begrunnelse = it.begrunnelse,
                      periodeFom = it.periodeFom,
                      periodeTom = it.periodeFom,
                      resultat = it.resultat)
        }
    }
    val tidslinjer: List<LocalDateTimeline<VilkårTmp>> = flattMedPerson.map { vilkårTmp -> vilkårResultatTilTimeline(vilkårTmp) }
    //val samletTidslinje: LocalDateTimeline<List<VilkårTmp>> = tidslinjer.reduce(::kombinerTidslinjerREDUCER)  }
    //val samletTidslinje: LocalDateTimeline<List<VilkårTmp>> = tidslinjer.reduce { lhs, rhs -> (kombinerTidslinjerREDUCER(lhs, rhs))  }
    val vilkårSegment = tidslinjer.first().toSegments().first()
    val initSammenlagtTidslinje = LocalDateTimeline(listOf(LocalDateSegment(vilkårSegment.fom, vilkårSegment.tom, listOf(vilkårSegment.value))))
    return tidslinjer.fold(initSammenlagtTidslinje) { lhs, rhs -> (kombinerTidslinjer(lhs, rhs)) }
}

private fun vilkårResultatTilTimeline(it: VilkårTmp): LocalDateTimeline<VilkårTmp> =
        LocalDateTimeline(listOf(LocalDateSegment(it.periodeFom,
                                                  it.periodeTom,
                                                  it)))
private fun  kombinerTidslinjer(lhs: LocalDateTimeline<List<VilkårTmp>>, rhs: LocalDateTimeline<VilkårTmp>): LocalDateTimeline<List<VilkårTmp>> {
    return lhs.combine(rhs, StandardCombinators::allValues, LocalDateTimeline.JoinStyle.CROSS_JOIN)
}

/*
TIL FORMAT:

class PeriodeResultat(
        //private val id: Long = 0,
        //var behandlingResultat: BehandlingResultat,
        val personIdent: String,
        val periodeFom: LocalDate?,
        val periodeTom: LocalDate?,
        var vilkårResultater: Set<VilkårResultat> = setOf())
class VilkårResultat(
        //val id: Long = 0,
        //var periodeResultat: PeriodeResultat,
        val vilkårType: Vilkår,
        val resultat: Resultat,
        var begrunnelse: String
)
*/

/*
@Suppress("REIFIED_TYPE_PARAMETER_NO_INLINE")
public fun <reified T : Any> LocalDateTimeline<*>.medType(): Boolean =
        T::class.java.isAssignableFrom(this::class.java.componentType)
private fun  kombinerTidslinjerReducer(lhs: LocalDateTimeline<*>, rhs: LocalDateTimeline<VilkårTmp>): LocalDateTimeline<List<VilkårTmp>> {
    //https://stackoverflow.com/questions/51136866/how-can-i-check-for-array-type-not-generic-type-in-kotlin
    //vurder å legge inn pr til tidslinjer for en istimelineof-metode ala isArrayOf: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/is-array-of.html
    return when {
        lhs.medType<VilkårTmp>() -> {
            lhs as LocalDateTimeline<VilkårTmp>
            val vilkårSegment = lhs.toSegments().first()
            val initSammenlagtTidslinje = LocalDateTimeline(listOf(LocalDateSegment(vilkårSegment.fom, vilkårSegment.tom, listOf(vilkårSegment.value))))
            initSammenlagtTidslinje.combine(rhs, StandardCombinators::allValues, LocalDateTimeline.JoinStyle.CROSS_JOIN)
        }
        lhs.medType<List<VilkårTmp>>() -> {
            lhs as LocalDateTimeline<List<VilkårTmp>>
            lhs.combine(rhs, StandardCombinators::allValues, LocalDateTimeline.JoinStyle.CROSS_JOIN)
        }
        else -> {
            throw IllegalArgumentException("prøverå reduce med type som ikke er gyldig")
        }
    }
}

val rvr = RestVilkårResultatTmp("", "", RestPeriode("", ""), Resultat.NEI)
val t1: LocalDateTimeline<List<RestVilkårResultatTmp>> =
        LocalDateTimeline(listOf(LocalDateSegment(LocalDate.now().minusMonths(4), LocalDate.now(), listOf(rvr))))
val t2: LocalDateTimeline<RestVilkårResultatTmp> = LocalDateTimeline(listOf(LocalDateSegment(LocalDate.now().minusMonths(2),
                                                                                          LocalDate.now().plusMonths(1),
                                                                                          rvr)))
val kombinert: LocalDateTimeline<List<RestVilkårResultatTmp>> =
        t1.combine(t2, StandardCombinators::allValues, LocalDateTimeline.JoinStyle.CROSS_JOIN)
*/
