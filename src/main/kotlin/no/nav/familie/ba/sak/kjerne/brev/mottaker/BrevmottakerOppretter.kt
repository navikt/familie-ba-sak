package no.nav.familie.ba.sak.kjerne.brev.mottaker

import no.nav.familie.ba.sak.kjerne.behandling.ValiderBrevmottakerService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BrevmottakerOppretter(
    private val brevmottakerRepository: BrevmottakerRepository,
    private val loggService: LoggService,
    private val validerBrevmottakerService: ValiderBrevmottakerService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun opprettBrevmottaker(brevmottaker: BrevmottakerDb): BrevmottakerDb {
        logger.debug("Oppretter brevmottaker av type {} for behandling {}", brevmottaker.type, brevmottaker.behandlingId)

        validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
            behandlingId = brevmottaker.behandlingId,
            nyBrevmottaker = brevmottaker,
            ekstraBarnLagtTilIBrev = emptyList(),
        )

        loggService.opprettBrevmottakerLogg(
            brevmottaker = brevmottaker,
            brevmottakerFjernet = false,
        )

        return brevmottakerRepository.save(brevmottaker)
    }
}
