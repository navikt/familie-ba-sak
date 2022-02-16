package no.nav.familie.ba.sak.integrasjoner.økonomi

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DataChunkRepository : JpaRepository<DataChunk, Long>
