package no.nav.familie.ba.sak.kjerne.vedtak.sammensattKontrollsak

import no.nav.familie.ba.sak.ekstern.restDomene.RestOpprettSammensattKontrollsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestSammensattKontrollsak
import no.nav.familie.ba.sak.ekstern.restDomene.tilSammensattKontrollsak
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SammensattKontrollsakService(
    private val sammensattKontrollsakRepository: SammensattKontrollsakRepository,
    private val loggService: LoggService,
) {
    fun finnSammensattKontrollsak(behandlingId: Long): SammensattKontrollsak? = sammensattKontrollsakRepository.finnSammensattKontrollsakForBehandling(behandlingId = behandlingId)

    @Transactional
    fun opprettSammensattKontrollsak(
        restOpprettSammensattKontrollsak: RestOpprettSammensattKontrollsak,
    ): SammensattKontrollsak {
        val opprettetSammensattKontrollsak = sammensattKontrollsakRepository.save(restOpprettSammensattKontrollsak.tilSammensattKontrollsak())

        loggService.loggSammensattKontrollsakLagtTil(opprettetSammensattKontrollsak)

        return opprettetSammensattKontrollsak
    }

    @Transactional
    fun oppdaterSammensattKontrollsak(
        restSammensattKontrollsak: RestSammensattKontrollsak,
    ): SammensattKontrollsak {
        val sammensattKontrollsak = sammensattKontrollsakRepository.hentSammensattKontrollsak(restSammensattKontrollsak.id)

        return sammensattKontrollsakRepository.save(
            sammensattKontrollsak.also {
                it.fritekst = restSammensattKontrollsak.fritekst
                loggService.loggSammensattKontrollsakEndret(it)
            },
        )
    }

    @Transactional
    fun slettSammensattKontrollsak(id: Long) {
        val sammensattKontrollsak = sammensattKontrollsakRepository.findByIdOrNull(id)

        sammensattKontrollsak?.let {
            loggService.loggSammensattKontrollsakFjernet(it.behandlingId)
            sammensattKontrollsakRepository.deleteById(id)
        }
    }
}
