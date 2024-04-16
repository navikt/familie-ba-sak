package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.BegrunnelseTeksterStepDefinition
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository

fun mockUtenlandskPeriodebeløpRepository(dataFraCucumber: BegrunnelseTeksterStepDefinition): UtenlandskPeriodebeløpRepository {
    val utenlandskPeriodebeløpRepository = mockk<UtenlandskPeriodebeløpRepository>()
    every { utenlandskPeriodebeløpRepository.finnFraBehandlingId(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.utenlandskPeriodebeløp[behandlingId] ?: emptyList()
    }
    every { utenlandskPeriodebeløpRepository.deleteAll(any<Iterable<UtenlandskPeriodebeløp>>()) } answers {
        val utenlandskPeriodebeløp = firstArg<Iterable<UtenlandskPeriodebeløp>>()
        utenlandskPeriodebeløp.forEach {
            dataFraCucumber.utenlandskPeriodebeløp[it.behandlingId] = dataFraCucumber.utenlandskPeriodebeløp[it.behandlingId]?.filter { utenlandskPeriodebeløp -> utenlandskPeriodebeløp != it } ?: emptyList()
        }
    }
    every { utenlandskPeriodebeløpRepository.saveAll(any<Iterable<UtenlandskPeriodebeløp>>()) } answers {
        val utenlandskPeriodebeløp = firstArg<Iterable<UtenlandskPeriodebeløp>>()
        utenlandskPeriodebeløp.forEach {
            dataFraCucumber.utenlandskPeriodebeløp[it.behandlingId] = (dataFraCucumber.utenlandskPeriodebeløp[it.behandlingId] ?: emptyList()) + it
        }
        utenlandskPeriodebeløp.toList()
    }
    return utenlandskPeriodebeløpRepository
}
