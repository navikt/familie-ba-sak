package no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import org.junit.jupiter.api.Test

class PersonopplysningGrunnlagForNyBehandlingServiceTest {
    val personidentService = mockk<PersonidentService>()
    val beregningService = mockk<BeregningService>()
    val persongrunnlagService = mockk<PersongrunnlagService>()

    private val personopplysningGrunnlagForNyBehandlingService = spyk(
        PersonopplysningGrunnlagForNyBehandlingService(
            personidentService = personidentService,
            beregningService = beregningService,
            persongrunnlagService = persongrunnlagService,
        )
    )

    @Test
    fun `Skal sende med barna fra forrige behandling ved førstegangsbehandling nummer to`() {
        val søker = lagPerson()
        val barnFraForrigeBehanlding = lagPerson(type = PersonType.BARN)
        val barn = lagPerson(type = PersonType.BARN)

        val barnFnr = barn.aktør.aktivFødselsnummer()
        val søkerFnr = søker.aktør.aktivFødselsnummer()

        val forrigeBehandling = lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)
        val behandling = lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)

        every { personidentService.hentOgLagreAktør(søkerFnr, true) } returns søker.aktør
        every { personidentService.hentOgLagreAktørIder(listOf(barnFnr), true) } returns listOf(barn.aktør)

        every { beregningService.finnBarnFraBehandlingMedTilkjentYtelse(forrigeBehandling.id) } returns
            listOf(barnFraForrigeBehanlding.aktør)

        every { persongrunnlagService.hentSøkersMålform(forrigeBehandling.id) } returns søker.målform

        every {
            persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns PersonopplysningGrunnlag(behandlingId = behandling.id)

        personopplysningGrunnlagForNyBehandlingService.opprettPersonopplysningGrunnlag(
            behandling = behandling,
            forrigeBehandlingSomErVedtatt = forrigeBehandling,
            søkerIdent = søkerFnr,
            barnasIdenter = listOf(barnFnr)
        )
        verify(exactly = 1) {
            persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                aktør = søker.aktør,
                barnFraInneværendeBehandling = listOf(barn.aktør),
                barnFraForrigeBehandling = listOf(barnFraForrigeBehanlding.aktør),
                behandling = behandling,
                målform = søker.målform
            )
        }
    }
}
