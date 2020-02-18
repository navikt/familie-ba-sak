package no.nav.familie.ba.sak.validering

import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.FagsakRepository
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonOnBehalfClient
import org.springframework.stereotype.Component
import javax.transaction.Transactional
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

@Component
class Fagsaktilgang(private val behandlingRepository: BehandlingRepository,
                    private val fagsakRepository: FagsakRepository,
                    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
                    private val integrasjonOnBehalfClient: IntegrasjonOnBehalfClient)
    : ConstraintValidator<FagsaktilgangConstraint, Long> {


    @Transactional
    override fun isValid(fagsakId: Long, ctx: ConstraintValidatorContext): Boolean {
        val fagsak = fagsakRepository.finnFagsak(fagsakId) ?: return true

        val personer = behandlingRepository.finnBehandlinger(fagsak.id)
                .mapNotNull { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(it.id)?.personer }
                .flatten()

        integrasjonOnBehalfClient.sjekkTilgangTilPersoner(personer).forEach { if (!it.harTilgang) return false }

        return true
    }

}
