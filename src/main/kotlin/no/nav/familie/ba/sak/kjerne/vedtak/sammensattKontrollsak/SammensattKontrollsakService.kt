package no.nav.familie.ba.sak.kjerne.vedtak.sammensattKontrollsak

import no.nav.familie.ba.sak.ekstern.restDomene.RestOpprettSammensattKontrollsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestSammensattKontrollsak
import no.nav.familie.ba.sak.ekstern.restDomene.tilSammensattKontrollsak
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SammensattKontrollsakService(
    private val sammensattKontrollsakRepository: SammensattKontrollsakRepository,
) {
    fun finnSammensattKontrollsak(behandlingId: Long): SammensattKontrollsak? = sammensattKontrollsakRepository.finnSammensattKontrollsakForBehandling(behandlingId = behandlingId)

    @Transactional
    fun opprettSammensattKontrollsak(
        restOpprettSammensattKontrollsak: RestOpprettSammensattKontrollsak,
    ): SammensattKontrollsak = sammensattKontrollsakRepository.save(restOpprettSammensattKontrollsak.tilSammensattKontrollsak())

    @Transactional
    fun oppdaterSammensattKontrollsak(
        restSammensattKontrollsak: RestSammensattKontrollsak,
    ): SammensattKontrollsak {
        val sammensattKontrollsak = sammensattKontrollsakRepository.hentSammensattKontrollsak(restSammensattKontrollsak.id)
        return sammensattKontrollsakRepository.save(sammensattKontrollsak.also { it.fritekst = restSammensattKontrollsak.fritekst })
    }

    @Transactional
    fun slettSammensattKontrollsak(id: Long) {
        sammensattKontrollsakRepository.deleteById(id)
    }
}
