package no.nav.familie.ba.sak.validering

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import javax.transaction.Transactional
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

@Component
class Behandlingstilgang(private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
                         internal val integrasjonClient: IntegrasjonClient)
    : ConstraintValidator<BehandlingstilgangConstraint, Long> {

    @Transactional
    override fun isValid(behandlingId: Long, ctx: ConstraintValidatorContext): Boolean {

        val personer = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)?.personer
                ?.map { it.personIdent.ident }
        if (personer != null) {
            integrasjonClient.sjekkTilgangTilPersoner(personer)
                    .filterNot { it.harTilgang }
                    .forEach {
                        logger.error("Bruker har ikke tilgang: ${it.begrunnelse}")
                        return false
                    }
        }
        return true
    }

    companion object {

        private val logger = LoggerFactory.getLogger(Behandlingstilgang::class.java)
    }
}
