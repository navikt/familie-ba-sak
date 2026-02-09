package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner

import no.nav.familie.ba.sak.common.kallEksternTjenesteRessurs
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsforhold
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.ArbeidsforholdRequest
import no.nav.familie.ba.sak.integrasjoner.retryVedException
import no.nav.familie.restklient.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate

@Component
class SystemOnlyIntegrasjonKlient(
    @Value("\${FAMILIE_INTEGRASJONER_API_URL}") private val integrasjonUri: URI,
    @Qualifier("jwtBearerClientCredentials") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "integrasjon") {
    fun hentArbeidsforholdMedSystembruker(
        ident: String,
        ansettelsesperiodeFom: LocalDate,
        ansettelsesperiodeTom: LocalDate? = null,
    ): List<Arbeidsforhold> {
        val uri =
            UriComponentsBuilder
                .fromUri(integrasjonUri)
                .pathSegment("aareg", "arbeidsforhold")
                .build()
                .toUri()

        return kallEksternTjenesteRessurs(
            tjeneste = "aareg",
            uri = uri,
            formål = "Hent arbeidsforhold",
        ) {
            retryVedException(5000).execute {
                postForEntity(uri, ArbeidsforholdRequest(ident, ansettelsesperiodeFom, ansettelsesperiodeTom))
            }
        }
    }
}
