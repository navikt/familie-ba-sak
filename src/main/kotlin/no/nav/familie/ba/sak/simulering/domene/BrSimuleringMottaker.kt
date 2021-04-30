package no.nav.familie.ba.sak.simulering.domene

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.simulering.MottakerType
import javax.persistence.*

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "BrSimuleringMottaker")
@Table(name = "BR_SIMULERING_MOTTAKER")
data class BrSimuleringMottaker(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "br_simulering_mottaker_seq_generator")
        @SequenceGenerator(name = "br_simulering_mottaker_seq_generator",
                           sequenceName = "br_simulering_mottaker_seq",
                           allocationSize = 50)
        val id: Long = 0,

        @Column(name = "mottaker_nummer", nullable = false)
        val mottakerNummer: String?,

        @Enumerated(EnumType.STRING)
        @Column(name = "mottaker_type", nullable = false)
        val mottakerType: MottakerType,

        @ManyToOne(optional = false) @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
        val behandling: Behandling,

        @OneToMany(mappedBy = "vedtakSimuleringMottaker",
                   cascade = [CascadeType.ALL],
                   fetch = FetchType.EAGER,
                   orphanRemoval = true)
        var brSimuleringPostering: List<BrSimuleringPostering> = emptyList(),
) : BaseEntitet() {

    override fun hashCode() = id.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is BrSimuleringMottaker) return false

        return (id == other.id)
    }

    override fun toString(): String {
        return "BrSimuleringMottaker(" +
               "id=$id, " +
               "mottakerNummer=$mottakerNummer, " +
               "mottakerType=$mottakerType, " +
               "behandling=$behandling, " +
               "vedtakSimuleringPostering=$brSimuleringPostering" +
               ")"
    }
}