package no.nav.familie.ba.sak.cucumber.mock.komponentMocks

import io.mockk.justRun
import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.autovedtak.nyutvidetklassekode.domene.NyUtvidetKlassekodeKjøringRepository

fun mockNyUtvidetKlassekodeKjøringRepository(): NyUtvidetKlassekodeKjøringRepository {
    val nyUtvidetKlassekodeKjøringRepository = mockk<NyUtvidetKlassekodeKjøringRepository>()

    justRun { nyUtvidetKlassekodeKjøringRepository.settBrukerNyKlassekodeTilTrue(any()) }
    justRun { nyUtvidetKlassekodeKjøringRepository.deleteByFagsakId(any()) }

    return nyUtvidetKlassekodeKjøringRepository
}
