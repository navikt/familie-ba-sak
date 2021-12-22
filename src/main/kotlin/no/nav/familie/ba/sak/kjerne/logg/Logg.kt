package no.nav.familie.ba.sak.kjerne.logg

import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Logg")
@Table(name = "logg")
data class Logg(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "logg_seq_generator")
    @SequenceGenerator(name = "logg_seq_generator", sequenceName = "logg_seq", allocationSize = 50)
    val id: Long = 0,

    @Column(name = "opprettet_av", nullable = false, updatable = false)
    val opprettetAv: String = SikkerhetContext.hentSaksbehandlerNavn(),

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "fk_behandling_id")
    val behandlingId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    val type: LoggType,

    @Column(name = "tittel")
    val tittel: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "rolle")
    val rolle: BehandlerRolle,

    /**
     * Feltet støtter markdown frontend.
     */
    @Column(name = "tekst")
    val tekst: String
)

enum class LoggType(val visningsnavn: String) {
    AUTOVEDTAK_TIL_MANUELL_BEHANDLING("Autovedtak til manuell behandling"),
    FØDSELSHENDELSE("Fødselshendelse"), // Deprecated, bruk livshendelse
    LIVSHENDELSE("Livshendelse"),
    BEHANDLENDE_ENHET_ENDRET("Behandlende enhet endret"),
    BEHANDLING_OPPRETTET("Behandling opprettet"),
    BEHANDLINGSTYPE_ENDRET("Endret behandlingstype"),
    BARN_LAGT_TIL("Barn lagt til på behandling"),
    DOKUMENT_MOTTATT("Dokument ble mottatt"),
    SØKNAD_REGISTRERT("Søknaden ble registrert"),
    VILKÅRSVURDERING("Vilkårsvurdering"),
    SEND_TIL_BESLUTTER("Send til beslutter"),
    SEND_TIL_SYSTEM("Send til system"),
    GODKJENNE_VEDTAK("Godkjenne vedtak"),
    MIGRERING_BEKREFTET("Migrering bekreftet"),
    DISTRIBUERE_BREV("Distribuere brev"),
    FERDIGSTILLE_BEHANDLING("Ferdigstille behandling"),
    HENLEGG_BEHANDLING("Henlegg behandling"),
}
