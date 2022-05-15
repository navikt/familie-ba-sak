package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.EøsSkjemaerForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.PersonopplysningGrunnlagForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import org.junit.jupiter.api.Test

class RegistrerGrunnlagForNyBehandlingStegEnhetTest {
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val personopplysningGrunnlagForNyBehandlingService: PersonopplysningGrunnlagForNyBehandlingService = mockk()
    private val vilkårsvurderingForNyBehandlingService: VilkårsvurderingForNyBehandlingService = mockk()
    private val kompetanseService: KompetanseService = mockk()
    private val featureToggleService: FeatureToggleService = mockk()
    private val valutakursService: ValutakursService = mockk()
    private val utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService = mockk()

    private val registrerGrunnlagForNyBehandlingSteg = RegistrerGrunnlagForNyBehandlingSteg(
        behandlingHentOgPersisterService,
        personopplysningGrunnlagForNyBehandlingService,
        vilkårsvurderingForNyBehandlingService,
        EøsSkjemaerForNyBehandlingService(
            featureToggleService,
            kompetanseService,
            valutakursService,
            utenlandskPeriodebeløpService
        )
    )

    @Test
    fun `Kopierer kompetanser, valutakurser og utenlandsk periodebeløp til ny behandling`() {
        val mor = lagPerson(type = PersonType.SØKER)
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)

        val behandling1 = lagBehandling()
        val behandling2 = lagBehandling()

        every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling2) } returns behandling1
        every {
            personopplysningGrunnlagForNyBehandlingService.opprettPersonopplysningGrunnlag(
                behandling = behandling2,
                forrigeBehandlingSomErVedtatt = behandling1,
                søkerIdent = mor.aktør.aktivFødselsnummer(),
                barnasIdenter = listOf(barn1.aktør.aktivFødselsnummer(), barn2.aktør.aktivFødselsnummer())
            )
        } just runs

        every {
            vilkårsvurderingForNyBehandlingService.opprettVilkårsvurderingUtenomHovedflyt(
                behandling = behandling2,
                forrigeBehandlingSomErVedtatt = behandling1
            )
        } just runs

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
            registrerGrunnlagForNyBehandlingDTO = RegistrerGrunnlagForNyBehandlingDTO(
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
