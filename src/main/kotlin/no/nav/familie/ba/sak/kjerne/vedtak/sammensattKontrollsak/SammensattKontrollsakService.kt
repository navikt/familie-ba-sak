package no.nav.familie.ba.sak.kjerne.vedtak.sammensattKontrollsak

import no.nav.familie.ba.sak.ekstern.restDomene.RestOpprettSammensattKontrollsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestSammensattKontrollsak
import no.nav.familie.ba.sak.ekstern.restDomene.tilSammensattKontrollsak
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import org.springframework.stereotype.Service

@Service
class SammensattKontrollsakService(
    private val sammensattKontrollsakRepository: SammensattKontrollsakRepository,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) {
    fun hentSammensattKontrollsak(behandlingId: Long): RestSammensattKontrollsak? {
        return sammensattKontrollsakRepository.finnSammensattKontrollsakForBehandling(behandlingId = behandlingId)?.tilRestSammensattKontrollsak()
    }

    fun opprettSammensattKontrollsak(
        restOpprettSammensattKontrollsak: RestOpprettSammensattKontrollsak,
    ): SammensattKontrollsak {
        val behandling = behandlingHentOgPersisterService.hent(restOpprettSammensattKontrollsak.behandlingId)
        return sammensattKontrollsakRepository.save(restOpprettSammensattKontrollsak.tilSammensattKontrollsak(behandling = behandling))
    }

    fun oppdaterSammensattKontrollsak(
        restSammensattKontrollsak: RestSammensattKontrollsak,
    ): SammensattKontrollsak {
        val sammensattKontrollsak = sammensattKontrollsakRepository.hentSammensattKontrollsak(restSammensattKontrollsak.id)
        return sammensattKontrollsakRepository.save(sammensattKontrollsak.also { it.fritekst = restSammensattKontrollsak.fritekst })
    }

    fun slettSammensattKontrollsak(id: Long) {
        sammensattKontrollsakRepository.deleteById(id)
    }
}
