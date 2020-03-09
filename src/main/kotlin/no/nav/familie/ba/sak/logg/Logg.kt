package no.nav.familie.ba.sak.logg

import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import java.time.LocalDateTime
import javax.persistence.*

@Entity(name = "Logg")
@Table(name = "logg")
data class Logg(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "logg_seq_generator")
        @SequenceGenerator(name = "logg_seq_generator", sequenceName = "logg_seq", allocationSize = 50)
        val id: Long = 0,

        @Column(name = "opprettet_av", nullable = false, updatable = false)
        val opprettetAv: String = SikkerhetContext.hentSaksbehandler(),

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

enum class LoggType {
    BEHANDLING_OPPRETTET
}