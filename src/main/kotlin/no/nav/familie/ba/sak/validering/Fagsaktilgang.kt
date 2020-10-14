package no.nav.familie.ba.sak.validering

import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
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

    private val logger = LoggerFactory.getLogger(this::class.java)

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

}
