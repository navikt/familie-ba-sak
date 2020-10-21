package no.nav.familie.ba.sak.opplysningsplikt.domene

import no.nav.familie.ba.sak.common.BaseEntitet
import javax.persistence.*

@Entity(name = "Opplysningsplikt")
@Table(name = "OPPLYSNINGSPLIKT")
data class Opplysningsplikt(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "opplysningsplikt_seq_generator")
        @SequenceGenerator(name = "opplysningsplikt_seq_generator", sequenceName = "opplysningsplikt_seq", allocationSize = 50)
        val id: Long = 0,

        @Column(name = "fk_behandling_id")
        val behandlingId: Long,

        @Column(name = "opplysningsplikt_status")
        var opplysningspliktStatus: OpplysningspliktStatus? = null,

        @Column(name = "begrunnelse")
        var begrunnelse: String? = null
) : BaseEntitet() {

}

enum class OpplysningspliktStatus {
    MOTTATT, IKKE_MOTTATT_AVSLAG, IKKE_MOTTATT_FORTSETT
}