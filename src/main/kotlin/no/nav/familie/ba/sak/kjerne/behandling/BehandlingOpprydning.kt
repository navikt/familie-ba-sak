package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.steg.RegistrerPersongrunnlagDTO
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class BehandlingOpprydning(
    private val stegService: StegService,
    private val behandlingRepository: BehandlingRepository
) {

    @Scheduled(initialDelay = 120000L, fixedDelay = Long.MAX_VALUE)
    fun startPersongrunnlagStegForBehandling1106052() {
        val behandling = behandlingRepository.finnBehandling(behandlingId = 1106052L)

        logger.info("Kjører opprydning på $behandling")
        if (behandling.steg == StegType.REGISTRERE_PERSONGRUNNLAG) {
            logger.info("Håndterer persongrunnlag steg for $behandling")
            stegService.håndterPersongrunnlag(
                behandling = behandling,
                registrerPersongrunnlagDTO = RegistrerPersongrunnlagDTO(
                    ident = behandling.fagsak.aktør.aktivFødselsnummer(),
                    barnasIdenter = emptyList()
                )
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BehandlingOpprydning::class.java)
    }
}
