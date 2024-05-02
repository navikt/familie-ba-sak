package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.behandling.SnikeIKøenService

fun mockSnikeIKøenService(): SnikeIKøenService {
    val snikeIKøenService = mockk<SnikeIKøenService>()
    every { snikeIKøenService.kanSnikeForbi(any()) } answers { true }
    return snikeIKøenService
}
