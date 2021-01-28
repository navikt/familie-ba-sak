package no.nav.familie.ba.sak.opplysningsplikt

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import javax.persistence.*

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Opplysningsplikt")
@Table(name = "OPPLYSNINGSPLIKT")
data class Opplysningsplikt(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "opplysningsplikt_seq_generator")
        @SequenceGenerator(name = "opplysningsplikt_seq_generator", sequenceName = "opplysningsplikt_seq", allocationSize = 50)
        val id: Long = 0,

        @Column(name = "fk_behandling_id")
        val behandlingId: Long,

        @Enumerated(EnumType.STRING)
        @Column(name = "status")
        var status: OpplysningspliktStatus = OpplysningspliktStatus.IKKE_SATT,

        @Column(name = "begrunnelse")
        var begrunnelse: String? = null
) : BaseEntitet() {

}

enum class OpplysningspliktStatus(val visningsTekst: String) {
    IKKE_SATT("ikke vurdert"),
    MOTTATT("oppfylt"),
    IKKE_MOTTATT_AVSLAG("avslått"),
    IKKE_MOTTATT_FORTSETT("delvis oppfylt")
}