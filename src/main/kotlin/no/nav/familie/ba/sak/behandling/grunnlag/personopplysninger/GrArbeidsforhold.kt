package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import javax.persistence.*

@Entity(name = "GrArbeidsforhold")
@Table(name = "PO_ARBEIDSFORHOLD")
data class GrArbeidsforhold(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_arbeidsforhold_seq_generator")
        @SequenceGenerator(name = "po_arbeidsforhold_seq_generator",
                sequenceName = "po_arbeidsforhold_seq",
                allocationSize = 50)
        val id: Long = 0,

        @Embedded
        val periode: DatoIntervallEntitet? = null,

        @Column(name = "arbeidsgiver_id")
        val arbeidsgiverId: String,

        @Column(name = "arbeidsgiver_type")
        val arbeidsgiverType: String,

        @JsonIgnore
        @ManyToOne(optional = false)
        @JoinColumn(name = "fk_po_person_id", nullable = false, updatable = false)
        val person: Person
) : BaseEntitet()