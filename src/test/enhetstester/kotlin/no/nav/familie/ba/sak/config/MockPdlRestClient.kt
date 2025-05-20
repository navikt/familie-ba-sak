package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlBostedsadresse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlBostedsadressePerson
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlMatrikkeladresse
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDateTime

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
    override fun hentBostedsadresserForPerson(aktør: Aktør): PdlBostedsadressePerson =
        PdlBostedsadressePerson(
            listOf(
                PdlBostedsadresse(
                    gyldigFraOgMed = LocalDateTime.now().minusYears(1),
                    gyldigTilOgMed = LocalDateTime.now().minusMonths(1),
                    vegadresse = null,
                    matrikkeladresse = PdlMatrikkeladresse(1234.toBigInteger()),
                    ukjentBosted = null,
                ),
            ),
        )
}
