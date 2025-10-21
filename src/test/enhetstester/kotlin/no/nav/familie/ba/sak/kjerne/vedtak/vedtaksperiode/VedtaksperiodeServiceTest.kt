package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagKompetanse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.KodeverkService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.overgangsstønad.OvergangsstønadService
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
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService = mockk()
    private val feilutbetaltValutaRepository: FeilutbetaltValutaRepository = mockk()
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val overgangsstønadService: OvergangsstønadService = mockk()
    private val refusjonEøsRepository = mockk<RefusjonEøsRepository>()
    private val integrasjonKlient = mockk<IntegrasjonKlient>()
    private val kompetanseRepository = mockk<KompetanseRepository>()

    private val vedtaksperiodeService =
        spyk(
            VedtaksperiodeService(
                persongrunnlagService = persongrunnlagService,
                andelTilkjentYtelseRepository = mockk(),
                vedtaksperiodeHentOgPersisterService = vedtaksperiodeHentOgPersisterService,
                vedtakRepository = mockk(),
                vilkårsvurderingService = mockk(relaxed = true),
                sanityService = mockk(),
                søknadGrunnlagService = mockk(relaxed = true),
                endretUtbetalingAndelRepository = mockk(),
                kompetanseRepository = kompetanseRepository,
                andelerTilkjentYtelseOgEndreteUtbetalingerService = andelerTilkjentYtelseOgEndreteUtbetalingerService,
                feilutbetaltValutaRepository = feilutbetaltValutaRepository,
                behandlingHentOgPersisterService = behandlingHentOgPersisterService,
                overgangsstønadService = overgangsstønadService,
                refusjonEøsRepository = refusjonEøsRepository,
                kodeverkService = KodeverkService(integrasjonKlient = integrasjonKlient),
                valutakursRepository = mockk(),
                utenlandskPeriodebeløpRepository = mockk(),
                featureToggleService = mockk(),
            ),
        )

    private val person = lagPerson()
    private val forrigeBehandling =
        lagBehandling().also {
            it.behandlingStegTilstand.add(BehandlingStegTilstand(0, it, StegType.BEHANDLING_AVSLUTTET))
        }
    private val behandling = lagBehandling(fagsak = forrigeBehandling.fagsak)
    private val vedtak = lagVedtak(behandling)

    private val endringstidspunkt = LocalDate.of(2022, 11, 1)
    private val ytelseOpphørtFørEndringstidspunkt =
        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = YearMonth.from(endringstidspunkt).minusMonths(3),
            tom = YearMonth.from(endringstidspunkt).minusMonths(2),
            person = person,
        )
    private val ytelseOpphørtSammeMåned =
        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = YearMonth.from(endringstidspunkt),
            tom = YearMonth.from(endringstidspunkt).plusYears(18),
            person = person,
        )

    @BeforeEach
    fun init() {
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns forrigeBehandling
        every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(any()) } returns forrigeBehandling
        every { behandlingHentOgPersisterService.hent(forrigeBehandling.id) } returns forrigeBehandling
        every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling
        every { vedtaksperiodeService.finnEndringstidspunktForBehandling(vedtak.behandling.id) } returns endringstidspunkt
        every { persongrunnlagService.hentAktiv(any()) } returns
            lagTestPersonopplysningGrunnlag(vedtak.behandling.id, person)
        every { persongrunnlagService.hentAktivThrows(any()) } returns
            lagTestPersonopplysningGrunnlag(vedtak.behandling.id, person)
        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                forrigeBehandling.id,
            )
        } returns listOf(ytelseOpphørtSammeMåned)

        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)
        } returns listOf(ytelseOpphørtFørEndringstidspunkt)
        every { feilutbetaltValutaRepository.finnFeilutbetaltValutaForBehandling(any()) } returns emptyList()
        every { overgangsstønadService.hentPerioderMedFullOvergangsstønad(any<Behandling>()) } returns emptyList()
        every { refusjonEøsRepository.finnRefusjonEøsForBehandling(any()) } returns emptyList()
        every { integrasjonKlient.hentLandkoderISO2() } returns mapOf(Pair("NO", "NORGE"))
    }

    @Test
    fun `nasjonal sak skal ikke ha årlig kontroll`() {
        val behandling = lagBehandling(behandlingKategori = BehandlingKategori.NASJONAL)
        val vedtak = Vedtak(behandling = behandling)

        every { kompetanseRepository.finnFraBehandlingId(behandlingId = behandling.id) } returns emptyList()
        assertFalse { vedtaksperiodeService.skalHaÅrligKontroll(vedtak) }
    }

    @Test
    fun `EØS med periode med utløpt tom skal ikke ha årlig kontroll`() {
        val vedtak = Vedtak(behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS))

        every { kompetanseRepository.finnFraBehandlingId(behandlingId = vedtak.behandling.id) } returns
            listOf(
                lagKompetanse(
                    fom = YearMonth.now().minusMonths(2),
                    tom = YearMonth.now(),
                ),
            )

        assertFalse { vedtaksperiodeService.skalHaÅrligKontroll(vedtak) }
    }

    @Test
    fun `EØS med periode med løpende tom skal ha årlig kontroll`() {
        val vedtak = Vedtak(behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS))

        every { kompetanseRepository.finnFraBehandlingId(behandlingId = vedtak.behandling.id) } returns
            listOf(
                lagKompetanse(
                    fom = YearMonth.now().minusMonths(1),
                    tom = YearMonth.now().plusMonths(5),
                ),
            )

        assertTrue { vedtaksperiodeService.skalHaÅrligKontroll(vedtak) }
    }

    @Test
    fun `EØS med periode uten tom skal ha årlig kontroll`() {
        val vedtak = Vedtak(behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS))

        every { kompetanseRepository.finnFraBehandlingId(behandlingId = vedtak.behandling.id) } returns
            listOf(
                lagKompetanse(
                    fom = YearMonth.now().minusMonths(1),
                    tom = null,
                ),
            )

        assertTrue { vedtaksperiodeService.skalHaÅrligKontroll(vedtak) }
    }

    @Test
    fun `skal beskrive perioder med for mye utbetalt for behandling med feilutbetalt valuta`() {
        val vedtak = Vedtak(behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS))
        val perioder =
            listOf(
                LocalDate.now() to LocalDate.now().plusYears(1),
                LocalDate.now().plusYears(2) to LocalDate.now().plusYears(3),
            )
        assertThat(vedtaksperiodeService.beskrivPerioderMedFeilutbetaltValuta(vedtak)).isNull()

        every {
            feilutbetaltValutaRepository.finnFeilutbetaltValutaForBehandling(vedtak.behandling.id)
        } returns
            perioder.map {
                FeilutbetaltValuta(1L, fom = it.first, tom = it.second, 200, true)
            }
        val periodebeskrivelser = vedtaksperiodeService.beskrivPerioderMedFeilutbetaltValuta(vedtak)

        perioder.forEach { periode ->
            assertThat(periodebeskrivelser!!.find { it.contains("${periode.first.year}") })
                .contains("Fra", "til", "${periode.second.year}", "er det utbetalt 200 kroner for mye per måned.")
        }
    }

    @Test
    fun `skal beskrive perioder med eøs refusjoner for behandlinger med avklarte refusjon eøs`() {
        val behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS)
        assertThat(
            vedtaksperiodeService.beskrivPerioderMedRefusjonEøs(
                behandling = behandling,
                avklart = true,
            ),
        ).isNull()

        every { refusjonEøsRepository.finnRefusjonEøsForBehandling(behandling.id) } returns
            listOf(
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
        assertThat(
            vedtaksperiodeService.beskrivPerioderMedRefusjonEøs(
                behandling = behandling,
                avklart = false,
            ),
        ).isNull()

        every { refusjonEøsRepository.finnRefusjonEøsForBehandling(behandling.id) } returns
            listOf(
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
