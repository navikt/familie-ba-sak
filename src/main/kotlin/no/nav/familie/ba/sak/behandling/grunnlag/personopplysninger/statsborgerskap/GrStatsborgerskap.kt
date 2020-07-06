package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.statsborgerskap

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import javax.persistence.*

@Entity(name = "GrStatsborgerskap")
@Table(name = "PO_STATSBORGERSKAP")
data class GrStatsborgerskap(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_statsborgerskap_seq_generator")
        @SequenceGenerator(name = "po_statsborgerskap_seq_generator",
                           sequenceName = "po_statsborgerskap_seq",
                           allocationSize = 50)
        val id: Long = 0,

        @Embedded
        val gyldigPeriode: DatoIntervallEntitet? = null,

        @Column(name = "landkode", nullable = false)
        val landkode: String,

        @Enumerated(EnumType.STRING) @Column(name = "medlemskap", nullable = false)
        val medlemskap: Medlemskap = Medlemskap.UKJENT,

        @ManyToOne(optional = false)
        @JoinColumn(name = "fk_po_person_id", nullable = false, updatable = false)
        val person: Person
) : BaseEntitet() {

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
}