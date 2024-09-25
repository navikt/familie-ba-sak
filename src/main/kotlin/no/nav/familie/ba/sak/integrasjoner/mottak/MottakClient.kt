package no.nav.familie.ba.sak.integrasjoner.mottak

import no.nav.familie.ba.sak.common.kallEksternTjeneste
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.http.util.UriUtil
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class MottakClient(
    @Value("\${FAMILIE_BAKS_MOTTAK_URL}") val mottakBaseUrl: URI,
    @Qualifier("jwtBearer") val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "baks-mottak") {
    fun hentStrengesteAdressebeskyttelsegraderingIDigitalSøknad(journalpostId: String): ADRESSEBESKYTTELSEGRADERING {
        val uri = UriUtil.uri(mottakBaseUrl, "soknad/adressebeskyttelse/${Tema.BAR.name}/$journalpostId")
        return kallEksternTjeneste<ADRESSEBESKYTTELSEGRADERING>(
            tjeneste = "baks-mottak",
            uri = uri,
            formål = "Finne ut om det finnes personer med adressebeskyttelsegradering i digital søknad",
        ) {
            getForEntity(uri)
        }
    }
}
