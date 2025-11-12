package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.TestClockProvider
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.tilPersonEnkel
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.TilpassKompetanserTilRegelverkService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.AutomatiskOppdaterValutakursService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.task.OpprettTaskService
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth

class VilkårsvurderingStegTest {
    private val vilkårService: VilkårService = mockk()
    private val beregningService: BeregningService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk(relaxed = true)
    private val behandlingstemaService: BehandlingstemaService = mockk(relaxed = true)
    private val tilbakestillBehandlingService: TilbakestillBehandlingService = mockk()
    private val tilpassKompetanserTilRegelverkService: TilpassKompetanserTilRegelverkService = mockk()
    private val vilkårsvurderingForNyBehandlingService: VilkårsvurderingForNyBehandlingService = mockk()
    private val automatiskOppdaterValutakursService: AutomatiskOppdaterValutakursService = mockk()
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService = mockk()
    private val featureToggleService: FeatureToggleService = mockk()
    private val opprettTaskService: OpprettTaskService = mockk()
    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>(relaxed = true)

    private val vilkårsvurderingSteg: VilkårsvurderingSteg =
        VilkårsvurderingSteg(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            behandlingstemaService = behandlingstemaService,
            vilkårService = vilkårService,
            beregningService = beregningService,
            persongrunnlagService = persongrunnlagService,
            tilbakestillBehandlingService = tilbakestillBehandlingService,
            tilpassKompetanserTilRegelverkService = tilpassKompetanserTilRegelverkService,
            vilkårsvurderingForNyBehandlingService = vilkårsvurderingForNyBehandlingService,
            månedligValutajusteringService = mockk(),
            clockProvider = TestClockProvider(),
            automatiskOppdaterValutakursService = automatiskOppdaterValutakursService,
            endretUtbetalingAndelService = endretUtbetalingAndelService,
            featureToggleService = featureToggleService,
            opprettTaskService = opprettTaskService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
        )

