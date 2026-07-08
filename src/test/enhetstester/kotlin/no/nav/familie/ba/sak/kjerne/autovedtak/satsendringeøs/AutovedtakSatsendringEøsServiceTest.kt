package no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.SatsendringEøsData
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.sats.EøsSats
import no.nav.familie.ba.sak.kjerne.eøs.sats.EøsSatserRegister
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class AutovedtakSatsendringEøsServiceTest {
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val satsendringEøsKjøringService = mockk<SatsendringEøsKjøringService>(relaxed = true)
    private val utenlandskPeriodebeløpService = mockk<UtenlandskPeriodebeløpService>()
    private val autovedtakService = mockk<AutovedtakService>()
    private val taskRepository = mockk<TaskRepositoryWrapper>()

    private val service =
        AutovedtakSatsendringEøsService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            satsendringEøsKjøringService = satsendringEøsKjøringService,
            utenlandskPeriodebeløpService = utenlandskPeriodebeløpService,
            autovedtakService = autovedtakService,
            taskRepository = taskRepository,
        )

    private val land = "SE"
    private val satsTidspunkt = YearMonth.of(2026, 1)
    private val forrigeSatsTidspunkt = satsTidspunkt.minusMonths(1)
    private val fagsak = lagFagsak(status = FagsakStatus.LØPENDE)
    private val behandling = lagBehandling(fagsak = fagsak, behandlingKategori = BehandlingKategori.EØS)
    private val satsendringEøsData = SatsendringEøsData(fagsakId = fagsak.id, utbetalingsland = land, satsTidspunkt = satsTidspunkt)

    private val forrigeSats =
        EøsSats(
            land = land,
            valuta = "SEK",
            beløp = BigDecimal("1000"),
            fom = forrigeSatsTidspunkt,
            tom = satsTidspunkt.minusMonths(1),
            intervall = Intervall.MÅNEDLIG,
        )

    private val gjeldendeSats =
        EøsSats(
            land = land,
            valuta = "SEK",
            beløp = BigDecimal("1200"),
            fom = satsTidspunkt,
            intervall = Intervall.MÅNEDLIG,
        )

    @BeforeEach
    fun setUp() {
        mockkObject(EøsSatserRegister)
        every { EøsSatserRegister.satser } returns listOf(forrigeSats, gjeldendeSats)
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns behandling
        every { utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(any()) } returns emptyList()
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(EøsSatserRegister)
    }

    @Nested
    inner class AutovedtakSkalIkkeGjennomføres {
        @Test
        fun `returnerer allerede utført når alle utenlandsk periodebeløp har korrekt beløp`() {
            // Arrange
            every { utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(BehandlingId(behandling.id)) } returns
                listOf(lagUtenlandskPeriodebeløp(beløp = gjeldendeSats.beløp))

            // Act
            val resultat = service.kjørBehandling(satsendringEøsData)

            // Assert
            assertThat(resultat).isEqualTo(SatsendringEøsSvar.SATSENDRING_EØS_ER_ALLEREDE_UTFØRT.melding)
        }

        @Test
        fun `returnerer ingen relevante utenlandsk periodebeløp når utenlandsk periodebeløp-listen er tom`() {
            // Act
            val resultat = service.kjørBehandling(satsendringEøsData)

            // Assert
            assertThat(resultat).isEqualTo(SatsendringEøsSvar.SATSENDRING_EØS_INGEN_RELEVANTE_UTENLANDSK_PERIODEBELØP.melding)
        }

        @Test
        fun `kaster feil når utenlandsk periodebeløp har annen valutakode enn gjeldende sats`() {
            // Arrange
            every { utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(BehandlingId(behandling.id)) } returns
                listOf(lagUtenlandskPeriodebeløp(valutakode = "DKK", beløp = forrigeSats.beløp))

            // Act & Assert
            assertThatThrownBy { service.kjørBehandling(satsendringEøsData) }
                .isInstanceOf(AutovedtakMåBehandlesManueltFeil::class.java)
                .hasMessage(SatsendringEøsSvar.SATSENDRING_EØS_MÅ_BEHANDLES_MANUELT.melding)
        }

        @Test
        fun `kaster feil når utenlandsk periodebeløp har annet intervall enn gjeldende sats`() {
            // Arrange
            every { utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(BehandlingId(behandling.id)) } returns
                listOf(lagUtenlandskPeriodebeløp(intervall = Intervall.KVARTALSVIS, beløp = forrigeSats.beløp))

            // Act & Assert
            assertThatThrownBy { service.kjørBehandling(satsendringEøsData) }
                .isInstanceOf(AutovedtakMåBehandlesManueltFeil::class.java)
                .hasMessage(SatsendringEøsSvar.SATSENDRING_EØS_MÅ_BEHANDLES_MANUELT.melding)
        }

        @Test
        fun `kaster feil når utenlandsk periodebeløp har avvikende beløp fra forrige sats`() {
            // Arrange
            every { utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(BehandlingId(behandling.id)) } returns
                listOf(lagUtenlandskPeriodebeløp(beløp = BigDecimal("999")))

            // Act & Assert
            assertThatThrownBy { service.kjørBehandling(satsendringEøsData) }
                .isInstanceOf(AutovedtakMåBehandlesManueltFeil::class.java)
                .hasMessage(SatsendringEøsSvar.SATSENDRING_EØS_MÅ_BEHANDLES_MANUELT.melding)
        }
    }

    @Nested
    inner class AutovedtakSkalGjennomføres {
        @BeforeEach
        fun setup() {
            val utenlandskPeriodebeløp = lagUtenlandskPeriodebeløp(beløp = forrigeSats.beløp)
            every { utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(any()) } returns listOf(utenlandskPeriodebeløp)
            every { autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(any()) } returns lagVedtak()
            every { taskRepository.save(any()) } returns mockk()
        }

        @Test
        fun `oppretter IverksettMotOppdragTask når steg er IVERKSETT_MOT_OPPDRAG`() {
            // Arrange
            val behandlingEtterResultat = lagBehandling(fagsak = fagsak, førsteSteg = StegType.IVERKSETT_MOT_OPPDRAG)
            val taskSlot = slot<Task>()
            every {
                autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(any(), any(), any(), any())
            } returns behandlingEtterResultat
            every { taskRepository.save(capture(taskSlot)) } returns mockk()

            // Act
            val resultat = service.kjørBehandling(satsendringEøsData)

            // Assert
            assertThat(resultat).isEqualTo(SatsendringEøsSvar.SATSENDRING_EØS_KJØRT_OK.melding)
            assertThat(taskSlot.captured.type).isEqualTo(IverksettMotOppdragTask.TASK_STEP_TYPE)
        }

        @Test
        fun `oppretter JournalførVedtaksbrevTask når steg er JOURNALFØR_VEDTAKSBREV`() {
            // Arrange
            val behandlingEtterResultat = lagBehandling(fagsak = fagsak, førsteSteg = StegType.JOURNALFØR_VEDTAKSBREV)
            val taskSlot = slot<Task>()
            every {
                autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(any(), any(), any(), any())
            } returns behandlingEtterResultat
            every { taskRepository.save(capture(taskSlot)) } returns mockk()

            // Act
            val resultat = service.kjørBehandling(satsendringEøsData)

            // Assert
            assertThat(resultat).isEqualTo(SatsendringEøsSvar.SATSENDRING_EØS_KJØRT_OK.melding)
            assertThat(taskSlot.captured.type).isEqualTo(JournalførVedtaksbrevTask.TASK_STEP_TYPE)
        }

        @Test
        fun `kaller settBehandlingId med behandlingen som opprettes, via førVilkårsvurdering-callback`() {
            // Arrange
            val behandlingEtterResultat = lagBehandling(fagsak = fagsak, førsteSteg = StegType.IVERKSETT_MOT_OPPDRAG)
            val førVilkårsvurderingSlot = slot<(Behandling) -> Unit>()
            every {
                autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                    behandlingType = any(),
                    behandlingÅrsak = any(),
                    fagsakId = any(),
                    kjørFørVilkårsvurdering = capture(førVilkårsvurderingSlot),
                )
            } answers {
                førVilkårsvurderingSlot.captured.invoke(behandlingEtterResultat)
                behandlingEtterResultat
            }

            // Act
            service.kjørBehandling(satsendringEøsData)

            // Assert
            verify(exactly = 1) {
                satsendringEøsKjøringService.settBehandlingId(fagsak.id, land, satsTidspunkt, behandlingEtterResultat.id)
            }
        }
    }

    private fun lagUtenlandskPeriodebeløp(
        valutakode: String = gjeldendeSats.valuta,
        intervall: Intervall = gjeldendeSats.intervall,
        beløp: BigDecimal = BigDecimal("1000"),
    ): UtenlandskPeriodebeløp =
        UtenlandskPeriodebeløp(
            fom = YearMonth.of(2025, 1),
            tom = null,
            barnAktører = setOf(lagAktør(randomFnr())),
            beløp = beløp,
            valutakode = valutakode,
            intervall = intervall,
            utbetalingsland = land,
            kalkulertMånedligBeløp = beløp,
        ).also { it.behandlingId = behandling.id }
}
