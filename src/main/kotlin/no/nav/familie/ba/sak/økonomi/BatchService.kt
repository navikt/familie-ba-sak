package no.nav.familie.ba.sak.økonomi

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class BatchService(val batchRepository: BatchRepository) {

    @Transactional
    fun hentLedigeBatchKjøringerFor(dato: LocalDate): Batch? {
        return batchRepository.findByKjøredatoAndLedig(dato)
    }

    @Transactional
    fun lagre(batch: Batch) {
        batchRepository.save(batch)
    }
}