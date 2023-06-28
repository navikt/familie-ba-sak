package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIUtbetalingUtil
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.YearMonth

class BehandlingsresultatStegTest {

    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()

    private val behandlingService: BehandlingService = mockk()

    private val simuleringService: SimuleringService = mockk()

    private val vedtakService: VedtakService = mockk()

    private val vedtaksperiodeService: VedtaksperiodeService = mockk()

    private val mockBehandlingsresultatService: BehandlingsresultatService = mockk()

    private val vilkårService: VilkårService = mockk()

    private val persongrunnlagService: PersongrunnlagService = mockk()

    private val beregningService: BeregningService = mockk()

    private lateinit var behandlingsresultatSteg: BehandlingsresultatSteg

    private lateinit var behandling: Behandling

    private val andelerTilkjentYtelseOgEndreteUtbetalingerService =
        mockk<AndelerTilkjentYtelseOgEndreteUtbetalingerService>()

    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository = mockk()

    @BeforeEach
    fun init() {
        behandlingsresultatSteg = BehandlingsresultatSteg(
            behandlingHentOgPersisterService,
            behandlingService,
            simuleringService,
            vedtakService,
            vedtaksperiodeService,
            mockBehandlingsresultatService,
            vilkårService,
            persongrunnlagService,
            beregningService,
            andelerTilkjentYtelseOgEndreteUtbetalingerService,
            andelTilkjentYtelseRepository,
        )

        behandling = lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
        )
    }

    @Test
    fun `skal kaste exception hvis behandlingsresultat er Avslått for en manuell migrering`() {
        every { mockBehandlingsresultatService.utledBehandlingsresultat(any()) } returns Behandlingsresultat.AVSLÅTT

        every {
            behandlingService.oppdaterBehandlingsresultat(
                any(),
                any(),
            )
        } returns behandling.copy(resultat = Behandlingsresultat.AVSLÅTT)

        val exception = assertThrows<RuntimeException> { behandlingsresultatSteg.utførStegOgAngiNeste(behandling, "") }
        assertEquals(
            "Du har fått behandlingsresultatet Avslått. " +
                "Dette er ikke støttet på migreringsbehandlinger. " +
                "Meld sak i Porten om du er uenig i resultatet.",
            exception.message,
        )
    }

    @Test
    fun `skal kaste exception hvis behandlingsresultat er Delvis Innvilget for en manuell migrering`() {
        every { mockBehandlingsresultatService.utledBehandlingsresultat(any()) } returns Behandlingsresultat.DELVIS_INNVILGET

        every {
            behandlingService.oppdaterBehandlingsresultat(
                any(),
                any(),
            )
        } returns behandling.copy(resultat = Behandlingsresultat.DELVIS_INNVILGET)

        val exception = assertThrows<RuntimeException> { behandlingsresultatSteg.utførStegOgAngiNeste(behandling, "") }
        assertEquals(
            "Du har fått behandlingsresultatet Delvis innvilget. " +
                "Dette er ikke støttet på migreringsbehandlinger. " +
                "Meld sak i Porten om du er uenig i resultatet.",
            exception.message,
        )
    }

    @Test
    fun `skal kaste exception hvis behandlingsresultat er Avslått,Endret og Opphørt for en manuell migrering`() {
        every { mockBehandlingsresultatService.utledBehandlingsresultat(any()) } returns Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT

        every {
            behandlingService.oppdaterBehandlingsresultat(
                any(),
                any(),
            )
        } returns behandling.copy(resultat = Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT)

        val exception = assertThrows<RuntimeException> { behandlingsresultatSteg.utførStegOgAngiNeste(behandling, "") }
        assertEquals(
            "Du har fått behandlingsresultatet Avslått, endret og opphørt. " +
                "Dette er ikke støttet på migreringsbehandlinger. " +
                "Meld sak i Porten om du er uenig i resultatet.",
            exception.message,
        )
    }

    @Test
    fun `skal kaste feil om det er endring etter migreringsdatoen til første behandling`() {
        val startdato = YearMonth.of(2023, 2)
        val endringTidslinje = "TTTFFFF".tilBoolskTidslinje(
            startdato,
        )

        assertThrows<FunksjonellFeil> {
            endringTidslinje.kastFeilVedEndringEtter(startdato, lagBehandling())
        }
    }

    @Test
    fun `skal ikke kaste feil om det ikke er endring etter migreringsdatoen til første behandling`() {
        val startdato = YearMonth.of(2023, 2)
        val treMånederEtterStartdato = startdato.plusMonths(3)

        val endringTidslinje = "TTTFFFF".tilBoolskTidslinje(
            startdato,
        )

        assertDoesNotThrow {
            endringTidslinje.kastFeilVedEndringEtter(treMånederEtterStartdato, lagBehandling())
        }
    }

    @Test
    fun `preValiderSteg - skal validere andeler ved satsendring og ikke kaste feil når endringene i andeler kun er relatert til endring i sats`() {
        val søker = lagPerson()
        val barn = lagPerson()
        val forrigeBehandling =
            lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING, årsak = BehandlingÅrsak.SØKNAD)

        val forrigeTilkjentYtelse = lagInitiellTilkjentYtelse(behandling = forrigeBehandling)
        forrigeTilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 2),
                    behandling = forrigeBehandling,
                    tilkjentYtelse = forrigeTilkjentYtelse,
                    beløp = SatsService.finnGjeldendeSatsForDato(
                        SatsType.ORBA,
                        YearMonth.of(2023, 1).førsteDagIInneværendeMåned(),
                    ),
                    aktør = barn.aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2023, 3),
                    tom = YearMonth.of(2033, 1),
                    behandling = forrigeBehandling,
                    tilkjentYtelse = forrigeTilkjentYtelse,
                    beløp = SatsService.finnGjeldendeSatsForDato(
                        SatsType.ORBA,
                        YearMonth.of(2023, 3).førsteDagIInneværendeMåned(),
                    ),
                    aktør = barn.aktør,
                ),
            ),
        )

        val behandling = lagBehandling(
            fagsak = forrigeBehandling.fagsak,
            behandlingType = BehandlingType.REVURDERING,
            årsak = BehandlingÅrsak.SATSENDRING,
        )
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling = behandling)
        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 2),
                    behandling = forrigeBehandling,
                    tilkjentYtelse = forrigeTilkjentYtelse,
                    beløp = SatsService.finnGjeldendeSatsForDato(
                        SatsType.ORBA,
                        YearMonth.of(2023, 1).førsteDagIInneværendeMåned(),
                    ),
                    aktør = barn.aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2023, 3),
                    tom = YearMonth.of(2023, 6),
                    behandling = forrigeBehandling,
                    tilkjentYtelse = forrigeTilkjentYtelse,
                    beløp = SatsService.finnGjeldendeSatsForDato(
                        SatsType.ORBA,
                        YearMonth.of(2023, 3).førsteDagIInneværendeMåned(),
                    ),
                    aktør = barn.aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2023, 7),
                    tom = YearMonth.of(2033, 1),
                    behandling = forrigeBehandling,
                    tilkjentYtelse = forrigeTilkjentYtelse,
                    beløp = SatsService.finnGjeldendeSatsForDato(
                        SatsType.ORBA,
                        YearMonth.of(2023, 7).førsteDagIInneværendeMåned(),
                    ),
                    aktør = barn.aktør,
                ),
            ),
        )
        lagMocksForPreValiderStegSatsendring(
            behandling = behandling,
            tilkjentYtelse = tilkjentYtelse,
            forrigeBehandling = forrigeBehandling,
            forrigeTilkjentYtelse = forrigeTilkjentYtelse,
            søker = søker,
            barn = listOf(barn),
        )

        assertThatCode { behandlingsresultatSteg.preValiderSteg(behandling) }.doesNotThrowAnyException()
    }

    @Test
    fun `preValiderSteg - skal validere andeler ved satsendring og kaste feil når endringene i andeler er relatert til noe annet enn endring i sats`() {
        val søker = lagPerson()
        val barn = lagPerson()
        val forrigeBehandling =
            lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING, årsak = BehandlingÅrsak.SØKNAD)
        val forrigeTilkjentYtelse = lagInitiellTilkjentYtelse(behandling = forrigeBehandling)
        forrigeTilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 2),
                    behandling = forrigeBehandling,
                    tilkjentYtelse = forrigeTilkjentYtelse,
                    beløp = SatsService.finnGjeldendeSatsForDato(
                        SatsType.ORBA,
                        YearMonth.of(2023, 1).førsteDagIInneværendeMåned(),
                    ),
                    aktør = barn.aktør,
                    prosent = BigDecimal(50),
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2023, 3),
                    tom = YearMonth.of(2033, 1),
                    behandling = forrigeBehandling,
                    tilkjentYtelse = forrigeTilkjentYtelse,
                    beløp = SatsService.finnGjeldendeSatsForDato(
                        SatsType.ORBA,
                        YearMonth.of(2023, 3).førsteDagIInneværendeMåned(),
                    ),
                    aktør = barn.aktør,
                    prosent = BigDecimal(50),
                ),
            ),
        )

        val behandling = lagBehandling(
            fagsak = forrigeBehandling.fagsak,
            behandlingType = BehandlingType.REVURDERING,
            årsak = BehandlingÅrsak.SATSENDRING,
        )
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling = behandling)
        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 2),
                    behandling = forrigeBehandling,
                    tilkjentYtelse = forrigeTilkjentYtelse,
                    beløp = SatsService.finnGjeldendeSatsForDato(
                        SatsType.ORBA,
                        YearMonth.of(2023, 1).førsteDagIInneværendeMåned(),
                    ),
                    aktør = barn.aktør,
                    prosent = BigDecimal(50),
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2023, 3),
                    tom = YearMonth.of(2023, 6),
                    behandling = forrigeBehandling,
                    tilkjentYtelse = forrigeTilkjentYtelse,
                    beløp = SatsService.finnGjeldendeSatsForDato(
                        SatsType.ORBA,
                        YearMonth.of(2023, 3).førsteDagIInneværendeMåned(),
                    ),
                    aktør = barn.aktør,
                    prosent = BigDecimal(100),
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2023, 7),
                    tom = YearMonth.of(2033, 1),
                    behandling = forrigeBehandling,
                    tilkjentYtelse = forrigeTilkjentYtelse,
                    beløp = SatsService.finnGjeldendeSatsForDato(
                        SatsType.ORBA,
                        YearMonth.of(2023, 7).førsteDagIInneværendeMåned(),
                    ),
                    aktør = barn.aktør,
                    prosent = BigDecimal(50),
                ),
            ),
        )
        lagMocksForPreValiderStegSatsendring(
            behandling = behandling,
            tilkjentYtelse = tilkjentYtelse,
            forrigeBehandling = forrigeBehandling,
            forrigeTilkjentYtelse = forrigeTilkjentYtelse,
            søker = søker,
            barn = listOf(barn),
        )

        assertThatThrownBy { behandlingsresultatSteg.preValiderSteg(behandling) }.isInstanceOf(Feil::class.java)
            .hasMessage("Satsendring kan ikke endre på prosenten til en andel")
    }

    private fun lagMocksForPreValiderStegSatsendring(
        behandling: Behandling,
        tilkjentYtelse: TilkjentYtelse,
        forrigeBehandling: Behandling,
        forrigeTilkjentYtelse: TilkjentYtelse,
        søker: Person,
        barn: List<Person>,
    ) {
        val personopplysningGrunnlag = mockk<PersonopplysningGrunnlag>()
        every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns mockk()
        every { vilkårService.hentVilkårsvurdering(behandling.id) } returns mockk()
        every { persongrunnlagService.hentBarna(any<Behandling>()) } returns emptyList()
        every { beregningService.hentTilkjentYtelseForBehandling(behandling.id) } returns tilkjentYtelse

        every { persongrunnlagService.hentAktivThrows(any()) } returns personopplysningGrunnlag
        every { personopplysningGrunnlag.søker } returns søker
        every { personopplysningGrunnlag.barna } returns barn
        every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns forrigeBehandling
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeTilkjentYtelse.andelerTilkjentYtelse.toList()
        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandling.id)
        } returns emptyList()

        every { beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomiTidslinje(behandling) } returns EndringIUtbetalingUtil.lagEndringIUtbetalingTidslinje(
            nåværendeAndeler = tilkjentYtelse.andelerTilkjentYtelse.toList(),
            forrigeAndeler = forrigeTilkjentYtelse.andelerTilkjentYtelse.toList(),
        )

        every {
            beregningService
                .hentAndelerFraForrigeIverksattebehandling(behandling)
        } returns emptyList()
    }

    fun String.tilBoolskTidslinje(startdato: YearMonth): Tidslinje<Boolean, Måned> {
        return tidslinje {
            this.mapIndexed { index, it ->
                Periode(
                    startdato.plusMonths(index.toLong()).tilTidspunkt(),
                    startdato.plusMonths(index.toLong()).tilTidspunkt(),
                    when (it) {
                        'T' -> true
                        'F' -> false
                        else -> throw Feil("Klarer ikke å konvertere \"$it\" til Boolean")
                    },
                )
            }
        }
    }
}
