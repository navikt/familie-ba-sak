package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.datagenerator.defaultFagsak
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlAdresserPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.FinnmarkstilleggData
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus.LØPENDE
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType.NORMAL
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE
import org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class AutovedtakFinnmarkstilleggServiceTest {
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val beregningService = mockk<BeregningService>()
    private val fagsakService = mockk<FagsakService>()
    private val pdlRestKlient = mockk<SystemOnlyPdlRestKlient>()
    private val simuleringService = mockk<SimuleringService>()
    private val autovedtakFinnmarkstilleggBegrunnelseService = mockk<AutovedtakFinnmarkstilleggBegrunnelseService>()
    private val autovedtakService = mockk<AutovedtakService>()
    private val behandlingService = mockk<BehandlingService>()
    private val taskService = mockk<TaskService>()

    private val autovedtakFinnmarkstilleggService =
        AutovedtakFinnmarkstilleggService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            persongrunnlagService = persongrunnlagService,
            beregningService = beregningService,
            fagsakService = fagsakService,
            pdlRestKlient = pdlRestKlient,
            simuleringService = simuleringService,
            autovedtakFinnmarkstilleggBegrunnelseService = autovedtakFinnmarkstilleggBegrunnelseService,
            autovedtakService = autovedtakService,
            behandlingService = behandlingService,
            taskService = taskService,
        )

    private val fagsak = defaultFagsak()
    private val behandling = lagBehandling(fagsak = fagsak)
    private val søkerIdent = "12345678910"
    private val barnIdent = "12345678911"
    private val persongrunnlag =
        lagTestPersonopplysningGrunnlag(
            behandling.id,
            søkerIdent,
            listOf(barnIdent),
        )

    private val adresse = Vegadresse(null, null, null, null, null, null, null, null)
    private val bostedsadresseUtenforFinnmark =
        Bostedsadresse(gyldigFraOgMed = LocalDate.of(2025, 9, 15), vegadresse = adresse.copy(kommunenummer = "0301"))

    private val bostedsadresseIFinnmark =
        Bostedsadresse(gyldigFraOgMed = LocalDate.of(2025, 9, 15), vegadresse = adresse.copy(kommunenummer = "5601"))

    @Nested
    inner class SkalAutovedtakBehandles {
        @BeforeEach
        fun setUp() {
            every { fagsakService.hentPåFagsakId(fagsak.id) } returns lagFagsak(status = LØPENDE, type = NORMAL)
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns behandling
            every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
            every { beregningService.hentTilkjentYtelseForBehandling(behandling.id) } returns lagTilkjentYtelse()
        }

        @ParameterizedTest
        @EnumSource(FagsakStatus::class, names = ["LØPENDE"], mode = EXCLUDE)
        fun `skal returnere false når fagsak ikke har løpende barnetrygd`(
            fagsakStatus: FagsakStatus,
        ) {
            // Arrange
            every { fagsakService.hentPåFagsakId(fagsak.id) } returns lagFagsak(status = fagsakStatus)

            // Act
            val skalAutovedtakBehandles = autovedtakFinnmarkstilleggService.skalAutovedtakBehandles(FinnmarkstilleggData(fagsakId = fagsak.id))

            // Assert
            assertThat(skalAutovedtakBehandles).isFalse()
        }

        @ParameterizedTest
        @EnumSource(FagsakType::class, names = ["SKJERMET_BARN", "INSTITUSJON"], mode = INCLUDE)
        fun `skal returnere false når fagsaktype ikke kan behandles`(
            fagsakType: FagsakType,
        ) {
            // Arrange
            every { fagsakService.hentPåFagsakId(fagsak.id) } returns lagFagsak(status = LØPENDE, type = fagsakType)

            // Act
            val skalAutovedtakBehandles = autovedtakFinnmarkstilleggService.skalAutovedtakBehandles(FinnmarkstilleggData(fagsakId = fagsak.id))

            // Assert
            assertThat(skalAutovedtakBehandles).isFalse()
        }

        @Test
        fun `skal returnere false når det ikke finnes noen siste iverksatte behandling`() {
            // Arrange
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns null

            // Act
            val skalAutovedtakBehandles = autovedtakFinnmarkstilleggService.skalAutovedtakBehandles(FinnmarkstilleggData(fagsakId = fagsak.id))

            // Assert
            assertThat(skalAutovedtakBehandles).isFalse()
        }

        @Test
        fun `skal returnere true når forrige behandling hadde andeler med ytelsetype FINNMARKSTILLEGG`() {
            // Arrange
            every { beregningService.hentTilkjentYtelseForBehandling(behandling.id) } returns
                lagTilkjentYtelse {
                    setOf(
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2025, 10),
                            tom = YearMonth.of(2025, 10),
                            ytelseType = YtelseType.FINNMARKSTILLEGG,
                        ),
                    )
                }

            every { pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(listOf(søkerIdent, barnIdent)) } returns
                mapOf(
                    søkerIdent to PdlAdresserPerson(bostedsadresse = listOf(bostedsadresseUtenforFinnmark), deltBosted = emptyList()),
                    barnIdent to PdlAdresserPerson(bostedsadresse = listOf(bostedsadresseUtenforFinnmark), deltBosted = emptyList()),
                )

            // Act
            val skalAutovedtakBehandles = autovedtakFinnmarkstilleggService.skalAutovedtakBehandles(FinnmarkstilleggData(fagsakId = fagsak.id))

            // Assert
            assertThat(skalAutovedtakBehandles).isTrue()
        }

        @Test
        fun `skal returnere false når ingen av personene bor i tilleggssone`() {
            // Arrange
            every { pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(listOf(søkerIdent, barnIdent)) } returns
                mapOf(
                    søkerIdent to PdlAdresserPerson(bostedsadresse = listOf(bostedsadresseUtenforFinnmark), deltBosted = emptyList()),
                    barnIdent to PdlAdresserPerson(bostedsadresse = listOf(bostedsadresseUtenforFinnmark), deltBosted = emptyList()),
                )

            // Act
            val skalAutovedtakBehandles = autovedtakFinnmarkstilleggService.skalAutovedtakBehandles(FinnmarkstilleggData(fagsakId = fagsak.id))

            // Assert
            assertThat(skalAutovedtakBehandles).isFalse()
        }

        @Test
        fun `skal returnere true når minst en person bor i tilleggssone`() {
            // Arrange
            every { pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(listOf(søkerIdent, barnIdent)) } returns
                mapOf(
                    søkerIdent to PdlAdresserPerson(bostedsadresse = listOf(bostedsadresseIFinnmark), deltBosted = emptyList()),
                    barnIdent to PdlAdresserPerson(bostedsadresse = listOf(bostedsadresseUtenforFinnmark), deltBosted = emptyList()),
                )

            // Act
            val skalAutovedtakBehandles = autovedtakFinnmarkstilleggService.skalAutovedtakBehandles(FinnmarkstilleggData(fagsakId = fagsak.id))

            // Assert
            assertThat(skalAutovedtakBehandles).isTrue()
        }

        @Test
        fun `skal kaste feil når minst en person bor i tilleggssone, men siste vedtatte behandling er av kategori EØS`() {
            // Arrange
            val behandling = lagBehandling(fagsak = fagsak, behandlingKategori = BehandlingKategori.EØS)
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns behandling
            every { beregningService.hentTilkjentYtelseForBehandling(behandling.id) } returns lagTilkjentYtelse { emptySet() }
            every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
            every { pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(listOf(søkerIdent, barnIdent)) } returns
                mapOf(
                    søkerIdent to PdlAdresserPerson(bostedsadresse = listOf(bostedsadresseIFinnmark), deltBosted = emptyList()),
                    barnIdent to PdlAdresserPerson(bostedsadresse = listOf(bostedsadresseUtenforFinnmark), deltBosted = emptyList()),
                )

            // Act
            val feil =
                assertThrows<AutovedtakMåBehandlesManueltFeil> {
                    autovedtakFinnmarkstilleggService.skalAutovedtakBehandles(FinnmarkstilleggData(fagsakId = fagsak.id))
                }

            // Assert
            assertThat(feil.message).isEqualTo("Automatisk behandling av Finnmarkstillegg kan ikke gjennomføres for EØS-saker.\nEndring av Finnmarkstillegg må håndteres manuelt.")
        }
    }

    @Nested
    inner class KjørBehandling {
        @BeforeEach
        fun setup() {
            every { fagsakService.hentAktør(fagsak.id) } returns persongrunnlag.søker.aktør
            every { autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(any(), any(), any(), any()) } returns behandling
            every { simuleringService.oppdaterSimuleringPåBehandling(any()) } returns emptyList()
            every { simuleringService.hentFeilutbetaling(any<Long>()) } returns BigDecimal.ZERO
            every { autovedtakFinnmarkstilleggBegrunnelseService.begrunnAutovedtakForFinnmarkstillegg(any()) } just Runs
            every { autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(any()) } returns lagVedtak()
            every { behandlingService.oppdaterStatusPåBehandling(any(), any()) } returns mockk()
            every { taskService.save(any()) } returns mockk()
        }

        @Test
        fun `skal kaste AutovedtakMåBehandlesManueltFeil når behandling fører til feilutbetaling`() {
            // Arrange
            every { simuleringService.hentFeilutbetaling(behandling.id) } returns BigDecimal.ONE

            // Act & Assert
            val feil =
                assertThrows<AutovedtakMåBehandlesManueltFeil> {
                    autovedtakFinnmarkstilleggService.kjørBehandling(FinnmarkstilleggData(fagsakId = fagsak.id))
                }

            assertThat(feil.message).isEqualTo("Automatisk behandling av Finnmarkstillegg fører til feilutbetaling.\nEndring av Finnmarkstillegg må håndteres manuelt.")
        }

        @Test
        fun `skal begrunne brev og oprette riktig task hvis behandlingsteg etter behandlingsresultat er IVERKSETT_MOT_OPPDRAG`() {
            // Arrange
            val behandlingEtterBehandlingsresultat = lagBehandling(førsteSteg = StegType.IVERKSETT_MOT_OPPDRAG)
            val taskSlot = slot<Task>()

            every { autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(any(), any(), any(), any()) } returns behandlingEtterBehandlingsresultat
            every { taskService.save(capture(taskSlot)) } returns mockk()

            // Act
            autovedtakFinnmarkstilleggService.kjørBehandling(FinnmarkstilleggData(fagsakId = fagsak.id))

            // Assert
            assertThat(taskSlot.captured.type).isEqualTo(IverksettMotOppdragTask.TASK_STEP_TYPE)
            verify(exactly = 1) {
                autovedtakFinnmarkstilleggBegrunnelseService.begrunnAutovedtakForFinnmarkstillegg(behandlingEtterBehandlingsresultat)
                autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(behandlingEtterBehandlingsresultat)
            }
        }

        @Test
        fun `skal ikke begrunne brev og opprette riktig task hvis behandlingsteg etter behandlingsresultat er FERDIGSTILLE_BEHANDLING`() {
            // Arrange
            val behandlingEtterBehandlingsresultat = lagBehandling(førsteSteg = StegType.FERDIGSTILLE_BEHANDLING)
            val taskSlot = slot<Task>()

            every { autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(any(), any(), any(), any()) } returns behandlingEtterBehandlingsresultat
            every { taskService.save(capture(taskSlot)) } returns mockk()

            // Act
            autovedtakFinnmarkstilleggService.kjørBehandling(FinnmarkstilleggData(fagsakId = fagsak.id))

            // Assert
            assertThat(taskSlot.captured.type).isEqualTo(FerdigstillBehandlingTask.TASK_STEP_TYPE)
            verify(exactly = 0) {
                autovedtakFinnmarkstilleggBegrunnelseService.begrunnAutovedtakForFinnmarkstillegg(behandling)
            }
            verify(exactly = 1) {
                autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(behandlingEtterBehandlingsresultat)
                behandlingService.oppdaterStatusPåBehandling(
                    behandlingEtterBehandlingsresultat.id,
                    BehandlingStatus.IVERKSETTER_VEDTAK,
                )
            }
        }
    }
}
