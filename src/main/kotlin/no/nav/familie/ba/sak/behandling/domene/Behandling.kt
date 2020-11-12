package no.nav.familie.ba.sak.behandling.domene

import no.nav.familie.ba.sak.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.steg.BehandlingStegStatus
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.steg.initSteg
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
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

        @OneToMany(mappedBy = "behandling", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
        val behandlingStegTilstand: MutableSet<BehandlingStegTilstand> = mutableSetOf(),

        @Enumerated(EnumType.STRING)
        @Column(name = "behandling_type", nullable = false)
        val type: BehandlingType,

        @Enumerated(EnumType.STRING)
        @Column(name = "opprettet_aarsak", nullable = false)
        val opprettetÅrsak: BehandlingÅrsak,

        @Column(name = "skal_behandles_automatisk", nullable = false, updatable = false)
        val skalBehandlesAutomatisk: Boolean = false,

        @Enumerated(EnumType.STRING)
        @Column(name = "kategori", nullable = false)
        val kategori: BehandlingKategori,

        @Enumerated(EnumType.STRING)
        @Column(name = "underkategori", nullable = false)
        val underkategori: BehandlingUnderkategori,

        @Column(name = "aktiv", nullable = false)
        var aktiv: Boolean = true,

        @Column(name = "gjeldende_for_fremtidig_utbetaling", nullable = false)
        var gjeldendeForFremtidigUtbetaling: Boolean = false,

        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false)
        var status: BehandlingStatus = initStatus(),
) : BaseEntitet() {

    val steg: StegType
        get() = behandlingStegTilstand.singleOrNull { it.behandlingStegStatus == BehandlingStegStatus.IKKE_UTFØRT }
                ?.behandlingSteg ?: behandlingStegTilstandSortert().last().behandlingSteg

    fun behandlingStegTilstandSortert(): List<BehandlingStegTilstand> {
        return behandlingStegTilstand.sortedBy { it.opprettetTidspunkt }
    }

    fun sendVedtaksbrev(): Boolean {
        return type !== BehandlingType.MIGRERING_FRA_INFOTRYGD
               && type !== BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT
               && type !== BehandlingType.TEKNISK_OPPHØR
    }

    override fun toString(): String {
        return "Behandling(id=$id, fagsak=${fagsak.id}, kategori=$kategori, underkategori=$underkategori, steg=$steg)"
    }

    fun leggTilBehandlingStegTilstand(steg: StegType): Behandling {
        if (steg != StegType.HENLEGG_SØKNAD) {
            behandlingStegTilstand.filter { steg.rekkefølge < it.behandlingSteg.rekkefølge }
                    .forEach { behandlingStegTilstand.remove(it) }

            behandlingStegTilstand.sortedBy { it.opprettetTidspunkt }.last().behandlingStegStatus = BehandlingStegStatus.UTFØRT
        }

        if (!behandlingStegTilstand.any{ it.behandlingSteg == steg }) {
            behandlingStegTilstand.add(BehandlingStegTilstand(behandling = this, behandlingSteg = steg))
        }

        if (steg == StegType.HENLEGG_SØKNAD || steg == StegType.BEHANDLING_AVSLUTTET) {
            behandlingStegTilstand.sortedBy { it.opprettetTidspunkt }.last().behandlingStegStatus = BehandlingStegStatus.UTFØRT
        } else {
            behandlingStegTilstand.sortedBy { it.opprettetTidspunkt }.last().behandlingStegStatus = BehandlingStegStatus.IKKE_UTFØRT
        }

        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()}. Neste steg er $steg.")
        return this
    }

    fun initBehandlingStegTilstand(): Behandling {
        behandlingStegTilstand.add(BehandlingStegTilstand(
                behandling = this,
                behandlingSteg = initSteg(behandlingType = type, behandlingÅrsak = opprettetÅrsak)))
        return this
    }

    companion object {

        val LOG: Logger = LoggerFactory.getLogger(Behandling::class.java)
    }
}

/**
 * Årsak er knyttet til en behandling og sier noe om hvorfor behandling ble opprettet.
 */
enum class BehandlingÅrsak(val visningsnavn: String) {

    SØKNAD("Søknad"),
    FØDSELSHENDELSE("Fødselshendelse"),
    ÅRLIG_KONTROLL("Årsak kontroll"),
    DØDSFALL("Dødsfall"),
    NYE_OPPLYSNINGER("Nye opplysninger"),
    TEKNISK_OPPHØR("Teknisk opphør")
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

fun initStatus(): BehandlingStatus {
    return BehandlingStatus.UTREDES
}

enum class BehandlingStatus {
    OPPRETTET,
    UTREDES,
    FATTER_VEDTAK,
    IVERKSETTER_VEDTAK,
    AVSLUTTET,
}
