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
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingId
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.beregning.EndringstidspunktService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.feilutbetaltValuta.FeilutbetaltValuta
import no.nav.familie.ba.sak.kjerne.vedtak.feilutbetaltValuta.FeilutbetaltValutaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class VedtaksperiodeServiceEnhetstest {
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService =
        mockk()
    private val endringstidspunktService: EndringstidspunktService = mockk()
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService = mockk()
    private val featureToggleService: FeatureToggleService = mockk()
    private val feilutbetaltValutaRepository: FeilutbetaltValutaRepository = mockk()
    private val brevmalService: BrevmalService = mockk()
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()

    private val vedtaksperiodeService = VedtaksperiodeService(
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
        utbetalingsperiodeMedBegrunnelserService = mockk(relaxed = true),
        kompetanseRepository = mockk(),
        andelerTilkjentYtelseOgEndreteUtbetalingerService = andelerTilkjentYtelseOgEndreteUtbetalingerService,
        featureToggleService = featureToggleService,
        feilutbetaltValutaRepository = feilutbetaltValutaRepository,
        brevmalService = brevmalService,
        behandlingHentOgPersisterService = behandlingHentOgPersisterService
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
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns forrigeBehandling
        every { endringstidspunktService.finnEndringstidspunktForBehandling(vedtak.behandling.behandlingId) } returns endringstidspunkt
        every { persongrunnlagService.hentAktiv(any()) } returns
            lagTestPersonopplysningGrunnlag(vedtak.behandling.behandlingId, person)
        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                forrigeBehandling.behandlingId
            )
        } returns listOf(ytelseOpphørtSammeMåned)

        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.behandlingId)
        } returns listOf(ytelseOpphørtFørEndringstidspunkt)
        every {
            featureToggleService.isEnabled(
                FeatureToggleConfig.EØS_INFORMASJON_OM_ÅRLIG_KONTROLL,
                any()
            )
        } returns true
        every { feilutbetaltValutaRepository.finnFeilutbetaltValutaForBehandling(any()) } returns emptyList()
    }

    @Test
    fun `genererVedtaksperioderMedBegrunnelser skal slå sammen opphørsperioder fra og med endringstidspunkt`() {
        val returnerteVedtaksperioderNårUtledetEndringstidspunktErLikSisteOpphørFom = vedtaksperiodeService
            .genererVedtaksperioderMedBegrunnelser(vedtak)
            .filter { it.type == Vedtaksperiodetype.OPPHØR }

        val førsteOpphørFomDato =
            returnerteVedtaksperioderNårUtledetEndringstidspunktErLikSisteOpphørFom.minOf { it.fom!! }
        val senesteOpphørTomDato =
            returnerteVedtaksperioderNårUtledetEndringstidspunktErLikSisteOpphørFom.sortedBy { it.tom ?: TIDENES_ENDE }
                .last().tom

        val returnerteVedtaksperioderNårOverstyrtEndringstidspunktErFørsteOpphørFom = vedtaksperiodeService
            .genererVedtaksperioderMedBegrunnelser(vedtak, manueltOverstyrtEndringstidspunkt = førsteOpphørFomDato)
            .filter { it.type == Vedtaksperiodetype.OPPHØR }
        val returnerteVedtaksperioderNårOverstyrtEndringstidspunktErFørFørsteOpphør = vedtaksperiodeService
            .genererVedtaksperioderMedBegrunnelser(
                vedtak,
                manueltOverstyrtEndringstidspunkt = førsteOpphørFomDato.minusMonths(1)
            )
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
        assertFalse { vedtaksperiodeService.skalHaÅrligKontroll(vedtak) }
    }

    @Test
    fun `EØS med periode med utløpt tom skal ikke ha årlig kontroll`() {
        val vedtak = Vedtak(behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS))
        every { vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(any()) } returns listOf(
            lagVedtaksperiodeMedBegrunnelser(vedtak = vedtak, tom = LocalDate.now())
        )
        assertFalse { vedtaksperiodeService.skalHaÅrligKontroll(vedtak) }
    }

    @Test
    fun `EØS med periode med løpende tom skal ha årlig kontroll`() {
        val vedtak = Vedtak(behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS))
        every { vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(any()) } returns listOf(
            lagVedtaksperiodeMedBegrunnelser(vedtak = vedtak, tom = LocalDate.now().plusMonths(1))
        )
        assertTrue { vedtaksperiodeService.skalHaÅrligKontroll(vedtak) }
    }

    @Test
    fun `EØS med periode uten tom skal ha årlig kontroll`() {
        val vedtak = Vedtak(behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS))
        every { vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(any()) } returns listOf(
            lagVedtaksperiodeMedBegrunnelser(vedtak = vedtak, tom = null)
        )
        assertTrue { vedtaksperiodeService.skalHaÅrligKontroll(vedtak) }
    }

    @Test
    fun `EØS skal ikke ha årlig kontroll når feature toggle er skrudd av`() {
        every {
            featureToggleService.isEnabled(FeatureToggleConfig.EØS_INFORMASJON_OM_ÅRLIG_KONTROLL, any())
        } returns false

        val vedtak = Vedtak(behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS))
        every { vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(any()) } returns listOf(
            lagVedtaksperiodeMedBegrunnelser(vedtak = vedtak, tom = null)
        )
        assertFalse { vedtaksperiodeService.skalHaÅrligKontroll(vedtak) }
    }

    @Test
    fun `skal beskrive perioder med for mye utbetalt for behandling med feilutbetalt valuta`() {
        val vedtak = Vedtak(behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS))
        val perioder = listOf(
            LocalDate.now() to LocalDate.now().plusYears(1),
            LocalDate.now().plusYears(2) to LocalDate.now().plusYears(3)
        )
        assertThat(vedtaksperiodeService.beskrivPerioderMedFeilutbetaltValuta(vedtak)).isNull()

        every {
            feilutbetaltValutaRepository.finnFeilutbetaltValutaForBehandling(vedtak.behandling.behandlingId.id)
        } returns perioder.map {
            FeilutbetaltValuta(BehandlingId(1L), fom = it.first, tom = it.second, 200)
        }
        val periodebeskrivelser = vedtaksperiodeService.beskrivPerioderMedFeilutbetaltValuta(vedtak)

        perioder.forEach { periode ->
            assertThat(periodebeskrivelser!!.find { it.contains("${periode.first.year}") })
                .contains("Fra", "til", "${periode.second.year}", "er det utbetalt 200 kroner for mye.")
        }
    }
}
