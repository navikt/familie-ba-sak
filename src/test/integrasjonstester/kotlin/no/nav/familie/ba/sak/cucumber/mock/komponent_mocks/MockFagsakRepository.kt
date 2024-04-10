package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.BegrunnelseTeksterStepDefinition
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository

fun mockFagsakRepository(dataFraCucumber: BegrunnelseTeksterStepDefinition): FagsakRepository {
    val fagsakRepository = mockk<FagsakRepository>()
    every { fagsakRepository.finnFagsak(any()) } answers {
        val id = firstArg<Long>()
        dataFraCucumber.fagsaker.values.single { it.id == id }
    }
    return fagsakRepository
}
