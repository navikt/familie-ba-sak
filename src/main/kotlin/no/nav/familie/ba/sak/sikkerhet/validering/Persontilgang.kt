package no.nav.familie.ba.sak.sikkerhet.validering

import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonInfo
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.kontrakter.felles.Ressurs
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

@Component
class Persontilgang(private val integrasjonClient: IntegrasjonClient) :
    ConstraintValidator<PersontilgangConstraint, ResponseEntity<Ressurs<RestPersonInfo>>> {

    private val logger = LoggerFactory.getLogger(Persontilgang::class.java)

    override fun isValid(response: ResponseEntity<Ressurs<RestPersonInfo>>, ctx: ConstraintValidatorContext): Boolean {
        val personInfo = response.body?.data ?: return true
        val personIdenter = personInfo.forelderBarnRelasjon.map { it.personIdent }.toMutableList()
        personIdenter.add(personInfo.personIdent)
        integrasjonClient.sjekkTilgangTilPersoner(personIdenter)
            .filterNot { it.harTilgang }
            .forEach {
                logger.error("Bruker har ikke tilgang: ${it.begrunnelse}")
                return false
            }
        return true
    }
}
