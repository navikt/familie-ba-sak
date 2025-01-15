package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagTilkjentYtelse
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.felles.utbetalingsgenerator.Utbetalingsgenerator
import no.nav.familie.felles.utbetalingsgenerator.domain.Behandlingsinformasjon
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

class UtbetalingsoppdragGeneratorTest {
    private val klassifiseringKorrigerer: KlassifiseringKorrigerer = mockk()
    private val unleashNextMedContextService: UnleashNextMedContextService = mockk()
    private val behandlingsinformasjonUtleder: BehandlingsinformasjonUtleder = mockk()
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository = mockk()
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val tilkjentYtelseRepository: TilkjentYtelseRepository = mockk()
    private val andelDataForOppdaterUtvidetKlassekodeBehandlingUtleder: AndelDataForOppdaterUtvidetKlassekodeBehandlingUtleder = mockk()
    private val utbetalingsoppdragGenerator =
        UtbetalingsoppdragGenerator(
            utbetalingsgenerator = Utbetalingsgenerator(),
            klassifiseringKorrigerer = klassifiseringKorrigerer,
            unleashNextMedContextService = unleashNextMedContextService,
            behandlingsinformasjonUtleder = behandlingsinformasjonUtleder,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            tilkjentYtelseRepository = tilkjentYtelseRepository,
            andelDataForOppdaterUtvidetKlassekodeBehandlingUtleder = andelDataForOppdaterUtvidetKlassekodeBehandlingUtleder,
        )

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `skal lage utbetalingsoppdrag for førstegangsbehandling`(erSimulering: Boolean) {
        // Arrange
        val saksbehandlerId = "123abc"
        val behandling = lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)
        val vedtak = lagVedtak(behandling = behandling)
        val barn = lagPerson()
        val andelerTilkjentYtelse =
            setOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    fom = LocalDate.now().toYearMonth(),
                    tom = LocalDate.now().toYearMonth(),
                    person = barn,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    kildeBehandlingId = null,
                    kalkulertUtbetalingsbeløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
                ),
            )
        val tilkjentYtelse =
            lagTilkjentYtelse(
                behandling = behandling,
                lagAndelerTilkjentYtelse = { andelerTilkjentYtelse },
            )

        every {
            behandlingHentOgPersisterService
                .hentForrigeBehandlingSomErIverksatt(behandling = behandling)
        } returns null
        every {
            andelTilkjentYtelseRepository
                .hentSisteAndelPerIdentOgType(fagsakId = behandling.fagsak.id)
        } returns emptyList()
        every {
            behandlingsinformasjonUtleder.utled(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns
            Behandlingsinformasjon(
                saksbehandlerId = saksbehandlerId,
                behandlingId = behandling.id.toString(),
                eksternBehandlingId = behandling.id,
                eksternFagsakId = behandling.fagsak.id,
                fagsystem = FagsystemBA.BARNETRYGD,
                personIdent = barn.aktør.aktivFødselsnummer(),
                vedtaksdato = LocalDate.now(),
                opphørAlleKjederFra = null,
            )

        every {
            unleashNextMedContextService.isEnabled(
                toggleId = FeatureToggleConfig.SKAL_BRUKE_NY_KLASSEKODE_FOR_UTVIDET_BARNETRYGD,
                behandlingId = behandling.id,
            )
        } returns true

        every {
            unleashNextMedContextService.isEnabled(
                toggleId = FeatureToggleConfig.BRUK_OVERSTYRING_AV_FOM_SISTE_ANDEL_UTVIDET,
            )
        } returns true

        every {
            klassifiseringKorrigerer.korrigerKlassifiseringVedBehov(
                beregnetUtbetalingsoppdrag = any(),
                behandling = vedtak.behandling,
            )
        } answers {
            firstArg()
        }

        every { tilkjentYtelseRepository.findByOppdatertUtvidetBarnetrygdKlassekodeIUtbetalingsoppdrag(any()) } returns emptyList()

        // Act
        val beregnetUtbetalingsoppdragLongId =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                tilkjentYtelse = tilkjentYtelse,
                erSimulering = erSimulering,
            )

        // Assert
        verify(exactly = 1) {
            behandlingsinformasjonUtleder.utled(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
        verify(exactly = 1) {
            klassifiseringKorrigerer.korrigerKlassifiseringVedBehov(any(), any())
        }

        assertThat(beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag.saksbehandlerId).isEqualTo(saksbehandlerId)
        assertThat(beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag.kodeEndring).isEqualTo(Utbetalingsoppdrag.KodeEndring.NY)
        assertThat(beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag.saksnummer).isEqualTo(behandling.fagsak.id.toString())
        assertThat(beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag.utbetalingsperiode).hasSize(1)
        assertThat(beregnetUtbetalingsoppdragLongId.andeler).hasSize(1)
        assertThat(
            beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag.utbetalingsperiode
                .single()
                .vedtakdatoFom,
        ).isEqualTo(LocalDate.now().førsteDagIInneværendeMåned())
        assertThat(
            beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag.utbetalingsperiode
                .single()
                .vedtakdatoTom,
        ).isEqualTo(LocalDate.now().sisteDagIMåned())
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `skal lage utbetalingsoppdrag for revurdering`(erSimulering: Boolean) {
        // Arrange
        val saksbehandlerId = "123abc"
        val barn = lagPerson()
        val forrigeBehandling = lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)
        val forrigeTilkjenteYtelse =
            lagTilkjentYtelse(behandling = forrigeBehandling, lagAndelerTilkjentYtelse = {
                setOf(
                    lagAndelTilkjentYtelse(
                        id = 1,
                        behandling = forrigeBehandling,
                        fom = LocalDate.now().toYearMonth(),
                        tom = LocalDate.now().toYearMonth(),
                        periodeIdOffset = 0,
                        forrigeperiodeIdOffset = null,
                        person = barn,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kildeBehandlingId = null,
                        kalkulertUtbetalingsbeløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
                    ),
                )
            })
        val behandling = lagBehandling(behandlingType = BehandlingType.REVURDERING)
        val vedtak = lagVedtak(behandling = behandling)
        val andelerTilkjentYtelse =
            setOf(
                lagAndelTilkjentYtelse(
                    id = 2,
                    behandling = behandling,
                    fom = LocalDate.now().toYearMonth(),
                    tom = LocalDate.now().plusMonths(2).toYearMonth(),
                    person = barn,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    kildeBehandlingId = null,
                    kalkulertUtbetalingsbeløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
                ),
            )
        val tilkjentYtelse =
            lagTilkjentYtelse(
                behandling = behandling,
                lagAndelerTilkjentYtelse = { andelerTilkjentYtelse },
            )

        every {
            behandlingHentOgPersisterService
                .hentForrigeBehandlingSomErIverksatt(behandling = behandling)
        } returns forrigeBehandling

        every { tilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(behandlingId = forrigeBehandling.id) } returns forrigeTilkjenteYtelse
        every {
            andelTilkjentYtelseRepository
                .hentSisteAndelPerIdentOgType(fagsakId = behandling.fagsak.id)
        } returns forrigeTilkjenteYtelse.andelerTilkjentYtelse.toList()
        every {
            behandlingsinformasjonUtleder.utled(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns
            Behandlingsinformasjon(
                saksbehandlerId = saksbehandlerId,
                behandlingId = behandling.id.toString(),
                eksternBehandlingId = behandling.id,
                eksternFagsakId = behandling.fagsak.id,
                fagsystem = FagsystemBA.BARNETRYGD,
                personIdent = barn.aktør.aktivFødselsnummer(),
                vedtaksdato = LocalDate.now(),
                opphørAlleKjederFra = null,
            )

        every {
            unleashNextMedContextService.isEnabled(
                toggleId = FeatureToggleConfig.SKAL_BRUKE_NY_KLASSEKODE_FOR_UTVIDET_BARNETRYGD,
                behandlingId = any(),
            )
        } returns true

        every {
            unleashNextMedContextService.isEnabled(
                toggleId = FeatureToggleConfig.BRUK_OVERSTYRING_AV_FOM_SISTE_ANDEL_UTVIDET,
            )
        } returns true

        every {
            klassifiseringKorrigerer.korrigerKlassifiseringVedBehov(
                beregnetUtbetalingsoppdrag = any(),
                behandling = vedtak.behandling,
            )
        } answers {
            firstArg()
        }

        every { tilkjentYtelseRepository.findByOppdatertUtvidetBarnetrygdKlassekodeIUtbetalingsoppdrag(any()) } returns emptyList()

        // Act
        val beregnetUtbetalingsoppdragLongId =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                tilkjentYtelse = tilkjentYtelse,
                erSimulering = erSimulering,
            )

        // Assert
        verify(exactly = 1) {
            behandlingsinformasjonUtleder.utled(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
        verify(exactly = 1) {
            klassifiseringKorrigerer.korrigerKlassifiseringVedBehov(any(), any())
        }

        assertThat(beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag.saksbehandlerId).isEqualTo(saksbehandlerId)
        assertThat(beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag.kodeEndring).isEqualTo(Utbetalingsoppdrag.KodeEndring.ENDR)
        assertThat(beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag.saksnummer).isEqualTo(behandling.fagsak.id.toString())
        assertThat(beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag.utbetalingsperiode).hasSize(1)
        assertThat(beregnetUtbetalingsoppdragLongId.andeler).hasSize(1)
        assertThat(
            beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag.utbetalingsperiode
                .single()
                .vedtakdatoFom,
        ).isEqualTo(LocalDate.now().førsteDagIInneværendeMåned())
        assertThat(
            beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag.utbetalingsperiode
                .single()
                .vedtakdatoTom,
        ).isEqualTo(LocalDate.now().plusMonths(2).sisteDagIMåned())
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `skal lage utbetalingsoppdrag for behandling med årsak OPPDATER_UTVIDET_KLASSEKODE`(erSimulering: Boolean) {
        // Arrange
        val saksbehandlerId = "123abc"
        val barn = lagPerson()
        val forrigeBehandling = lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)
        val forrigeTilkjenteYtelse =
            lagTilkjentYtelse(behandling = forrigeBehandling, lagAndelerTilkjentYtelse = {
                setOf(
                    lagAndelTilkjentYtelse(
                        id = 1,
                        behandling = forrigeBehandling,
                        fom = LocalDate.now().toYearMonth(),
                        tom = LocalDate.now().toYearMonth(),
                        periodeIdOffset = 0,
                        forrigeperiodeIdOffset = null,
                        person = barn,
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                        kildeBehandlingId = null,
                        kalkulertUtbetalingsbeløp = SatsService.finnSisteSatsFor(SatsType.UTVIDET_BARNETRYGD).beløp,
                    ),
                )
            })
        val behandling = lagBehandling(
            behandlingType = BehandlingType.REVURDERING,
            årsak = BehandlingÅrsak.OPPDATER_UTVIDET_KLASSEKODE
        )
        val vedtak = lagVedtak(behandling = behandling)
        val andelTilkjentYtelse =
            lagAndelTilkjentYtelse(
                id = 2,
                behandling = behandling,
                fom = LocalDate.now().toYearMonth(),
                tom = LocalDate.now().toYearMonth(),
                person = barn,
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                kildeBehandlingId = null,
                kalkulertUtbetalingsbeløp = SatsService.finnSisteSatsFor(SatsType.UTVIDET_BARNETRYGD).beløp,
            )
        val andelerTilkjentYtelse =
            setOf(
                andelTilkjentYtelse,
            )
        val tilkjentYtelse =
            lagTilkjentYtelse(
                behandling = behandling,
                lagAndelerTilkjentYtelse = { andelerTilkjentYtelse },
            )

        every {
            behandlingHentOgPersisterService
                .hentForrigeBehandlingSomErIverksatt(behandling = behandling)
        } returns forrigeBehandling

        every { tilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(behandlingId = forrigeBehandling.id) } returns forrigeTilkjenteYtelse
        every {
            andelTilkjentYtelseRepository
                .hentSisteAndelPerIdentOgType(fagsakId = behandling.fagsak.id)
        } returns forrigeTilkjenteYtelse.andelerTilkjentYtelse.toList()
        every {
            behandlingsinformasjonUtleder.utled(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns
            Behandlingsinformasjon(
                saksbehandlerId = saksbehandlerId,
                behandlingId = behandling.id.toString(),
                eksternBehandlingId = behandling.id,
                eksternFagsakId = behandling.fagsak.id,
                fagsystem = FagsystemBA.BARNETRYGD,
                personIdent = barn.aktør.aktivFødselsnummer(),
                vedtaksdato = LocalDate.now(),
                opphørAlleKjederFra = null,
            )

        every {
            unleashNextMedContextService.isEnabled(
                toggleId = FeatureToggleConfig.SKAL_BRUKE_NY_KLASSEKODE_FOR_UTVIDET_BARNETRYGD,
                behandlingId = any(),
            )
        } returns true

        every {
            unleashNextMedContextService.isEnabled(
                toggleId = FeatureToggleConfig.BRUK_OVERSTYRING_AV_FOM_SISTE_ANDEL_UTVIDET,
            )
        } returns true

        every {
            klassifiseringKorrigerer.korrigerKlassifiseringVedBehov(
                beregnetUtbetalingsoppdrag = any(),
                behandling = vedtak.behandling,
            )
        } answers {
            firstArg()
        }

        every { andelDataForOppdaterUtvidetKlassekodeBehandlingUtleder.finnForrigeAndelerForOppdaterUtvidetKlassekodeBehandling(any(), any()) } returns emptyList()

        every { tilkjentYtelseRepository.findByOppdatertUtvidetBarnetrygdKlassekodeIUtbetalingsoppdrag(any()) } returns emptyList()
        // Act
        val beregnetUtbetalingsoppdragLongId =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                tilkjentYtelse = tilkjentYtelse,
                erSimulering = erSimulering,
            )

        // Assert
        verify(exactly = 1) {
            behandlingsinformasjonUtleder.utled(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
        verify(exactly = 1) {
            klassifiseringKorrigerer.korrigerKlassifiseringVedBehov(any(), any())
        }

        assertThat(beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag.saksbehandlerId).isEqualTo(saksbehandlerId)
        assertThat(beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag.kodeEndring).isEqualTo(Utbetalingsoppdrag.KodeEndring.ENDR)
        assertThat(beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag.saksnummer).isEqualTo(behandling.fagsak.id.toString())
        assertThat(beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag.utbetalingsperiode).hasSize(1)
        assertThat(beregnetUtbetalingsoppdragLongId.andeler).hasSize(1)
        assertThat(
            beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag.utbetalingsperiode
                .single()
                .vedtakdatoFom,
        ).isEqualTo(LocalDate.now().førsteDagIInneværendeMåned())
        assertThat(
            beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag.utbetalingsperiode
                .single()
                .vedtakdatoTom,
        ).isEqualTo(LocalDate.now().sisteDagIMåned())
    }
}
