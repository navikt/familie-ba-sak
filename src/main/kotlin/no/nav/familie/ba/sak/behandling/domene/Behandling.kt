package no.nav.familie.ba.sak.behandling.domene

import no.nav.familie.ba.sak.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.steg.BehandlingStegStatus
import no.nav.familie.ba.sak.behandling.steg.FØRSTE_STEG
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.Feil
import org.hibernate.annotations.SortComparator
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

        @OneToMany(mappedBy = "behandling", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
        @SortComparator(BehandlingStegComparator::class)
        val behandlingStegTilstand: MutableSet<BehandlingStegTilstand> = sortedSetOf(comparator),

        @Enumerated(EnumType.STRING)
        @Column(name = "resultat", nullable = false)
        var resultat: BehandlingResultat = BehandlingResultat.IKKE_VURDERT,

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

        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false)
        var status: BehandlingStatus = initStatus(),
) : BaseEntitet() {

    val steg: StegType
        get() = behandlingStegTilstand.last().behandlingSteg

    fun sendVedtaksbrev(): Boolean {
        return type !== BehandlingType.MIGRERING_FRA_INFOTRYGD
               && type !== BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT
               && type !== BehandlingType.TEKNISK_OPPHØR
    }

    fun opprettBehandleSakOppgave(): Boolean {
        return !skalBehandlesAutomatisk && (type == BehandlingType.FØRSTEGANGSBEHANDLING || type == BehandlingType.REVURDERING)
    }

    override fun toString(): String {
        return "Behandling(id=$id, fagsak=${fagsak.id}, kategori=$kategori, underkategori=$underkategori, steg=$steg)"
    }

    fun erTekniskOpphør(): Boolean {
        return if (type == BehandlingType.TEKNISK_OPPHØR
                   || opprettetÅrsak == BehandlingÅrsak.TEKNISK_OPPHØR) {
            if (type == BehandlingType.TEKNISK_OPPHØR
                && opprettetÅrsak == BehandlingÅrsak.TEKNISK_OPPHØR)
                true else throw Feil("Behandling er teknisk opphør, men årsak ${opprettetÅrsak} og type ${type} samsvarer ikke.")
        } else {
            false
        }
    }

    fun erHenlagt() =
            resultat == BehandlingResultat.HENLAGT_FEILAKTIG_OPPRETTET || resultat == BehandlingResultat.HENLAGT_SØKNAD_TRUKKET

    fun leggTilBehandlingStegTilstand(steg: StegType): Behandling {
        if (steg != StegType.HENLEGG_SØKNAD) {
            fjernAlleSenereSteg(steg)
            setSisteStegSomUtført()
        }

        leggTilStegOmDetIkkeFinnesFraFør(steg)

        if (steg == StegType.HENLEGG_SØKNAD || steg == StegType.BEHANDLING_AVSLUTTET) {
            setSisteStegSomUtført()
        } else {
            setSisteStegSomIkkeUtført()
        }
        return this
    }

    private fun leggTilStegOmDetIkkeFinnesFraFør(steg: StegType) {
        if (!behandlingStegTilstand.any { it.behandlingSteg == steg }) {
            behandlingStegTilstand.add(BehandlingStegTilstand(behandling = this, behandlingSteg = steg))
        }
    }

    private fun setSisteStegSomUtført() {
        behandlingStegTilstand.last().behandlingStegStatus = BehandlingStegStatus.UTFØRT
    }

    private fun setSisteStegSomIkkeUtført() {
        behandlingStegTilstand.last().behandlingStegStatus = BehandlingStegStatus.IKKE_UTFØRT
    }

    private fun fjernAlleSenereSteg(steg: StegType) {
        behandlingStegTilstand.filter { steg.rekkefølge < it.behandlingSteg.rekkefølge }
                .forEach {
                    behandlingStegTilstand.remove(it)
                }
    }

    fun initBehandlingStegTilstand(): Behandling {
        behandlingStegTilstand.add(BehandlingStegTilstand(
                behandling = this,
                behandlingSteg = FØRSTE_STEG))
        return this
    }

    companion object {

        val comparator = BehandlingStegComparator()
    }
}

enum class BehandlingResultat(val brevMal: String, val displayName: String) {
    INNVILGET(brevMal = "innvilget", displayName = "Innvilget"),
    ENDRING_OG_LØPENDE(brevMal = "endring_og_lopende", displayName = "Endring og løpende"),
    ENDRING_OG_OPPHØRT(brevMal = "endring_og_opphort", displayName = "Endring og opphør"),
    OPPHØRT(brevMal = "opphor", displayName = "Opphørt"),
    AVSLÅTT(brevMal = "avslag", displayName = "Avslått"),
    FORTSATT_INNVILGET(brevMal = "ukjent", displayName = "Fortsatt innvilget"),
    DELVIS_INNVILGET(brevMal = "ukjent", displayName = "Delvis innvilget"),
    HENLAGT_FEILAKTIG_OPPRETTET(brevMal = "ukjent", displayName = "Henlagt feilaktig opprettet"),
    HENLAGT_SØKNAD_TRUKKET(brevMal = "ukjent", displayName = "Henlagt søknad trukket"),
    IKKE_VURDERT(brevMal = "ukjent", displayName = "Ikke vurdert"),
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
    TEKNISK_OPPHØR("Teknisk opphør") // Kan være tilbakeføring til infotrygd, feilutbetaling
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

class BehandlingStegComparator : Comparator<BehandlingStegTilstand> {

    override fun compare(bst1: BehandlingStegTilstand, bst2: BehandlingStegTilstand): Int {
        return bst1.opprettetTidspunkt.compareTo(bst2.opprettetTidspunkt)
    }
}
