package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import javax.persistence.*

@Entity(name = "GrBostedsadresseperiode")
@Table(name = "PO_BOSTEDSADRESSEPERIODE")
data class GrBostedsadresseperiode(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_bostedsadresseperiode_seq_generator")
        @SequenceGenerator(name = "po_bostedsadresseperiode_seq_generator",
                           sequenceName = "po_bostedsadresseperiode_seq",
                           allocationSize = 50)
        val id: Long = 0,

        @Embedded
        val periode: DatoIntervallEntitet? = null
) : BaseEntitet()