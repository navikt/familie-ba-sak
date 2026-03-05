package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.tilPersonEnkel
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class TilkjentYtelseValideringServiceTest {
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val beregningServiceMock = mockk<BeregningService>()
    private val totrinnskontrollServiceMock = mockk<TotrinnskontrollService>()
    private val persongrunnlagServiceMock = mockk<PersongrunnlagService>()
    private val behandlingSøknadsinfoService = mockk<BehandlingSøknadsinfoService>()

    private lateinit var tilkjentYtelseValideringService: TilkjentYtelseValideringService

    @BeforeEach
    fun setUp() {
        tilkjentYtelseValideringService =
            TilkjentYtelseValideringService(
                beregningService = beregningServiceMock,
                totrinnskontrollService = totrinnskontrollServiceMock,
                persongrunnlagService = persongrunnlagServiceMock,
                behandlingHentOgPersisterService = behandlingHentOgPersisterService,
                behandlingSøknadsinfoService = behandlingSøknadsinfoService,
            )

        every {
            beregningServiceMock.hentRelevanteTilkjentYtelserForBarn(
                barnAktør = barn1.aktør,
                fagsakId = any(),
            )
        } answers { emptyList() }
        every {
            beregningServiceMock.hentRelevanteTilkjentYtelserForBarn(
                barnAktør = barn2.aktør,
                fagsakId = any(),
            )
        } answers { emptyList() }
        every {
            beregningServiceMock.hentRelevanteTilkjentYtelserForBarn(
                barnAktør = barn3MedUtbetalinger.aktør,
                fagsakId = any(),
            )
        } answers {
            listOf(
                TilkjentYtelse(
                    behandling = lagBehandling(),
                    endretDato = LocalDate.now().minusYears(1),
                    opprettetDato = LocalDate.now().minusYears(1),
                ),
            )
        }
    }

    @Test
    fun `Skal returnere false hvis ingen barn allerede mottar barnetrygd`() {
        Assertions.assertFalse(
            tilkjentYtelseValideringService.barnetrygdLøperForAnnenForelder(
                behandling = lagBehandling(),
                barna = listOf(barn1, barn2),
            ),
        )
    }

    @Test
    fun `Skal returnere true hvis det løper barnetrygd for minst ett barn`() {
        Assertions.assertTrue(
            tilkjentYtelseValideringService.barnetrygdLøperForAnnenForelder(
                behandling = lagBehandling(),
                barna = listOf(barn1, barn3MedUtbetalinger),
            ),
        )
    }

    @Test
    fun `Skal returnere liste med personer som har etterbetaling som er mer enn 3 år tilbake i tid`() {
        val behandling = lagBehandling()
        val person1 = tilfeldigPerson()
        val person2 = tilfeldigPerson()

        val tilkjentYtelse =
            TilkjentYtelse(
                behandling = behandling,
                opprettetDato = LocalDate.now(),
                endretDato = LocalDate.now(),
                andelerTilkjentYtelse =
                    mutableSetOf(
                        lagAndelTilkjentYtelse(
                            fom = inneværendeMåned().minusYears(4),
                            tom = inneværendeMåned(),
                            beløp = 2108,
                            person = person1,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = inneværendeMåned().minusYears(4),
                            tom = inneværendeMåned(),
                            beløp = 2108,
                            person = person2,
                        ),
                    ),
            )

        val forrigeBehandling = lagBehandling()

        val forrigeTilkjentYtelse =
            TilkjentYtelse(
                behandling = forrigeBehandling,
                opprettetDato = LocalDate.now(),
                endretDato = LocalDate.now(),
                andelerTilkjentYtelse =
                    mutableSetOf(
                        lagAndelTilkjentYtelse(
                            fom = inneværendeMåned().minusYears(4),
                            tom = inneværendeMåned(),
                            beløp = 2108,
                            person = person1,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = inneværendeMåned().minusYears(4),
                            tom = inneværendeMåned(),
                            beløp = 1054,
                            person = person2,
                        ),
                    ),
            )

        every { beregningServiceMock.hentOptionalTilkjentYtelseForBehandling(behandlingId = behandling.id) } answers { tilkjentYtelse }
        every { behandlingHentOgPersisterService.hent(behandlingId = behandling.id) } answers { behandling }
        every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling = behandling) } answers { forrigeBehandling }
        every { beregningServiceMock.hentOptionalTilkjentYtelseForBehandling(behandlingId = forrigeBehandling.id) } answers { forrigeTilkjentYtelse }
        every { behandlingSøknadsinfoService.hentSøknadMottattDato(any()) } returns behandling.opprettetTidspunkt

        Assertions.assertTrue(tilkjentYtelseValideringService.finnAktørerMedUgyldigEtterbetalingsperiode(behandlingId = behandling.id).size == 1)
        Assertions.assertEquals(
            person2.aktør,
            tilkjentYtelseValideringService
                .finnAktørerMedUgyldigEtterbetalingsperiode(behandlingId = behandling.id)
                .single(),
        )
    }

    @Nested
    inner class ValiderAtBarnIkkeFårFlereUtbetalingerSammePeriode {
        private val dagensDato = LocalDate.now()

        @Test
        fun `skal kaste exception of man finner utbetalinger som overstiger 100 prosent`() {
            // Arrange
            val mor = lagPerson(type = PersonType.SØKER, fødselsdato = dagensDato.minusYears(35))
            val far = lagPerson(type = PersonType.SØKER, fødselsdato = dagensDato.minusYears(36))
            val barn1 = lagPerson(type = PersonType.BARN, fødselsdato = dagensDato.minusYears(8))
            val barn2 = lagPerson(type = PersonType.BARN, fødselsdato = dagensDato.minusYears(6))
            val barn3 = lagPerson(type = PersonType.BARN, fødselsdato = dagensDato.minusYears(6))

            val fagsakMor = lagFagsak(aktør = mor.aktør)
            val behandlingMor = lagBehandling(fagsak = fagsakMor)
            val tilkjentYtelseMor =
                lagTilkjentYtelse(
                    behandling = behandlingMor,
                    lagAndelerTilkjentYtelse = { tilkjentYtelse ->
                        setOf(
                            lagAndelTilkjentYtelse(
                                id = 1L,
                                tilkjentYtelse = tilkjentYtelse,
                                behandling = behandlingMor,
                                aktør = barn1.aktør,
                                fom = YearMonth.of(2025, 5),
                                tom = YearMonth.of(2035, 5),
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kalkulertUtbetalingsbeløp = 1968,
                                nasjonaltPeriodebeløp = 1968,
                                beløpUtenEndretUtbetaling = null,
                                prosent = BigDecimal(100),
                                sats = 1968,
                            ),
                            lagAndelTilkjentYtelse(
                                id = 2L,
                                tilkjentYtelse = tilkjentYtelse,
                                behandling = behandlingMor,
                                aktør = barn2.aktør,
                                fom = YearMonth.of(2025, 5),
                                tom = YearMonth.of(2037, 7),
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kalkulertUtbetalingsbeløp = 1968,
                                nasjonaltPeriodebeløp = 1968,
                                beløpUtenEndretUtbetaling = null,
                                prosent = BigDecimal(100),
                                sats = 1968,
                            ),
                            lagAndelTilkjentYtelse(
                                id = 3L,
                                tilkjentYtelse = tilkjentYtelse,
                                behandling = behandlingMor,
                                aktør = barn3.aktør,
                                fom = YearMonth.of(2025, 5),
                                tom = YearMonth.of(2037, 7),
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kalkulertUtbetalingsbeløp = 1968,
                                nasjonaltPeriodebeløp = 1968,
                                beløpUtenEndretUtbetaling = null,
                                prosent = BigDecimal(100),
                                sats = 1968,
                            ),
                        )
                    },
                )

            val fagsakFar = lagFagsak(aktør = far.aktør)
            val behandlingFar = lagBehandling(fagsak = fagsakFar)
            val tilkjentYtelseFar =
                lagTilkjentYtelse(
                    behandling = behandlingFar,
                    lagAndelerTilkjentYtelse = { tilkjentYtelse ->
                        setOf(
                            lagAndelTilkjentYtelse(
                                id = 4L,
                                tilkjentYtelse = tilkjentYtelse,
                                behandling = behandlingFar,
                                aktør = barn1.aktør,
                                fom = YearMonth.of(2025, 10),
                                tom = YearMonth.of(2025, 11),
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kalkulertUtbetalingsbeløp = 0,
                                nasjonaltPeriodebeløp = 0,
                                beløpUtenEndretUtbetaling = 984,
                                prosent = BigDecimal(0),
                                sats = 1968,
                            ),
                            lagAndelTilkjentYtelse(
                                id = 5L,
                                tilkjentYtelse = tilkjentYtelse,
                                behandling = behandlingFar,
                                aktør = barn2.aktør,
                                fom = YearMonth.of(2025, 10),
                                tom = YearMonth.of(2025, 11),
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kalkulertUtbetalingsbeløp = 0,
                                nasjonaltPeriodebeløp = 0,
                                beløpUtenEndretUtbetaling = 984,
                                prosent = BigDecimal(0),
                                sats = 1968,
                            ),
                            lagAndelTilkjentYtelse(
                                id = 6L,
                                tilkjentYtelse = tilkjentYtelse,
                                behandling = behandlingFar,
                                aktør = barn3.aktør,
                                fom = YearMonth.of(2025, 10),
                                tom = YearMonth.of(2025, 11),
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kalkulertUtbetalingsbeløp = 0,
                                nasjonaltPeriodebeløp = 0,
                                beløpUtenEndretUtbetaling = 984,
                                prosent = BigDecimal(0),
                                sats = 1968,
                            ),
                            lagAndelTilkjentYtelse(
                                id = 7L,
                                tilkjentYtelse = tilkjentYtelse,
                                behandling = behandlingFar,
                                aktør = barn1.aktør,
                                fom = YearMonth.of(2025, 12),
                                tom = YearMonth.of(2025, 12),
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kalkulertUtbetalingsbeløp = 984,
                                nasjonaltPeriodebeløp = 984,
                                beløpUtenEndretUtbetaling = 984,
                                prosent = BigDecimal(50),
                                sats = 1968,
                            ),
                            lagAndelTilkjentYtelse(
                                id = 8L,
                                tilkjentYtelse = tilkjentYtelse,
                                behandling = behandlingFar,
                                aktør = barn2.aktør,
                                fom = YearMonth.of(2025, 12),
                                tom = YearMonth.of(2025, 12),
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kalkulertUtbetalingsbeløp = 984,
                                nasjonaltPeriodebeløp = 984,
                                beløpUtenEndretUtbetaling = 984,
                                prosent = BigDecimal(50),
                                sats = 1968,
                            ),
                            lagAndelTilkjentYtelse(
                                id = 9L,
                                tilkjentYtelse = tilkjentYtelse,
                                behandling = behandlingFar,
                                aktør = barn3.aktør,
                                fom = YearMonth.of(2025, 12),
                                tom = YearMonth.of(2025, 12),
                                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                                kalkulertUtbetalingsbeløp = 984,
                                nasjonaltPeriodebeløp = 984,
                                beløpUtenEndretUtbetaling = 984,
                                prosent = BigDecimal(50),
                                sats = 1968,
                            ),
                        )
                    },
                )

            every {
                beregningServiceMock.hentTilkjentYtelseForBehandling(behandlingMor.id)
            } returns tilkjentYtelseMor

            every {
                persongrunnlagServiceMock.hentSøkerOgBarnPåBehandlingThrows(behandlingMor.id)
            } returns
                listOf(
                    mor.tilPersonEnkel(),
                    barn1.tilPersonEnkel(),
                    barn2.tilPersonEnkel(),
                    barn3.tilPersonEnkel(),
                )

            every {
                beregningServiceMock.hentRelevanteTilkjentYtelserForBarn(any(), eq(fagsakMor.id))
            } returns listOf(tilkjentYtelseFar)

            // Act & assert
            val exception =
                assertThrows<UtbetalingsikkerhetFeil> {
                    tilkjentYtelseValideringService.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(behandlingMor)
                }
            assertThat(exception.message).contains("Vi finner utbetalinger som overstiger 100%")
        }
    }

    companion object {
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)
        val barn3MedUtbetalinger = lagPerson(type = PersonType.BARN)
    }
}
