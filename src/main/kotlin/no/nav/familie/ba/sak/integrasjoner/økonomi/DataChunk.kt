package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "DataChunk")
@Table(name = "DATA_CHUNK")
data class DataChunk(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "data_chunk_seq")
    @SequenceGenerator(name = "data_chunk_seq")
    val id: Long = 0,

    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_batch_id", nullable = false, updatable = false)
    val batch: Batch,

    @Column(name = "transaksjons_id", nullable = false)
    val transaksjonsId: UUID,

    @Column(name = "chunk_nr", nullable = false)
    val chunkNr: Int,

    @Column(name = "er_sendt", nullable = false)
    var erSendt: Boolean = false
)
