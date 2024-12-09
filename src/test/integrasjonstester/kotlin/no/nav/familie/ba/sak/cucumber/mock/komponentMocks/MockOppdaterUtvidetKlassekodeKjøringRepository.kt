package no.nav.familie.ba.sak.cucumber.mock.komponentMocks

import io.mockk.justRun
import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.autovedtak.oppdaterutvidetklassekode.domene.OppdaterUtvidetKlassekodeKjøringRepository

fun mockOppdaterUtvidetKlassekodeKjøringRepository(): OppdaterUtvidetKlassekodeKjøringRepository {
    val oppdaterUtvidetKlassekodeKjøringRepository = mockk<OppdaterUtvidetKlassekodeKjøringRepository>()

    justRun { oppdaterUtvidetKlassekodeKjøringRepository.settBrukerNyKlassekodeTilTrue(any()) }
    justRun { oppdaterUtvidetKlassekodeKjøringRepository.deleteByFagsakId(any()) }

    return oppdaterUtvidetKlassekodeKjøringRepository
}
