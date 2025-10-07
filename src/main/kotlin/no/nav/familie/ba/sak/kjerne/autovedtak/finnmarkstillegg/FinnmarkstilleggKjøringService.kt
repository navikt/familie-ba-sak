package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg

import no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg.domene.FinnmarkstilleggKjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg.domene.FinnmarkstilleggKjøringRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class FinnmarkstilleggKjøringService(
    private val finnmarkstilleggKjøringRepository: FinnmarkstilleggKjøringRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun lagreFinnmarkstilleggkjøringer(fagsakIderSomIkkeSkalOpprettesTaskFor: Set<Long>) {
        finnmarkstilleggKjøringRepository.saveAll(fagsakIderSomIkkeSkalOpprettesTaskFor.map { FinnmarkstilleggKjøring(fagsakId = it) })
    }
}
