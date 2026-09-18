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
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.strengtfortrolig.StrengtFortroligService
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
    private val strengtFortroligService = mockk<StrengtFortroligService>()

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
                strengtFortroligService = strengtFortroligService,
            )

        every {
            beregningServiceMock.hentRelevanteTilkjentYtelserForPerson(
                aktør = barn1.aktør,
                fagsakId = any(),
            )
        } answers { emptyList() }
        every {
            beregningServiceMock.hentRelevanteTilkjentYtelserForPerson(
                aktør = barn2.aktør,
                fagsakId = any(),
            )
        } answers { emptyList() }
        every {
            beregningServiceMock.hentRelevanteTilkjentYtelserForPerson(
                aktør = barn3MedUtbetalinger.aktør,
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
        every { strengtFortroligService.hentSkjermedeBarnUtenLøpendeAndelerSaksbehandlerIkkeHarTilgangTil(behandling.fagsak) } returns emptySet()

        Assertions.assertTrue(tilkjentYtelseValideringService.finnAktørerMedUgyldigEtterbetalingsperiode(behandlingId = behandling.id).size == 1)
        Assertions.assertEquals(
            person2.aktør,
            tilkjentYtelseValideringService
                .finnAktørerMedUgyldigEtterbetalingsperiode(behandlingId = behandling.id)
                .single(),
        )
    }

    @Test
    fun `Skal filtrere ut barn med diskresjonskode uten løpende andeler som saksbehandler ikke har tilgang til`() {
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
                            beløp = 1054,
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
        every { strengtFortroligService.hentSkjermedeBarnUtenLøpendeAndelerSaksbehandlerIkkeHarTilgangTil(behandling.fagsak) } returns setOf(person2.aktør.aktivFødselsnummer())

        val aktørerMedUgyldigEtterbetalingsperiode = tilkjentYtelseValideringService.finnAktørerMedUgyldigEtterbetalingsperiode(behandlingId = behandling.id)

        assertThat(aktørerMedUgyldigEtterbetalingsperiode.size).isEqualTo(1)
        assertThat(person1.aktør).isEqualTo(aktørerMedUgyldigEtterbetalingsperiode.single())
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
                beregningServiceMock.hentRelevanteTilkjentYtelserForPerson(any(), eq(fagsakMor.id))
            } returns listOf(tilkjentYtelseFar)

            // Act & assert
            val exception =
                assertThrows<UtbetalingsikkerhetFeil> {
                    tilkjentYtelseValideringService.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(behandlingMor)
                }
            assertThat(exception.message).contains("Vi finner utbetalinger som overstiger 100%")
        }
    }

    @Nested
    inner class BarnetrygdUtbetalesForBarnIAnnenFagsakIMåned {
        private val behandling = lagBehandling()
        private val barn = lagPerson(type = PersonType.BARN)
        private val vurderingsmåned = YearMonth.of(2025, 11)

        @Test
        fun `skal returnere true når det utbetales barnetrygd for barnet i en annen fagsak i vurderingsmåneden`() {
            // Arrange
            stubRelevantTilkjentYtelseForBarnet(
                fom = vurderingsmåned.minusMonths(3),
                tom = vurderingsmåned.plusMonths(3),
            )

            // Act
            val finnesUtbetalingIAnnenFagsak =
                tilkjentYtelseValideringService.barnetrygdUtbetalesForBarnIAnnenFagsakIMåned(
                    behandling = behandling,
                    barna = listOf(barn),
                    måned = vurderingsmåned,
                )

            // Assert
            assertThat(finnesUtbetalingIAnnenFagsak).isTrue()
        }

        @Test
        fun `skal returnere false når utbetalingen i den andre fagsaken opphørte før vurderingsmåneden`() {
            // Arrange
            stubRelevantTilkjentYtelseForBarnet(
                fom = vurderingsmåned.minusMonths(6),
                tom = vurderingsmåned.minusMonths(1),
            )

            // Act
            val finnesUtbetalingIAnnenFagsak =
                tilkjentYtelseValideringService.barnetrygdUtbetalesForBarnIAnnenFagsakIMåned(
                    behandling = behandling,
                    barna = listOf(barn),
                    måned = vurderingsmåned,
                )

            // Assert
            assertThat(finnesUtbetalingIAnnenFagsak).isFalse()
        }

        @Test
        fun `skal returnere false når utbetalingen i den andre fagsaken først starter etter vurderingsmåneden`() {
            // Arrange
            stubRelevantTilkjentYtelseForBarnet(
                fom = vurderingsmåned.plusMonths(1),
                tom = vurderingsmåned.plusMonths(6),
            )

            // Act
            val finnesUtbetalingIAnnenFagsak =
                tilkjentYtelseValideringService.barnetrygdUtbetalesForBarnIAnnenFagsakIMåned(
                    behandling = behandling,
                    barna = listOf(barn),
                    måned = vurderingsmåned,
                )

            // Assert
            assertThat(finnesUtbetalingIAnnenFagsak).isFalse()
        }

        @Test
        fun `skal returnere false når andelen i vurderingsmåneden er en nullutbetaling`() {
            // Arrange
            stubRelevantTilkjentYtelseForBarnet(
                fom = vurderingsmåned.minusMonths(3),
                tom = vurderingsmåned.plusMonths(3),
                kalkulertUtbetalingsbeløp = 0,
            )

            // Act
            val finnesUtbetalingIAnnenFagsak =
                tilkjentYtelseValideringService.barnetrygdUtbetalesForBarnIAnnenFagsakIMåned(
                    behandling = behandling,
                    barna = listOf(barn),
                    måned = vurderingsmåned,
                )

            // Assert
            assertThat(finnesUtbetalingIAnnenFagsak).isFalse()
        }

        @Test
        fun `skal se bort fra andeler som tilhører andre personer enn barnet`() {
            // Arrange
            val annenPerson = lagPerson()
            stubRelevantTilkjentYtelseForBarnet(
                fom = vurderingsmåned.minusMonths(3),
                tom = vurderingsmåned.plusMonths(3),
                aktør = annenPerson.aktør,
            )

            // Act
            val finnesUtbetalingIAnnenFagsak =
                tilkjentYtelseValideringService.barnetrygdUtbetalesForBarnIAnnenFagsakIMåned(
                    behandling = behandling,
                    barna = listOf(barn),
                    måned = vurderingsmåned,
                )

            // Assert
            assertThat(finnesUtbetalingIAnnenFagsak).isFalse()
        }

        @Test
        fun `skal returnere false når barnet ikke har relevante tilkjente ytelser i andre fagsaker`() {
            // Arrange
            every {
                beregningServiceMock.hentRelevanteTilkjentYtelserForPerson(
                    aktør = barn.aktør,
                    fagsakId = behandling.fagsak.id,
                )
            } returns emptyList()

            // Act
            val finnesUtbetalingIAnnenFagsak =
                tilkjentYtelseValideringService.barnetrygdUtbetalesForBarnIAnnenFagsakIMåned(
                    behandling = behandling,
                    barna = listOf(barn),
                    måned = vurderingsmåned,
                )

            // Assert
            assertThat(finnesUtbetalingIAnnenFagsak).isFalse()
        }

        @Test
        fun `skal returnere true når andelen dekker nøyaktig vurderingsmåneden`() {
            // Arrange
            stubRelevantTilkjentYtelseForBarnet(fom = vurderingsmåned, tom = vurderingsmåned)

            // Act
            val finnesUtbetalingIAnnenFagsak =
                tilkjentYtelseValideringService.barnetrygdUtbetalesForBarnIAnnenFagsakIMåned(
                    behandling = behandling,
                    barna = listOf(barn),
                    måned = vurderingsmåned,
                )

            // Assert
            assertThat(finnesUtbetalingIAnnenFagsak).isTrue()
        }

        @Test
        fun `skal se bort fra søkers ytelser selv om de ligger på barnets aktør`() {
            // Arrange
            stubRelevantTilkjentYtelseForBarnet(
                fom = vurderingsmåned.minusMonths(3),
                tom = vurderingsmåned.plusMonths(3),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
            )

            // Act
            val finnesUtbetalingIAnnenFagsak =
                tilkjentYtelseValideringService.barnetrygdUtbetalesForBarnIAnnenFagsakIMåned(
                    behandling = behandling,
                    barna = listOf(barn),
                    måned = vurderingsmåned,
                )

            // Assert
            assertThat(finnesUtbetalingIAnnenFagsak).isFalse()
        }

        @Test
        fun `skal returnere true når ordinær barnetrygd er differanseberegnet til null, men finnmarkstillegget utbetales`() {
            // Arrange
            val annenBehandling = lagBehandling(fagsak = lagFagsak(id = behandling.fagsak.id + 1))
            val tilkjentYtelse =
                lagTilkjentYtelse(behandling = annenBehandling) { tilkjentYtelse ->
                    setOf(
                        lagAndelTilkjentYtelse(
                            fom = vurderingsmåned,
                            tom = vurderingsmåned,
                            ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                            behandling = annenBehandling,
                            tilkjentYtelse = tilkjentYtelse,
                            aktør = barn.aktør,
                            kalkulertUtbetalingsbeløp = 0,
                        ),
                        lagAndelTilkjentYtelse(
                            fom = vurderingsmåned,
                            tom = vurderingsmåned,
                            ytelseType = YtelseType.FINNMARKSTILLEGG,
                            behandling = annenBehandling,
                            tilkjentYtelse = tilkjentYtelse,
                            aktør = barn.aktør,
                            kalkulertUtbetalingsbeløp = 500,
                        ),
                    )
                }
            every {
                beregningServiceMock.hentRelevanteTilkjentYtelserForPerson(
                    aktør = barn.aktør,
                    fagsakId = behandling.fagsak.id,
                )
            } returns listOf(tilkjentYtelse)

            // Act
            val finnesUtbetalingIAnnenFagsak =
                tilkjentYtelseValideringService.barnetrygdUtbetalesForBarnIAnnenFagsakIMåned(
                    behandling = behandling,
                    barna = listOf(barn),
                    måned = vurderingsmåned,
                )

            // Assert
            assertThat(finnesUtbetalingIAnnenFagsak).isTrue()
        }

        @Test
        fun `skal returnere true når bare ett av flere barn har utbetaling i vurderingsmåneden`() {
            // Arrange
            val barnUtenUtbetaling = lagPerson(type = PersonType.BARN)
            every {
                beregningServiceMock.hentRelevanteTilkjentYtelserForPerson(
                    aktør = barnUtenUtbetaling.aktør,
                    fagsakId = behandling.fagsak.id,
                )
            } returns emptyList()
            stubRelevantTilkjentYtelseForBarnet(fom = vurderingsmåned, tom = vurderingsmåned)

            // Act
            val finnesUtbetalingIAnnenFagsak =
                tilkjentYtelseValideringService.barnetrygdUtbetalesForBarnIAnnenFagsakIMåned(
                    behandling = behandling,
                    barna = listOf(barnUtenUtbetaling, barn),
                    måned = vurderingsmåned,
                )

            // Assert
            assertThat(finnesUtbetalingIAnnenFagsak).isTrue()
        }

        private fun stubRelevantTilkjentYtelseForBarnet(
            fom: YearMonth,
            tom: YearMonth,
            kalkulertUtbetalingsbeløp: Int = 1766,
            aktør: Aktør = barn.aktør,
            ytelseType: YtelseType = YtelseType.ORDINÆR_BARNETRYGD,
        ) {
            val annenBehandling = lagBehandling(fagsak = lagFagsak(id = behandling.fagsak.id + 1))
            val tilkjentYtelse =
                lagTilkjentYtelse(behandling = annenBehandling) { tilkjentYtelse ->
                    setOf(
                        lagAndelTilkjentYtelse(
                            fom = fom,
                            tom = tom,
                            ytelseType = ytelseType,
                            behandling = annenBehandling,
                            tilkjentYtelse = tilkjentYtelse,
                            aktør = aktør,
                            kalkulertUtbetalingsbeløp = kalkulertUtbetalingsbeløp,
                        ),
                    )
                }

            every {
                beregningServiceMock.hentRelevanteTilkjentYtelserForPerson(
                    aktør = barn.aktør,
                    fagsakId = behandling.fagsak.id,
                )
            } returns listOf(tilkjentYtelse)
        }
    }

    companion object {
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)
        val barn3MedUtbetalinger = lagPerson(type = PersonType.BARN)
    }
}
