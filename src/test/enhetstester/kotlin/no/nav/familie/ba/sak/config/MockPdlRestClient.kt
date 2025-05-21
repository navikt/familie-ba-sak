package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.datagenerator.lagMatrikkeladresse
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

@Service
@Profile("mock-pdl-client")
@Primary
class MockPdlRestClient(
    restOperations: RestOperations,
    personidentService: PersonidentService,
) : PdlRestClient(
        pdlBaseUrl = URI("dummy_uri"),
        restTemplate = restOperations,
        personidentService = personidentService,
    ) {
    override fun hentBostedsadresserForPerson(fødselsnummer: String): List<Bostedsadresse> =
        listOf(
            Bostedsadresse(
                gyldigFraOgMed = LocalDate.now().minusYears(1),
                gyldigTilOgMed = null,
                vegadresse = null,
                matrikkeladresse = lagMatrikkeladresse(1234L),
                ukjentBosted = null,
            ),
        )
}
