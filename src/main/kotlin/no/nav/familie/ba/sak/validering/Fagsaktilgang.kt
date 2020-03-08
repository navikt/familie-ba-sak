package no.nav.familie.ba.sak.validering

import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonOnBehalfClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import javax.transaction.Transactional
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

@Component
class Fagsaktilgang(private val behandlingRepository: BehandlingRepository,
                    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
                    private val integrasjonOnBehalfClient: IntegrasjonOnBehalfClient)
    : ConstraintValidator<FagsaktilgangConstraint, Long> {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun isValid(fagsakId: Long, ctx: ConstraintValidatorContext): Boolean {

        val personer: Set<Person> = behandlingRepository.finnBehandlinger(fagsakId)
                .mapNotNull { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(it.id)?.personer }
                .flatten()
                .toSet()

        integrasjonOnBehalfClient.sjekkTilgangTilPersoner(personer)
                .filterNot { it.harTilgang }
                .forEach {
                    logger.error("Bruker har ikke tilgang: ${it.begrunnelse}")
                    return false
                }

        return true
    }

}
