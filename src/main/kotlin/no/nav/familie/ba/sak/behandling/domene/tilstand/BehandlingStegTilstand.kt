package no.nav.familie.ba.sak.behandling.domene.tilstand

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.steg.BehandlingSteg
import no.nav.familie.ba.sak.behandling.steg.BehandlingStegStatus
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.common.BaseEntitet
import javax.persistence.*

@Entity(name = "BehandlingStegTilstand")
@Table(name = "BEHANDLING_STEG_TILSTAND")
data class BehandlingStegTilstand(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "behandling_steg_tilstand_seq_generator")
        @SequenceGenerator(name = "behandling_steg_tilstand_seq_generator",
                           sequenceName = "behandling_steg_tilstand_seq",
                           allocationSize = 50)
        val id: Long = 0,

        @ManyToOne(optional = false)
        @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
        val behandling: Behandling,

        @Enumerated(EnumType.STRING)
        @Column(name = "behandling_steg", nullable = false)
        val behandlingSteg: StegType,

        @Enumerated(EnumType.STRING)
        @Column(name = "behandling_steg_status", nullable = false)
        val behandlingStegStatus: BehandlingStegStatus = BehandlingStegStatus.UDEFINERT

        ) : BaseEntitet() {

    override fun toString(): String {
        return "BehandlingStegTilstand(id=$id, beahndling=${behandling.id}, behandlingSteg=$behandlingSteg, behandlingStegStatus=$behandlingStegStatus)"
    }
}
