package no.nav.familie.ba.sak.kjerne.institusjon

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InstitusjonService(val fagsakRepository: FagsakRepository) {

    @Transactional
    fun RegistrerInstitusjonForFagsak(fagsakId: Long, institusjon: Institusjon) {
        var fagsak = fagsakRepository.finnFagsak(fagsakId)
        if (fagsak != null && fagsak.type == FagsakType.INSTITUSJON) {
            fagsak.institusjon = institusjon
            fagsakRepository.save((fagsak))
        } else {
            throw Feil("Registrer institusjon for fagsak som er ${fagsak?.type}")
        }
    }

    @Transactional
    fun RegistrerVergeForFagsak(fagsakId: Long, verge: Verge) {
        var fagsak = fagsakRepository.finnFagsak(fagsakId)
        if (fagsak != null && fagsak.type == FagsakType.INSTITUSJON) {
            fagsak.verge = verge
            fagsakRepository.save((fagsak))
        } else {
            throw Feil("Registrer verge for fagsak som er ${fagsak?.type}")
        }
    }
}
