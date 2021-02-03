package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.brev.BrevKlient
import no.nav.familie.ba.sak.brev.domene.maler.Brev
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
@Profile("mock-brev-klient")
@Primary
class BrevKlientMock : BrevKlient(
        familieBrevUri = "brev_uri_mock",
        restTemplate = RestTemplate()
) {

    override fun genererBrev(målform: String, brev: Brev): ByteArray {
        return TEST_PDF
    }
}