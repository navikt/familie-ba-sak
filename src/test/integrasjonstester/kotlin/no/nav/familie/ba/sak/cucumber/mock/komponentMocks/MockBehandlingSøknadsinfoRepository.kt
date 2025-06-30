package no.nav.familie.ba.sak.cucumber.mock.komponentMocks

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoRepository

fun mockBehandlingSøknadsinfoRepository(): BehandlingSøknadsinfoRepository {
    val behandlingSøknadsinfoRepository = mockk<BehandlingSøknadsinfoRepository>()
    every { behandlingSøknadsinfoRepository.findByBehandlingId(any()) } returns emptySet()
    return behandlingSøknadsinfoRepository
}
