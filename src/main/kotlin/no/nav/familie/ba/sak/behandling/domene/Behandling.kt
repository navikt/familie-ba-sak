package no.nav.familie.ba.sak.behandling.domene

import no.nav.familie.ba.sak.common.BaseEntitet
import javax.persistence.*

@Entity(name = "Behandling")
@Table(name = "BEHANDLING")
data class Behandling(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "behandling_seq_generator")
        @SequenceGenerator(name = "behandling_seq_generator", sequenceName = "behandling_seq", allocationSize = 50)
        val id: Long = 0,

        @ManyToOne(optional = false)
        @JoinColumn(name = "fk_fagsak_id", nullable = false, updatable = false)
        val fagsak: Fagsak,

        @Column(name = "journalpost_id")
        val journalpostID: String? = null,

        @Enumerated(EnumType.STRING)
        @Column(name = "behandling_type", nullable = false)
        val type: BehandlingType,

        @Column(name = "oppgaveId")
        val oppgaveId: String? = null,

        @Enumerated(EnumType.STRING)
        @Column(name = "kategori", nullable = false)
        val kategori: BehandlingKategori,

        @Enumerated(EnumType.STRING)
        @Column(name = "underkategori", nullable = false)
        val underkategori: BehandlingUnderkategori,

        @Column(name = "aktiv", nullable = false)
        var aktiv: Boolean = true,

        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false)
        var status: BehandlingStatus = BehandlingStatus.OPPRETTET,

        @Enumerated(EnumType.STRING)
        @Column(name = "resultat", nullable = false)
        val resultat: BehandlingResultat = BehandlingResultat.IKKE_VURDERT,

        @Column(name = "begrunnelse", columnDefinition = "TEXT")
        val begrunnelse: String = ""
) : BaseEntitet()

fun BehandlingResultat.toDokGenTemplate(): String {
    return when (this) {
        BehandlingResultat.INNVILGET -> "Innvilget"
        BehandlingResultat.AVSLÅTT -> "Avslag"
        BehandlingResultat.OPPHØRT -> "Opphørt"
        else -> error("Invalid/Unsupported vedtak result")
    }
}

enum class BehandlingType {
    FØRSTEGANGSBEHANDLING,
    REVURDERING,
    MIGRERING_FRA_INFOTRYGD,
    KLAGE,
    MIGRERING_FRA_INFOTRYGD_OPPHØRT
}

enum class BehandlingResultat {
    IKKE_VURDERT, INNVILGET, AVSLÅTT, OPPHØRT, HENLAGT
}

enum class BehandlingKategori {
    EØS,
    NASJONAL
}

enum class BehandlingUnderkategori {
    UTVIDET,
    ORDINÆR
}

enum class BehandlingStatus {
    OPPRETTET,
    UNDER_BEHANDLING,
    SENDT_TIL_BESLUTTER,
    GODKJENT,
    LAGT_PA_KO_FOR_SENDING_MOT_OPPDRAG,
    SENDT_TIL_IVERKSETTING,
    IVERKSATT,
    FERDIGSTILT
}
