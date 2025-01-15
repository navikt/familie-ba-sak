package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import lagSøknadDTO
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagRepository
import no.nav.familie.kontrakter.felles.objectMapper
import randomFnr

fun mockSøknadGrunnlagRepository(dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition): SøknadGrunnlagRepository {
    val søknadGrunnlagRepository = mockk<SøknadGrunnlagRepository>()
    every { søknadGrunnlagRepository.hentAktiv(any()) } answers {
        val behandlingId = firstArg<Long>()
        val behandling = dataFraCucumber.behandlinger[behandlingId]!!
        if (behandling.opprettetÅrsak == BehandlingÅrsak.SØKNAD) {
            SøknadGrunnlag(
                behandlingId = behandlingId,
                søknad =
                    objectMapper.writeValueAsString(
                        lagSøknadDTO(
                            randomFnr(),
                            emptyList(),
                        ),
                    ),
            )
        } else {
            null
        }
    }
    return søknadGrunnlagRepository
}
