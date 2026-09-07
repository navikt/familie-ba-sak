package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.datagenerator.lagSøknadDTO
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagRepository
import no.nav.familie.kontrakter.felles.jsonMapper

fun mockSøknadGrunnlagRepository(dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition): SøknadGrunnlagRepository {
    val søknadGrunnlagRepository = mockk<SøknadGrunnlagRepository>()
    every { søknadGrunnlagRepository.hentAktiv(any()) } answers {
        val behandlingId = firstArg<Long>()
        val behandling = dataFraCucumber.behandlinger[behandlingId]!!
        if (behandling.opprettetÅrsak == BehandlingÅrsak.SØKNAD) {
            val fremstiltKravFor = dataFraCucumber.personerFremstiltKravFor[behandlingId] ?: emptyList()
            val persongrunnlag = dataFraCucumber.persongrunnlag[behandlingId]
            val barnIdenter =
                persongrunnlag
                    ?.barna
                    .orEmpty()
                    .filter { it.aktør in fremstiltKravFor }
                    .map { it.aktør.aktivFødselsnummer() }
            // Søker er blant personene det er fremstilt krav for når det er søkt om utvidet barnetrygd
            val erSøktOmUtvidet = behandling.fagsak.aktør in fremstiltKravFor
            SøknadGrunnlag(
                behandlingId = behandlingId,
                søknad =
                    jsonMapper.writeValueAsString(
                        lagSøknadDTO(
                            søkerIdent = persongrunnlag?.søker?.aktør?.aktivFødselsnummer() ?: randomFnr(),
                            barnasIdenter = barnIdenter,
                            underkategori = if (erSøktOmUtvidet) BehandlingUnderkategori.UTVIDET else BehandlingUnderkategori.ORDINÆR,
                        ),
                    ),
            )
        } else {
            null
        }
    }
    return søknadGrunnlagRepository
}
