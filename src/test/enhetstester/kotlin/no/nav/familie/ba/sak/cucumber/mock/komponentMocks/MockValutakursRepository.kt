package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursRepository

fun mockValutakursRepository(dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition): ValutakursRepository {
    val valutakursRepository = mockk<ValutakursRepository>()
    every { valutakursRepository.finnFraBehandlingId(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.valutakurs[behandlingId] ?: emptyList()
    }
    every { valutakursRepository.deleteAll(any<Iterable<Valutakurs>>()) } answers {
        val valutakurser = firstArg<Iterable<Valutakurs>>()
        valutakurser.forEach {
            dataFraCucumber.valutakurs[it.behandlingId] = dataFraCucumber.valutakurs[it.behandlingId]?.filter { valutakurs -> valutakurs != it } ?: emptyList()
        }
    }
    every { valutakursRepository.saveAll(any<Iterable<Valutakurs>>()) } answers {
        val valutakurser = firstArg<Iterable<Valutakurs>>()
        valutakurser.forEach {
            dataFraCucumber.valutakurs[it.behandlingId] = (dataFraCucumber.valutakurs[it.behandlingId] ?: emptyList()) + it
        }
        valutakurser.toList()
    }

    return valutakursRepository
}
