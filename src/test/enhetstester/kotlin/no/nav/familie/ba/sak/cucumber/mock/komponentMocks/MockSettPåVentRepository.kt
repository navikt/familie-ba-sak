package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentRepository

fun mockSettPåVentRepository(): SettPåVentRepository {
    val settPåVentRepository = mockk<SettPåVentRepository>()

    every { settPåVentRepository.findByBehandlingIdAndAktiv(any(), any()) } returns null
    return settPåVentRepository
}
