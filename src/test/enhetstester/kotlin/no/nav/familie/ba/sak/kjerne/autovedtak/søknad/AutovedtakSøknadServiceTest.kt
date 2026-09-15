package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.lagØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.datagenerator.lagØkonomiSimuleringPostering
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.SøknadData
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.Søknad
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class AutovedtakSøknadServiceTest {
    private val autovedtakService = mockk<AutovedtakService>()
    private val simuleringService = mockk<SimuleringService>()
    private val taskService = mockk<TaskService>()
    private val autovedtakSøknadBegrunnelseService = mockk<AutovedtakSøknadBegrunnelseService>()
    private val vilkårsvurderingService = mockk<VilkårsvurderingService>()

    private val autovedtakSøknadService =
        AutovedtakSøknadService(
            autovedtakService = autovedtakService,
            simuleringService = simuleringService,
            taskService = taskService,
            autovedtakSøknadBegrunnelseService = autovedtakSøknadBegrunnelseService,
            vilkårsvurderingService = vilkårsvurderingService,
        )

    private val fagsak = lagFagsak()
    private val behandling = lagBehandling(fagsak = fagsak, førsteSteg = StegType.IVERKSETT_MOT_OPPDRAG)
    private val søknad =
        Søknad(
            fagsakId = fagsak.id,
            søkersIdent = "12345678910",
            barnasIdenter = listOf("12345678911"),
        )
    private val søknadData = SøknadData(søknad = søknad)

    @Nested
    inner class SkalAutovedtakBehandles {
        @Test
        fun `skal alltid returnere true`() {
            // Act
            val skalAutovedtakBehandles = autovedtakSøknadService.skalAutovedtakBehandles(søknadData)

            // Assert
            assertThat(skalAutovedtakBehandles).isTrue()
        }
    }

    @Nested
    inner class KjørBehandling {
        @BeforeEach
        fun setup() {
            every {
                autovedtakService.opprettAutomatiskBehandlingMedFiltreringOgKjørTilBehandlingsresultat(
                    fagsakId = søknad.fagsakId,
                    behandlingType = any(),
                    behandlingÅrsak = any(),
                    filtrerAutomatiskBehandlingData = any(),
                )
            } returns behandling

            every {
                vilkårsvurderingService.hentAktivForBehandlingThrows(behandling.id)
            } returns lagVilkårsvurdering(behandling = behandling)

            every {
                simuleringService.oppdaterSimuleringPåBehandling(behandling)
            } returns
                listOf(
                    lagØkonomiSimuleringMottaker(
                        behandling = behandling,
                        økonomiSimuleringPostering = listOf(lagØkonomiSimuleringPostering(beløp = 100)),
                    ),
                )

            every { simuleringService.hentFeilutbetaling(behandling.id) } returns BigDecimal.ZERO
            every { autovedtakSøknadBegrunnelseService.begrunnAutovedtakForSøknad() } just Runs
            every { autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(behandling) } returns lagVedtak(behandling = behandling)
            every { taskService.save(any()) } returns mockk()
        }

        @Test
        fun `skal kaste AutovedtakMåBehandlesManueltFeil når vilkårsvurderingen ikke er oppfylt`() {
            // Arrange
            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = behandling,
                    lagPersonResultater = {
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                aktør = fagsak.aktør,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            resultat = Resultat.IKKE_OPPFYLT,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )
            every { vilkårsvurderingService.hentAktivForBehandlingThrows(behandling.id) } returns vilkårsvurdering

            // Act & Assert
            val feil =
                assertThrows<AutovedtakMåBehandlesManueltFeil> {
                    autovedtakSøknadService.kjørBehandling(søknadData)
                }
            assertThat(feil.message).isEqualTo("Vilkårsvurderingen er ikke oppfylt.\nBehandling av søknad må håndteres manuelt.")
        }

        @Test
        fun `skal kaste AutovedtakMåBehandlesManueltFeil når automatisk behandling ikke fører til noen utbetaling`() {
            // Arrange
            every { simuleringService.oppdaterSimuleringPåBehandling(behandling) } returns
                listOf(
                    lagØkonomiSimuleringMottaker(
                        behandling = behandling,
                        økonomiSimuleringPostering = listOf(lagØkonomiSimuleringPostering(beløp = 0)),
                    ),
                )

            // Act & Assert
            val feil =
                assertThrows<AutovedtakMåBehandlesManueltFeil> {
                    autovedtakSøknadService.kjørBehandling(søknadData)
                }
            assertThat(feil.message).isEqualTo("Automatisk behandling av søknad fører til ingen utbetaling.\nBehandling av søknad må håndteres manuelt.")
        }

        @Test
        fun `skal kaste AutovedtakMåBehandlesManueltFeil når automatisk behandling fører til feilutbetaling`() {
            // Arrange
            every { simuleringService.hentFeilutbetaling(behandling.id) } returns BigDecimal.ONE

            // Act & Assert
            val feil =
                assertThrows<AutovedtakMåBehandlesManueltFeil> {
                    autovedtakSøknadService.kjørBehandling(søknadData)
                }
            assertThat(feil.message).isEqualTo("Automatisk behandling av søknad fører til feilutbetaling.\nBehandling av søknad må håndteres manuelt.")
        }

        @Test
        fun `skal begrunne vedtak og opprette IverksettMotOppdragTask når behandlingsteg er IVERKSETT_MOT_OPPDRAG`() {
            // Arrange
            val taskSlot = slot<Task>()
            every { taskService.save(capture(taskSlot)) } returns mockk()

            // Act
            val resultat = autovedtakSøknadService.kjørBehandling(søknadData)

            // Assert
            assertThat(resultat).isEqualTo(AutovedtakStegService.BEHANDLING_FERDIG)
            assertThat(taskSlot.captured.type).isEqualTo(IverksettMotOppdragTask.TASK_STEP_TYPE)
            verify(exactly = 1) {
                autovedtakSøknadBegrunnelseService.begrunnAutovedtakForSøknad()
                autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(behandling)
            }
        }

        @Test
        fun `skal kaste Feil når behandlingsteg etter behandlingsresultat ikke er IVERKSETT_MOT_OPPDRAG`() {
            // Arrange
            val behandlingUtenIverksettelse = lagBehandling(fagsak = fagsak, førsteSteg = StegType.FERDIGSTILLE_BEHANDLING)
            every {
                autovedtakService.opprettAutomatiskBehandlingMedFiltreringOgKjørTilBehandlingsresultat(
                    fagsakId = søknad.fagsakId,
                    behandlingType = any(),
                    behandlingÅrsak = any(),
                    filtrerAutomatiskBehandlingData = any(),
                )
            } returns behandlingUtenIverksettelse
            every {
                vilkårsvurderingService.hentAktivForBehandlingThrows(behandlingUtenIverksettelse.id)
            } returns lagVilkårsvurdering(behandling = behandlingUtenIverksettelse)

            every {
                simuleringService.oppdaterSimuleringPåBehandling(behandlingUtenIverksettelse)
            } returns
                listOf(
                    lagØkonomiSimuleringMottaker(
                        behandling = behandlingUtenIverksettelse,
                        økonomiSimuleringPostering = listOf(lagØkonomiSimuleringPostering(beløp = 100)),
                    ),
                )

            every { simuleringService.hentFeilutbetaling(behandlingUtenIverksettelse.id) } returns BigDecimal.ZERO

            every {
                autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(behandlingUtenIverksettelse)
            } returns lagVedtak(behandling = behandlingUtenIverksettelse)

            // Act & Assert
            assertThrows<Feil> {
                autovedtakSøknadService.kjørBehandling(søknadData)
            }
            verify(exactly = 0) {
                autovedtakSøknadBegrunnelseService.begrunnAutovedtakForSøknad()
            }
        }
    }
}
