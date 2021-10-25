package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDate
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
