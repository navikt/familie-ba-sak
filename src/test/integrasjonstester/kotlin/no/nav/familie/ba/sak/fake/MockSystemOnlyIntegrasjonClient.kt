package no.nav.familie.ba.sak.fake

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.SystemOnlyIntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Ansettelsesperiode
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsforhold
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsgiver
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Periode
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.LocalDate

@Service
@Profile("mock-system-only-integrasjon-client")
@Primary
class MockSystemOnlyIntegrasjonClient :
    SystemOnlyIntegrasjonClient(
        integrasjonUri = URI("http://dummy-uri"),
        restOperations = RestTemplate(),
    ) {
    override fun hentArbeidsforholdMedSystembruker(
        ident: String,
        ansettelsesperiodeFom: LocalDate,
    ): List<Arbeidsforhold> =
        listOf(
            Arbeidsforhold(
                arbeidsgiver =
                    Arbeidsgiver(
                        organisasjonsnummer = "123456789",
                    ),
                ansettelsesperiode =
                    Ansettelsesperiode(
                        periode =
                            Periode(
                                fom = ansettelsesperiodeFom,
                                tom = null,
                            ),
                    ),
            ),
        )
}
