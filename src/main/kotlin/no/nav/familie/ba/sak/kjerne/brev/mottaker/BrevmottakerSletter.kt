package no.nav.familie.ba.sak.kjerne.brev.mottaker

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BrevmottakerSletter(
    private val brevmottakerRepository: BrevmottakerRepository,
    private val loggService: LoggService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun slettBrevmottaker(
        id: Long,
        behandlingId: Long,
    ) {
        logger.debug("Sletter brevmottaker {} for behandling {}", id, behandlingId)

        val brevmottaker = brevmottakerRepository.findByIdOrNull(id)?.takeIf { it.behandlingId == behandlingId }

        if (brevmottaker == null) {
            throw Feil("Finner ikke brevmottaker med id=$id på behandling med id=$behandlingId")
        }

        loggService.opprettBrevmottakerLogg(
            brevmottaker = brevmottaker,
            brevmottakerFjernet = true,
        )

        brevmottakerRepository.deleteById(id)
    }
}
