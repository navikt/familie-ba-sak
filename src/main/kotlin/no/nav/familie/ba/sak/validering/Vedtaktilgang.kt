package no.nav.familie.ba.sak.validering

import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.vedtak.VedtakRepository
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonOnBehalfClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import javax.transaction.Transactional
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

@Component
class Vedtaktilgang(private val vedtakRepository: VedtakRepository,
                    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
                    private val integrasjonOnBehalfClient: IntegrasjonOnBehalfClient)
    : ConstraintValidator<VedtaktilgangConstraint, Long> {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun isValid(vedtakId: Long, ctx: ConstraintValidatorContext): Boolean {

        val personer = vedtakRepository.findById(vedtakId)
                .map { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(it.behandling.id)?.personer }

        if (personer.isEmpty) {
            return false
        }

        integrasjonOnBehalfClient.sjekkTilgangTilPersoner(personer.get())
                .filterNot { it.harTilgang }
                .forEach {
                    logger.error("Bruker har ikke tilgang: ${it.begrunnelse}")
                    return false
                }

        return true
    }

}