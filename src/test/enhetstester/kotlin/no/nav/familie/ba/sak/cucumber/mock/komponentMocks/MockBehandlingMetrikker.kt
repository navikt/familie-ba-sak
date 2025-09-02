package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingMetrikker

fun mockBehandlingMetrikker(): BehandlingMetrikker {
    val behandlingMetrikker = mockk<BehandlingMetrikker>()
    every { behandlingMetrikker.tellNøkkelTallVedOpprettelseAvBehandling(any()) } just runs
    every { behandlingMetrikker.oppdaterBehandlingMetrikker(any()) } just runs
    return behandlingMetrikker
}
