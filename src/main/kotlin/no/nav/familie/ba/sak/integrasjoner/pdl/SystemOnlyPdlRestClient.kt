package no.nav.familie.ba.sak.integrasjoner.pdl

import no.nav.familie.ba.sak.common.kallEksternTjeneste
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlAdressebeskyttelseResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlBaseResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlPersonRequest
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlPersonRequestVariables
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Adressebeskyttelse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI

@Service
class SystemOnlyPdlRestClient(
    @Value("\${PDL_URL}") pdlBaseUrl: URI,
    @Qualifier("jwtBearerClientCredentials") override val restTemplate: RestOperations,
    override val personidentService: PersonidentService,
) : PdlRestClient(pdlBaseUrl, restTemplate, personidentService) {

    @Cacheable("adressebeskyttelse", cacheManager = "shortCache")
    fun hentAdressebeskyttelse(aktør: Aktør): List<Adressebeskyttelse> {
        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(aktør.aktivFødselsnummer()),
            query = hentGraphqlQuery("hent-adressebeskyttelse")
        )

        val pdlResponse: PdlBaseResponse<PdlAdressebeskyttelseResponse> = kallEksternTjeneste(
            tjeneste = "pdl",
            uri = pdlUri,
            formål = "Hent adressebeskyttelse"
        ) {
            postForEntity(pdlUri, pdlPersonRequest, httpHeaders())
        }

        return feilsjekkOgReturnerData(
            ident = aktør.aktivFødselsnummer(),
            pdlResponse = pdlResponse
        ) {
            it.person!!.adressebeskyttelse
        }
    }
}

fun List<Adressebeskyttelse>.tilAdressebeskyttelse() =
    this.firstOrNull()?.gradering ?: ADRESSEBESKYTTELSEGRADERING.UGRADERT
