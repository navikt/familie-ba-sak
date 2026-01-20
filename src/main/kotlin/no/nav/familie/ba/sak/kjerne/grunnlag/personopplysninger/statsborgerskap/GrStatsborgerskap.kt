package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.erInnenfor
import no.nav.familie.ba.sak.ekstern.restDomene.RegisteropplysningDto
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import java.time.LocalDate

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrStatsborgerskap")
@Table(name = "PO_STATSBORGERSKAP")
data class GrStatsborgerskap(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_statsborgerskap_seq_generator")
    @SequenceGenerator(
        name = "po_statsborgerskap_seq_generator",
        sequenceName = "po_statsborgerskap_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @Embedded
    val gyldigPeriode: DatoIntervallEntitet? = null,
    @Column(name = "landkode", nullable = false)
    val landkode: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "medlemskap", nullable = false)
    val medlemskap: Medlemskap = Medlemskap.UKJENT,
    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_po_person_id", nullable = false, updatable = false)
    val person: Person,
) : BaseEntitet() {
    fun tilKopiForNyPerson(nyPerson: Person): GrStatsborgerskap = copy(id = 0, person = nyPerson)

    fun gjeldendeNå(): Boolean {
        if (gyldigPeriode == null) return true
        return LocalDate.now().erInnenfor(gyldigPeriode)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GrStatsborgerskap

        if (gyldigPeriode != other.gyldigPeriode) return false
        if (landkode != other.landkode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = gyldigPeriode.hashCode()
        result = 31 * result + landkode.hashCode()
        return result
    }

    fun tilRegisteropplysningDto() =
        RegisteropplysningDto(
            fom = this.gyldigPeriode?.fom,
            tom = this.gyldigPeriode?.tom,
            verdi = this.landkode,
        )
}

fun Statsborgerskap.fom() = this.gyldigFraOgMed ?: this.bekreftelsesdato

fun GrStatsborgerskap.iNordiskLand() = Norden.entries.map { it.name }.contains(this.landkode)

fun List<GrStatsborgerskap>.iUkraina() = this.any { it.landkode == "UKR" }

fun List<GrStatsborgerskap>.filtrerGjeldendeNå(): List<GrStatsborgerskap> = this.filter { it.gjeldendeNå() }

fun List<GrStatsborgerskap>.hentSterkesteMedlemskap(): Medlemskap? {
    val nåværendeMedlemskap = finnNåværendeMedlemskap(this)
    return nåværendeMedlemskap.finnSterkesteMedlemskap()
}

fun finnNåværendeMedlemskap(statsborgerskap: List<GrStatsborgerskap>?): List<Medlemskap> = statsborgerskap?.filtrerGjeldendeNå()?.map { it.medlemskap } ?: emptyList()

fun Iterable<Medlemskap>.finnSterkesteMedlemskap(): Medlemskap? =
    when {
        contains(Medlemskap.NORDEN) -> Medlemskap.NORDEN
        contains(Medlemskap.EØS) -> Medlemskap.EØS
        contains(Medlemskap.TREDJELANDSBORGER) -> Medlemskap.TREDJELANDSBORGER
        contains(Medlemskap.STATSLØS) -> Medlemskap.STATSLØS
        contains(Medlemskap.UKJENT) -> Medlemskap.UKJENT
        else -> null
    }

fun lagErNordiskStatsborgerTidslinje(statsborgerskap: List<GrStatsborgerskap>): Tidslinje<Boolean> {
    val statsborgerskapGruppertPåNavn =
        statsborgerskap
            .groupBy { it.landkode }
            .mapValues { (_, perLand) ->
                val unikeStatsborgerskapInnslag = perLand.distinct()
                val innslagMedDato = unikeStatsborgerskapInnslag.filter { it.gyldigPeriode?.fom != null || it.gyldigPeriode?.tom != null }

                innslagMedDato.ifEmpty { unikeStatsborgerskapInnslag }
            }

    return statsborgerskapGruppertPåNavn.values
        .map { statsborgerskapSammeLand ->
            statsborgerskapSammeLand
                .map { Periode(it, it.gyldigPeriode?.fom, it.gyldigPeriode?.tom) }
                .tilTidslinje()
        }.kombiner { statsborgerskap -> statsborgerskap.any { it.iNordiskLand() } }
}
