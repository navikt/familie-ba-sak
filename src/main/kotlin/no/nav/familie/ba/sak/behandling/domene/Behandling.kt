package no.nav.familie.ba.sak.behandling.domene

import no.nav.familie.ba.sak.behandling.domene.vilkår.SamletVilkårResultat
import no.nav.familie.ba.sak.common.BaseEntitet
import javax.persistence.*

@Entity(name = "Behandling")
@Table(name = "BEHANDLING")
data class Behandling(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "behandling_seq")
        @SequenceGenerator(name = "behandling_seq")
        val id: Long? = null,

        @ManyToOne(optional = false)
        @JoinColumn(name = "fk_fagsak_id", nullable = false, updatable = false)
        val fagsak: Fagsak,

        @Column(name = "journalpost_id")
        val journalpostID: String? = null,

        @Enumerated(EnumType.STRING)
        @Column(name = "behandling_type", nullable = false)
        val type: BehandlingType,

        /**
         * saksnummer fra GSAK.
         */
        @Column(name = "saksnummer")
        val saksnummer: String? = null,

        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false)
        var status: BehandlingStatus = BehandlingStatus.OPPRETTET,

        @ManyToOne
        @JoinColumn(name = "samlet_vilkar_resultat_id", updatable = false, nullable = true)
        var samletVilkårResultat: SamletVilkårResultat? = null,

        @Column(name = "aktiv", nullable = false)
        var aktiv: Boolean = true,

        @Column(name = "oppgaveId")
        val oppgaveId: String? = null
) : BaseEntitet()

enum class BehandlingType {
    FØRSTEGANGSBEHANDLING,
    REVURDERING,
    MIGRERING_FRA_INFOTRYGD,
    KLAGE,
    MIGRERING_FRA_INFOTRYGD_OPPHØRT
}

enum class BehandlingKategorier(gyldigeUnderkategorier: List<BehandlingUnderkategorier>) {
    ORDINÆR(listOf(BehandlingUnderkategorier.EØS, BehandlingUnderkategorier.NATIONAL)),
    UTVIDET(listOf(BehandlingUnderkategorier.EØS, BehandlingUnderkategorier.NATIONAL))
}

enum class BehandlingUnderkategorier {
    EØS,
    NATIONAL
}

enum class BehandlingStatus {
    OPPRETTET, UNDER_BEHANDLING, LAGT_PA_KO_FOR_SENDING_MOT_OPPDRAG, SENDT_TIL_IVERKSETTING, IVERKSATT
}
