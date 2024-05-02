package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher

fun mockSaksstatistikkEventPublisher(): SaksstatistikkEventPublisher {
    val saksstatistikkEventPublisher = mockk<SaksstatistikkEventPublisher>()
    every { saksstatistikkEventPublisher.publiserSaksstatistikk(any()) } just runs
    every { saksstatistikkEventPublisher.publiserBehandlingsstatistikk(any()) } just runs
    return saksstatistikkEventPublisher
}