    val behandling =
        lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
        )
    val søker = lagPerson(type = PersonType.SØKER, fødselsdato = LocalDate.of(1984, 1, 1))
    val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 1, 1))

    @BeforeEach
    fun setup() {
        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søker.aktør.aktivFødselsnummer(),
                barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()),
            )
        every { tilbakestillBehandlingService.tilbakestillDataTilVilkårsvurderingssteg(behandling) } returns Unit
        every { beregningService.genererTilkjentYtelseFraVilkårsvurdering(any(), any()) } returns
            lagInitiellTilkjentYtelse(
                behandling,
            )

        every { tilpassKompetanserTilRegelverkService.tilpassKompetanserTilRegelverk(BehandlingId(behandling.id)) } just Runs
        justRun { automatiskOppdaterValutakursService.oppdaterAndelerMedValutakurser(any()) }
        every { featureToggleService.isEnabled(FeatureToggle.PREUTFYLLING_ENDRET_UTBETALING_3ÅR_ELLER_3MND) } returns false
    }

    @Test
    fun `skal fortsette til neste steg når helmanuell migreringsbehandling har del bosted`() {
        val vikårsvurdering = Vilkårsvurdering(behandling = behandling)
        val søkerPersonResultat =
            lagPersonResultat(
                vilkårsvurdering = vikårsvurdering,
                person = søker,
                resultat = Resultat.OPPFYLT,
                periodeFom = søker.fødselsdato,
                periodeTom = LocalDate.now(),
                lagFullstendigVilkårResultat = true,
                personType = PersonType.SØKER,
                erDeltBosted = false,
            )
        val barnPersonResultat =
            lagPersonResultat(
                vilkårsvurdering = vikårsvurdering,
                person = barn,
                resultat = Resultat.OPPFYLT,
                periodeFom = barn.fødselsdato,
                periodeTom = LocalDate.now(),
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN,
                erDeltBosted = true,
            )
        vikårsvurdering.personResultater = setOf(søkerPersonResultat, barnPersonResultat)
        every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns vikårsvurdering

        assertDoesNotThrow { vilkårsvurderingSteg.utførStegOgAngiNeste(behandling, null) }
    }

    @Test
    fun `skal validere når regelverk er konsistent`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 =
            tilfeldigPerson(
                personType = PersonType.BARN,
                fødselsdato = LocalDate.now().minusMonths(2).withDayOfMonth(1),
            )

        val behandling = lagBehandling()

        val vilkårsvurderingBygger =
            VilkårsvurderingBuilder(behandling)
                .forPerson(søker, YearMonth.now())
                .medVilkår("N>", Vilkår.BOSATT_I_RIKET, Vilkår.LOVLIG_OPPHOLD)
                .forPerson(barn1, barn1.fødselsdato.toYearMonth())
                .medVilkår("+>", Vilkår.UNDER_18_ÅR, Vilkår.GIFT_PARTNERSKAP)
                .medVilkår("N>", Vilkår.BOSATT_I_RIKET, Vilkår.LOVLIG_OPPHOLD, Vilkår.BOR_MED_SØKER)
                .byggPerson()

        val vilkårsvurdering = vilkårsvurderingBygger.byggVilkårsvurdering()
        val søkerOgBarnPåBehandling = listOf(søker.tilPersonEnkel(), barn1.tilPersonEnkel())

        every { vilkårService.hentVilkårsvurdering(behandling.id) } returns vilkårsvurdering
        every { persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(behandling.id) } returns søkerOgBarnPåBehandling

        assertDoesNotThrow { vilkårsvurderingSteg.preValiderSteg(behandling, null) }
    }

    @Test
    fun `validering skal feile når det er blanding av regelverk på vilkårene for barnet`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)

        val behandling = lagBehandling()

        val vilkårsvurderingBygger =
            VilkårsvurderingBuilder(behandling)
                .forPerson(søker, YearMonth.now())
                .medVilkår("EEEEEEEEEEEEE", Vilkår.BOSATT_I_RIKET, Vilkår.LOVLIG_OPPHOLD)
                .forPerson(barn1, YearMonth.now())
                .medVilkår("+++++++++++++", Vilkår.UNDER_18_ÅR, Vilkår.GIFT_PARTNERSKAP)
                .medVilkår("   EEEENNNNEE", Vilkår.BOSATT_I_RIKET)
                .medVilkår("     EEENNEEE", Vilkår.LOVLIG_OPPHOLD)
                .medVilkår("NNNNNNNNNNEEE", Vilkår.BOR_MED_SØKER)
                .byggPerson()

        val vilkårsvurdering = vilkårsvurderingBygger.byggVilkårsvurdering()
        val søkerOgBarnPåBehandling = listOf(søker.tilPersonEnkel(), barn1.tilPersonEnkel())

        every { vilkårService.hentVilkårsvurdering(behandling.id) } returns vilkårsvurdering
        every { persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(behandling.id) } returns søkerOgBarnPåBehandling

        val exception = assertThrows<FunksjonellFeil> { vilkårsvurderingSteg.preValiderSteg(behandling, null) }
        assertTrue(exception.message?.contains("Det er forskjellig regelverk for en eller flere perioder for søker eller barna.") == true)
    }

    @Nested
    inner class ValiderFinnmarkOgSvalbardBehandling {
        @Test
        fun `Skal kaste feil i finnmarkstillegg-behandlinger om forrige vedtatte behandling ikke finnes`() {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.FINNMARKSTILLEGG)
            val vilkårsvurdering =
                lagVilkårsvurdering(1L, behandling) {
                    setOf(
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            person = søker,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = søker.fødselsdato,
                            periodeTom = null,
                            lagFullstendigVilkårResultat = true,
                            personType = PersonType.SØKER,
                        ),
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            person = barn,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = barn.fødselsdato,
                            periodeTom = null,
                            lagFullstendigVilkårResultat = true,
                            personType = PersonType.BARN,
                        ),
                    )
                }

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns null
            every { persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(behandling.id) } returns listOf(søker.tilPersonEnkel(), barn.tilPersonEnkel())
            every { vilkårService.hentVilkårsvurdering(behandling.id) } returns vilkårsvurdering

            // Act & Assert
            assertThatThrownBy { vilkårsvurderingSteg.preValiderSteg(behandling, null) }.hasMessage(
                "Kan ikke kjøre behandling med årsak ${behandling.opprettetÅrsak} dersom det ikke finnes en tidligere behandling. Behandling: ${behandling.id}",
            )
        }

        @Test
        fun `Skal kaste feil i svalbardtillegg-behandlinger om forrige vedtatte behandling ikke finnes`() {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.SVALBARDTILLEGG)
            val vilkårsvurdering =
                lagVilkårsvurdering(2L, behandling) {
                    setOf(
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            person = søker,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = søker.fødselsdato,
                            periodeTom = null,
                            lagFullstendigVilkårResultat = true,
                            personType = PersonType.SØKER,
                        ),
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            person = barn,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = barn.fødselsdato,
                            periodeTom = null,
                            lagFullstendigVilkårResultat = true,
                            personType = PersonType.BARN,
                        ),
                    )
                }

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns null
            every { persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(behandling.id) } returns listOf(søker.tilPersonEnkel(), barn.tilPersonEnkel())
            every { vilkårService.hentVilkårsvurdering(behandling.id) } returns vilkårsvurdering

            // Act & Assert
            assertThatThrownBy { vilkårsvurderingSteg.preValiderSteg(behandling, null) }.hasMessage(
                "Kan ikke kjøre behandling med årsak ${behandling.opprettetÅrsak} dersom det ikke finnes en tidligere behandling. Behandling: ${behandling.id}",
            )
        }

        @Nested
        inner class FinnesPerioderDerBarnMedDeltBostedIkkeBorMedSøkerIFinnmark {
            @Test
            fun `Skal opprette manuell oppgave i finnmarkstillegg-behandlinger dersom søker bor i Finnmark, og barn med delt bosted ikke bor i Finnmark`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.FINNMARKSTILLEGG)
                val forrigeBehandling = lagBehandling()
                val vilkårsvurdering =
                    lagVilkårsvurdering(1L, behandling) {
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                person = søker,
                                resultat = Resultat.OPPFYLT,
                                periodeFom = søker.fødselsdato,
                                periodeTom = null,
                                lagFullstendigVilkårResultat = true,
                                personType = PersonType.SØKER,
                            ),
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                person = barn,
                                resultat = Resultat.OPPFYLT,
                                periodeFom = barn.fødselsdato,
                                periodeTom = null,
                                lagFullstendigVilkårResultat = true,
                                personType = PersonType.BARN,
                            ),
                        )
                    }

                vilkårsvurdering.personResultater
                    .first { it.aktør == søker.aktør }
                    .vilkårResultater
                    .first { it.vilkårType == Vilkår.BOSATT_I_RIKET }
                    .utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)

                vilkårsvurdering.personResultater
                    .first { it.aktør == barn.aktør }
                    .vilkårResultater
                    .first { it.vilkårType == Vilkår.BOR_MED_SØKER }
                    .utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED)

                every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns forrigeBehandling
                every { persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(behandling.id) } returns listOf(søker.tilPersonEnkel(), barn.tilPersonEnkel())
                every { vilkårService.hentVilkårsvurdering(behandling.id) } returns vilkårsvurdering
                every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns
                    listOf(lagAndelTilkjentYtelse(fom = barn.fødselsdato.toYearMonth(), tom = barn.fødselsdato.plusYears(18).toYearMonth(), kalkulertUtbetalingsbeløp = 1000, aktør = barn.aktør))
                every { vilkårService.hentVilkårsvurderingThrows(forrigeBehandling.id) } returns lagVilkårsvurdering(forrigeBehandling.id)
                every { featureToggleService.isEnabled(FeatureToggle.OPPRETT_MANUELL_OPPGAVE_AUTOVEDTAK_FINNMARK_SVALBARD) } returns true
                justRun { opprettTaskService.opprettOppgaveForFinnmarksOgSvalbardtilleggTask(any(), any()) }

                // Act
                vilkårsvurderingSteg.preValiderSteg(behandling, null)

                // Assert
                verify(exactly = 1) {
                    opprettTaskService.opprettOppgaveForFinnmarksOgSvalbardtilleggTask(
                        behandling.fagsak.id,
                        "Finnmarkstillegg kan ikke behandles automatisk som følge av adresseendring. Det finnes perioder der søker er bosatt i Finnmark/Nord-Troms samtidig som et barn med delt barnetrygd ikke er bosatt i Finnmark/Nord-Troms.\nEndring av Finnmarkstillegg må håndteres manuelt.",
                    )
                }
            }

            @Test
            fun `Skal ikke opprette manuell oppgave i finnmarkstillegg-behandlinger dersom søker bor i Finnmark, og barn med delt bosted ikke bor i Finnmark men ikke har løpende andeler`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.FINNMARKSTILLEGG)
                val forrigeBehandling = lagBehandling()
                val vilkårsvurdering =
                    lagVilkårsvurdering(1L, behandling) {
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                person = søker,
                                resultat = Resultat.OPPFYLT,
                                periodeFom = søker.fødselsdato,
                                periodeTom = null,
                                lagFullstendigVilkårResultat = true,
                                personType = PersonType.SØKER,
                            ),
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                person = barn,
                                resultat = Resultat.OPPFYLT,
                                periodeFom = barn.fødselsdato,
                                periodeTom = null,
                                lagFullstendigVilkårResultat = true,
                                personType = PersonType.BARN,
                            ),
                        )
                    }

                vilkårsvurdering.personResultater
                    .first { it.aktør == søker.aktør }
                    .vilkårResultater
                    .first { it.vilkårType == Vilkår.BOSATT_I_RIKET }
                    .utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)

                vilkårsvurdering.personResultater
                    .first { it.aktør == barn.aktør }
                    .vilkårResultater
                    .first { it.vilkårType == Vilkår.BOR_MED_SØKER }
                    .utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED)

                every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns forrigeBehandling
                every { persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(behandling.id) } returns listOf(søker.tilPersonEnkel(), barn.tilPersonEnkel())
                every { vilkårService.hentVilkårsvurdering(behandling.id) } returns vilkårsvurdering
                every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns
                    listOf(lagAndelTilkjentYtelse(fom = barn.fødselsdato.toYearMonth(), tom = barn.fødselsdato.plusYears(18).toYearMonth(), kalkulertUtbetalingsbeløp = 0, aktør = barn.aktør))
                every { vilkårService.hentVilkårsvurderingThrows(forrigeBehandling.id) } returns lagVilkårsvurdering(forrigeBehandling.id)
                every { featureToggleService.isEnabled(FeatureToggle.OPPRETT_MANUELL_OPPGAVE_AUTOVEDTAK_FINNMARK_SVALBARD) } returns true
                justRun { opprettTaskService.opprettOppgaveForFinnmarksOgSvalbardtilleggTask(any(), any()) }

                // Act
                vilkårsvurderingSteg.preValiderSteg(behandling, null)

                // Assert
                verify(exactly = 0) { opprettTaskService.opprettOppgaveForFinnmarksOgSvalbardtilleggTask(any(), any()) }
            }

            @Test
            fun `Skal opprette manuell oppgave i finnmarkstillegg-behandlinger dersom søker bor i Finnmark med ett barn, og ett barn med delt bosted ikke bor i Finnmark`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.FINNMARKSTILLEGG)
                val barn2 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 1, 1))
                val forrigeBehandling = lagBehandling()

                val vilkårsvurdering =
                    lagVilkårsvurdering(1L, behandling) {
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                person = søker,
                                resultat = Resultat.OPPFYLT,
                                periodeFom = søker.fødselsdato,
                                periodeTom = null,
                                lagFullstendigVilkårResultat = true,
                                personType = PersonType.SØKER,
                            ),
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                person = barn,
                                resultat = Resultat.OPPFYLT,
                                periodeFom = barn.fødselsdato,
                                periodeTom = null,
                                lagFullstendigVilkårResultat = true,
                                personType = PersonType.BARN,
                            ),
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                person = barn2,
                                resultat = Resultat.OPPFYLT,
                                periodeFom = barn2.fødselsdato,
                                periodeTom = null,
                                lagFullstendigVilkårResultat = true,
                                personType = PersonType.BARN,
                            ),
                        )
                    }

                vilkårsvurdering.personResultater
                    .first { it.aktør == søker.aktør }
                    .vilkårResultater
                    .first { it.vilkårType == Vilkår.BOSATT_I_RIKET }
                    .utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)

                vilkårsvurdering.personResultater
                    .first { it.aktør == barn2.aktør }
                    .vilkårResultater
                    .first { it.vilkårType == Vilkår.BOSATT_I_RIKET }
                    .utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)

                vilkårsvurdering.personResultater
                    .first { it.aktør == barn.aktør }
                    .vilkårResultater
                    .first { it.vilkårType == Vilkår.BOR_MED_SØKER }
                    .utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED)

                every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns forrigeBehandling
                every { persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(behandling.id) } returns listOf(søker.tilPersonEnkel(), barn.tilPersonEnkel(), barn2.tilPersonEnkel())
                every { vilkårService.hentVilkårsvurdering(behandling.id) } returns vilkårsvurdering
                every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns
                    listOf(
                        lagAndelTilkjentYtelse(fom = barn.fødselsdato.toYearMonth(), tom = barn.fødselsdato.plusYears(18).toYearMonth(), kalkulertUtbetalingsbeløp = 1000, aktør = barn.aktør),
                        lagAndelTilkjentYtelse(fom = barn2.fødselsdato.toYearMonth(), tom = barn2.fødselsdato.plusYears(18).toYearMonth(), kalkulertUtbetalingsbeløp = 1000, aktør = barn2.aktør),
                    )
                every { vilkårService.hentVilkårsvurderingThrows(forrigeBehandling.id) } returns lagVilkårsvurdering(forrigeBehandling.id)
                every { featureToggleService.isEnabled(FeatureToggle.OPPRETT_MANUELL_OPPGAVE_AUTOVEDTAK_FINNMARK_SVALBARD) } returns true
                justRun { opprettTaskService.opprettOppgaveForFinnmarksOgSvalbardtilleggTask(any(), any()) }

                // Act
                vilkårsvurderingSteg.preValiderSteg(behandling, null)

                // Assert
                verify(exactly = 1) {
                    opprettTaskService.opprettOppgaveForFinnmarksOgSvalbardtilleggTask(
                        behandling.fagsak.id,
                        "Finnmarkstillegg kan ikke behandles automatisk som følge av adresseendring. Det finnes perioder der søker er bosatt i Finnmark/Nord-Troms samtidig som et barn med delt barnetrygd ikke er bosatt i Finnmark/Nord-Troms.\nEndring av Finnmarkstillegg må håndteres manuelt.",
                    )
                }
            }

            @Test
            fun `Skal ikke opprette manuell oppgave i finnmarkstillegg-behandlinger dersom søker ikke bor i Finnmark, og barn med delt bosted ikke bor i Finnmark`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.FINNMARKSTILLEGG)
                val forrigeBehandling = lagBehandling()
                val vilkårsvurdering =
                    lagVilkårsvurdering(1L, behandling) {
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                person = søker,
                                resultat = Resultat.OPPFYLT,
                                periodeFom = søker.fødselsdato,
                                periodeTom = null,
                                lagFullstendigVilkårResultat = true,
                                personType = PersonType.SØKER,
                            ),
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                person = barn,
                                resultat = Resultat.OPPFYLT,
                                periodeFom = barn.fødselsdato,
                                periodeTom = null,
                                lagFullstendigVilkårResultat = true,
                                personType = PersonType.BARN,
                            ),
                        )
                    }

                vilkårsvurdering.personResultater
                    .first { it.aktør == barn.aktør }
                    .vilkårResultater
                    .first { it.vilkårType == Vilkår.BOR_MED_SØKER }
                    .utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED)

                every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns forrigeBehandling
                every { persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(behandling.id) } returns listOf(søker.tilPersonEnkel(), barn.tilPersonEnkel())
                every { vilkårService.hentVilkårsvurdering(behandling.id) } returns vilkårsvurdering
                every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns
                    listOf(lagAndelTilkjentYtelse(fom = barn.fødselsdato.toYearMonth(), tom = barn.fødselsdato.plusYears(18).toYearMonth(), kalkulertUtbetalingsbeløp = 0, aktør = barn.aktør))
                every { vilkårService.hentVilkårsvurderingThrows(forrigeBehandling.id) } returns lagVilkårsvurdering(forrigeBehandling.id)
                every { featureToggleService.isEnabled(FeatureToggle.OPPRETT_MANUELL_OPPGAVE_AUTOVEDTAK_FINNMARK_SVALBARD) } returns true
                justRun { opprettTaskService.opprettOppgaveForFinnmarksOgSvalbardtilleggTask(any(), any()) }

                // Act
                vilkårsvurderingSteg.preValiderSteg(behandling, null)

                // Assert
                verify(exactly = 0) { opprettTaskService.opprettOppgaveForFinnmarksOgSvalbardtilleggTask(any(), any()) }
            }
        }
    }
}
