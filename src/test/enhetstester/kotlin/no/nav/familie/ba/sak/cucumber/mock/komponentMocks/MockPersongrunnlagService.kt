package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.datagenerator.tilPersonEnkel
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør

fun mockPersongrunnlagService(dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition): PersongrunnlagService {
    val persongrunnlagService = mockk<PersongrunnlagService>()

    val finnPerson = { aktør: Aktør -> dataFraCucumber.persongrunnlag.flatMap { it.value.personer }.first { it.aktør == aktør } }

    every { persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(any()) } answers {
        val behandlingId = firstArg<Long>()
        val personopplysningGrunnlag =
            dataFraCucumber.persongrunnlag[behandlingId]
                ?: throw Feil("Fant ikke persongrunnlag for behandling $behandlingId")
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
    every { persongrunnlagService.hentSøkersMålform(any()) } answers {
        val behandlingsId = firstArg<Long>()
        dataFraCucumber.persongrunnlag[behandlingsId]!!.søker.målform
    }
    every { persongrunnlagService.oppdaterAdresserPåPersoner(any()) } answers {
        val personopplysningGrunnlag = firstArg<PersonopplysningGrunnlag>()
        dataFraCucumber.persongrunnlag[personopplysningGrunnlag.behandlingId] = personopplysningGrunnlag
        personopplysningGrunnlag
    }
    every { persongrunnlagService.lagreOgDeaktiverGammel(any()) } answers {
        val personopplysningGrunnlag = firstArg<PersonopplysningGrunnlag>()
        dataFraCucumber.persongrunnlag[personopplysningGrunnlag.behandlingId] = personopplysningGrunnlag
        personopplysningGrunnlag
    }

    every { persongrunnlagService.oppdaterStatsborgerskapPåPersoner(any()) } answers {
        val personopplysningGrunnlag = firstArg<PersonopplysningGrunnlag>()
        dataFraCucumber.persongrunnlag[personopplysningGrunnlag.behandlingId] = personopplysningGrunnlag
        personopplysningGrunnlag
    }

    every { persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(any(), any(), any(), any(), any()) } answers {
        val aktør = firstArg<Aktør>()
        val barnFraInneværendeBehandling = secondArg<List<Aktør>>()
        val behandling = thirdArg<Behandling>()
        val barnFraForrigeBehandling = args[4] as List<Aktør>

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)

        val søker = finnPerson(aktør).copy(personopplysningGrunnlag = personopplysningGrunnlag)
        personopplysningGrunnlag.personer.add(søker)
        barnFraInneværendeBehandling.union(barnFraForrigeBehandling).forEach { barnsAktør ->
            personopplysningGrunnlag.personer.add(
                finnPerson(barnsAktør).copy(personopplysningGrunnlag = personopplysningGrunnlag),
            )
        }
        dataFraCucumber.persongrunnlag[behandling.id] = personopplysningGrunnlag
        personopplysningGrunnlag
    }

    return persongrunnlagService
}
