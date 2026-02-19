package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.arbeidsforhold

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsforhold
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.ArbeidsgiverType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDate

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrArbeidsforhold")
@Table(name = "PO_ARBEIDSFORHOLD")
data class GrArbeidsforhold(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_arbeidsforhold_seq_generator")
    @SequenceGenerator(
        name = "po_arbeidsforhold_seq_generator",
        sequenceName = "po_arbeidsforhold_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @Embedded
    val periode: DatoIntervallEntitet? = null,
    @Column(name = "arbeidsgiver_id")
    val arbeidsgiverId: String?,
    @Column(name = "arbeidsgiver_type")
    val arbeidsgiverType: String?,
    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_po_person_id", nullable = false, updatable = false)
    val person: Person,
) : BaseEntitet() {
    fun tilKopiForNyPerson(nyPerson: Person) = copy(id = 0, person = nyPerson)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GrArbeidsforhold

        if (periode != other.periode) return false
        if (arbeidsgiverId != other.arbeidsgiverId) return false
        if (arbeidsgiverType != other.arbeidsgiverType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = periode?.hashCode() ?: 0
        result = 31 * result + (arbeidsgiverId?.hashCode() ?: 0)
        result = 31 * result + (arbeidsgiverType?.hashCode() ?: 0)
        return result
    }

    companion object {
        fun Arbeidsforhold.tilGrArbeidsforhold(person: Person): GrArbeidsforhold {
            val arbeidsgiverId =
                when (this.arbeidsgiver?.type) {
                    ArbeidsgiverType.Organisasjon -> this.arbeidsgiver.organisasjonsnummer
                    ArbeidsgiverType.Person -> this.arbeidsgiver.offentligIdent
                    else -> null
                }

            return GrArbeidsforhold(
                periode = DatoIntervallEntitet(ansettelsesperiode?.periode?.fom, ansettelsesperiode?.periode?.tom),
                arbeidsgiverType = this.arbeidsgiver?.type?.name,
                arbeidsgiverId = arbeidsgiverId,
                person = person,
            )
        }
    }
}

fun List<GrArbeidsforhold>.harLøpendeArbeidsforhold(): Boolean =
    this.any {
        it.periode?.tom == null || it.periode.tom >= LocalDate.now()
    }
