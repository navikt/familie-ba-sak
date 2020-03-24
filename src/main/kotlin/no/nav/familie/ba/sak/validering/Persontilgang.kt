package no.nav.familie.ba.sak.validering

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.RestPersonInfo
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonOnBehalfClient
import no.nav.familie.kontrakter.felles.Ressurs
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

@Component
class Persontilgang(private val integrasjonOnBehalfClient: IntegrasjonOnBehalfClient)
    : ConstraintValidator<PersontilgangConstraint, ResponseEntity<Ressurs<RestPersonInfo>>> {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun isValid(response: ResponseEntity<Ressurs<RestPersonInfo>>, ctx: ConstraintValidatorContext): Boolean {
        val personInfo = response.body?.data!!
        val personIdenter = personInfo.familierelasjoner.map { it.personIdent }.toMutableList()
        personIdenter.add(personInfo.personIdent)
        integrasjonOnBehalfClient.sjekkTilgangTilPersoner(personIdenter)
                .filterNot { it.harTilgang }
                .forEach {
                    logger.error("Bruker har ikke tilgang: ${it.begrunnelse}")
                    return false
                }

        return true
    }

}
