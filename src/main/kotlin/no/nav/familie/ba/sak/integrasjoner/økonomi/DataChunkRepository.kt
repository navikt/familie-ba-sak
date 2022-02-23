package no.nav.familie.ba.sak.integrasjoner.økonomi

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DataChunkRepository : JpaRepository<DataChunk, Long> {
    fun findByTransaksjonsIdAndChunkNr(transaksjonsId: UUID, chunkNr: Int): DataChunk
    fun findByTransaksjonsId(transaksjonsId: UUID): List<DataChunk>
    fun findByErSendt(erSendt: Boolean): List<DataChunk>
}
