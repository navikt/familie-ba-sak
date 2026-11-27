package no.nav.familie.ba.sak.kjerne.brev.mottaker

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BrevmottakerHenter(
    private val brevmottakerRepository: BrevmottakerRepository,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentBrevmottakere(behandlingId: Long): List<BrevmottakerDb> {
        logger.debug("Henter brevmottakere for behandling {}", behandlingId)

        return brevmottakerRepository.finnBrevmottakerePåBehandling(behandlingId)
    }
}
