package no.nav.familie.ba.sak.validering

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.vedtak.VedtakRepository
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import javax.transaction.Transactional
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

@Component
class Vedtaktilgang(private val vedtakRepository: VedtakRepository,
                    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
                    private val integrasjonClient: IntegrasjonClient)
    : ConstraintValidator<VedtaktilgangConstraint, Long> {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun isValid(vedtakId: Long, ctx: ConstraintValidatorContext): Boolean {

        val vedtak = vedtakRepository.finnVedtak(vedtakId)
        val personer = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(vedtak.behandling.id)?.personer?.map {
            it.personIdent.ident
        } ?: emptyList()

        integrasjonClient.sjekkTilgangTilPersoner(personer)
                .filterNot { it.harTilgang }
                .forEach {
                    logger.error("Bruker har ikke tilgang: ${it.begrunnelse}")
                    return false
                }

        return true
    }

}