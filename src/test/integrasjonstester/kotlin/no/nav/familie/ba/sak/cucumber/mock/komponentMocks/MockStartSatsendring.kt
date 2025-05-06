package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.StartSatsendring

fun mockStartSatsendring(): StartSatsendring {
    val startSatsendring = mockk<StartSatsendring>(relaxed = true)
    every { startSatsendring.sjekkOgOpprettSatsendringVedGammelSats(fagsakId = any()) } returns false
    return startSatsendring
}
