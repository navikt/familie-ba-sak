package no.nav.familie.ba.sak.kjerne.behandling.domene

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
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
import no.nav.familie.ba.sak.kjerne.verge.Verge
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.Regelverk
import org.hibernate.annotations.SortComparator
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.familie.kontrakter.felles.Behandlingstema as OppgaveBehandlingTema
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype as OppgaveBehandlingType

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
    var resultat: Behandlingsresultat = Behandlingsresultat.IKKE_VURDERT,

    @Enumerated(EnumType.STRING)
    @Column(name = "behandling_type", nullable = false)
    val type: BehandlingType,

    @Enumerated(EnumType.STRING)
    @Column(name = "opprettet_aarsak", nullable = false)
    val opprettetÅrsak: BehandlingÅrsak,

    @Column(name = "skal_behandles_automatisk", nullable = false, updatable = true)
    var skalBehandlesAutomatisk: Boolean = false,

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

    var overstyrtEndringstidspunkt: LocalDate? = null,

    @OneToOne(mappedBy = "behandling", optional = true)
    val verge: Verge? = null
) : BaseEntitet() {

    val steg: StegType
        get() = behandlingStegTilstand.last().behandlingSteg

    fun opprettBehandleSakOppgave(): Boolean {
        return !skalBehandlesAutomatisk && (
            type == BehandlingType.FØRSTEGANGSBEHANDLING ||
                type == BehandlingType.REVURDERING
            )
    }

    override fun toString(): String {
        return "Behandling(" +
            "id=$id, " +
            "fagsak=${fagsak.id}, " +
            "fagsakType=${fagsak.type}, " +
            "type=$type, " +
            "kategori=$kategori, " +
            "underkategori=$underkategori, " +
            "automatisk=$skalBehandlesAutomatisk, " +
            "opprettetÅrsak=$opprettetÅrsak, " +
            "status=$status, " +
            "resultat=$resultat, " +
            "steg=$steg)"
    }

    fun låstForEndringerTidspunkt(): LocalDateTime? = this.behandlingStegTilstand
        .filter { it.behandlingSteg.rekkefølge >= StegType.BESLUTTE_VEDTAK.rekkefølge }
        .minOfOrNull { it.opprettetTidspunkt }

    // Skal kun brukes på gamle behandlinger
    fun erTekniskOpphør(): Boolean {
        return if (type == BehandlingType.TEKNISK_OPPHØR ||
            opprettetÅrsak == BehandlingÅrsak.TEKNISK_OPPHØR
        ) {
            if (type == BehandlingType.TEKNISK_OPPHØR &&
                opprettetÅrsak == BehandlingÅrsak.TEKNISK_OPPHØR
            ) {
                true
            } else {
                throw Feil(
                    "Behandling er teknisk opphør, men årsak $opprettetÅrsak " +
                        "og type $type samsvarer ikke."
                )
            }
        } else {
            false
        }
    }

    fun validerBehandlingstype(sisteBehandlingSomErVedtatt: Behandling? = null) {
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
            ) {
                throw Feil("Behandling er teknisk endring, men årsak $opprettetÅrsak og type $type samsvarer ikke.")
            }
        }

        if (type == BehandlingType.REVURDERING && sisteBehandlingSomErVedtatt == null) {
            throw Feil("Kan ikke opprette revurdering på $fagsak uten noen andre behandlinger som er vedtatt")
        }
    }

    fun erBehandlingMedVedtaksbrevutsending(): Boolean {
        return when {
            type == BehandlingType.TEKNISK_ENDRING -> false
            opprettetÅrsak == BehandlingÅrsak.SATSENDRING -> false
            erManuellMigrering() -> false
            erMigrering() -> false
            else -> true
        }
    }

    fun erHenlagt() =
        resultat == Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET ||
            resultat == Behandlingsresultat.HENLAGT_SØKNAD_TRUKKET ||
            resultat == Behandlingsresultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE ||
            resultat == Behandlingsresultat.HENLAGT_TEKNISK_VEDLIKEHOLD

    fun erVedtatt() = status == BehandlingStatus.AVSLUTTET && !erHenlagt()

    fun erSøknad() = opprettetÅrsak == BehandlingÅrsak.SØKNAD

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

    fun skalRettFraBehandlingsresultatTilIverksetting(erEndringFraForrigeBehandlingSendtTilØkonomi: Boolean): Boolean {
        return when {
            skalBehandlesAutomatisk && erOmregning() &&
                resultat in listOf(Behandlingsresultat.FORTSATT_INNVILGET, Behandlingsresultat.FORTSATT_OPPHØRT) -> true

            skalBehandlesAutomatisk && erMigrering() && resultat == Behandlingsresultat.INNVILGET -> true
            skalBehandlesAutomatisk && erFødselshendelse() && resultat == Behandlingsresultat.INNVILGET -> true
            skalBehandlesAutomatisk && erSatsendring() && erEndringFraForrigeBehandlingSendtTilØkonomi -> true
            else -> false
        }
    }

    private fun leggTilStegOmDetIkkeFinnesFraFør(steg: StegType) {
        if (behandlingStegTilstand.none { it.behandlingSteg == steg }) {
            behandlingStegTilstand.add(
                BehandlingStegTilstand(
                    behandling = this,
                    behandlingSteg = steg,
                    behandlingStegStatus = if (steg == SISTE_STEG) {
                        BehandlingStegStatus.UTFØRT
                    } else {
                        BehandlingStegStatus.IKKE_UTFØRT
                    }
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

    fun erSmåbarnstillegg() = this.opprettetÅrsak == BehandlingÅrsak.SMÅBARNSTILLEGG

    fun erKlage() = this.opprettetÅrsak == BehandlingÅrsak.KLAGE

    fun erMigrering() =
        type == BehandlingType.MIGRERING_FRA_INFOTRYGD || type == BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT

    fun erSatsendring() = this.opprettetÅrsak == BehandlingÅrsak.SATSENDRING

    fun erManuellMigreringForEndreMigreringsdato() = erMigrering() &&
        opprettetÅrsak == BehandlingÅrsak.ENDRE_MIGRERINGSDATO

    fun erHelmanuellMigrering() = erMigrering() && opprettetÅrsak == BehandlingÅrsak.HELMANUELL_MIGRERING

    fun erManuellMigrering() = erManuellMigreringForEndreMigreringsdato() || erHelmanuellMigrering()

    fun erAutomatiskEøsMigrering() =
        erMigrering() && opprettetÅrsak == BehandlingÅrsak.MIGRERING && kategori == BehandlingKategori.EØS

    fun erTekniskEndring() = opprettetÅrsak == BehandlingÅrsak.TEKNISK_ENDRING

    fun erTekniskEndringMedOpphør() =
        erTekniskEndring() && resultat in listOf(Behandlingsresultat.OPPHØRT, Behandlingsresultat.ENDRET_OG_OPPHØRT)

    fun erTekniskBehandling() = opprettetÅrsak == BehandlingÅrsak.TEKNISK_OPPHØR || erTekniskEndring()

    fun erKorrigereVedtak() = opprettetÅrsak == BehandlingÅrsak.KORREKSJON_VEDTAKSBREV

    fun kanLeggeTilOgFjerneUtvidetVilkår() =
        erManuellMigrering() || erTekniskEndring() || erKorrigereVedtak() || erKlage()

    private fun erOmregning() =
        this.opprettetÅrsak.erOmregningsårsak()

    private fun erFødselshendelse() = this.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE

    fun hentYtelseTypeTilVilkår(): YtelseType = when (underkategori) {
        BehandlingUnderkategori.UTVIDET -> YtelseType.UTVIDET_BARNETRYGD
        BehandlingUnderkategori.ORDINÆR -> YtelseType.ORDINÆR_BARNETRYGD
        BehandlingUnderkategori.INSTITUSJON -> YtelseType.ORDINÆR_BARNETRYGD
    }

    fun harUtførtSteg(steg: StegType) =
        this.behandlingStegTilstand.any {
            it.behandlingSteg == steg && it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
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
enum class Behandlingsresultat(val displayName: String) {

    // Søknad
    INNVILGET(displayName = "Innvilget"),
    INNVILGET_OG_OPPHØRT(displayName = "Innvilget og opphørt"),
    INNVILGET_OG_ENDRET(displayName = "Innvilget og endret"),
    INNVILGET_ENDRET_OG_OPPHØRT(displayName = "Innvilget, endret og opphørt"),
    ENDRET_OG_FORTSATT_INNVILGET("Endret og fortsatt innvilget"),

    DELVIS_INNVILGET(displayName = "Delvis innvilget"),
    DELVIS_INNVILGET_OG_OPPHØRT(displayName = "Delvis innvilget og opphørt"),
    DELVIS_INNVILGET_OG_ENDRET(displayName = "Delvis innvilget og endret"),
    DELVIS_INNVILGET_ENDRET_OG_OPPHØRT(displayName = "Delvis innvilget, endret og opphørt"),

    AVSLÅTT(displayName = "Avslått"),
    AVSLÅTT_OG_OPPHØRT(displayName = "Avslått og opphørt"),
    AVSLÅTT_OG_ENDRET(displayName = "Avslått og endret"),
    AVSLÅTT_ENDRET_OG_OPPHØRT(displayName = "Avslått, endret og opphørt"),

    // Revurdering uten søknad
    ENDRET_UTBETALING(displayName = "Endret utbetaling"),
    ENDRET_UTEN_UTBETALING(displayName = "Endret, uten endret utbetaling"),
    ENDRET_OG_OPPHØRT(displayName = "Endret og opphørt"),
    OPPHØRT(displayName = "Opphørt"),
    FORTSATT_OPPHØRT(displayName = "Fortsatt opphørt"),
    FORTSATT_INNVILGET(displayName = "Fortsatt innvilget"),

    // Henlagt
    HENLAGT_FEILAKTIG_OPPRETTET(displayName = "Henlagt feilaktig opprettet"),
    HENLAGT_SØKNAD_TRUKKET(displayName = "Henlagt søknad trukket"),
    HENLAGT_AUTOMATISK_FØDSELSHENDELSE(displayName = "Henlagt avslått i automatisk vilkårsvurdering"),
    HENLAGT_TEKNISK_VEDLIKEHOLD(displayName = "Henlagt teknisk vedlikehold"),

    IKKE_VURDERT(displayName = "Ikke vurdert");

    fun kanIkkeSendesTilOppdrag(): Boolean =
        this in listOf(FORTSATT_INNVILGET, AVSLÅTT, FORTSATT_OPPHØRT, ENDRET_UTEN_UTBETALING)

    fun erAvslått(): Boolean = this in listOf(AVSLÅTT, AVSLÅTT_OG_OPPHØRT, AVSLÅTT_OG_ENDRET, AVSLÅTT_ENDRET_OG_OPPHØRT)

    fun erFortsattInnvilget(): Boolean = this in listOf(FORTSATT_INNVILGET, ENDRET_OG_FORTSATT_INNVILGET)
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
    OMREGNING_SMÅBARNSTILLEGG("Omregning småbarnstillegg"),
    SATSENDRING("Satsendring"),
    SMÅBARNSTILLEGG("Småbarnstillegg"),
    MIGRERING("Migrering"),
    ENDRE_MIGRERINGSDATO("Endre migreringsdato"),
    HELMANUELL_MIGRERING("Manuell migrering");

    fun erOmregningsårsak(): Boolean =
        this == OMREGNING_6ÅR || this == OMREGNING_18ÅR || this == OMREGNING_SMÅBARNSTILLEGG

    fun hentOverstyrtDokumenttittelForOmregningsbehandling(): String? {
        return when (this) {
            OMREGNING_6ÅR -> "Vedtak om endret barnetrygd - barn 6 år"
            OMREGNING_18ÅR -> "Vedtak om endret barnetrygd - barn 18 år"
            OMREGNING_SMÅBARNSTILLEGG -> "Vedtak om endret barnetrygd - småbarnstillegg"
            else -> null
        }
    }

    fun erManuellMigreringsårsak(): Boolean = this == HELMANUELL_MIGRERING || this == ENDRE_MIGRERINGSDATO

    fun erFørstegangMigreringsårsak(): Boolean = this == HELMANUELL_MIGRERING || this == MIGRERING

    fun årsakSomKanEndreBehandlingKategori(): Boolean =
        this == SØKNAD || this == ÅRLIG_KONTROLL || this == NYE_OPPLYSNINGER ||
            this == KLAGE || this == ENDRE_MIGRERINGSDATO || this == MIGRERING || this == HELMANUELL_MIGRERING
}

enum class BehandlingType(val visningsnavn: String) {
    FØRSTEGANGSBEHANDLING("Førstegangsbehandling"),
    REVURDERING("Revurdering"),
    MIGRERING_FRA_INFOTRYGD("Migrering fra infotrygd"),
    MIGRERING_FRA_INFOTRYGD_OPPHØRT("Opphør migrering fra infotrygd"),
    TEKNISK_OPPHØR("Teknisk opphør"), // Ikke lenger i bruk. Bruk heller teknisk endring
    TEKNISK_ENDRING("Teknisk endring")
}

enum class BehandlingKategori(val visningsnavn: String, val nivå: Int) {
    EØS("EØS", 2),
    NASJONAL("Nasjonal", 1);

    fun tilOppgavebehandlingType(): OppgaveBehandlingType {
        return when (this) {
            EØS -> OppgaveBehandlingType.EØS
            NASJONAL -> OppgaveBehandlingType.NASJONAL
        }
    }

    fun tilRegelverk(): Regelverk =
        when (this) {
            EØS -> Regelverk.EØS
            NASJONAL -> Regelverk.NASJONAL
        }
}

fun List<BehandlingKategori>.finnHøyesteKategori(): BehandlingKategori? = this.maxByOrNull { it.nivå }

enum class BehandlingUnderkategori(val visningsnavn: String, val nivå: Int) {
    INSTITUSJON("Institusjon", 3),
    UTVIDET("Utvidet", 2),
    ORDINÆR("Ordinær", 1);

    fun tilOppgaveBehandlingTema(): OppgaveBehandlingTema {
        return when (this) {
            INSTITUSJON -> OppgaveBehandlingTema.NasjonalInstitusjon
            UTVIDET -> OppgaveBehandlingTema.UtvidetBarnetrygd
            ORDINÆR -> OppgaveBehandlingTema.OrdinærBarnetrygd
        }
    }
}

fun List<BehandlingUnderkategori?>.finnHøyesteKategori(): BehandlingUnderkategori? =
    this.filterNotNull().maxByOrNull { it.nivå }

fun initStatus(): BehandlingStatus {
    return BehandlingStatus.UTREDES
}

enum class BehandlingStatus {
    OPPRETTET, // Denne brukes ikke
    UTREDES,
    FATTER_VEDTAK,
    IVERKSETTER_VEDTAK,
    AVSLUTTET,
    SATT_PÅ_VENT;

    fun erLåstMenIkkeAvsluttet() = this == FATTER_VEDTAK || this == IVERKSETTER_VEDTAK
}

fun BehandlingStatus.erÅpen(): Boolean {
    return this != BehandlingStatus.AVSLUTTET
}

class BehandlingStegComparator : Comparator<BehandlingStegTilstand> {

    override fun compare(bst1: BehandlingStegTilstand, bst2: BehandlingStegTilstand): Int {
        return bst1.opprettetTidspunkt.compareTo(bst2.opprettetTidspunkt)
    }
}
