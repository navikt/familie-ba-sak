package no.nav.familie.ba.sak.logg

import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.common.BaseEntitet
import javax.persistence.*

@Entity(name = "Logg")
@Table(name = "logg")
data class Logg(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "logg_seq_generator")
        @SequenceGenerator(name = "logg_seq_generator", sequenceName = "logg_seq", allocationSize = 50)
        val id: Long = 0,

        @Column(name = "fk_behandling_id")
        val behandlingId: Long,

        @Enumerated(EnumType.STRING)
        @Column(name = "type")
        val type: LoggType,

        @Column(name = "tittel")
        val tittel: String,

        @Enumerated(EnumType.STRING) @Column(name = "rolle")
        val rolle: BehandlerRolle,

        /**
         * Feltet støtter markdown frontend.
         */
        @Column(name = "tekst")
        val tekst: String
) : BaseEntitet()

enum class LoggType {
    BEHANDLING_OPPRETTET
}