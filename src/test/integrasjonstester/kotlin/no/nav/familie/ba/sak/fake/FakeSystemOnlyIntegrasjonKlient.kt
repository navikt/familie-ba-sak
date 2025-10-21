package no.nav.familie.ba.sak.fake

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.SystemOnlyIntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Ansettelsesperiode
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsforhold
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsgiver
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Periode
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.LocalDate

class FakeSystemOnlyIntegrasjonKlient :
    SystemOnlyIntegrasjonKlient(
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
