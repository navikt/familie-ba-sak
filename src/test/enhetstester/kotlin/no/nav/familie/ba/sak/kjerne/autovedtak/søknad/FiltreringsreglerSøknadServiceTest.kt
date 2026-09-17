package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.TestClockProvider
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.FiltreringsregelEvaluator
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.FiltreringsreglerFaktaSøknad
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.domene.FiltreringResultatRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.FiltrerAutomatiskBehandlingData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.YearMonth

class FiltreringsreglerSøknadServiceTest {
    private val personidentService = mockk<PersonidentService>()
    private val personopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val tilkjentYtelseValideringService = mockk<TilkjentYtelseValideringService>()
    private val filtreringsregelEvaluator = mockk<FiltreringsregelEvaluator>()
    private val inneværendeMåned = YearMonth.of(2024, 5)

    private val filtreringsreglerSøknadService =
        FiltreringsreglerSøknadService(
            personopplysningerService = mockk<PersonopplysningerService>(relaxed = true),
            personidentService = personidentService,
            personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
            vilkårsvurderingRepository = mockk(relaxed = true),
            filtreringResultatRepository = mockk<FiltreringResultatRepository>(relaxed = true),
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            tilkjentYtelseValideringService = tilkjentYtelseValideringService,
            filtreringsregelEvaluator = filtreringsregelEvaluator,
            clockProvider = TestClockProvider.lagClockProviderMedFastTidspunkt(inneværendeMåned),
        )

    @Test
    fun `skal slå opp barnetrygd til annen mottaker for inneværende måned og legge resultatet i fakta`() {
        // Arrange
        val søkersIdent = randomFnr()
        val barnsIdent = randomFnr()
        val behandling = lagBehandling()
        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søkersIdent,
                barnasIdenter = listOf(barnsIdent),
            )
        val barn = personopplysningGrunnlag.barna.single()
        val månedSlot = slot<YearMonth>()
        val faktaSlot = slot<FiltreringsreglerFaktaSøknad>()

        every { personidentService.hentAktør(søkersIdent) } returns personopplysningGrunnlag.søker.aktør
        every { personidentService.hentAktørIder(listOf(barnsIdent)) } returns listOf(barn.aktør)
        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns personopplysningGrunnlag
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) } returns null
        every {
            tilkjentYtelseValideringService.barnetrygdUtbetalesForBarnIAnnenFagsakIMåned(
                behandling = behandling,
                barna = listOf(barn),
                måned = capture(månedSlot),
            )
        } returns true
        every { filtreringsregelEvaluator.evaluerFiltreringsregler(any(), capture(faktaSlot)) } returns emptyList()

        // Act
        filtreringsreglerSøknadService.kjørFiltreringsregler(
            filtrerAutomatiskBehandlingData = FiltrerAutomatiskBehandlingData(søkersIdent, listOf(barnsIdent)),
            behandling = behandling,
        )

        // Assert
        assertThat(månedSlot.captured).isEqualTo(inneværendeMåned)
        assertThat(faktaSlot.captured.utbetalesBarnetrygdForBarnetTilAnnenMottakerIInneværendeMåned).isTrue()
    }
}
