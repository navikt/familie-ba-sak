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
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.beregning.SmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.endringstidspunkt.EndringstidspunktService
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.feilutbetaltValuta.FeilutbetaltValuta
import no.nav.familie.ba.sak.kjerne.vedtak.feilutbetaltValuta.FeilutbetaltValutaRepository
import no.nav.familie.ba.sak.kjerne.vedtak.refusjonEøs.RefusjonEøs
import no.nav.familie.ba.sak.kjerne.vedtak.refusjonEøs.RefusjonEøsRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class VedtaksperiodeServiceTest {
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService =
        mockk()
    private val endringstidspunktService: EndringstidspunktService = mockk()
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService = mockk()
    private val featureToggleService: FeatureToggleService = mockk()
    private val feilutbetaltValutaRepository: FeilutbetaltValutaRepository = mockk()
    private val brevmalService: BrevmalService = mockk()
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val småbarnstilleggService: SmåbarnstilleggService = mockk()
    private val refusjonEøsRepository = mockk<RefusjonEøsRepository>()
    private val integrasjonClient = mockk<IntegrasjonClient>()

    private val vedtaksperiodeService = VedtaksperiodeService(
        personidentService = mockk(),
        persongrunnlagService = persongrunnlagService,
        andelTilkjentYtelseRepository = mockk(),
        vedtaksperiodeHentOgPersisterService = vedtaksperiodeHentOgPersisterService,
        vedtakRepository = mockk(),
        vilkårsvurderingService = mockk(relaxed = true),
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
        behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        småbarnstilleggService = småbarnstilleggService,
        refusjonEøsRepository = refusjonEøsRepository,
        integrasjonClient = integrasjonClient,
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
        person = person,
    )
    private val ytelseOpphørtSammeMåned = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
        fom = YearMonth.from(endringstidspunkt),
        tom = YearMonth.from(endringstidspunkt).plusYears(18),
        person = person,
    )

    @BeforeEach
    fun init() {
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns forrigeBehandling
        every { endringstidspunktService.finnEndringstidspunktForBehandling(vedtak.behandling.id) } returns endringstidspunkt
        every { persongrunnlagService.hentAktiv(any()) } returns
            lagTestPersonopplysningGrunnlag(vedtak.behandling.id, person)
        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                forrigeBehandling.id,
            )
        } returns listOf(ytelseOpphørtSammeMåned)

        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)
        } returns listOf(ytelseOpphørtFørEndringstidspunkt)
        every {
            featureToggleService.isEnabled(
                FeatureToggleConfig.EØS_INFORMASJON_OM_ÅRLIG_KONTROLL,
                any(),
            )
        } returns true
        every { feilutbetaltValutaRepository.finnFeilutbetaltValutaForBehandling(any()) } returns emptyList()
        every { småbarnstilleggService.hentPerioderMedFullOvergangsstønad(any()) } returns emptyList()
        every { refusjonEøsRepository.finnRefusjonEøsForBehandling(any()) } returns emptyList()
        every { integrasjonClient.hentLandkoderISO2() } returns mapOf(Pair("NO", "NORGE"))
    }

    @Test
    fun `genererVedtaksperioderMedBegrunnelser skal slå sammen opphørsperioder fra og med endringstidspunkt`() {
        every { featureToggleService.isEnabled(FeatureToggleConfig.BRUKE_TIDSLINJE_I_STEDET_FOR) } returns true

        val returnerteVedtaksperioderNårUtledetEndringstidspunktErLikSisteOpphørFom = vedtaksperiodeService
            .genererVedtaksperioderMedBegrunnelserGammel(vedtak)
            .filter { it.type == Vedtaksperiodetype.OPPHØR }

        val førsteOpphørFomDato =
            returnerteVedtaksperioderNårUtledetEndringstidspunktErLikSisteOpphørFom.minOf { it.fom!! }
        val senesteOpphørTomDato =
            returnerteVedtaksperioderNårUtledetEndringstidspunktErLikSisteOpphørFom.sortedBy { it.tom ?: TIDENES_ENDE }
                .last().tom

        val returnerteVedtaksperioderNårOverstyrtEndringstidspunktErFørsteOpphørFom = vedtaksperiodeService
            .genererVedtaksperioderMedBegrunnelserGammel(
                vedtak,
                manueltOverstyrtEndringstidspunkt = førsteOpphørFomDato,
            )
            .filter { it.type == Vedtaksperiodetype.OPPHØR }
        val returnerteVedtaksperioderNårOverstyrtEndringstidspunktErFørFørsteOpphør = vedtaksperiodeService
            .genererVedtaksperioderMedBegrunnelserGammel(
                vedtak,
                manueltOverstyrtEndringstidspunkt = førsteOpphørFomDato.minusMonths(1),
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
            lagVedtaksperiodeMedBegrunnelser(vedtak = vedtak, tom = LocalDate.now()),
        )
        assertFalse { vedtaksperiodeService.skalHaÅrligKontroll(vedtak) }
    }

    @Test
    fun `EØS med periode med løpende tom skal ha årlig kontroll`() {
        val vedtak = Vedtak(behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS))
        every { vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(any()) } returns listOf(
            lagVedtaksperiodeMedBegrunnelser(vedtak = vedtak, tom = LocalDate.now().plusMonths(1)),
        )
        assertTrue { vedtaksperiodeService.skalHaÅrligKontroll(vedtak) }
    }

    @Test
    fun `EØS med periode uten tom skal ha årlig kontroll`() {
        val vedtak = Vedtak(behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS))
        every { vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(any()) } returns listOf(
            lagVedtaksperiodeMedBegrunnelser(vedtak = vedtak, tom = null),
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
            lagVedtaksperiodeMedBegrunnelser(vedtak = vedtak, tom = null),
        )
        assertFalse { vedtaksperiodeService.skalHaÅrligKontroll(vedtak) }
    }

    @Test
    fun `skal beskrive perioder med for mye utbetalt for behandling med feilutbetalt valuta`() {
        val vedtak = Vedtak(behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS))
        val perioder = listOf(
            LocalDate.now() to LocalDate.now().plusYears(1),
            LocalDate.now().plusYears(2) to LocalDate.now().plusYears(3),
        )
        assertThat(vedtaksperiodeService.beskrivPerioderMedFeilutbetaltValuta(vedtak)).isNull()

        every {
            feilutbetaltValutaRepository.finnFeilutbetaltValutaForBehandling(vedtak.behandling.id)
        } returns perioder.map {
            FeilutbetaltValuta(1L, fom = it.first, tom = it.second, 200)
        }
        val periodebeskrivelser = vedtaksperiodeService.beskrivPerioderMedFeilutbetaltValuta(vedtak)

        perioder.forEach { periode ->
            assertThat(periodebeskrivelser!!.find { it.contains("${periode.first.year}") })
                .contains("Fra", "til", "${periode.second.year}", "er det utbetalt 200 kroner for mye.")
        }
    }

    @Test
    fun `skal beskrive perioder med eøs refusjoner for behandlinger med avklarte refusjon eøs`() {
        val behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS)
        assertThat(vedtaksperiodeService.beskrivPerioderMedRefusjonEøs(behandling = behandling, avklart = true)).isNull()

        every { refusjonEøsRepository.finnRefusjonEøsForBehandling(behandling.id) } returns listOf(
            RefusjonEøs(
                behandlingId = 1L,
                fom = LocalDate.of(2020, 1, 1),
                tom = LocalDate.of(2022, 1, 1),
                refusjonsbeløp = 200,
                land = "NO",
                refusjonAvklart = true,
            ),
        )

        val perioder = vedtaksperiodeService.beskrivPerioderMedRefusjonEøs(behandling = behandling, avklart = true)

        assertThat(perioder?.size).isEqualTo(1)
        assertThat(perioder?.single()).isEqualTo("Fra januar 2020 til januar 2022 blir etterbetaling på 200 kroner per måned utbetalt til myndighetene i Norge.")
    }

    @Test
    fun `skal beskrive perioder med eøs refusjoner for behandlinger med uavklarte refusjon eøs`() {
        val behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS)
        assertThat(vedtaksperiodeService.beskrivPerioderMedRefusjonEøs(behandling = behandling, avklart = false)).isNull()

        every { refusjonEøsRepository.finnRefusjonEøsForBehandling(behandling.id) } returns listOf(
            RefusjonEøs(
                behandlingId = 1L,
                fom = LocalDate.of(2020, 1, 1),
                tom = LocalDate.of(2022, 1, 1),
                refusjonsbeløp = 200,
                land = "NO",
                refusjonAvklart = false,
            ),
        )

        val perioder = vedtaksperiodeService.beskrivPerioderMedRefusjonEøs(behandling = behandling, avklart = false)

        assertThat(perioder?.size).isEqualTo(1)
        assertThat(perioder?.single()).isEqualTo("Fra januar 2020 til januar 2022 blir ikke etterbetaling på 200 kroner per måned utbetalt nå siden det er utbetalt barnetrygd i Norge.")
    }
}
