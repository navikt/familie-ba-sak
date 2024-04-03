package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.BegrunnelseTeksterStepDefinition
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService

fun mockSøknadGrunnlagService(dataFraCucumber: BegrunnelseTeksterStepDefinition): SøknadGrunnlagService {
    val søknadGrunnlagService = mockk<SøknadGrunnlagService>()
    every { søknadGrunnlagService.hentAktiv(any()) } answers {
        val behandlingId = firstArg<Long>()
        val behandling = dataFraCucumber.behandlinger[behandlingId]!!
        if (behandling.opprettetÅrsak == BehandlingÅrsak.SØKNAD) {
            error("Ikke implementert")
        } else {
            null
        }
    }
    return søknadGrunnlagService
}
