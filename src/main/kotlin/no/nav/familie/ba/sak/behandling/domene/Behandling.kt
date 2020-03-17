package no.nav.familie.ba.sak.behandling.domene

import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.steg.initSteg
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
        @Column(name = "steg", nullable = false)
        var steg: StegType = initSteg(null),

        @Enumerated(EnumType.STRING)
        @Column(name = "resultat", nullable = false)
        var resultat: BehandlingResultat = BehandlingResultat.IKKE_VURDERT,

        @Column(name = "begrunnelse", columnDefinition = "TEXT")
        var begrunnelse: String = ""
) : BaseEntitet() {

    override fun toString(): String {
        return "Behandling(id=$id, fagsak=${fagsak.id}, kategori=$kategori, underkategori=$underkategori)"
    }
}

fun BehandlingResultat.toDokGenTemplate(): String {
    return when (this) {
        BehandlingResultat.INNVILGET -> "Innvilget"
        BehandlingResultat.AVSLÅTT -> "Avslag"
        BehandlingResultat.OPPHØRT -> "Opphor"
        else -> error("Invalid/Unsupported vedtak result")
    }
}

enum class BehandlingType(val visningsnavn: String) {
    FØRSTEGANGSBEHANDLING("Førstegangsbehandling"),
    REVURDERING("Revurdering"),
    MIGRERING_FRA_INFOTRYGD("Migrering fra infotrygd"),
    KLAGE("Klage"),
    MIGRERING_FRA_INFOTRYGD_OPPHØRT("Opphør migrering fra infotrygd")
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
