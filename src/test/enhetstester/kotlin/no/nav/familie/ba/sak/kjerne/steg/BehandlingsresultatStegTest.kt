package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ba.sak.TestClockProvider
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatService
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatStegValideringService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursRepository
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.småbarnstillegg.SmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

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
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService = mockk<AndelerTilkjentYtelseOgEndreteUtbetalingerService>()
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository = mockk()
    private val utenlandskPeriodebeløpRepository: UtenlandskPeriodebeløpRepository = mockk()
    private val valutakursRepository: ValutakursRepository = mockk()
    private val valutakursService = mockk<ValutakursService>()
    private val kompetanseRepository = mockk<KompetanseRepository>()
    private val småbarnstilleggService = mockk<SmåbarnstilleggService>()
    private val tilbakestillBehandlingService = mockk<TilbakestillBehandlingService>()
    private val clockProvider = TestClockProvider.Companion.lagClockProviderMedFastTidspunkt(LocalDate.of(2025, 10, 10))
    private val behandlingsresultatstegValideringService = mockk<BehandlingsresultatStegValideringService>()

    private val behandlingsresultatSteg: BehandlingsresultatSteg =
        BehandlingsresultatSteg(
            behandlingService = behandlingService,
            simuleringService = simuleringService,
            vedtakService = vedtakService,
            vedtaksperiodeService = vedtaksperiodeService,
            behandlingsresultatService = mockBehandlingsresultatService,
            persongrunnlagService = persongrunnlagService,
            beregningService = beregningService,
            småbarnstilleggService = småbarnstilleggService,
            tilbakestillBehandlingService = tilbakestillBehandlingService,
            behandlingsresultatstegValideringService = behandlingsresultatstegValideringService,
        )

    private val behandling =
        lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
        )

    @BeforeEach
    fun init() {
        every { simuleringService.oppdaterSimuleringPåBehandling(any()) } returns emptyList()
        every { simuleringService.hentSimuleringPåBehandling(any()) } returns emptyList()
        every { valutakursService.hentValutakurser(any()) } returns emptyList()
        every { kompetanseRepository.finnFraBehandlingId(any()) } returns emptyList()
        justRun {
            tilbakestillBehandlingService
                .slettTilbakekrevingsvedtakMotregningHvisBehandlingIkkeAvregner(any())
        }
    }

    @Nested
    inner class UtførStegOgAngiNesteTest {
        @Test
        fun `Skal gå rett fra behandlingsresultat til iverksetting for alle fødselshendelser`() {
            val fødselshendelseBehandling =
                behandling.copy(
                    skalBehandlesAutomatisk = true,
                    opprettetÅrsak = BehandlingÅrsak.FØDSELSHENDELSE,
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                )
            val vedtak =
                lagVedtak(
                    fødselshendelseBehandling,
                )
            every { mockBehandlingsresultatService.utledBehandlingsresultat(any()) } returns Behandlingsresultat.INNVILGET_OG_ENDRET
            every { behandlingService.nullstillEndringstidspunkt(fødselshendelseBehandling.id) } just runs
            every { behandlingService.oppdaterBehandlingsresultat(any(), any(), any()) } returns
                fødselshendelseBehandling.copy(resultat = Behandlingsresultat.INNVILGET_OG_ENDRET)
            every {
                behandlingService.oppdaterStatusPåBehandling(
                    fødselshendelseBehandling.id,
                    BehandlingStatus.IVERKSETTER_VEDTAK,
                )
            } returns fødselshendelseBehandling.copy(status = BehandlingStatus.IVERKSETTER_VEDTAK)
            every { vedtakService.hentAktivForBehandlingThrows(fødselshendelseBehandling.id) } returns vedtak
            every { vedtaksperiodeService.oppdaterVedtakMedVedtaksperioder(vedtak) } just runs
            every { beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(fødselshendelseBehandling) } returns EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING
            every { utenlandskPeriodebeløpRepository.finnFraBehandlingId(fødselshendelseBehandling.id) } returns emptyList()
            every { valutakursRepository.finnFraBehandlingId(fødselshendelseBehandling.id) } returns emptyList()

            Assertions.assertEquals(
                behandlingsresultatSteg.utførStegOgAngiNeste(fødselshendelseBehandling, ""),
                StegType.IVERKSETT_MOT_OPPDRAG,
            )
        }
    }

    @Nested
    inner class PreValiderStegTest {
        @ParameterizedTest
        @EnumSource(
            value = BehandlingÅrsak::class,
            names = ["SATSENDRING", "MÅNEDLIG_VALUTAJUSTERING", "FINNMARKSTILLEGG", "SVALBARDTILLEGG"],
            mode = EXCLUDE,
        )
        fun `skal ikke valideres om behandlingen ikke har riktig årsak for behandling som skal automatisk behandles`(
            behandlingÅrsak: BehandlingÅrsak,
        ) {
            // Arrange
            val behandling = lagBehandling(skalBehandlesAutomatisk = true, årsak = behandlingÅrsak)

            // Act & assert
            assertDoesNotThrow { behandlingsresultatSteg.preValiderSteg(behandling) }
        }
    }
}
