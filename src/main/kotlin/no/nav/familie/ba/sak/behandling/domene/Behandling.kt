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
        val behandlingStegTilstand: MutableList<BehandlingStegTilstand> = mutableListOf(),

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

        @Enumerated(EnumType.STRING)
        @Column(name = "steg", nullable = false)
        var steg: StegType = initSteg()
) : BaseEntitet() {

    //TODO: Etter at oppgaven er klar skal steg fjernes og stegTemp skal endre navn til steg.
    val stegTemp: StegType
        get() = behandlingStegTilstand.firstOrNull { it.behandlingStegStatus == BehandlingStegStatus.IKKE_UTFØRT }?.behandlingSteg
                ?: steg

    fun sendVedtaksbrev(): Boolean {
        return type !== BehandlingType.MIGRERING_FRA_INFOTRYGD
               && type !== BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT
               && type !== BehandlingType.TEKNISK_OPPHØR
    }

    override fun toString(): String {
        return "Behandling(id=$id, fagsak=${fagsak.id}, kategori=$kategori, underkategori=$underkategori, steg=$steg)"
    }

    fun leggTilBehandlingStegTilstand(steg: StegType): Behandling {
        //Logg-steg for feilsøking
        behandlingStegTilstand.forEach{ LOG.info("Alle Behandlingssteg: ${it.behandlingSteg}, ${it.behandlingStegStatus}")}

        val sisteBehandlingStegTilstand = behandlingStegTilstand.filter {
            it.behandlingStegStatus == BehandlingStegStatus.IKKE_UTFØRT
        }.single()
        sisteBehandlingStegTilstand.behandlingStegStatus = BehandlingStegStatus.UTFØRT
        behandlingStegTilstand.add(BehandlingStegTilstand(behandling = this, behandlingSteg = steg))

        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} har utført ${sisteBehandlingStegTilstand.behandlingSteg}. Neste steg er $steg.")
        return this
    }

    fun initBehandlingStegTilstand(): Behandling {
        behandlingStegTilstand.add(BehandlingStegTilstand(
                behandling = this,
                behandlingSteg = initSteg(behandlingType = type, behandlingÅrsak = opprettetÅrsak)))

        behandlingStegTilstand.forEach{ LOG.info("Alle init Behandlingssteg: ${it.behandlingSteg}, ${it.behandlingStegStatus}")}
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
