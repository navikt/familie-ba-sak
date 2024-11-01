package no.nav.familie.ba.sak.cucumber.mock.komponentMocks

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingMigreringsinfoRepository

fun mockBehandlingMigreringsinfoRepository(): BehandlingMigreringsinfoRepository {
    val behandlingMigreringsinfoRepository = mockk<BehandlingMigreringsinfoRepository>()
    every { behandlingMigreringsinfoRepository.finnSisteMigreringsdatoPåFagsak(any()) } returns null
    return behandlingMigreringsinfoRepository
}