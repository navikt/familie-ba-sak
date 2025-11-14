package no.nav.familie.ba.sak.fake

import no.nav.familie.ba.sak.internal.TestVerktøyService
import no.nav.familie.ba.sak.kjerne.brev.BrevKlient
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brev
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseMedData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.testfiler.Testfil.TEST_PDF
import org.springframework.web.client.RestTemplate

class FakeBrevKlient(
    testVerktøyService: TestVerktøyService,
) : BrevKlient(
        familieBrevUri = "brev_uri_mock",
        restTemplate = RestTemplate(),
        sanityDataset = "",
        testVerktøyService = testVerktøyService,
    ) {
    val genererteBrev = mutableListOf<Brev>()

    override fun genererBrev(
        målform: String,
        brev: Brev,
    ): ByteArray {
        genererteBrev.add(brev)
        return TEST_PDF
    }

    override fun hentBegrunnelsestekst(
        begrunnelseData: BegrunnelseMedData,
        vedtaksperiode: VedtaksperiodeMedBegrunnelser,
    ): String = "Dummytekst for ${begrunnelseData.apiNavn}"
}
