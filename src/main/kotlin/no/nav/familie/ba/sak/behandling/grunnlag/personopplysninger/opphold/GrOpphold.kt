package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.opphold

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.kontrakter.felles.personinfo.OPPHOLDSTILLATELSE
import javax.persistence.*

@Entity(name = "GrOpphold")
@Table(name = "PO_OPPHOLD")
data class GrOpphold(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_opphold_seq_generator")
        @SequenceGenerator(name = "po_opphold_seq_generator",
                           sequenceName = "po_opphold_seq",
                           allocationSize = 50)
        val id: Long = 0,

        @Embedded
        val gyldigPeriode: DatoIntervallEntitet? = null,

        @Column(name = "type", nullable = false)
        val type: OPPHOLDSTILLATELSE,
        
        @ManyToOne(optional = false)
        @JoinColumn(name = "fk_po_person_id", nullable = false, updatable = false)
        val person: Person
) : BaseEntitet() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GrOpphold

        if (gyldigPeriode != other.gyldigPeriode) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = gyldigPeriode.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}