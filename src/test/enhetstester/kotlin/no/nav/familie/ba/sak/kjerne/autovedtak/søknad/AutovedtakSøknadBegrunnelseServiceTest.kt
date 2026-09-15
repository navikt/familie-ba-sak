package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.datagenerator.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class AutovedtakSøknadBegrunnelseServiceTest {
    private val beregningService = mockk<BeregningService>()
    private val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    private val vedtakService = mockk<VedtakService>()
    private val vedtaksperiodeHentOgPersisterService = mockk<VedtaksperiodeHentOgPersisterService>()

    private val autovedtakSøknadBegrunnelseService =
        AutovedtakSøknadBegrunnelseService(
            beregningService = beregningService,
            vedtaksperiodeService = vedtaksperiodeService,
            vedtakService = vedtakService,
            vedtaksperiodeHentOgPersisterService = vedtaksperiodeHentOgPersisterService,
        )

    private val behandling = lagBehandling(årsak = BehandlingÅrsak.AUTOMATISK_BEHANDLING_AV_SØKNAD, skalBehandlesAutomatisk = true)
    private val vedtak = lagVedtak(behandling = behandling)
    private val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2025, 10, 20))

    private fun lagAndel(
        fom: YearMonth,
        tom: YearMonth,
        sats: Int,
        ytelseType: YtelseType = YtelseType.ORDINÆR_BARNETRYGD,
        prosent: BigDecimal = BigDecimal(100),
    ) = lagAndelTilkjentYtelse(fom = fom, tom = tom, person = barn, behandling = behandling, ytelseType = ytelseType, sats = sats, beløp = if (prosent.signum() > 0) sats else 0, prosent = prosent)

    private val andeler =
        listOf(
            lagAndel(fom = YearMonth.of(2025, 11), tom = YearMonth.of(2026, 1), sats = 1968, prosent = BigDecimal.ZERO),
            lagAndel(fom = YearMonth.of(2026, 2), tom = YearMonth.of(2026, 4), sats = 1968),
            lagAndel(fom = YearMonth.of(2026, 5), tom = YearMonth.of(2043, 9), sats = 2012),
            lagAndel(fom = YearMonth.of(2026, 2), tom = YearMonth.of(2026, 4), sats = 500, ytelseType = YtelseType.FINNMARKSTILLEGG),
            lagAndel(fom = YearMonth.of(2026, 5), tom = YearMonth.of(2043, 9), sats = 512, ytelseType = YtelseType.FINNMARKSTILLEGG),
        )

    private fun lagPeriode(
        type: Vedtaksperiodetype,
        fom: LocalDate,
        tom: LocalDate,
    ) = lagVedtaksperiodeMedBegrunnelser(vedtak = vedtak, fom = fom, tom = tom, type = type, begrunnelser = mutableSetOf())

    private val periodeUtenUtbetaling = lagPeriode(Vedtaksperiodetype.OPPHØR, fom = LocalDate.of(2025, 11, 1), tom = LocalDate.of(2025, 12, 31))
    private val periodeUtenUtbetalingEtterVilkårsendring = lagPeriode(Vedtaksperiodetype.OPPHØR, fom = LocalDate.of(2026, 1, 1), tom = LocalDate.of(2026, 1, 31))
    private val periodeMedNyInnvilgelse = lagPeriode(Vedtaksperiodetype.UTBETALING, fom = LocalDate.of(2026, 2, 1), tom = LocalDate.of(2026, 4, 30))
    private val periodeMedSatsøkning = lagPeriode(Vedtaksperiodetype.UTBETALING, fom = LocalDate.of(2026, 5, 1), tom = LocalDate.of(2043, 9, 30))

    @BeforeEach
    fun setUp() {
        every { beregningService.hentAndelerTilkjentYtelseForBehandling(behandling.id) } returns andeler
        every { vedtakService.hentAktivForBehandlingThrows(behandling.id) } returns vedtak
        every { vedtaksperiodeHentOgPersisterService.lagre(any<List<VedtaksperiodeMedBegrunnelser>>()) } answers { firstArg() }
    }

    @Test
    fun `skal begrunne ny innvilgelse, satsøkning, tillegg og alle perioder uten utbetaling`() {
        // Arrange
        val vedtaksperioder = listOf(periodeUtenUtbetaling, periodeUtenUtbetalingEtterVilkårsendring, periodeMedNyInnvilgelse, periodeMedSatsøkning)
        every { beregningService.hentAndelerFraForrigeVedtatteBehandling(behandling) } returns emptyList()
        every { vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak) } returns vedtaksperioder

        // Act
        autovedtakSøknadBegrunnelseService.begrunnAutovedtakForSøknad(behandling)

        // Assert
        assertThat(periodeUtenUtbetaling.begrunnelser.map { it.standardbegrunnelse }).containsExactly(Standardbegrunnelse.ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TILBAKE_I_TID_AUTOMATISK)
        assertThat(periodeUtenUtbetalingEtterVilkårsendring.begrunnelser.map { it.standardbegrunnelse }).containsExactly(Standardbegrunnelse.ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TILBAKE_I_TID_AUTOMATISK)
        assertThat(periodeMedNyInnvilgelse.begrunnelser.map { it.standardbegrunnelse }).containsExactlyInAnyOrder(
            Standardbegrunnelse.INNVILGET_AUTOMATISK_SØKNAD,
            Standardbegrunnelse.INNVILGET_FINNMARKSTILLEGG_UTEN_DATO,
        )
        assertThat(periodeMedSatsøkning.begrunnelser.map { it.standardbegrunnelse }).containsExactly(Standardbegrunnelse.INNVILGET_SATSENDRING)
        verify(exactly = 1) { vedtaksperiodeHentOgPersisterService.lagre(vedtaksperioder) }
    }

    @Test
    fun `skal kreve manuell behandling når det ikke finnes noen ny innvilgelse`() {
        // Arrange
        every { beregningService.hentAndelerFraForrigeVedtatteBehandling(behandling) } returns andeler

        // Act & Assert
        val feil =
            assertThrows<AutovedtakMåBehandlesManueltFeil> {
                autovedtakSøknadBegrunnelseService.begrunnAutovedtakForSøknad(behandling)
            }
        assertThat(feil.message).isEqualTo("Det er forsøkt å begrunne autovedtak søknad men det ble ikke funnet noen perioder med ny innvilgelse.\nSøknaden må behandles manuelt.")
        verify(exactly = 0) { vedtaksperiodeHentOgPersisterService.lagre(any<List<VedtaksperiodeMedBegrunnelser>>()) }
    }

    @Test
    fun `skal kaste feil når perioden som skal begrunnes ikke finnes`() {
        // Arrange
        every { beregningService.hentAndelerFraForrigeVedtatteBehandling(behandling) } returns emptyList()
        every { vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak) } returns listOf(periodeUtenUtbetaling, periodeMedSatsøkning)

        // Act & Assert
        val feil =
            assertThrows<Feil> {
                autovedtakSøknadBegrunnelseService.begrunnAutovedtakForSøknad(behandling)
            }
        assertThat(feil.message).isEqualTo("Finner ikke aktuell periode å begrunne ved autovedtak. Se securelogger for detaljer.")
        verify(exactly = 0) { vedtaksperiodeHentOgPersisterService.lagre(any<List<VedtaksperiodeMedBegrunnelser>>()) }
    }
}
