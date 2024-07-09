package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.tilPersonEnkel
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService

fun mockPersongrunnlagService(dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition): PersongrunnlagService {
    val persongrunnlagService = mockk<PersongrunnlagService>()
    every { persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(any()) } answers {
        val behandlingId = firstArg<Long>()
        val personopplysningGrunnlag = dataFraCucumber.persongrunnlag[behandlingId] ?: error("Fant ikke persongrunnlag for behandling $behandlingId")
        personopplysningGrunnlag.personer.map { it.tilPersonEnkel() }
    }
    every { persongrunnlagService.hentAktivThrows(any()) } answers {
        val behandlingsId = firstArg<Long>()
        dataFraCucumber.persongrunnlag[behandlingsId]!!
    }
    every { persongrunnlagService.hentBarna(any<Long>()) } answers {
        val behandlingsId = firstArg<Long>()
        dataFraCucumber.persongrunnlag[behandlingsId]!!.barna
    }
    return persongrunnlagService
}
