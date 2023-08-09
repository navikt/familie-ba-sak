package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler

import io.mockk.CapturingSlot
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.tilfeldigSøker
import no.nav.familie.ba.sak.datagenerator.vilkårsvurdering.lagVilkårsvurderingMedOverstyrendeResultater
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.VergeResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.domene.FødselshendelsefiltreringResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.domene.FødselshendelsefiltreringResultatRepository
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.domene.erOppfylt
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class FiltreringsreglerServiceTest {

    @MockK
    private lateinit var personopplysningerService: PersonopplysningerService

    @MockK
    private lateinit var personidentService: PersonidentService

    @MockK
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @MockK
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @MockK
    private lateinit var localDateService: LocalDateService

    @MockK
    private lateinit var fødselshendelsefiltreringResultatRepository: FødselshendelsefiltreringResultatRepository

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var tilkjentYtelseValideringService: TilkjentYtelseValideringService

    @InjectMockKs
    private lateinit var filtreringsreglerService: FiltreringsreglerService

    @Test
    fun `kjørFiltreringsregler - skal sette morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato til true og gi resultat ikke oppfylt når mors vilkår om utvidet barnetrygd er oppfylt i tidsrommet barnet er mellom 0 og 18 år`() {
        val mor = tilfeldigSøker(fødselsdato = LocalDate.of(1985, 1, 1))
        val barn = tilfeldigPerson(fødselsdato = LocalDate.of(2021, 1, 1))
        val nyBehandlingHendelse = NyBehandlingHendelse(mor.aktør.aktørId, listOf(barn.aktør.aktørId))
        val behandling = lagBehandling()

        val fødselshendelsefiltreringResultatSlot =
            settOppMocksHvorAlleFiltreringsreglerBlirOppfylt(mor, listOf(barn), behandling)

        clearMocks(vilkårsvurderingRepository)
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(behandling.id) } returns lagVilkårsvurderingMedOverstyrendeResultater(
            mor,
            listOf(barn),
            behandling,
            overstyrendeVilkårResultater = mapOf(
                Pair(
                    mor.aktør.aktørId,
                    listOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            periodeFom = LocalDate.of(2020, 11, 1),
                            periodeTom = LocalDate.of(2021, 2, 1),
                        ),
                    ),
                ),
            ),
        )

        mockkObject(FiltreringsregelEvaluering)
        val filtreringsreglerFaktaSlot = slot<FiltreringsreglerFakta>()

        filtreringsreglerService.kjørFiltreringsregler(nyBehandlingHendelse, behandling)

        verify { FiltreringsregelEvaluering.evaluerFiltreringsregler(capture(filtreringsreglerFaktaSlot)) }

        val fødselshendelsefiltreringResultat = fødselshendelsefiltreringResultatSlot.captured
        val filtreringsreglerFakta = filtreringsreglerFaktaSlot.captured

        assertThat(filtreringsreglerFakta.morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato).isTrue

        assertThat(fødselshendelsefiltreringResultat.single { it.resultat == Resultat.IKKE_OPPFYLT }.filtreringsregel).isEqualTo(
            Filtreringsregel.MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO,
        )
        assertThat(fødselshendelsefiltreringResultat.erOppfylt()).isFalse
    }

    @Test
    fun `kjørFiltreringsregler - skal sette morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato til false og gi resultat oppfylt når mors vilkår om utvidet barnetrygd er oppfylt utenfor tidsrommet barnet er mellom 0 og 18 år`() {
        val mor = tilfeldigSøker(fødselsdato = LocalDate.of(1985, 1, 1))
        val barn = tilfeldigPerson(fødselsdato = LocalDate.of(2021, 1, 1))
        val nyBehandlingHendelse = NyBehandlingHendelse(mor.aktør.aktørId, listOf(barn.aktør.aktørId))
        val behandling = lagBehandling()

        val fødselshendelsefiltreringResultatSlot =
            settOppMocksHvorAlleFiltreringsreglerBlirOppfylt(mor, listOf(barn), behandling)

        clearMocks(vilkårsvurderingRepository)
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(behandling.id) } returns lagVilkårsvurderingMedOverstyrendeResultater(
            mor,
            listOf(barn),
            behandling,
            overstyrendeVilkårResultater = mapOf(
                Pair(
                    mor.aktør.aktørId,
                    listOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            periodeFom = LocalDate.of(2020, 11, 1),
                            periodeTom = LocalDate.of(2021, 1, 1),
                        ),
                    ),
                ),
            ),
        )

        mockkObject(FiltreringsregelEvaluering)
        val filtreringsreglerFaktaSlot = slot<FiltreringsreglerFakta>()

        filtreringsreglerService.kjørFiltreringsregler(nyBehandlingHendelse, behandling)

        verify { FiltreringsregelEvaluering.evaluerFiltreringsregler(capture(filtreringsreglerFaktaSlot)) }

        val fødselshendelsefiltreringResultat = fødselshendelsefiltreringResultatSlot.captured
        val filtreringsreglerFakta = filtreringsreglerFaktaSlot.captured

        assertThat(filtreringsreglerFakta.morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato).isFalse

        assertThat(fødselshendelsefiltreringResultat.single { it.filtreringsregel == Filtreringsregel.MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO }.resultat).isEqualTo(
            Resultat.OPPFYLT,
        )
        assertThat(fødselshendelsefiltreringResultat.erOppfylt()).isTrue
    }

    @Test
    fun `kjørFiltreringsregler - skal sette morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato til true og gi resultat ikke oppfylt så lenge en av vilkårsperiodene for utvidet barnetrygd er oppfylt i tidsrommet barnet er mellom 0 og 18`() {
        val mor = tilfeldigSøker(fødselsdato = LocalDate.of(1985, 1, 1))
        val barn = tilfeldigPerson(fødselsdato = LocalDate.of(2021, 1, 1))
        val nyBehandlingHendelse = NyBehandlingHendelse(mor.aktør.aktørId, listOf(barn.aktør.aktørId))
        val behandling = lagBehandling()

        val fødselshendelsefiltreringResultatSlot =
            settOppMocksHvorAlleFiltreringsreglerBlirOppfylt(mor, listOf(barn), behandling)

        clearMocks(vilkårsvurderingRepository)
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(behandling.id) } returns lagVilkårsvurderingMedOverstyrendeResultater(
            mor,
            listOf(barn),
            behandling,
            overstyrendeVilkårResultater = mapOf(
                Pair(
                    mor.aktør.aktørId,
                    listOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            periodeFom = LocalDate.of(2020, 11, 1),
                            periodeTom = LocalDate.of(2021, 1, 1),
                        ),
                        lagVilkårResultat(
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            periodeFom = LocalDate.of(2022, 1, 1),
                            periodeTom = LocalDate.of(2023, 1, 1),
                        ),
                    ),
                ),
            ),
        )

        mockkObject(FiltreringsregelEvaluering)
        val filtreringsreglerFaktaSlot = slot<FiltreringsreglerFakta>()

        filtreringsreglerService.kjørFiltreringsregler(nyBehandlingHendelse, behandling)

        verify { FiltreringsregelEvaluering.evaluerFiltreringsregler(capture(filtreringsreglerFaktaSlot)) }

        val fødselshendelsefiltreringResultat = fødselshendelsefiltreringResultatSlot.captured
        val filtreringsreglerFakta = filtreringsreglerFaktaSlot.captured

        assertThat(filtreringsreglerFakta.morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato).isTrue

        assertThat(fødselshendelsefiltreringResultat.single { it.resultat == Resultat.IKKE_OPPFYLT }.filtreringsregel).isEqualTo(
            Filtreringsregel.MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO,
        )
        assertThat(fødselshendelsefiltreringResultat.erOppfylt()).isFalse
    }

    @Test
    fun `kjørFiltreringsregler - skal sette morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato til true og gi resultat ikke oppfylt når tom-dato er null på vilkåret utvidet barnetrygd`() {
        val mor = tilfeldigSøker(fødselsdato = LocalDate.of(1985, 1, 1))
        val barn = tilfeldigPerson(fødselsdato = LocalDate.of(2021, 1, 1))
        val nyBehandlingHendelse = NyBehandlingHendelse(mor.aktør.aktørId, listOf(barn.aktør.aktørId))
        val behandling = lagBehandling()

        val fødselshendelsefiltreringResultatSlot =
            settOppMocksHvorAlleFiltreringsreglerBlirOppfylt(mor, listOf(barn), behandling)

        clearMocks(vilkårsvurderingRepository)
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(behandling.id) } returns lagVilkårsvurderingMedOverstyrendeResultater(
            mor,
            listOf(barn),
            behandling,
            overstyrendeVilkårResultater = mapOf(
                Pair(
                    mor.aktør.aktørId,
                    listOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            periodeFom = LocalDate.of(2020, 11, 1),
                            periodeTom = LocalDate.of(2021, 1, 1),
                        ),
                        lagVilkårResultat(
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            periodeFom = LocalDate.of(2022, 1, 1),
                            periodeTom = null,
                        ),
                    ),
                ),
            ),
        )

        mockkObject(FiltreringsregelEvaluering)
        val filtreringsreglerFaktaSlot = slot<FiltreringsreglerFakta>()

        filtreringsreglerService.kjørFiltreringsregler(nyBehandlingHendelse, behandling)

        verify { FiltreringsregelEvaluering.evaluerFiltreringsregler(capture(filtreringsreglerFaktaSlot)) }

        val fødselshendelsefiltreringResultat = fødselshendelsefiltreringResultatSlot.captured
        val filtreringsreglerFakta = filtreringsreglerFaktaSlot.captured

        assertThat(filtreringsreglerFakta.morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato).isTrue

        assertThat(fødselshendelsefiltreringResultat.single { it.resultat == Resultat.IKKE_OPPFYLT }.filtreringsregel).isEqualTo(
            Filtreringsregel.MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO,
        )
        assertThat(fødselshendelsefiltreringResultat.erOppfylt()).isFalse
    }

    @Test
    fun `kjørFiltreringsregler - skal sette morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato til false og gi resultat oppfylt når begge barnas fødselsdatoer er etter tom på vilkåret utvidet barnetrygd`() {
        val mor = tilfeldigSøker(fødselsdato = LocalDate.of(1985, 1, 1))
        val barn1 = tilfeldigPerson(fødselsdato = LocalDate.of(2021, 1, 1))
        val barn2 = tilfeldigPerson(fødselsdato = LocalDate.of(2020, 1, 1))

        val nyBehandlingHendelse =
            NyBehandlingHendelse(mor.aktør.aktørId, listOf(barn1.aktør.aktørId, barn2.aktør.aktørId))
        val behandling = lagBehandling()

        val fødselshendelsefiltreringResultatSlot =
            settOppMocksHvorAlleFiltreringsreglerBlirOppfylt(mor, listOf(barn1, barn2), behandling)

        clearMocks(vilkårsvurderingRepository)
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(behandling.id) } returns lagVilkårsvurderingMedOverstyrendeResultater(
            mor,
            listOf(barn1, barn2),
            behandling,
            overstyrendeVilkårResultater = mapOf(
                Pair(
                    mor.aktør.aktørId,
                    listOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            periodeFom = LocalDate.of(2019, 11, 1),
                            periodeTom = LocalDate.of(2020, 1, 1),
                        ),
                    ),
                ),
            ),
        )

        mockkObject(FiltreringsregelEvaluering)
        val filtreringsreglerFaktaSlot = slot<FiltreringsreglerFakta>()

        filtreringsreglerService.kjørFiltreringsregler(nyBehandlingHendelse, behandling)

        verify { FiltreringsregelEvaluering.evaluerFiltreringsregler(capture(filtreringsreglerFaktaSlot)) }

        val fødselshendelsefiltreringResultat = fødselshendelsefiltreringResultatSlot.captured
        val filtreringsreglerFakta = filtreringsreglerFaktaSlot.captured

        assertThat(filtreringsreglerFakta.morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato).isFalse

        assertThat(fødselshendelsefiltreringResultat.single { it.filtreringsregel == Filtreringsregel.MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO }.resultat).isEqualTo(
            Resultat.OPPFYLT,
        )
        assertThat(fødselshendelsefiltreringResultat.erOppfylt()).isTrue
    }

    @Test
    fun `kjørFiltreringsregler - skal sette morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato til true og gi resultat ikke oppfylt når mors vilkår om utvidet barnetrygd er oppfylt i tidsrommet et av barna er mellom 0 og 18 år`() {
        val mor = tilfeldigSøker(fødselsdato = LocalDate.of(1985, 1, 1))
        val barn1 = tilfeldigPerson(fødselsdato = LocalDate.of(2021, 1, 1))
        val barn2 = tilfeldigPerson(fødselsdato = LocalDate.of(2020, 1, 1))

        val nyBehandlingHendelse =
            NyBehandlingHendelse(mor.aktør.aktørId, listOf(barn1.aktør.aktørId, barn2.aktør.aktørId))
        val behandling = lagBehandling()

        val fødselshendelsefiltreringResultatSlot =
            settOppMocksHvorAlleFiltreringsreglerBlirOppfylt(mor, listOf(barn1, barn2), behandling)

        clearMocks(vilkårsvurderingRepository)
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(behandling.id) } returns lagVilkårsvurderingMedOverstyrendeResultater(
            mor,
            listOf(barn1, barn2),
            behandling,
            overstyrendeVilkårResultater = mapOf(
                Pair(
                    mor.aktør.aktørId,
                    listOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            periodeFom = LocalDate.of(2019, 11, 1),
                            periodeTom = LocalDate.of(2021, 1, 1),
                        ),
                    ),
                ),
            ),
        )

        mockkObject(FiltreringsregelEvaluering)
        val filtreringsreglerFaktaSlot = slot<FiltreringsreglerFakta>()

        filtreringsreglerService.kjørFiltreringsregler(nyBehandlingHendelse, behandling)

        verify { FiltreringsregelEvaluering.evaluerFiltreringsregler(capture(filtreringsreglerFaktaSlot)) }

        val fødselshendelsefiltreringResultat = fødselshendelsefiltreringResultatSlot.captured
        val filtreringsreglerFakta = filtreringsreglerFaktaSlot.captured

        assertThat(filtreringsreglerFakta.morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato).isTrue

        assertThat(fødselshendelsefiltreringResultat.single { it.resultat == Resultat.IKKE_OPPFYLT }.filtreringsregel).isEqualTo(
            Filtreringsregel.MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO,
        )
        assertThat(fødselshendelsefiltreringResultat.erOppfylt()).isFalse
    }

    private fun settOppMocksHvorAlleFiltreringsreglerBlirOppfylt(
        mor: Person,
        barna: List<Person>,
        behandling: Behandling,
    ): CapturingSlot<List<FødselshendelsefiltreringResultat>> {
        every { personidentService.hentAktør(mor.aktør.aktørId) } returns mor.aktør
        every { personidentService.hentAktørIder(barna.map { it.aktør.aktørId }) } returns barna.map { it.aktør }

        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns lagTestPersonopplysningGrunnlag(
            behandling.id,
            mor,
            *barna.toTypedArray(),
        )
        every { behandlingService.hentMigreringsdatoPåFagsak(behandling.fagsak.id) } returns null
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(behandling.id) } returns lagVilkårsvurderingMedOverstyrendeResultater(
            mor,
            barna,
            behandling,
            overstyrendeVilkårResultater = mapOf(
                Pair(
                    mor.aktør.aktørId,
                    listOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            periodeFom = barna.minOf { it.fødselsdato }.minusYears(1),
                            periodeTom = barna.minOf { it.fødselsdato }.minusYears(1).plusMonths(6),
                        ),
                    ),
                ),
            ),
        )

        every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(mor.aktør) } returns PersonInfo(
            forelderBarnRelasjon =
            barna.map {
                ForelderBarnRelasjon(
                    aktør = it.aktør,
                    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                )
            }.toSet(),
            fødselsdato = mor.fødselsdato,
        )

        every { personopplysningerService.harVerge(mor.aktør) } returns VergeResponse(false)

        every { localDateService.now() } returns LocalDate.now()

        every {
            tilkjentYtelseValideringService.barnetrygdLøperForAnnenForelder(
                behandling,
                barna,
            )
        } returns false

        val fødselshendelsefiltreringResultatSlot = slot<List<FødselshendelsefiltreringResultat>>()

        every {
            fødselshendelsefiltreringResultatRepository.saveAll<FødselshendelsefiltreringResultat>(
                capture(
                    fødselshendelsefiltreringResultatSlot,
                ),
            )
        } returns mockk()
        return fødselshendelsefiltreringResultatSlot
    }
}
