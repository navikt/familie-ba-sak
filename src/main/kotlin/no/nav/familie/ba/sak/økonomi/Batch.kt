package no.nav.familie.ba.sak.økonomi

import java.time.LocalDate
import javax.persistence.*

@Entity(name = "Batch")
@Table(name = "BATCH")
data class Batch(

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "batch_seq")
        @SequenceGenerator(name = "batch_seq")
        val id: Long = 0,

        @Column(name = "kjoredato", nullable = false)
        val kjøreDato: LocalDate,

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        var status: KjøreStatus = KjøreStatus.LEDIG
)