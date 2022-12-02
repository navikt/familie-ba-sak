package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.beregning.EndringstidspunktService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class VedtaksperiodeServiceEnhetstest {

    private val behandlingRepository: BehandlingRepository = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService = mockk()
    private val endringstidspunktService: EndringstidspunktService = mockk()
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService = mockk()

    private val VedtaksperiodeService = VedtaksperiodeService(
        behandlingRepository = behandlingRepository,
        personidentService = mockk(),
        persongrunnlagService = persongrunnlagService,
        andelTilkjentYtelseRepository = mockk(),
        vedtaksperiodeHentOgPersisterService = vedtaksperiodeHentOgPersisterService,
        vedtakRepository = mockk(),
        vilkårsvurderingRepository = mockk(relaxed = true),
        sanityService = mockk(),
        søknadGrunnlagService = mockk(relaxed = true),
        endretUtbetalingAndelRepository = mockk(),
        endringstidspunktService = endringstidspunktService,
        featureToggleService = mockk(),
        utbetalingsperiodeMedBegrunnelserService = mockk(relaxed = true),
        kompetanseRepository = mockk(),
        andelerTilkjentYtelseOgEndreteUtbetalingerService = andelerTilkjentYtelseOgEndreteUtbetalingerService
    )

    private val person = lagPerson()
    private val forrigeBehandling = lagBehandling().also {
        it.behandlingStegTilstand.add(BehandlingStegTilstand(0, it, StegType.BEHANDLING_AVSLUTTET))
    }
    private val behandling = lagBehandling(fagsak = forrigeBehandling.fagsak)
    private val vedtak = lagVedtak(behandling)

    private val endringstidspunkt = LocalDate.of(2022, 11, 1)
    private val ytelseOpphørtFørEndringstidspunkt = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
        fom = YearMonth.from(endringstidspunkt).minusMonths(3),
        tom = YearMonth.from(endringstidspunkt).minusMonths(2),
        person = person
    )
    private val ytelseOpphørtSammeMåned = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
        fom = YearMonth.from(endringstidspunkt),
        tom = YearMonth.from(endringstidspunkt).plusYears(18),
        person = person
    )

    @BeforeEach
    fun init() {
        every { behandlingRepository.finnIverksatteBehandlinger(any()) } returns listOf(behandling, forrigeBehandling)
        every { endringstidspunktService.finnEndringstidpunkForBehandling(vedtak.behandling.id) } returns endringstidspunkt
        every { persongrunnlagService.hentAktiv(any()) } returns
            lagTestPersonopplysningGrunnlag(vedtak.behandling.id, person)
        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(forrigeBehandling.id)
        } returns listOf(ytelseOpphørtSammeMåned)

        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)
        } returns listOf(ytelseOpphørtFørEndringstidspunkt)
    }

    @Test
    fun `genererVedtaksperioderMedBegrunnelser skal slå sammen opphørsperioder fra og med endringstidspunkt`() {
        val returnerteVedtaksperioderNårUtledetEndringstidspunktErLikSisteOpphørFom = VedtaksperiodeService
            .genererVedtaksperioderMedBegrunnelser(vedtak)
            .filter { it.type == Vedtaksperiodetype.OPPHØR }

        val førsteOpphørFomDato =
            returnerteVedtaksperioderNårUtledetEndringstidspunktErLikSisteOpphørFom.minOf { it.fom!! }
        val senesteOpphørTomDato =
            returnerteVedtaksperioderNårUtledetEndringstidspunktErLikSisteOpphørFom.sortedBy { it.tom ?: TIDENES_ENDE }.last().tom

        val returnerteVedtaksperioderNårOverstyrtEndringstidspunktErFørsteOpphørFom = VedtaksperiodeService
            .genererVedtaksperioderMedBegrunnelser(vedtak, manueltOverstyrtEndringstidspunkt = førsteOpphørFomDato)
            .filter { it.type == Vedtaksperiodetype.OPPHØR }
        val returnerteVedtaksperioderNårOverstyrtEndringstidspunktErFørFørsteOpphør = VedtaksperiodeService
            .genererVedtaksperioderMedBegrunnelser(vedtak, manueltOverstyrtEndringstidspunkt = førsteOpphørFomDato.minusMonths(1))
            .filter { it.type == Vedtaksperiodetype.OPPHØR }

        assertThat(returnerteVedtaksperioderNårUtledetEndringstidspunktErLikSisteOpphørFom).hasSize(2).last()
            .hasFieldOrPropertyWithValue("fom", endringstidspunkt)
        assertThat(returnerteVedtaksperioderNårOverstyrtEndringstidspunktErFørFørsteOpphør).hasSize(1).first()
            .hasFieldOrPropertyWithValue("fom", førsteOpphørFomDato)
            .hasFieldOrPropertyWithValue("tom", senesteOpphørTomDato)
        assertThat(returnerteVedtaksperioderNårOverstyrtEndringstidspunktErFørFørsteOpphør)
            .isEqualTo(returnerteVedtaksperioderNårOverstyrtEndringstidspunktErFørsteOpphørFom)
    }

    @Test
    fun `nasjonal skal ikke ha årlig kontroll`() {
        val behandling = lagBehandling(behandlingKategori = BehandlingKategori.NASJONAL)
        val vedtak = Vedtak(behandling = behandling)
        assertFalse { VedtaksperiodeService.skalHaÅrligKontroll(vedtak) }
    }

    @Test
    fun `EØS uten periode uten tom skal ikke ha årlig kontroll`() {
        val vedtak = Vedtak(behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS))
        every { vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(any()) } returns listOf(
            lagVedtaksperiodeMedBegrunnelser(vedtak = vedtak, tom = LocalDate.now())
        )
        assertFalse { VedtaksperiodeService.skalHaÅrligKontroll(vedtak) }
    }

    @Test
    fun `EØS med periode uten tom skal ha årlig kontroll`() {
        val vedtak = Vedtak(behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS))
        every { vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(any()) } returns listOf(
            lagVedtaksperiodeMedBegrunnelser(vedtak = vedtak, tom = null)
        )
        assertTrue { VedtaksperiodeService.skalHaÅrligKontroll(vedtak) }
    }
}
