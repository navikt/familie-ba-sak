package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse

fun mockKompetanseRepository(dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition): KompetanseRepository {
    val kompetanseRepository = mockk<KompetanseRepository>()
    every { kompetanseRepository.finnFraBehandlingId(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.kompetanser[behandlingId] ?: emptyList()
    }
    every { kompetanseRepository.deleteAll(any<Iterable<Kompetanse>>()) } answers {
        val kompetanser = firstArg<Iterable<Kompetanse>>()
        kompetanser.forEach {
            dataFraCucumber.kompetanser[it.behandlingId] = dataFraCucumber.kompetanser[it.behandlingId]?.filter { kompetanse -> kompetanse != it } ?: emptyList()
        }
    }
    every { kompetanseRepository.saveAll(any<Iterable<Kompetanse>>()) } answers {
        val kompetanser = firstArg<Iterable<Kompetanse>>()
        kompetanser.forEach {
            dataFraCucumber.kompetanser[it.behandlingId] = (dataFraCucumber.kompetanser[it.behandlingId] ?: emptyList()) + it
        }
        kompetanser.toMutableList()
    }
    return kompetanseRepository
}
