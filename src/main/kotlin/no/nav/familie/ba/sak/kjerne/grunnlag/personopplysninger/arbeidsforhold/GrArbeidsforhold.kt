package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.arbeidsforhold

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrArbeidsforhold")
@Table(name = "PO_ARBEIDSFORHOLD")
data class GrArbeidsforhold(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_arbeidsforhold_seq_generator")
    @SequenceGenerator(
        name = "po_arbeidsforhold_seq_generator",
        sequenceName = "po_arbeidsforhold_seq",
        allocationSize = 50
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
    val person: Person
) : BaseEntitet()

fun List<GrArbeidsforhold>.harLøpendeArbeidsforhold(): Boolean = this.any {
    it.periode?.tom == null || it.periode.tom >= LocalDate.now()
}
