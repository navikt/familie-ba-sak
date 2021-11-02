package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.kjerne.dokument.BrevKlient
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Brev
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseData
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
@Profile("mock-brev-klient")
@Primary
class BrevKlientMock : BrevKlient(
    familieBrevUri = "brev_uri_mock",
    restTemplate = RestTemplate(),
    sanityDataset = "",
) {

    override fun genererBrev(målform: String, brev: Brev): ByteArray {
        return TEST_PDF
    }

    override fun hentBegrunnelsestekst(begrunnelseData: BegrunnelseData): String {
        return "Dummytekst for ${begrunnelseData.apiNavn}"
    }
}
