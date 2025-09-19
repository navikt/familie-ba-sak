package no.nav.familie.ba.sak.kjerne.autovedtak.svalbardstillegg

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.defaultFagsak
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.autovedtak.svalbardtillegg.AutovedtakSvalbardtilleggBegrunnelseService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth

class AutovedtakSvalbardtilleggBegrunnelseServiceTest {
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val beregningService = mockk<BeregningService>()
    private val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    private val vedtakService = mockk<VedtakService>()
    private val vedtaksperiodeHentOgPersisterService = mockk<VedtaksperiodeHentOgPersisterService>()

    private val autovedtakSvalbardtilleggBegrunnelseService =
        AutovedtakSvalbardtilleggBegrunnelseService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            beregningService = beregningService,
            vedtaksperiodeService = vedtaksperiodeService,
            vedtakService = vedtakService,
            vedtaksperiodeHentOgPersisterService = vedtaksperiodeHentOgPersisterService,
        )

    private val fagsak = defaultFagsak()
    private val forrigeBehandling = lagBehandling(fagsak = fagsak, id = 0)
    private val behandling = lagBehandling(fagsak = fagsak, id = 1)
    private val barn = lagPerson(type = PersonType.BARN)
    private val barn2 = lagPerson(type = PersonType.BARN)

    @Test
    fun `Skal kaste feil dersom det ikke finnes noen perioder å begrunne`() {
        // Arrange
        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2025, 11),
                    tom = YearMonth.of(2025, 12),
                    person = barn,
                    behandling = forrigeBehandling,
                    beløp = 500,
                    sats = 500,
                    ytelseType = YtelseType.SVALBARDTILLEGG,
                ),
            )

        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2025, 11),
                    tom = YearMonth.of(2025, 12),
                    person = barn,
                    behandling = behandling,
                    beløp = 500,
                    sats = 500,
                    ytelseType = YtelseType.SVALBARDTILLEGG,
                ),
            )

        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsak.id) } returns forrigeBehandling
        every { beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(forrigeBehandling.id) } returns forrigeAndeler
        every { beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(behandling.id) } returns nåværendeAndeler

        // Act && Assert
        val feilmelding =
            assertThrows<Feil> {
                autovedtakSvalbardtilleggBegrunnelseService.begrunnAutovedtakForSvalbardtillegg(behandling)
            }.message

        assertThat(feilmelding).isEqualTo("Det er forsøkt å begrunne autovedtak men det ble ikke funnet noen perioder med innvilgelse eller reduksjon.")
    }

    @Test
    fun `Skal lagre vedtaksperiode med innvilgelsebegrunnelse dersom det finnes perioder med innvilgelse`() {
        // Arrange
        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2025, 11),
                    tom = YearMonth.of(2025, 12),
                    person = barn,
                    behandling = behandling,
                    beløp = 500,
                    sats = 500,
                    ytelseType = YtelseType.SVALBARDTILLEGG,
                ),
            )

        val oppdaterteVedtaksperioderSlot = slot<List<VedtaksperiodeMedBegrunnelser>>()

        val vedtaksperiode =
            listOf(
                lagVedtaksperiodeMedBegrunnelser(
                    fom = LocalDate.of(2025, 11, 1),
                    type = Vedtaksperiodetype.UTBETALING,
                    begrunnelser = mutableSetOf(),
                ),
            )

        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsak.id) } returns forrigeBehandling
        every { beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(forrigeBehandling.id) } returns emptyList()
        every { beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(behandling.id) } returns nåværendeAndeler
        every { vedtaksperiodeService.hentPersisterteVedtaksperioder(any()) } returns vedtaksperiode
        every { vedtakService.hentAktivForBehandlingThrows(behandling.id) } returns mockk()
        every { vedtaksperiodeHentOgPersisterService.lagre(capture(oppdaterteVedtaksperioderSlot)) } answers { firstArg() }

        // Act
        autovedtakSvalbardtilleggBegrunnelseService.begrunnAutovedtakForSvalbardtillegg(behandling)

        val oppdaterteVedtaksperioder = oppdaterteVedtaksperioderSlot.captured

        assertThat(oppdaterteVedtaksperioder.size).isEqualTo(1)
        assertThat(oppdaterteVedtaksperioder[0].begrunnelser.map { it.standardbegrunnelse }[0]).isEqualTo(Standardbegrunnelse.INNVILGET_SVALBARDTILLEGG)
    }

    @Test
    fun `Skal lagre vedtaksperiode med reduksjonsbegrunnelse dersom det finnes perioder med reduksjon`() {
        // Arrange
        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2025, 11),
                    tom = YearMonth.of(2025, 12),
                    person = barn,
                    behandling = forrigeBehandling,
                    beløp = 500,
                    sats = 500,
                    ytelseType = YtelseType.SVALBARDTILLEGG,
                ),
            )

        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2025, 12),
                    tom = YearMonth.of(2025, 12),
                    person = barn,
                    behandling = behandling,
                    beløp = 500,
                    sats = 500,
                    ytelseType = YtelseType.SVALBARDTILLEGG,
                ),
            )

        val oppdaterteVedtaksperioderSlot = slot<List<VedtaksperiodeMedBegrunnelser>>()

        val vedtaksperiode =
            listOf(
                lagVedtaksperiodeMedBegrunnelser(
                    fom = LocalDate.of(2025, 11, 1),
                    type = Vedtaksperiodetype.UTBETALING,
                    begrunnelser = mutableSetOf(),
                ),
            )

        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsak.id) } returns forrigeBehandling
        every { beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(forrigeBehandling.id) } returns forrigeAndeler
        every { beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(behandling.id) } returns nåværendeAndeler
        every { vedtaksperiodeService.hentPersisterteVedtaksperioder(any()) } returns vedtaksperiode
        every { vedtakService.hentAktivForBehandlingThrows(behandling.id) } returns mockk()
        every { vedtaksperiodeHentOgPersisterService.lagre(capture(oppdaterteVedtaksperioderSlot)) } answers { firstArg() }

        // Act
        autovedtakSvalbardtilleggBegrunnelseService.begrunnAutovedtakForSvalbardtillegg(behandling)

        val oppdaterteVedtaksperioder = oppdaterteVedtaksperioderSlot.captured

        assertThat(oppdaterteVedtaksperioder.size).isEqualTo(1)
        assertThat(oppdaterteVedtaksperioder[0].begrunnelser.map { it.standardbegrunnelse }[0]).isEqualTo(Standardbegrunnelse.REDUKSJON_SVALBARDTILLEGG)
    }

    @Test
    fun `Skal lagre vedtaksperiode med både reduksjon og innvilgelse begrunnelse dersom det finnes perioder med begge`() {
        // Arrange
        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2025, 10),
                    tom = YearMonth.of(2025, 12),
                    person = barn,
                    behandling = forrigeBehandling,
                    beløp = 500,
                    sats = 500,
                    ytelseType = YtelseType.SVALBARDTILLEGG,
                ),
            )

        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2025, 10),
                    tom = YearMonth.of(2025, 12),
                    person = barn2,
                    behandling = behandling,
                    beløp = 500,
                    sats = 500,
                    ytelseType = YtelseType.SVALBARDTILLEGG,
                ),
            )

        val oppdaterteVedtaksperioderSlot = slot<List<VedtaksperiodeMedBegrunnelser>>()

        val vedtaksperiode =
            listOf(
                lagVedtaksperiodeMedBegrunnelser(
                    fom = LocalDate.of(2025, 10, 1),
                    type = Vedtaksperiodetype.UTBETALING,
                    begrunnelser = mutableSetOf(),
                ),
            )

        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsak.id) } returns forrigeBehandling
        every { beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(forrigeBehandling.id) } returns forrigeAndeler
        every { beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(behandling.id) } returns nåværendeAndeler
        every { vedtaksperiodeService.hentPersisterteVedtaksperioder(any()) } returns vedtaksperiode
        every { vedtakService.hentAktivForBehandlingThrows(behandling.id) } returns mockk()
        every { vedtaksperiodeHentOgPersisterService.lagre(capture(oppdaterteVedtaksperioderSlot)) } answers { firstArg() }

        // Act
        autovedtakSvalbardtilleggBegrunnelseService.begrunnAutovedtakForSvalbardtillegg(behandling)

        val oppdaterteVedtaksperioder = oppdaterteVedtaksperioderSlot.captured

        assertThat(oppdaterteVedtaksperioder.size).isEqualTo(1)
        assertThat(oppdaterteVedtaksperioder[0].begrunnelser.map { it.standardbegrunnelse })
            .contains(
                Standardbegrunnelse.REDUKSJON_SVALBARDTILLEGG,
                Standardbegrunnelse.INNVILGET_SVALBARDTILLEGG,
            )
    }
}
