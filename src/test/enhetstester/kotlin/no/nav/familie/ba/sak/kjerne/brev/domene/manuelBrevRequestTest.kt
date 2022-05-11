package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.ForlengetSvartidsbrev
import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class manuelBrevRequestTest {
    private val årsaker = listOf("1", "2", "3")
    private val forlengetSvartidsbrevRequest = ManueltBrevRequest(
        brevmal = Brevmal.FORLENGET_SVARTIDSBREV,
        multiselectVerdier = årsaker,
        mottakerIdent = "testident",
        mottakerNavn = "testnavn",
        enhet = Enhet("testenhetId", "testenhet"),
        antallUkerSvarfrist = 3
    )

    @Test
    fun `Forlenget svartidsbrev request skal gi forlenget svartid brevmal med riktig data`() {
        val brev = forlengetSvartidsbrevRequest.tilBrev()

        Assertions.assertEquals(brev::class, ForlengetSvartidsbrev::class)
        brev as ForlengetSvartidsbrev

        Assertions.assertEquals(brev.mal, Brevmal.FORLENGET_SVARTIDSBREV)

        Assertions.assertEquals(brev.data.flettefelter.antallUkerSvarfrist!!.single(), "3")
        Assertions.assertEquals(brev.data.flettefelter.aarsakerSvartidsbrev!!, årsaker)
    }
}
