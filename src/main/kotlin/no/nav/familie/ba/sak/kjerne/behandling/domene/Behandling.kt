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
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.FORTSATT_INNVILGET
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.FORTSATT_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.steg.BehandlingStegStatus
import no.nav.familie.ba.sak.kjerne.steg.FØRSTE_STEG
import no.nav.familie.ba.sak.kjerne.steg.SISTE_STEG
import no.nav.familie.ba.sak.kjerne.steg.StegType
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
    @Column(name = "aktivert_tid", nullable = false)
    var aktivertTidspunkt: LocalDateTime = LocalDateTime.now(),
) : BaseEntitet() {
    val steg: StegType
        get() = behandlingStegTilstand.last().behandlingSteg

    fun opprettBehandleSakOppgave(): Boolean =
        !skalBehandlesAutomatisk &&
            (
                type == BehandlingType.FØRSTEGANGSBEHANDLING ||
                    type == BehandlingType.REVURDERING
            )

    override fun toString(): String =
        "Behandling(" +
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

    fun validerBehandlingstype(sisteBehandlingSomErVedtatt: Behandling? = null) {
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

    fun erBehandlingMedVedtaksbrevutsending(): Boolean =
        when {
            type == BehandlingType.TEKNISK_ENDRING -> false
            opprettetÅrsak == BehandlingÅrsak.SMÅBARNSTILLEGG_ENDRING_FRAM_I_TID -> false
            erSatsendringEllerMånedligValutajustering() -> false
            erManuellMigrering() -> false
            erMigrering() -> false
            erIverksetteKAVedtak() -> false
            erFinnmarksEllerSvalbardtillegg() && resultat in setOf(FORTSATT_INNVILGET, FORTSATT_OPPHØRT) -> false
            else -> true
        }

    fun erHenlagt() =
        resultat == Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET ||
            resultat == Behandlingsresultat.HENLAGT_SØKNAD_TRUKKET ||
            resultat == Behandlingsresultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE ||
            resultat == Behandlingsresultat.HENLAGT_AUTOMATISK_SMÅBARNSTILLEGG ||
            resultat == Behandlingsresultat.HENLAGT_TEKNISK_VEDLIKEHOLD

    fun erVedtatt() = status == BehandlingStatus.AVSLUTTET && !erHenlagt()

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
        behandlingStegTilstand
            .filter { it.behandlingSteg == StegType.FERDIGSTILLE_BEHANDLING }
            .forEach { behandlingStegTilstand.remove(it) }
        leggTilStegOmDetIkkeFinnesFraFør(StegType.HENLEGG_BEHANDLING)
        return this
    }

    fun skalRettFraBehandlingsresultatTilIverksetting(erEndringFraForrigeBehandlingSendtTilØkonomi: Boolean): Boolean =
        when {
            skalBehandlesAutomatisk && erOmregning() && resultat in listOf(FORTSATT_INNVILGET, FORTSATT_OPPHØRT) -> true
            skalBehandlesAutomatisk && erMigrering() && !erManuellMigreringForEndreMigreringsdato() && resultat == Behandlingsresultat.INNVILGET -> true
            skalBehandlesAutomatisk && erFødselshendelse() -> true
            skalBehandlesAutomatisk && erSatsendring() && erEndringFraForrigeBehandlingSendtTilØkonomi -> true
            skalBehandlesAutomatisk && this.opprettetÅrsak == BehandlingÅrsak.SMÅBARNSTILLEGG_ENDRING_FRAM_I_TID && this.resultat == FORTSATT_INNVILGET -> true
            skalBehandlesAutomatisk && erMånedligValutajustering() -> true
            skalBehandlesAutomatisk && erFinnmarksEllerSvalbardtillegg() -> true
            else -> false
        }

    private fun leggTilStegOmDetIkkeFinnesFraFør(steg: StegType) {
        if (behandlingStegTilstand.none { it.behandlingSteg == steg }) {
            behandlingStegTilstand.add(
                BehandlingStegTilstand(
                    behandling = this,
                    behandlingSteg = steg,
                    behandlingStegStatus =
                        if (steg == SISTE_STEG) {
                            BehandlingStegStatus.UTFØRT
                        } else {
                            BehandlingStegStatus.IKKE_UTFØRT
                        },
                ),
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
        behandlingStegTilstand
            .filter { steg.rekkefølge < it.behandlingSteg.rekkefølge }
            .forEach {
                behandlingStegTilstand.remove(it)
            }
    }

    fun initBehandlingStegTilstand(): Behandling {
        behandlingStegTilstand.add(
            BehandlingStegTilstand(
                behandling = this,
                behandlingSteg = FØRSTE_STEG,
            ),
        )
        return this
    }

    fun erFørstegangsbehandling() = type == BehandlingType.FØRSTEGANGSBEHANDLING

    fun erRevurdering() = type == BehandlingType.REVURDERING

    fun erSmåbarnstillegg() = this.opprettetÅrsak == BehandlingÅrsak.SMÅBARNSTILLEGG

    fun erKlage() = this.opprettetÅrsak == BehandlingÅrsak.KLAGE

    fun erIverksetteKAVedtak() = this.opprettetÅrsak == BehandlingÅrsak.IVERKSETTE_KA_VEDTAK

    fun erMigrering() = type == BehandlingType.MIGRERING_FRA_INFOTRYGD || type == BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT

    fun erSatsendring() = this.opprettetÅrsak == BehandlingÅrsak.SATSENDRING

    fun erMånedligValutajustering() = this.opprettetÅrsak == BehandlingÅrsak.MÅNEDLIG_VALUTAJUSTERING

    fun erFinnmarkstillegg() = this.opprettetÅrsak == BehandlingÅrsak.FINNMARKSTILLEGG

    fun erSvalbardtillegg() = this.opprettetÅrsak == BehandlingÅrsak.SVALBARDTILLEGG

    fun erFinnmarksEllerSvalbardtillegg() = erFinnmarkstillegg() || erSvalbardtillegg()

    fun erSatsendringEllerMånedligValutajustering() = erSatsendring() || erMånedligValutajustering()

    fun erSatsendringMånedligValutajusteringFinnmarkstilleggEllerSvalbardtillegg() = erFinnmarksEllerSvalbardtillegg() || erSatsendringEllerMånedligValutajustering()

    fun erOppdaterUtvidetKlassekode() = this.opprettetÅrsak == BehandlingÅrsak.OPPDATER_UTVIDET_KLASSEKODE

    fun erAutomatiskOgSkalHaTidligereBehandling() = erSatsendringMånedligValutajusteringFinnmarkstilleggEllerSvalbardtillegg() || erSmåbarnstillegg() || erOmregning()

    fun erManuellMigreringForEndreMigreringsdato() =
        erMigrering() &&
            opprettetÅrsak == BehandlingÅrsak.ENDRE_MIGRERINGSDATO

    fun erHelmanuellMigrering() = erMigrering() && opprettetÅrsak == BehandlingÅrsak.HELMANUELL_MIGRERING

    fun erManuellMigrering() = erManuellMigreringForEndreMigreringsdato() || erHelmanuellMigrering()

    fun erEndreMigreringsdato() = opprettetÅrsak == BehandlingÅrsak.ENDRE_MIGRERINGSDATO

    fun erTekniskEndring() = opprettetÅrsak == BehandlingÅrsak.TEKNISK_ENDRING

    fun erRevurderingKlage() = type == BehandlingType.REVURDERING && opprettetÅrsak in setOf(BehandlingÅrsak.KLAGE, BehandlingÅrsak.IVERKSETTE_KA_VEDTAK)

    fun erRevurderingEllerTekniskEndring() = type == BehandlingType.REVURDERING || type == BehandlingType.TEKNISK_ENDRING

    fun erKorrigereVedtak() = opprettetÅrsak == BehandlingÅrsak.KORREKSJON_VEDTAKSBREV

    fun kanLeggeTilOgFjerneUtvidetVilkår() = erManuellMigrering() || erTekniskEndring() || erKorrigereVedtak() || erKlage() || erIverksetteKAVedtak()

    fun erOmregning() = this.opprettetÅrsak.erOmregningsårsak()

    fun erFalskIdentitet() = this.opprettetÅrsak == BehandlingÅrsak.FALSK_IDENTITET

    private fun erFødselshendelse() = this.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE

    fun harUtførtSteg(steg: StegType) =
        this.behandlingStegTilstand.any {
            it.behandlingSteg == steg && it.behandlingStegStatus == BehandlingStegStatus.UTFØRT
        }

    fun tilOppgaveBehandlingTema(): OppgaveBehandlingTema =
        when {
            this.fagsak.type == FagsakType.INSTITUSJON -> OppgaveBehandlingTema.NasjonalInstitusjon
            this.underkategori == BehandlingUnderkategori.UTVIDET -> OppgaveBehandlingTema.UtvidetBarnetrygd
            else -> OppgaveBehandlingTema.OrdinærBarnetrygd
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
enum class Behandlingsresultat(
    val displayName: String,
) {
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
    ENDRET_OG_FORTSATT_OPPHØRT(displayName = "Endret og fortsatt opphørt"),
    OPPHØRT(displayName = "Opphørt"),
    FORTSATT_OPPHØRT(displayName = "Fortsatt opphørt"),
    FORTSATT_INNVILGET(displayName = "Fortsatt innvilget"),

    // Henlagt
    HENLAGT_FEILAKTIG_OPPRETTET(displayName = "Henlagt feilaktig opprettet"),
    HENLAGT_SØKNAD_TRUKKET(displayName = "Henlagt søknad trukket"),
    HENLAGT_AUTOMATISK_FØDSELSHENDELSE(displayName = "Henlagt avslått i automatisk vilkårsvurdering (fødselshendelse)"),
    HENLAGT_AUTOMATISK_SMÅBARNSTILLEGG(displayName = "Henlagt avslått i automatisk vilkårsvurdering (småbarnstillegg)"),
    HENLAGT_TEKNISK_VEDLIKEHOLD(displayName = "Henlagt teknisk vedlikehold"),

    IKKE_VURDERT(displayName = "Ikke vurdert"),
    ;

    fun erAvslått(): Boolean = this in listOf(AVSLÅTT, AVSLÅTT_OG_OPPHØRT, AVSLÅTT_OG_ENDRET, AVSLÅTT_ENDRET_OG_OPPHØRT)

    fun erFortsattInnvilget(): Boolean = this in listOf(FORTSATT_INNVILGET, ENDRET_OG_FORTSATT_INNVILGET)
}

/**
 * Årsak er knyttet til en behandling og sier noe om hvorfor behandling ble opprettet.
 */
enum class BehandlingÅrsak(
    val visningsnavn: String,
) {
    SØKNAD("Søknad"),
    FØDSELSHENDELSE("Fødselshendelse"),
    ÅRLIG_KONTROLL("Årsak kontroll"),
    DØDSFALL_BRUKER("Dødsfall bruker"),
    NYE_OPPLYSNINGER("Nye opplysninger"),
    KLAGE("Klage"),
    TEKNISK_ENDRING("Teknisk endring"), // Brukes i tilfeller ved systemfeil og vi ønsker å iverksette mot OS på nytt
    KORREKSJON_VEDTAKSBREV("Korrigere vedtak med egen brevmal"),
    OMREGNING_6ÅR("Omregning 6 år"), // Behandlingsårsak som forsvant i forbindelse med satsendring 2024-09-01
    OMREGNING_18ÅR("Omregning 18 år"),
    OMREGNING_SMÅBARNSTILLEGG("Omregning småbarnstillegg"),
    SATSENDRING("Satsendring"),
    SMÅBARNSTILLEGG("Småbarnstillegg"),
    SMÅBARNSTILLEGG_ENDRING_FRAM_I_TID("Småbarnstillegg endring fram i tid"),
    MIGRERING("Migrering"),
    ENDRE_MIGRERINGSDATO("Endre migreringsdato"),
    HELMANUELL_MIGRERING("Manuell migrering"),
    MÅNEDLIG_VALUTAJUSTERING("Månedlig valutajustering"),
    OPPDATER_UTVIDET_KLASSEKODE("Ny klassekode for utvidet barnetrygd"),
    IVERKSETTE_KA_VEDTAK("Iverksette KA-vedtak"),
    FINNMARKSTILLEGG("Finnmarkstillegg"),
    SVALBARDTILLEGG("Svalbardtillegg"),
    FALSK_IDENTITET("Falsk identitet"),
    ;

    fun erOmregningsårsak(): Boolean = this == OMREGNING_18ÅR || this == OMREGNING_SMÅBARNSTILLEGG

    fun hentOverstyrtDokumenttittelForOmregningsbehandling(): String? =
        when (this) {
            OMREGNING_18ÅR -> "Vedtak om endret barnetrygd - barn 18 år"
            OMREGNING_SMÅBARNSTILLEGG -> "Vedtak om endret barnetrygd - småbarnstillegg"
            else -> null
        }

    fun erManuellMigreringsårsak(): Boolean = this == HELMANUELL_MIGRERING || this == ENDRE_MIGRERINGSDATO

    fun erFørstegangMigreringsårsak(): Boolean = this == HELMANUELL_MIGRERING || this == MIGRERING
}

enum class BehandlingType(
    val visningsnavn: String,
) {
    FØRSTEGANGSBEHANDLING("Førstegangsbehandling"),
    REVURDERING("Revurdering"),
    MIGRERING_FRA_INFOTRYGD("Migrering fra infotrygd"),
    MIGRERING_FRA_INFOTRYGD_OPPHØRT("Opphør migrering fra infotrygd"),
    TEKNISK_ENDRING("Teknisk endring"),
}

enum class BehandlingKategori(
    val visningsnavn: String,
    val nivå: Int,
) {
    EØS("EØS", 2),
    NASJONAL("Nasjonal", 1),
    ;

    fun tilOppgavebehandlingType(): OppgaveBehandlingType =
        when (this) {
            EØS -> OppgaveBehandlingType.EØS
            NASJONAL -> OppgaveBehandlingType.NASJONAL
        }

    fun tilRegelverk(): Regelverk =
        when (this) {
            EØS -> Regelverk.EØS
            NASJONAL -> Regelverk.NASJONAL
        }
}

fun List<BehandlingKategori>.finnHøyesteKategori(): BehandlingKategori? = this.maxByOrNull { it.nivå }

enum class BehandlingUnderkategori(
    val visningsnavn: String,
) {
    UTVIDET("Utvidet"),
    ORDINÆR("Ordinær"),
}

fun initStatus(): BehandlingStatus = BehandlingStatus.UTREDES

enum class BehandlingStatus {
    UTREDES,
    SATT_PÅ_VENT,
    SATT_PÅ_MASKINELL_VENT,
    FATTER_VEDTAK,
    IVERKSETTER_VEDTAK,
    AVSLUTTET,
    ;

    fun erLåstForVidereRedigering() = this != UTREDES

    fun erStatusIverksetterVedtakEllerAvsluttet() = this in listOf(IVERKSETTER_VEDTAK, AVSLUTTET)
}

class BehandlingStegComparator : Comparator<BehandlingStegTilstand> {
    override fun compare(
        bst1: BehandlingStegTilstand,
        bst2: BehandlingStegTilstand,
    ): Int = bst1.opprettetTidspunkt.compareTo(bst2.opprettetTidspunkt)
}
