package no.nav.familie.ba.sak.behandling.domene

import no.nav.familie.ba.sak.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.steg.BehandlingStegStatus
import no.nav.familie.ba.sak.behandling.steg.FØRSTE_STEG
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.ba.sak.tilbakekreving.Tilbakekreving
import org.hibernate.annotations.SortComparator
import javax.persistence.*


@EntityListeners(RollestyringMotDatabase::class)
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
                true else throw Feil("Behandling er teknisk opphør, men årsak $opprettetÅrsak og type $type samsvarer ikke.")
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

    fun erKlage(): Boolean = this.opprettetÅrsak == BehandlingÅrsak.KLAGE

    fun erMigrering() = type == BehandlingType.MIGRERING_FRA_INFOTRYGD || type == BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT

    companion object {

        val comparator = BehandlingStegComparator()
    }
}

/**
 * Enum for de ulike hovedresultatene en behandling kan ha.
 *
 * Et behandlingsresultater beskriver det samlede resultatet for vurderinger gjort i inneværende behandling.
 * Behandlingsresultatet er delt opp i tre deler:
 * 1. Hvis søknad - hva er resultatet på søknaden.
 * 2. Finnes det noen andre endringer (utenom rent opphør)
 * 3. Fører behandlingen til et opphør
 *
 * @displayName benyttes for visning av resultat
 * @erStøttetIManuellBehandling benyttes til å validere om resultatet av vilkårsvurderingen er støttet i løsningen for manuell behandling.
 * Gir feilmelding til bruker dersom man vurderer noe til et resultat vi ikke støtter. Denne er midlertidig til vi støtter alle resultater.
 */
enum class BehandlingResultat(val displayName: String,
                              val erStøttetIManuellBehandling: Boolean = false) {

    // Søknad
    INNVILGET(displayName = "Innvilget", erStøttetIManuellBehandling = true),
    INNVILGET_OG_OPPHØRT(displayName = "Innvilget og opphørt", erStøttetIManuellBehandling = true),
    INNVILGET_OG_ENDRET(displayName = "Innvilget og endret", erStøttetIManuellBehandling = true),
    INNVILGET_ENDRET_OG_OPPHØRT(displayName = "Innvilget, endret og opphørt", erStøttetIManuellBehandling = true),

    DELVIS_INNVILGET(displayName = "Delvis innvilget"),
    DELVIS_INNVILGET_OG_OPPHØRT(displayName = "Delvis innvilget og opphørt"),
    DELVIS_INNVILGET_OG_ENDRET(displayName = "Delvis innvilget og endret"),
    DELVIS_INNVILGET_ENDRET_OG_OPPHØRT(displayName = "Delvis innvilget, endret og opphørt"),

    AVSLÅTT(displayName = "Avslått", erStøttetIManuellBehandling = true),
    AVSLÅTT_OG_OPPHØRT(displayName = "Avslått og opphørt", erStøttetIManuellBehandling = true),
    AVSLÅTT_OG_ENDRET(displayName = "Avslått og endret", erStøttetIManuellBehandling = true),
    AVSLÅTT_ENDRET_OG_OPPHØRT(displayName = "Avslått, endret og opphørt", erStøttetIManuellBehandling = true),

    // Revurdering uten søknad
    ENDRET(displayName = "Endret", erStøttetIManuellBehandling = true),
    ENDRET_OG_OPPHØRT(displayName = "Endret og opphørt", erStøttetIManuellBehandling = true),
    OPPHØRT(displayName = "Opphørt", erStøttetIManuellBehandling = true),
    FORTSATT_INNVILGET(displayName = "Fortsatt innvilget", erStøttetIManuellBehandling = true),

    // Henlagt
    HENLAGT_FEILAKTIG_OPPRETTET(displayName = "Henlagt feilaktig opprettet",
                                erStøttetIManuellBehandling = true),
    HENLAGT_SØKNAD_TRUKKET(displayName = "Henlagt søknad trukket", erStøttetIManuellBehandling = true),

    IKKE_VURDERT(displayName = "Ikke vurdert")
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
    KLAGE("Klage"),
    TEKNISK_OPPHØR("Teknisk opphør"), // Kan være tilbakeføring til infotrygd, feilutbetaling
    OMREGNING_6ÅR("Omregning 6 år"),
    OMREGNING_18ÅR("Omregning 18 år"),
    MIGRERING("Migrering"),
}

enum class BehandlingType(val visningsnavn: String) {
    FØRSTEGANGSBEHANDLING("Førstegangsbehandling"),
    REVURDERING("Revurdering"),
    MIGRERING_FRA_INFOTRYGD("Migrering fra infotrygd"),
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
