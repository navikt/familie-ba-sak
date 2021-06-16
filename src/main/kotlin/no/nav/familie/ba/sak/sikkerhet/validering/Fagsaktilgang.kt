package no.nav.familie.ba.sak.sikkerhet.validering

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import javax.transaction.Transactional
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

@Component
class Fagsaktilgang(private val behandlingRepository: BehandlingRepository,
                    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
                    internal val integrasjonClient: IntegrasjonClient,
                    private val fagsakRepository: FagsakRepository)
    : ConstraintValidator<FagsaktilgangConstraint, Long> {

    @Transactional
    override fun isValid(fagsakId: Long, ctx: ConstraintValidatorContext): Boolean {
        val fagsak = fagsakRepository.finnFagsak(fagsakId)
        val personer: MutableSet<String> = behandlingRepository.finnBehandlinger(fagsakId)
                .asSequence()
                .mapNotNull { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(it.id)?.personer }
                .flatten()
                .map { it.personIdent.ident }
                .toMutableSet()

        personer.addAll(fagsak?.søkerIdenter?.map { it.personIdent.ident } ?: emptyList())

        integrasjonClient.sjekkTilgangTilPersoner(personer.toList())
                .filterNot { it.harTilgang }
                .forEach {
                    logger.error("Bruker har ikke tilgang: ${it.begrunnelse}")
                    return false
                }

        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Fagsaktilgang::class.java)
    }
}
