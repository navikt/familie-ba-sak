package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.cucumber.BegrunnelseTeksterStepDefinition
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.kontrakter.felles.objectMapper

fun mockSøknadGrunnlagService(dataFraCucumber: BegrunnelseTeksterStepDefinition): SøknadGrunnlagService {
    val søknadGrunnlagService = mockk<SøknadGrunnlagService>()
    every { søknadGrunnlagService.hentAktiv(any()) } answers {
        val behandlingId = firstArg<Long>()
        val behandling = dataFraCucumber.behandlinger[behandlingId]!!
        if (behandling.opprettetÅrsak == BehandlingÅrsak.SØKNAD) {
            SøknadGrunnlag(behandlingId = behandlingId, søknad = objectMapper.writeValueAsString(lagSøknadDTO(randomFnr(), emptyList())))
        } else {
            null
        }
    }
    return søknadGrunnlagService
}
