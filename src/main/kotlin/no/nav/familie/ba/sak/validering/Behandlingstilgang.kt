package no.nav.familie.ba.sak.validering

import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonOnBehalfClient
import org.springframework.stereotype.Component
import javax.transaction.Transactional
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

@Component
class Behandlingstilgang(private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
                         private val integrasjonOnBehalfClient: IntegrasjonOnBehalfClient)
    : ConstraintValidator<BehandlingstilgangConstraint, Long> {

    @Transactional
    override fun isValid(behandlingId: Long, ctx: ConstraintValidatorContext): Boolean {

        val personer = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)?.personer
        if (personer != null) {
            integrasjonOnBehalfClient.sjekkTilgangTilPersoner(personer).forEach { if (!it.harTilgang) return false }
        }

        return true
    }


}
