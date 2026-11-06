package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner

import no.nav.familie.ba.sak.common.kallEksternTjenesteRessurs
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.RETRY_BACKOFF_5000MS
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsforhold
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.ArbeidsforholdRequest
import no.nav.familie.ba.sak.integrasjoner.retryVedException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate

@Component
class SystemOnlyIntegrasjonKlient(
    @Value("\${FAMILIE_INTEGRASJONER_API_URL}") private val integrasjonFssUri: URI,
    @Qualifier("integrasjonerM2mRestClient") private val restClientFss: RestClient,
    @Value("\${FAMILIE_INTEGRASJONER_GCP_API_URL}") private val integrasjonGcpUri: URI,
    @Qualifier("integrasjonerGcpM2mRestClient") private val restClientGcp: RestClient,
    private val featureToggleService: FeatureToggleService,
    @Value("$RETRY_BACKOFF_5000MS") private val retryBackoffDelay: Long,
) {
    private fun hentIntegrasjonKlient(): Pair<URI, RestClient> =
        if (featureToggleService.isEnabled(FeatureToggle.BRUK_FAMILIE_INTEGRASJONER_I_GCP, defaultValue = false)) {
            integrasjonGcpUri to restClientGcp
        } else {
            integrasjonFssUri to restClientFss
        }

    fun hentArbeidsforholdMedSystembruker(
        ident: String,
        ansettelsesperiodeFom: LocalDate,
        ansettelsesperiodeTom: LocalDate? = null,
    ): List<Arbeidsforhold> {
        val (integrasjonUri, restClient) = hentIntegrasjonKlient()
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
            retryVedException(retryBackoffDelay).execute {
                restClient
                    .post()
                    .uri(uri)
                    .body(ArbeidsforholdRequest(ident, ansettelsesperiodeFom, ansettelsesperiodeTom))
                    .retrieve()
                    .body()!!
            }
        }
    }
}
