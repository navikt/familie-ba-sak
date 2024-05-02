package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.integrasjoner.ef.EfSakRestClient
import no.nav.familie.kontrakter.felles.ef.EksternePerioderResponse

fun mockEfSakRestClient(): EfSakRestClient {
    val efSakRestClient = mockk<EfSakRestClient>()
    every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } answers {
        EksternePerioderResponse(emptyList())
    }
    return efSakRestClient
}
