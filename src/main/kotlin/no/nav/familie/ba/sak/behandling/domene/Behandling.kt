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

        @Enumerated(EnumType.STRING)
        @Column(name = "behandling_opprinnelse", nullable = false)
        val opprinnelse: BehandlingOpprinnelse,

        @Enumerated(EnumType.STRING)
        @Column(name = "kategori", nullable = false)
        val kategori: BehandlingKategori,

        @Enumerated(EnumType.STRING)
        @Column(name = "underkategori", nullable = false)
        val underkategori: BehandlingUnderkategori,

        @Column(name = "aktiv", nullable = false)
        var aktiv: Boolean = true,

        @Column(name = "gjeldende_for_utbetaling", nullable = false)
        var gjeldendeForUtbetaling: Boolean = false,

        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false)
        var status: BehandlingStatus = BehandlingStatus.OPPRETTET,

        @Enumerated(EnumType.STRING)
        @Column(name = "steg", nullable = false)
        var steg: StegType = initSteg(null)
) : BaseEntitet() {

    override fun toString(): String {
        return "Behandling(id=$id, fagsak=${fagsak.id}, kategori=$kategori, underkategori=$underkategori)"
    }
}

/**
 * Opprinnelse er knyttet til en behandling og sier noe om hvordan behandling ble opprettet.
 */
enum class BehandlingOpprinnelse {
    MANUELL,
    AUTOMATISK_VED_FØDSELSHENDELSE,
    AUTOMATISK_VED_JOURNALFØRING;

    /**
     * Ved noen opprinnelser så skal en behandling føre til en oppgave dersom det automatiske løpet feiler.
     */
    fun skalOppretteOppgave(): Boolean {
        return when (this) {
            MANUELL -> false
            AUTOMATISK_VED_FØDSELSHENDELSE -> true
            AUTOMATISK_VED_JOURNALFØRING -> false
        }
    }
}

enum class BehandlingType(val visningsnavn: String) {
    FØRSTEGANGSBEHANDLING("Førstegangsbehandling"),
    REVURDERING("Revurdering"),
    MIGRERING_FRA_INFOTRYGD("Migrering fra infotrygd"),
    KLAGE("Klage"),
    MIGRERING_FRA_INFOTRYGD_OPPHØRT("Opphør migrering fra infotrygd"),
    TEKNISK_OPPHØR("Teknisk opphør")
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
    UNDERKJENT_AV_BESLUTTER,
    SENDT_TIL_BESLUTTER,
    GODKJENT,
    SENDT_TIL_IVERKSETTING,
    IVERKSATT,
    FERDIGSTILT
}
