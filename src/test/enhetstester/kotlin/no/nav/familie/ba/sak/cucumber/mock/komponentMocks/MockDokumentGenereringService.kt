package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.brev.DokumentGenereringService

fun mockDokumentGenereringService(): DokumentGenereringService {
    val dokumentGenereringService = mockk<DokumentGenereringService>()
    every { dokumentGenereringService.genererBrevForVedtak(any()) } returns byteArrayOf()
    return dokumentGenereringService
}
