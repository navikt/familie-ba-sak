package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus

fun mockØkonomiKlient(): ØkonomiKlient {
    val økonomiKlient = mockk<ØkonomiKlient>()
    every { økonomiKlient.iverksettOppdrag(any()) } returns ""
    every { økonomiKlient.hentStatus(any()) } returns OppdragStatus.KVITTERT_OK
    return økonomiKlient
}
