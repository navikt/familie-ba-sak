package no.nav.familie.ba.sak.kjerne.endretutbetaling

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.ekstern.restDomene.RestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.tilRestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class EndretUtbetalingAndelServiceTest {
    private val mockEndretUtbetalingAndelRepository = mockk<EndretUtbetalingAndelRepository>()
    private val mockPersongrunnlagService = mockk<PersongrunnlagService>()
    private val mockPersonopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()
    private val mockAndelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val mockVilkårsvurderingService = mockk<VilkårsvurderingService>()
    private val mockEndretUtbetalingAndelHentOgPersisterService = mockk<EndretUtbetalingAndelHentOgPersisterService>()
    private val mockBeregningService = mockk<BeregningService>()
    private val mockBehandlingSøknadsinfoService = mockk<BehandlingSøknadsinfoService>()
    private val mockUnleashService = mockk<UnleashNextMedContextService>()

    private lateinit var endretUtbetalingAndelService: EndretUtbetalingAndelService

    @BeforeEach
    fun setup() {
        endretUtbetalingAndelService =
            EndretUtbetalingAndelService(
                endretUtbetalingAndelRepository = mockEndretUtbetalingAndelRepository,
                personopplysningGrunnlagRepository = mockPersonopplysningGrunnlagRepository,
                beregningService = mockBeregningService,
                persongrunnlagService = mockPersongrunnlagService,
                andelTilkjentYtelseRepository = mockAndelTilkjentYtelseRepository,
                vilkårsvurderingService = mockVilkårsvurderingService,
                endretUtbetalingAndelHentOgPersisterService = mockEndretUtbetalingAndelHentOgPersisterService,
                behandlingSøknadsinfoService = mockBehandlingSøknadsinfoService,
                unleashService = mockUnleashService,
            )
    }

    @Test
    fun `Skal kaste feil hvis endringsperiode har årsak delt bosted, men ikke overlapper med delt bosted perioder`() {
        val behandling = lagBehandling()
        val barn = lagPerson(type = PersonType.BARN)
        val endretUtbetalingAndel =
            lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
                behandlingId = behandling.id,
                personer = setOf(barn),
                årsak = Årsak.DELT_BOSTED,
                fom = YearMonth.now().minusMonths(5),
                tom = YearMonth.now().minusMonths(1),
            )
        val restEndretUtbetalingAndel = endretUtbetalingAndel.tilRestEndretUtbetalingAndel()

        val andelerTilkjentYtelse =
            listOf<AndelTilkjentYtelse>(
                lagAndelTilkjentYtelse(
                    person = barn,
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().plusMonths(5),
                ),
                lagAndelTilkjentYtelse(
                    person = barn,
                    fom = YearMonth.now().plusMonths(6),
                    tom = YearMonth.now().plusMonths(11),
                ),
            )

        val vilkårsvurderingUtenDeltBosted =
            Vilkårsvurdering(
                behandling = behandling,
            )
        vilkårsvurderingUtenDeltBosted.personResultater =
            setOf(
                lagPersonResultat(
                    vilkårsvurdering = vilkårsvurderingUtenDeltBosted,
                    person = barn,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = endretUtbetalingAndel.fom?.minusMonths(1)?.førsteDagIInneværendeMåned(),
                    periodeTom = LocalDate.now(),
                    erDeltBosted = false,
                    lagFullstendigVilkårResultat = true,
                    personType = PersonType.BARN,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                ),
            )

        every { mockEndretUtbetalingAndelRepository.getReferenceById(any()) } returns endretUtbetalingAndel.endretUtbetalingAndel
        every { mockPersongrunnlagService.hentPersonerPåBehandling(any(), behandling) } returns listOf(barn)
        every { mockPersonopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                barn,
            )
        every { mockAndelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling.id) } returns andelerTilkjentYtelse
        every { mockEndretUtbetalingAndelHentOgPersisterService.hentForBehandling(behandlingId = behandling.id) } returns emptyList()
        every { mockVilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id) } returns vilkårsvurderingUtenDeltBosted
        every { mockUnleashService.isEnabled(FeatureToggle.SKAL_SPLITTE_ENDRET_UTBETALING_ANDELER) } returns true

        val feil =
            assertThrows<FunksjonellFeil> {
                endretUtbetalingAndelService.oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
                    behandling = behandling,
                    endretUtbetalingAndelId = endretUtbetalingAndel.id,
                    restEndretUtbetalingAndel = restEndretUtbetalingAndel,
                )
            }
        Assertions.assertEquals(
            "Du har valgt årsaken 'delt bosted', denne samstemmer ikke med vurderingene gjort på vilkårsvurderingssiden i perioden du har valgt.",
            feil.frontendFeilmelding,
        )
    }

    @Test
    fun `Skal splitte EndretUtbetalingAndel med tom til og med-dato og flere personer`() {
        // Arrange
        val behandling = lagBehandling()
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)

        val endretUtbetalingAndel =
            lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
                behandlingId = behandling.id,
                personer = setOf(barn1, barn2),
                prosent = BigDecimal.ZERO,
                årsak = Årsak.ENDRE_MOTTAKER,
                fom = YearMonth.now(),
                tom = null,
            )

        val restEndretUtbetalingAndel = endretUtbetalingAndel.tilRestEndretUtbetalingAndel()

        val andelerTilkjentYtelse =
            listOf(
                lagAndelTilkjentYtelse(
                    person = barn1,
                    fom = YearMonth.now().minusYears(1),
                    tom = YearMonth.now().plusYears(1),
                ),
                lagAndelTilkjentYtelse(
                    person = barn2,
                    fom = YearMonth.now().minusYears(1),
                    tom = YearMonth.now().plusYears(2),
                ),
            )

        val lagretEndretUtbetalingAndelerSlot = slot<List<EndretUtbetalingAndel>>()
        val slettetEndretUtbetalingAndelSlot = slot<EndretUtbetalingAndel>()

        every { mockEndretUtbetalingAndelRepository.getReferenceById(any()) } returns EndretUtbetalingAndel(behandlingId = behandling.id)
        every { mockEndretUtbetalingAndelRepository.delete(capture(slettetEndretUtbetalingAndelSlot)) } returns mockk()
        every { mockEndretUtbetalingAndelRepository.saveAll(capture(lagretEndretUtbetalingAndelerSlot)) } returnsArgument 0
        every { mockPersongrunnlagService.hentPersonerPåBehandling(any(), behandling) } returns listOf(barn1, barn2)
        every { mockPersonopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns lagTestPersonopplysningGrunnlag(behandling.id, barn1, barn2)
        every { mockAndelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling.id) } returns andelerTilkjentYtelse
        every { mockEndretUtbetalingAndelHentOgPersisterService.hentForBehandling(behandlingId = behandling.id) } returns emptyList()
        every { mockUnleashService.isEnabled(FeatureToggle.SKAL_SPLITTE_ENDRET_UTBETALING_ANDELER) } returns true
        every { mockBeregningService.oppdaterBehandlingMedBeregning(any(), any(), any()) } returns mockk()
        every { mockVilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id) } returns mockk()

        // Act
        endretUtbetalingAndelService.oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
            behandling = behandling,
            endretUtbetalingAndelId = endretUtbetalingAndel.id,
            restEndretUtbetalingAndel = restEndretUtbetalingAndel,
        )

        // Assert
        val lagredeEndretUtbetalingAndeler = lagretEndretUtbetalingAndelerSlot.captured

        assertThat(lagredeEndretUtbetalingAndeler).hasSize(2)

        assertThat(lagredeEndretUtbetalingAndeler[0].fom).isEqualTo(YearMonth.now())
        assertThat(lagredeEndretUtbetalingAndeler[0].tom).isEqualTo(YearMonth.now().plusYears(1))
        assertThat(lagredeEndretUtbetalingAndeler[0].personer).containsExactlyInAnyOrder(barn1, barn2)

        assertThat(lagredeEndretUtbetalingAndeler[1].fom).isEqualTo(YearMonth.now().plusYears(1).plusMonths(1))
        assertThat(lagredeEndretUtbetalingAndeler[1].tom).isEqualTo(YearMonth.now().plusYears(2))
        assertThat(lagredeEndretUtbetalingAndeler[1].personer).containsExactly(barn2)

        val slettetEndretUtbetalingAndel = slettetEndretUtbetalingAndelSlot.captured
        assertThat(slettetEndretUtbetalingAndel).isEqualTo(endretUtbetalingAndel.endretUtbetalingAndel)
    }

    @Test
    fun `Skal håndtere RestEndretUtbetalingAndel med blanding av nytt og gammelt felt for personIdent(er)`() {
        // Arrange
        val behandling = lagBehandling()
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)

        val endretUtbetalingAndel = EndretUtbetalingAndel(behandlingId = behandling.id)

        val andelerTilkjentYtelse =
            listOf(
                lagAndelTilkjentYtelse(
                    person = barn1,
                    fom = YearMonth.of(2025, 1),
                    tom = YearMonth.of(2025, 1),
                ),
                lagAndelTilkjentYtelse(
                    person = barn2,
                    fom = YearMonth.of(2025, 1),
                    tom = YearMonth.of(2025, 1),
                ),
            )

        val vilkårsvurdering =
            Vilkårsvurdering(behandling = behandling).apply {
                personResultater =
                    setOf(
                        lagPersonResultat(
                            vilkårsvurdering = this,
                            person = barn1,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.of(2024, 12, 1),
                            periodeTom = LocalDate.of(2025, 2, 1),
                            lagFullstendigVilkårResultat = true,
                            personType = PersonType.BARN,
                            vilkårType = Vilkår.BOR_MED_SØKER,
                        ),
                        lagPersonResultat(
                            vilkårsvurdering = this,
                            person = barn2,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.of(2024, 12, 1),
                            periodeTom = LocalDate.of(2025, 2, 1),
                            lagFullstendigVilkårResultat = true,
                            personType = PersonType.BARN,
                            vilkårType = Vilkår.BOR_MED_SØKER,
                        ),
                    )
            }

        val lagretEndretUtbetalingAndel = slot<EndretUtbetalingAndel>()

        every { mockEndretUtbetalingAndelRepository.getReferenceById(any()) } returns endretUtbetalingAndel
        every { mockPersongrunnlagService.hentPersonerPåBehandling(any(), behandling) } answers {
            listOf(barn1, barn2).filter { it.aktør.aktivFødselsnummer() in firstArg<List<String>>() }
        }
        every { mockPersonopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns
            lagTestPersonopplysningGrunnlag(behandling.id, barn1, barn2)
        every { mockAndelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling.id) } returns andelerTilkjentYtelse
        every { mockEndretUtbetalingAndelHentOgPersisterService.hentForBehandling(behandlingId = behandling.id) } returns listOf(endretUtbetalingAndel)
        every { mockVilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id) } returns vilkårsvurdering
        every { mockEndretUtbetalingAndelRepository.saveAndFlush(capture(lagretEndretUtbetalingAndel)) } returns mockk()
        every { mockBeregningService.oppdaterBehandlingMedBeregning(any(), any(), any()) } returns mockk()
        every { mockUnleashService.isEnabled(FeatureToggle.SKAL_SPLITTE_ENDRET_UTBETALING_ANDELER) } returns true

        var restEndretUtbetalingAndel =
            RestEndretUtbetalingAndel(
                id = 0,
                personIdent = null,
                personIdenter = null,
                prosent = BigDecimal.ZERO,
                fom = YearMonth.of(2025, 1),
                tom = YearMonth.of(2025, 1),
                årsak = Årsak.ALLEREDE_UTBETALT,
                avtaletidspunktDeltBosted = LocalDate.now(),
                søknadstidspunkt = LocalDate.now(),
                begrunnelse = "Test begrunnelse",
                erTilknyttetAndeler = true,
            )

        // Act & Assert
        assertThrows<FunksjonellFeil> {
            endretUtbetalingAndelService.oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
                behandling = behandling,
                endretUtbetalingAndelId = 0,
                restEndretUtbetalingAndel = restEndretUtbetalingAndel,
            )
        }

        // Arrange
        restEndretUtbetalingAndel =
            restEndretUtbetalingAndel.copy(
                personIdenter = null,
                personIdent = barn1.aktør.aktivFødselsnummer(),
            )

        // Act & Assert
        assertDoesNotThrow {
            endretUtbetalingAndelService.oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
                behandling = behandling,
                endretUtbetalingAndelId = 0,
                restEndretUtbetalingAndel = restEndretUtbetalingAndel,
            )
        }

        // Assert
        assertThat(lagretEndretUtbetalingAndel.captured).isEqualTo(
            endretUtbetalingAndel.copy(
                personer = mutableSetOf(barn1),
            ),
        )

        // Arrange
        restEndretUtbetalingAndel =
            restEndretUtbetalingAndel.copy(
                personIdenter = listOf(barn1.aktør.aktivFødselsnummer(), barn2.aktør.aktivFødselsnummer()),
                personIdent = barn1.aktør.aktivFødselsnummer(),
            )

        // Act & Assert
        assertDoesNotThrow {
            endretUtbetalingAndelService.oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
                behandling = behandling,
                endretUtbetalingAndelId = 0,
                restEndretUtbetalingAndel = restEndretUtbetalingAndel,
            )
        }

        // Assert
        assertThat(lagretEndretUtbetalingAndel.captured).isEqualTo(
            endretUtbetalingAndel.copy(
                personer = mutableSetOf(barn1, barn2),
            ),
        )
    }

    @Nested
    inner class GenererEndretUtbetalingAndelerMedÅrsakEtterbetaling3ÅrEller3Mnd {
        private val behandling = lagBehandling()

        @Test
        fun `Skal generere endret utbetaling andeler med årsak etterbetaling 3 måneder`() {
            // Arrange
            val søker = lagPerson(type = PersonType.SØKER)
            val barn = lagPerson(type = PersonType.BARN)

            val fomAndelTilkjentYtelse = YearMonth.of(2020, 1)
            val tomAndelTilkjentYtelse = YearMonth.of(2025, 12)
            val søknadMottattDato = LocalDateTime.of(2025, 4, 15, 0, 0)
            val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søker, barn)

            every { mockEndretUtbetalingAndelRepository.findByBehandlingId(any()) } returns emptyList()
            every { mockEndretUtbetalingAndelRepository.saveAllAndFlush<EndretUtbetalingAndel>(any()) } returnsArgument 0
            every { mockEndretUtbetalingAndelRepository.deleteAllById(any()) } just Runs
            every { mockPersonopplysningGrunnlagRepository.findByBehandlingAndAktiv(any()) } returns personopplysningGrunnlag
            every { mockBeregningService.oppdaterBehandlingMedBeregning(any(), any()) } returns mockk()
            every { mockBehandlingSøknadsinfoService.hentSøknadMottattDato(any()) } returns søknadMottattDato
            every { mockPersongrunnlagService.hentPersonerPåBehandling(any(), any()) } returns listOf(søker, barn)
            every { mockBeregningService.hentAndelerTilkjentYtelseForBehandling(any()) } returns
                listOf(
                    lagAndelTilkjentYtelse(
                        behandling = behandling,
                        person = barn,
                        fom = fomAndelTilkjentYtelse,
                        tom = tomAndelTilkjentYtelse,
                        beløp = 2000,
                    ),
                )
            every { mockBeregningService.hentAndelerFraForrigeIverksattebehandling(any()) } returns
                listOf(
                    lagAndelTilkjentYtelse(
                        behandling = behandling,
                        person = barn,
                        fom = fomAndelTilkjentYtelse,
                        tom = tomAndelTilkjentYtelse,
                        beløp = 1000,
                    ),
                )

            // Act
            endretUtbetalingAndelService.genererEndretUtbetalingAndelerMedÅrsakEtterbetaling3ÅrEller3Mnd(behandling = behandling)

            // Assert
            val forventetEndretUtbetalingAndel =
                EndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    personer = mutableSetOf(barn),
                    prosent = BigDecimal.ZERO,
                    fom = fomAndelTilkjentYtelse,
                    tom = søknadMottattDato.minusMonths(4).toLocalDate().toYearMonth(),
                    årsak = Årsak.ETTERBETALING_3MND,
                    søknadstidspunkt = søknadMottattDato.toLocalDate(),
                    begrunnelse = "Fylt ut automatisk fra søknadstidspunkt.",
                )
            verify(exactly = 1) { mockEndretUtbetalingAndelRepository.deleteAllById(emptyList()) }
            verify(exactly = 1) { mockEndretUtbetalingAndelRepository.saveAllAndFlush(listOf(forventetEndretUtbetalingAndel)) }
            verify(exactly = 2) { mockBeregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag) }
        }

        @Test
        fun `Skal ikke generere endret utbetaling andeler hvis søknadMottattDato er null`() {
            // Arrange
            every { mockBehandlingSøknadsinfoService.hentSøknadMottattDato(any()) } returns null

            // Act
            endretUtbetalingAndelService.genererEndretUtbetalingAndelerMedÅrsakEtterbetaling3ÅrEller3Mnd(behandling = behandling)

            // Assert
            verify(exactly = 0) { mockEndretUtbetalingAndelRepository.deleteAllById(any()) }
            verify(exactly = 0) { mockEndretUtbetalingAndelRepository.saveAllAndFlush<EndretUtbetalingAndel>(any()) }
            verify(exactly = 0) { mockBeregningService.oppdaterBehandlingMedBeregning(any(), any()) }
        }
    }
}
