package no.nav.familie.ba.sak.integrasjoner.oppgave.domene

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import java.time.LocalDateTime
import javax.persistence.*

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Oppgave")
@Table(name = "OPPGAVE")
data class DbOppgave(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "oppgave_seq_generator")
        @SequenceGenerator(name = "oppgave_seq_generator", sequenceName = "oppgave_seq", allocationSize = 50)
        val id: Long = 0,

        @ManyToOne
        @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
        val behandling: Behandling,

        @Column(name = "gsak_id", nullable = false, updatable = false)
        val gsakId: String,

        @Enumerated(EnumType.STRING)
        @Column(name = "type", nullable = false, updatable = false)
        val type: Oppgavetype,

        @Column(name = "opprettet_tid", nullable = false, updatable = false)
        val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),

        @Column(name = "ferdigstilt", nullable = false, updatable = true)
        var erFerdigstilt: Boolean = false
)

