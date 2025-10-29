package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.integrasjoner.ef.EfSakRestKlient
import no.nav.familie.kontrakter.felles.ef.EksternePerioderResponse

fun mockEfSakRestKlient(): EfSakRestKlient {
    val efSakRestKlient = mockk<EfSakRestKlient>()
    every { efSakRestKlient.hentPerioderMedFullOvergangsstønad(any()) } answers {
        EksternePerioderResponse(emptyList())
    }
    return efSakRestKlient
}
