package no.nav.familie.ba.sak.kjerne.endretutbetaling

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.ekstern.restDomene.RestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class EndretUtbetalingAndelServiceTest {
    private val mockEndretUtbetalingAndelRepository = mockk<EndretUtbetalingAndelRepository>()
    private val mockPersongrunnlagService = mockk<PersongrunnlagService>()
    private val mockPersonopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()
    private val mockAndelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val mockVilkårsvurderingService = mockk<VilkårsvurderingService>()
    private val mockEndretUtbetalingAndelHentOgPersisterService = mockk<EndretUtbetalingAndelHentOgPersisterService>()
    private val mockBeregningService = mockk<BeregningService>()

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
}
