package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import org.junit.jupiter.api.Test

class RegistrerGrunnlagForNyBehandlingStegEnhetTest {
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val beregningService: BeregningService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val personidentService: PersonidentService = mockk()
    private val vilkårService: VilkårService = mockk()
    private val kompetanseService: KompetanseService = mockk()
    private val featureToggleService: FeatureToggleService = mockk()
    private val valutakursService: ValutakursService = mockk()
    private val utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService = mockk()

    private val registrerGrunnlagForNyBehandlingSteg = RegistrerGrunnlagForNyBehandlingSteg(
        behandlingService,
        behandlingHentOgPersisterService,
        beregningService,
        persongrunnlagService,
        personidentService,
        vilkårService,
        kompetanseService,
        featureToggleService,
        valutakursService,
        utenlandskPeriodebeløpService,
    )

    @Test
    fun `Kopierer kompetanser, valutakurser og utenlandsk periodebeløp til ny behandling`() {
        val mor = lagPerson(type = PersonType.SØKER)
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)

        val behandling1 = lagBehandling()
        val behandling2 = lagBehandling()

        every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling2) } returns behandling1
        every { personidentService.hentOgLagreAktør(any(), any()) } returns mor.aktør
        every { personidentService.hentOgLagreAktørIder(any(), any()) } returns listOf(barn1.aktør, barn2.aktør)
        every { persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(any(), any(), behandling2, any()) } returns
            lagTestPersonopplysningGrunnlag(behandling2.id, mor, barn1, barn2)
        every { featureToggleService.isEnabled(any()) } returns true
        every { kompetanseService.kopierOgErstattKompetanser(behandling1.id, behandling2.id) } just runs
        every { valutakursService.kopierOgErstattValutakurser(behandling1.id, behandling2.id) } just runs
        every {
            utenlandskPeriodebeløpService.kopierOgErstattUtenlandskePeriodebeløp(
                behandling1.id,
                behandling2.id
            )
        } just runs

        registrerGrunnlagForNyBehandlingSteg.utførStegOgAngiNeste(
            behandling = behandling2,
            data = RegistrerGrunnlagForNyBehandlingDTO(
                ident = mor.aktør.aktivFødselsnummer(),
                barnasIdenter = listOf(barn1.aktør.aktivFødselsnummer(), barn2.aktør.aktivFødselsnummer())
            )
        )

        verify(exactly = 1) {
            kompetanseService.kopierOgErstattKompetanser(behandling1.id, behandling2.id)
            valutakursService.kopierOgErstattValutakurser(behandling1.id, behandling2.id)
            utenlandskPeriodebeløpService.kopierOgErstattUtenlandskePeriodebeløp(behandling1.id, behandling2.id)
        }
    }
}
