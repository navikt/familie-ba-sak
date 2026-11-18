package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.TestClockProvider.Companion.lagClockProviderMedFastTidspunkt
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.felles.utbetalingsgenerator.Utbetalingsgenerator
import no.nav.familie.felles.utbetalingsgenerator.domain.Behandlingsinformasjon
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.time.YearMonth

class AvgrensetSimuleringGeneratorTest {
    private val forrigeBehandling = lagBehandling(id = 1)
    private val behandling = lagBehandling(id = 2, fagsak = forrigeBehandling.fagsak)
    private val barn = lagPerson().aktør
    private val søker = behandling.fagsak.aktør
    private val januar = YearMonth.of(2025, 1)
    private val mai = YearMonth.of(2025, 5)
    private val desember = YearMonth.of(2025, 12)

    @ParameterizedTest
    @CsvSource(
        "2025-12, 2025-05, 2025-05",
        "2025-12, 2025-06, 2025-06",
        "2025-12, 2025-07, 2025-06",
        "2025-12, 2025-12, 2025-06",
        "2025-12, 2026-01, 2025-06",
        "2026-02, 2026-03, 2025-08",
    )
    fun `skal simulere fra tidligste av første endring og seks måneder siden`(
        inneværendeMåned: String,
        førsteEndring: String,
        forventetStart: String,
    ) {
        // Arrange
        val dagensMåned = YearMonth.parse(inneværendeMåned)
        val endringsmåned = YearMonth.parse(førsteEndring)
        val simulerFra = YearMonth.parse(forventetStart)
        val fom = januar.minusYears(1)
        val tom = dagensMåned.plusMonths(6)
        val forrige = listOf(andel(barn, fom, tom, 2000), andel(søker, fom, tom, 1000))
        val nye =
            listOf(
                andel(barn, fom, tom, 2000),
                andel(søker, fom, endringsmåned.minusMonths(1), 1000),
                andel(søker, endringsmåned, tom, 1500),
            )

        // Act
        val oppdrag = lagOppdrag(forrige, nye, dagensDato = dagensMåned.atEndOfMonth())

        // Assert
        assertThat(oppdrag.nyePerioder().minOf { it.vedtakdatoFom }).isEqualTo(simulerFra.atDay(1))
        assertThat(oppdrag.nyePerioder().map { it.vedtakdatoFom }).containsOnly(simulerFra.atDay(1), endringsmåned.atDay(1))
        assertThat(oppdrag.utbetalingsperiode.mapNotNull { it.opphør?.opphørDatoFom }).containsExactlyInAnyOrder(simulerFra.atDay(1), simulerFra.atDay(1))
        assertThat(oppdrag.beløpIMåned(simulerFra.minusMonths(1))).isZero()
        assertThat(oppdrag.beløpIMåned(endringsmåned)).isEqualTo(3500)
        if (simulerFra < endringsmåned) {
            assertThat(oppdrag.beløpIMåned(simulerFra)).isEqualTo(3000)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `skal simulere barn og søker fra første endring også ved eksisterende periodegrense`(harPeriodegrense: Boolean) {
        // Arrange
        val barnAndeler =
            if (harPeriodegrense) {
                listOf(
                    andel(barn, januar, mai.minusMonths(1), 2000),
                    andel(barn, mai, desember, 2000),
                )
            } else {
                listOf(andel(barn, januar, desember, 2000))
            }
        val forrige = barnAndeler + andel(søker, januar, desember, 1000)
        val nye =
            barnAndeler +
                andel(søker, januar, mai.minusMonths(1), 1000) +
                andel(søker, mai, desember, 1500)
        val opprinneligeForrige = forrige.map { it.tilAndelDataLongId() }
        val opprinneligeNye = nye.map { it.tilAndelDataLongId() }

        // Act
        val oppdrag = lagOppdrag(forrige, nye)

        // Assert
        assertThat(oppdrag.nyePerioder()).hasSize(2)
        assertThat(oppdrag.nyePerioder().map { it.vedtakdatoFom }).containsOnly(mai.atDay(1))
        assertThat(oppdrag.beløpIMåned(mai)).isEqualTo(3500)
        assertThat(oppdrag.utbetalingsperiode.mapNotNull { it.opphør?.opphørDatoFom }).containsExactlyInAnyOrder(mai.atDay(1), mai.atDay(1))
        assertThat(forrige.map { it.tilAndelDataLongId() }).isEqualTo(opprinneligeForrige)
        assertThat(nye.map { it.tilAndelDataLongId() }).isEqualTo(opprinneligeNye)
    }

    @Test
    fun `skal finne tidligste endring på tvers av personer og ytelsetyper`() {
        // Arrange
        val juli = mai.plusMonths(2)
        val forrige =
            listOf(
                andel(barn, januar, desember, 2000),
                andel(søker, januar, desember, 1000),
            )
        val nye =
            listOf(
                andel(barn, januar, juli.minusMonths(1), 2000),
                andel(barn, juli, desember, 2500),
                andel(søker, januar, desember, 1000),
                andel(søker, mai, desember, 500, YtelseType.SMÅBARNSTILLEGG),
            )

        // Act
        val oppdrag = lagOppdrag(forrige, nye)

        // Assert
        assertThat(oppdrag.nyePerioder().map { it.vedtakdatoFom }).containsOnly(mai.atDay(1), juli.atDay(1))
        assertThat(oppdrag.beløpIMåned(mai)).isEqualTo(3500)
        assertThat(oppdrag.beløpIMåned(juli)).isEqualTo(4000)
        assertThat(oppdrag.nyePerioder().map { it.klassifisering }).contains(YtelsetypeBA.SMÅBARNSTILLEGG.klassifisering)
    }

    @Test
    fun `skal ignorere endret periodisering uten beløpsendring før første reelle endring`() {
        // Arrange
        val mars = januar.plusMonths(2)
        val forrige = listOf(andel(barn, januar, desember, 2000), andel(søker, januar, desember, 1000))
        val nye =
            listOf(
                andel(barn, januar, mars.minusMonths(1), 2000),
                andel(barn, mars, desember, 2000),
                andel(søker, januar, mai.minusMonths(1), 1000),
                andel(søker, mai, desember, 1500),
            )

        // Act
        val oppdrag = lagOppdrag(forrige, nye)

        // Assert
        assertThat(oppdrag.nyePerioder()).hasSize(2)
        assertThat(oppdrag.nyePerioder().map { it.vedtakdatoFom }).containsOnly(mai.atDay(1))
        assertThat(oppdrag.beløpIMåned(mai)).isEqualTo(3500)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `skal ta med opphør og uendrede kjeder når en utbetaling forsvinner`(harNullandel: Boolean) {
        // Arrange
        val forrige =
            listOf(
                andel(barn, januar, desember, 2000),
                andel(søker, januar, desember, 1000),
            )
        val nye =
            listOf(
                andel(barn, januar, desember, 2000),
                andel(søker, januar, mai.minusMonths(1), 1000),
            ) + if (harNullandel) listOf(andel(søker, mai, desember, 0)) else emptyList()

        // Act
        val oppdrag = lagOppdrag(forrige, nye)

        // Assert
        assertThat(oppdrag.nyePerioder()).hasSize(1)
        assertThat(oppdrag.beløpIMåned(mai)).isEqualTo(2000)
        assertThat(oppdrag.utbetalingsperiode.mapNotNull { it.opphør?.opphørDatoFom }).containsExactlyInAnyOrder(mai.atDay(1), mai.atDay(1))
    }

    @Test
    fun `skal opphøre alle kjeder fra første bortfall når alle andeler fjernes`() {
        // Arrange
        val forrige =
            listOf(
                andel(barn, januar, desember, 2000),
                andel(søker, januar, desember, 1000),
            )

        // Act
        val oppdrag = lagOppdrag(forrige, emptyList())

        // Assert
        assertThat(oppdrag.nyePerioder()).isEmpty()
        assertThat(oppdrag.utbetalingsperiode).hasSize(2)
        assertThat(oppdrag.utbetalingsperiode.mapNotNull { it.opphør?.opphørDatoFom }).containsOnly(januar.atDay(1))
    }

    @Test
    fun `skal bevare hull og utelate kjeder som er avsluttet før første endring`() {
        // Arrange
        val mars = mai.minusMonths(2)
        val juli = mai.plusMonths(2)
        val avsluttetBarn = lagPerson().aktør
        val nyttBarn = lagPerson().aktør
        val uendrede =
            listOf(
                andel(barn, januar, mars, 2000),
                andel(barn, juli, desember, 2200),
                andel(avsluttetBarn, januar, mars, 1800),
            )
        val forrige = uendrede + andel(søker, januar, desember, 1000)
        val nye =
            uendrede +
                andel(søker, januar, mai.minusMonths(1), 1000) +
                andel(søker, mai, desember, 1500) +
                andel(nyttBarn, juli, desember, 2300)

        // Act
        val oppdrag = lagOppdrag(forrige, nye)

        // Assert
        assertThat(oppdrag.nyePerioder()).hasSize(3)
        assertThat(oppdrag.beløpIMåned(mai)).isEqualTo(1500)
        assertThat(oppdrag.beløpIMåned(juli)).isEqualTo(6000)
        assertThat(oppdrag.utbetalingsperiode.mapNotNull { it.opphør?.opphørDatoFom }).containsExactlyInAnyOrder(mai.atDay(1), juli.atDay(1))
        assertThat(oppdrag.nyePerioder().map { it.sats.toInt() }).doesNotContain(1800)
    }

    @Test
    fun `skal bruke første nye utbetaling som endringstidspunkt ved forlengelse`() {
        // Arrange
        val forrige =
            listOf(
                andel(barn, januar, desember, 2000),
                andel(søker, januar, mai.minusMonths(1), 1000),
            )
        val nye =
            listOf(
                andel(barn, januar, desember, 2000),
                andel(søker, januar, desember, 1000),
            )

        // Act
        val oppdrag = lagOppdrag(forrige, nye)

        // Assert
        assertThat(oppdrag.nyePerioder()).hasSize(2)
        assertThat(oppdrag.nyePerioder().map { it.vedtakdatoFom }).containsOnly(mai.atDay(1))
        assertThat(oppdrag.beløpIMåned(mai)).isEqualTo(3000)
    }

    @Test
    fun `skal ikke opprette kunstige endringer for uendrede utbetalinger eller nye nullandeler`() {
        // Arrange
        val forrige = listOf(andel(barn, januar, desember, 2000))
        val nye = forrige + andel(søker, januar.minusMonths(1), desember, 0)

        // Act
        val oppdrag = lagOppdrag(forrige, nye)

        // Assert
        assertThat(oppdrag.utbetalingsperiode).isEmpty()
    }

    @Test
    fun `skal redusere ti års månedlige perioder til seks måneders historikk og inneværende måned for alle personer`() {
        // Arrange
        val personer = listOf(barn, søker, lagPerson().aktør)
        val førsteMåned = desember.minusMonths(119)
        val forrige =
            personer.flatMapIndexed { personIndeks, aktør ->
                (0L..119L).map { måned ->
                    andel(aktør, førsteMåned.plusMonths(måned), førsteMåned.plusMonths(måned), 1000 + måned.toInt() + personIndeks)
                }
            }
        val nye =
            forrige.map {
                if (it.aktør == søker && it.stønadFom == desember) {
                    it.copy(kalkulertUtbetalingsbeløp = it.kalkulertUtbetalingsbeløp + 100)
                } else {
                    it
                }
            }

        // Act
        val avgrenset = lagOppdrag(forrige, nye)
        val fullHistorikk = lagOppdrag(forrige, nye, togglePå = false)

        // Assert
        assertThat(avgrenset.utbetalingsperiode).hasSize(24)
        assertThat(avgrenset.nyePerioder()).hasSize(21)
        assertThat(avgrenset.nyePerioder().minOf { it.vedtakdatoFom }).isEqualTo(desember.minusMonths(6).atDay(1))
        assertThat(avgrenset.utbetalingsperiode.mapNotNull { it.opphør?.opphørDatoFom }).containsOnly(desember.minusMonths(6).atDay(1))
        assertThat(fullHistorikk.utbetalingsperiode).hasSize(363)
        (0L..6L).forEach { måned ->
            assertThat(avgrenset.beløpIMåned(desember.minusMonths(måned))).isEqualTo(fullHistorikk.beløpIMåned(desember.minusMonths(måned)))
        }
    }

    @Test
    fun `skal fortsatt simulere full historikk når toggle er av`() {
        // Arrange
        val forrige = listOf(andel(barn, januar, desember, 2000), andel(søker, januar, desember, 1000))
        val nye =
            listOf(
                andel(barn, januar, desember, 2000),
                andel(søker, januar, mai.minusMonths(1), 1000),
                andel(søker, mai, desember, 1500),
            )

        // Act
        val oppdrag = lagOppdrag(forrige, nye, togglePå = false)

        // Assert
        assertThat(oppdrag.nyePerioder()).hasSize(3)
        assertThat(oppdrag.beløpIMåned(januar)).isEqualTo(3000)
        assertThat(oppdrag.beløpIMåned(mai)).isEqualTo(3500)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `skal ikke endre iverksettelse uansett toggle`(togglePå: Boolean) {
        // Arrange
        val forrige = listOf(andel(barn, januar, desember, 2000), andel(søker, januar, desember, 1000))
        val nye =
            listOf(
                andel(barn, januar, desember, 2000),
                andel(søker, januar, mai.minusMonths(1), 1000),
                andel(søker, mai, desember, 1500),
            )

        // Act
        val oppdrag = lagOppdrag(forrige, nye, erSimulering = false, togglePå = togglePå)

        // Assert
        assertThat(oppdrag.nyePerioder()).hasSize(1)
        assertThat(oppdrag.beløpIMåned(mai)).isEqualTo(1500)
        assertThat(oppdrag.utbetalingsperiode.mapNotNull { it.opphør }).isEmpty()
    }

    @Test
    fun `skal bevare eksplisitt opphør ved endret migreringsdato`() {
        // Arrange
        val mars = januar.plusMonths(2)
        val forrige = listOf(andel(barn, mars, desember, 2000), andel(søker, mars, desember, 1000))
        val nye =
            listOf(
                andel(barn, mars, desember, 2000),
                andel(søker, mars, mai.minusMonths(1), 1000),
                andel(søker, mai, desember, 1500),
            )

        // Act
        val oppdrag = lagOppdrag(forrige, nye, opphørAlleKjederFra = januar)

        // Assert
        assertThat(oppdrag.nyePerioder()).hasSize(3)
        assertThat(oppdrag.utbetalingsperiode.mapNotNull { it.opphør?.opphørDatoFom }).containsOnly(januar.atDay(1))
        assertThat(oppdrag.beløpIMåned(mars)).isEqualTo(3000)
        assertThat(oppdrag.beløpIMåned(mai)).isEqualTo(3500)
    }

    private fun andel(
        aktør: Aktør,
        fom: YearMonth,
        tom: YearMonth,
        beløp: Int,
        type: YtelseType = if (aktør == søker) YtelseType.UTVIDET_BARNETRYGD else YtelseType.ORDINÆR_BARNETRYGD,
    ): AndelTilkjentYtelse =
        lagAndelTilkjentYtelse(
            behandling = behandling,
            aktør = aktør,
            fom = fom,
            tom = tom,
            beløp = beløp,
            ytelseType = type,
            kildeBehandlingId = null,
        )

    private fun lagOppdrag(
        forrigeAndeler: List<AndelTilkjentYtelse>,
        nyeAndeler: List<AndelTilkjentYtelse>,
        erSimulering: Boolean = true,
        togglePå: Boolean = true,
        opphørAlleKjederFra: YearMonth? = null,
        dagensDato: LocalDate = desember.atDay(1),
    ): Utbetalingsoppdrag {
        val informasjon =
            Behandlingsinformasjon(
                saksbehandlerId = "test",
                behandlingId = forrigeBehandling.id.toString(),
                eksternBehandlingId = forrigeBehandling.id,
                eksternFagsakId = behandling.fagsak.id,
                fagsystem = FagsystemBA.BARNETRYGD,
                personIdent = søker.aktivFødselsnummer(),
                vedtaksdato = LocalDate.of(2025, 12, 1),
                opphørAlleKjederFra = null,
            )
        val andelerMedId = forrigeAndeler.mapIndexed { indeks, andel -> andel.copy(id = indeks + 1L) }
        val iverksatteAndeler =
            Utbetalingsgenerator()
                .lagUtbetalingsoppdrag(
                    behandlingsinformasjon = informasjon,
                    forrigeAndeler = emptyList(),
                    nyeAndeler = andelerMedId.map { it.tilAndelDataLongId() },
                    sisteAndelPerKjede = emptyMap(),
                ).andeler
                .associateBy { it.id }
        val forrigeTilkjentYtelse =
            lagTilkjentYtelse(behandling = forrigeBehandling, lagAndelerTilkjentYtelse = {
                andelerMedId
                    .map { andel ->
                        val iverksatt = iverksatteAndeler[andel.id]
                        andel.copy(
                            behandlingId = forrigeBehandling.id,
                            periodeOffset = iverksatt?.periodeId,
                            forrigePeriodeOffset = iverksatt?.forrigePeriodeId,
                            kildeBehandlingId = forrigeBehandling.id,
                        )
                    }.toSet()
            })
        val tilkjentYtelse =
            lagTilkjentYtelse(behandling = behandling, lagAndelerTilkjentYtelse = {
                nyeAndeler.mapIndexed { indeks, andel -> andel.copy(id = forrigeAndeler.size + indeks + 1L) }.toSet()
            })
        val opprinneligeForrige = forrigeTilkjentYtelse.tilAndelData()
        val opprinneligeNye = tilkjentYtelse.tilAndelData()
        val sisteAndeler =
            forrigeTilkjentYtelse.andelerTilkjentYtelse
                .filter { it.periodeOffset != null }
                .groupBy { IdentOgType(it.aktør.aktivFødselsnummer(), it.type.tilYtelseType()) }
                .values
                .map { kjede -> kjede.maxBy { requireNotNull(it.periodeOffset) } }
        val behandlingsinformasjonUtleder =
            mockk<BehandlingsinformasjonUtleder> {
                every { utled(any(), any(), any(), any(), any()) } answers {
                    informasjon.copy(
                        behandlingId = behandling.id.toString(),
                        eksternBehandlingId = behandling.id,
                        opphørAlleKjederFra = opphørAlleKjederFra,
                        opphørKjederFraFørsteUtbetaling = lastArg<Boolean>() && opphørAlleKjederFra == null,
                    )
                }
            }
        val generator =
            UtbetalingsoppdragGenerator(
                utbetalingsgenerator = Utbetalingsgenerator(),
                klassifiseringKorrigerer = mockk<KlassifiseringKorrigerer> { every { korrigerKlassifiseringVedBehov(any(), any()) } answers { firstArg() } },
                behandlingsinformasjonUtleder = behandlingsinformasjonUtleder,
                andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository> { every { hentSisteAndelPerIdentOgType(any()) } returns sisteAndeler },
                behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService> { every { hentForrigeBehandlingSomErIverksatt(any()) } returns forrigeBehandling },
                tilkjentYtelseRepository = mockk<TilkjentYtelseRepository> { every { findByBehandlingAndHasUtbetalingsoppdrag(any()) } returns forrigeTilkjentYtelse },
                featureToggleService = mockk<FeatureToggleService> { every { isEnabled(FeatureToggle.SIMULER_KUN_ENDREDE_PERIODER, any<Long>()) } returns togglePå },
                avgrensetSimuleringGenerator =
                    AvgrensetSimuleringGenerator(
                        utbetalingsgenerator = Utbetalingsgenerator(),
                        behandlingsinformasjonUtleder = behandlingsinformasjonUtleder,
                        clockProvider = lagClockProviderMedFastTidspunkt(dagensDato),
                    ),
            )
        val oppdrag =
            generator
                .lagUtbetalingsoppdrag("test", lagVedtak(behandling = behandling), tilkjentYtelse, erSimulering)
                .utbetalingsoppdrag

        assertThat(forrigeTilkjentYtelse.tilAndelData()).isEqualTo(opprinneligeForrige)
        assertThat(tilkjentYtelse.tilAndelData()).isEqualTo(opprinneligeNye)
        assertThat(oppdrag.nyePerioder().mapNotNull { it.forrigePeriodeId }).allMatch { periodeId ->
            periodeId in sisteAndeler.map { it.periodeOffset } || periodeId in oppdrag.nyePerioder().map { it.periodeId }
        }
        return oppdrag
    }

    private fun Utbetalingsoppdrag.nyePerioder(): List<Utbetalingsperiode> = utbetalingsperiode.filter { it.opphør == null }

    private fun Utbetalingsoppdrag.beløpIMåned(måned: YearMonth): Int =
        nyePerioder()
            .filter { it.vedtakdatoFom <= måned.atDay(1) && it.vedtakdatoTom >= måned.atEndOfMonth() }
            .sumOf { it.sats.toInt() }
}
