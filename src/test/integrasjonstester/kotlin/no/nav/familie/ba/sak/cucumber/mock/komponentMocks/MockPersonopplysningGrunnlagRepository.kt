package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository

fun mockPersonopplysningGrunnlagRepository(behandlingIdTilPersongrunnlag: Map<Long, PersonopplysningGrunnlag>): PersonopplysningGrunnlagRepository {
    val personopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()
    every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) } answers {
        val behandlingsId = firstArg<Long>()
        behandlingIdTilPersongrunnlag[behandlingsId] ?: error("Fant ikke personopplysninggrunnlag for behandling $behandlingsId")
    }
    return personopplysningGrunnlagRepository
}
