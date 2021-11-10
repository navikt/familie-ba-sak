package no.nav.familie.ba.sak.kjerne.behandling.domene

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.steg.BehandlingStegStatus
import no.nav.familie.ba.sak.kjerne.steg.FØRSTE_STEG
import no.nav.familie.ba.sak.kjerne.steg.SISTE_STEG
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype
import org.hibernate.annotations.SortComparator
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.SequenceGenerator
import javax.persistence.Table

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
    @Column(name = "kategori", nullable = false, updatable = true)
    var kategori: BehandlingKategori,

    @Enumerated(EnumType.STRING)
    @Column(name = "underkategori", nullable = false, updatable = true)
    var underkategori: BehandlingUnderkategori,

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

    // Skal kun brukes på gamle behandlinger
    fun erTekniskOpphør(): Boolean {
        return if (type == BehandlingType.TEKNISK_OPPHØR ||
            opprettetÅrsak == BehandlingÅrsak.TEKNISK_OPPHØR
        ) {
            if (type == BehandlingType.TEKNISK_OPPHØR &&
                opprettetÅrsak == BehandlingÅrsak.TEKNISK_OPPHØR
            )
                true else throw Feil("Behandling er teknisk opphør, men årsak $opprettetÅrsak og type $type samsvarer ikke.")
        } else {
            false
        }
    }

    fun validerBehandling() {
        if (type == BehandlingType.TEKNISK_OPPHØR) {
            throw FunksjonellFeil(
                melding = "Kan ikke lage teknisk opphør behandling.",
                frontendFeilmelding = "Kan ikke lage teknisk opphør behandling, bruk heller teknisk endring."
            )
        }

        if (type == BehandlingType.TEKNISK_ENDRING ||
            opprettetÅrsak == BehandlingÅrsak.TEKNISK_ENDRING
        ) {
            if (type != BehandlingType.TEKNISK_ENDRING ||
                opprettetÅrsak != BehandlingÅrsak.TEKNISK_ENDRING
            )
                throw Feil("Behandling er teknisk endring, men årsak $opprettetÅrsak og type $type samsvarer ikke.")
        }
    }

    fun erBehandlingMedVedtaksbrevutsending(): Boolean {
        return type != BehandlingType.TEKNISK_ENDRING && opprettetÅrsak != BehandlingÅrsak.SATSENDRING
    }

    fun erHenlagt() =
        resultat == BehandlingResultat.HENLAGT_FEILAKTIG_OPPRETTET ||
            resultat == BehandlingResultat.HENLAGT_SØKNAD_TRUKKET ||
            resultat == BehandlingResultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE

    fun leggTilBehandlingStegTilstand(nesteSteg: StegType): Behandling {
        if (nesteSteg != StegType.HENLEGG_BEHANDLING) {
            fjernAlleSenereSteg(nesteSteg)
        }

        if (steg != nesteSteg) {
            setSisteStegSomUtført()
        } else {
            setSisteStegSomIkkeUtført()
        }

        leggTilStegOmDetIkkeFinnesFraFør(nesteSteg)
        return this
    }

    fun leggTilHenleggStegOmDetIkkeFinnesFraFør(): Behandling {
        leggTilStegOmDetIkkeFinnesFraFør(StegType.HENLEGG_BEHANDLING)
        return this
    }

    fun skalRettFraBehandlingsresultatTilIverksetting(): Boolean {
        return when {
            skalBehandlesAutomatisk && erOmregning() && resultat == BehandlingResultat.FORTSATT_INNVILGET -> true
            skalBehandlesAutomatisk && resultat == BehandlingResultat.INNVILGET -> true
            skalBehandlesAutomatisk && erSatsendring() && resultat == BehandlingResultat.ENDRET -> true
            else -> false
        }
    }

    private fun leggTilStegOmDetIkkeFinnesFraFør(steg: StegType) {
        if (behandlingStegTilstand.none { it.behandlingSteg == steg }) {
            behandlingStegTilstand.add(
                BehandlingStegTilstand(
                    behandling = this,
                    behandlingSteg = steg,
                    behandlingStegStatus = if (steg == SISTE_STEG) BehandlingStegStatus.UTFØRT else BehandlingStegStatus.IKKE_UTFØRT
                )
            )
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
        behandlingStegTilstand.add(
            BehandlingStegTilstand(
                behandling = this,
                behandlingSteg = FØRSTE_STEG
            )
        )
        return this
    }

    fun erKlage() = this.opprettetÅrsak == BehandlingÅrsak.KLAGE

    fun erMigrering() =
        type == BehandlingType.MIGRERING_FRA_INFOTRYGD || type == BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT

    fun erOmregning() =
        this.opprettetÅrsak == BehandlingÅrsak.OMREGNING_6ÅR || this.opprettetÅrsak == BehandlingÅrsak.OMREGNING_18ÅR

    fun erSatsendring() = this.opprettetÅrsak == BehandlingÅrsak.SATSENDRING

    fun hentYtelseTypeTilVilkår(): YtelseType = when (underkategori) {
        BehandlingUnderkategori.UTVIDET -> YtelseType.UTVIDET_BARNETRYGD
        else -> YtelseType.ORDINÆR_BARNETRYGD
    }

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
 */
enum class BehandlingResultat(val displayName: String) {

    // Søknad
    INNVILGET(displayName = "Innvilget"),
    INNVILGET_OG_OPPHØRT(displayName = "Innvilget og opphørt"),
    INNVILGET_OG_ENDRET(displayName = "Innvilget og endret"),
    INNVILGET_ENDRET_OG_OPPHØRT(displayName = "Innvilget, endret og opphørt"),

    DELVIS_INNVILGET(displayName = "Delvis innvilget"),
    DELVIS_INNVILGET_OG_OPPHØRT(displayName = "Delvis innvilget og opphørt"),
    DELVIS_INNVILGET_OG_ENDRET(displayName = "Delvis innvilget og endret"),
    DELVIS_INNVILGET_ENDRET_OG_OPPHØRT(displayName = "Delvis innvilget, endret og opphørt"),

    AVSLÅTT(displayName = "Avslått"),
    AVSLÅTT_OG_OPPHØRT(displayName = "Avslått og opphørt"),
    AVSLÅTT_OG_ENDRET(displayName = "Avslått og endret"),
    AVSLÅTT_ENDRET_OG_OPPHØRT(displayName = "Avslått, endret og opphørt"),

    // Revurdering uten søknad
    ENDRET(displayName = "Endret"),
    ENDRET_OG_OPPHØRT(displayName = "Endret og opphørt"),
    OPPHØRT(displayName = "Opphørt"),
    FORTSATT_INNVILGET(displayName = "Fortsatt innvilget"),

    // Henlagt
    HENLAGT_FEILAKTIG_OPPRETTET(displayName = "Henlagt feilaktig opprettet"),
    HENLAGT_SØKNAD_TRUKKET(displayName = "Henlagt søknad trukket"),
    HENLAGT_AUTOMATISK_FØDSELSHENDELSE(displayName = "Henlagt avslått i automatisk vilkårsvurdering"),

    IKKE_VURDERT(displayName = "Ikke vurdert")
}

/**
 * Årsak er knyttet til en behandling og sier noe om hvorfor behandling ble opprettet.
 */
enum class BehandlingÅrsak(val visningsnavn: String) {

    SØKNAD("Søknad"),
    FØDSELSHENDELSE("Fødselshendelse"),
    ÅRLIG_KONTROLL("Årsak kontroll"),
    DØDSFALL_BRUKER("Dødsfall bruker"),
    NYE_OPPLYSNINGER("Nye opplysninger"),
    KLAGE("Klage"),
    TEKNISK_OPPHØR("Teknisk opphør"), // Ikke lenger i bruk. Bruk heller teknisk endring
    TEKNISK_ENDRING("Teknisk endring"), // Brukes i tilfeller ved systemfeil og vi ønsker å iverksette mot OS på nytt
    KORREKSJON_VEDTAKSBREV("Korrigere vedtak med egen brevmal"),
    OMREGNING_6ÅR("Omregning 6 år"),
    OMREGNING_18ÅR("Omregning 18 år"),
    SATSENDRING("Satsendring"),
    MIGRERING("Migrering"),
}

enum class BehandlingType(val visningsnavn: String) {
    FØRSTEGANGSBEHANDLING("Førstegangsbehandling"),
    REVURDERING("Revurdering"),
    MIGRERING_FRA_INFOTRYGD("Migrering fra infotrygd"),
    MIGRERING_FRA_INFOTRYGD_OPPHØRT("Opphør migrering fra infotrygd"),
    TEKNISK_OPPHØR("Teknisk opphør"), // Ikke lenger i bruk. Bruk heller teknisk endring
    TEKNISK_ENDRING("Teknisk endring")
}

enum class BehandlingKategori(val visningsnavn: String) {
    EØS("EØS"),
    NASJONAL("Nasjonal");

    fun tilBehandlingstype(): Behandlingstype {
        return when (this) {
            EØS -> Behandlingstype.EØS
            NASJONAL -> Behandlingstype.NASJONAL
        }
    }
}

enum class BehandlingUnderkategori(val visningsnavn: String) {
    UTVIDET("Utvidet"),
    ORDINÆR("Ordinær");

    fun tilBehandlingstema(): Behandlingstema {
        return when (this) {
            UTVIDET -> Behandlingstema.UtvidetBarnetrygd
            ORDINÆR -> Behandlingstema.OrdinærBarnetrygd
        }
    }
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
