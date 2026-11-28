package no.nav.familie.ba.sak.kjerne.behandling

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BehandlingControllerTest {
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val behandlingService = mockk<BehandlingService>()
    private val behandlingstemaService = mockk<BehandlingstemaService>()
    private val tilgangService = mockk<TilgangService>()
    private val utvidetBehandlingService = mockk<UtvidetBehandlingService>()
    private val tilbakestillBehandlingService = mockk<TilbakestillBehandlingService>()

    private val behandlingController =
        BehandlingController(
            stegService = mockk(),
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            behandlingService = behandlingService,
            behandlingstemaService = behandlingstemaService,
            taskRepository = mockk(),
            tilgangService = tilgangService,
            utvidetBehandlingService = utvidetBehandlingService,
            tilkjentYtelseValideringService = mockk(),
            tilbakestillBehandlingService = tilbakestillBehandlingService,
        )

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilBehandling(any(), any()) }
        justRun { tilgangService.verifiserHarTilgangTilHandling(any(), any()) }
    }

    @Nested
    inner class EndreBehandlingstema {
        private val behandling =
            lagBehandling(behandlingKategori = BehandlingKategori.NASJONAL, underkategori = BehandlingUnderkategori.ORDINÆR, førsteSteg = StegType.BEHANDLINGSRESULTAT)

        @BeforeEach
        fun setUp() {
            justRun { tilgangService.validerKanRedigereBehandling(any()) }
            every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling
            every { behandlingstemaService.oppdaterSaksbehandletBehandlingstema(behandling, any(), any()) } answers {
                behandling.apply {
                    kategori = secondArg()
                    underkategori = thirdArg()
                }
            }
            every { tilbakestillBehandlingService.resettStegVedEndringPåVilkår(behandling.id) } returns behandling
            every { utvidetBehandlingService.lagUtvidetBehandlingDto(behandlingId = behandling.id) } returns mockk()
        }

        @Test
        fun `skal tilbakestille behandlingen til vilkårsvurdering når kategori endres`() {
            // Act
            behandlingController.endreBehandlingstema(
                behandlingId = behandling.id,
                endreBehandlingstema =
                    EndreBehandlingstemaDto(
                        behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
                        behandlingKategori = BehandlingKategori.EØS,
                    ),
            )

            // Assert
            verify(exactly = 1) { tilbakestillBehandlingService.resettStegVedEndringPåVilkår(behandling.id) }
        }

        @Test
        fun `skal tilbakestille behandlingen til vilkårsvurdering når kun underkategori endres`() {
            // Act
            behandlingController.endreBehandlingstema(
                behandlingId = behandling.id,
                endreBehandlingstema =
                    EndreBehandlingstemaDto(
                        behandlingUnderkategori = BehandlingUnderkategori.UTVIDET,
                        behandlingKategori = BehandlingKategori.NASJONAL,
                    ),
            )

            // Assert
            verify(exactly = 1) { tilbakestillBehandlingService.resettStegVedEndringPåVilkår(behandling.id) }
        }

        @Test
        fun `skal ikke tilbakestille behandlingen når den ikke er forbi vilkårsvurderingen`() {
            // Arrange
            val behandlingFørVilkårsvurdering = lagBehandling(førsteSteg = StegType.VILKÅRSVURDERING)
            every { behandlingHentOgPersisterService.hent(behandlingFørVilkårsvurdering.id) } returns behandlingFørVilkårsvurdering
            every { behandlingstemaService.oppdaterSaksbehandletBehandlingstema(behandlingFørVilkårsvurdering, any(), any()) } returns behandlingFørVilkårsvurdering
            every { utvidetBehandlingService.lagUtvidetBehandlingDto(behandlingId = behandlingFørVilkårsvurdering.id) } returns mockk()

            // Act
            behandlingController.endreBehandlingstema(
                behandlingId = behandlingFørVilkårsvurdering.id,
                endreBehandlingstema =
                    EndreBehandlingstemaDto(
                        behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
                        behandlingKategori = BehandlingKategori.EØS,
                    ),
            )

            // Assert
            verify(exactly = 0) { tilbakestillBehandlingService.resettStegVedEndringPåVilkår(any()) }
        }

        @Test
        fun `skal ikke tilbakestille behandlingen når behandlingstema er uendret`() {
            // Act
            behandlingController.endreBehandlingstema(
                behandlingId = behandling.id,
                endreBehandlingstema =
                    EndreBehandlingstemaDto(
                        behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
                        behandlingKategori = BehandlingKategori.NASJONAL,
                    ),
            )

            // Assert
            verify(exactly = 0) { tilbakestillBehandlingService.resettStegVedEndringPåVilkår(any()) }
        }
    }

    @Nested
    inner class HentMigreringsdato {
        private val behandlingId = 1L

        @Test
        fun `skal returnere migreringsdatoen på behandlingen`() {
            // Arrange
            val migreringsdato = LocalDate.of(2020, 2, 1)
            every { behandlingService.hentMigreringsdatoIBehandling(behandlingId = behandlingId) } returns migreringsdato

            // Act
            val respons = behandlingController.hentMigreringsdato(behandlingId)

            // Assert
            assertThat(respons.body?.data).isEqualTo(migreringsdato)
        }

        @Test
        fun `skal returnere null når behandlingen ikke har migreringsdato`() {
            // Arrange
            every { behandlingService.hentMigreringsdatoIBehandling(behandlingId = behandlingId) } returns null

            // Act
            val respons = behandlingController.hentMigreringsdato(behandlingId)

            // Assert
            assertThat(respons.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(respons.body?.data).isNull()
        }
    }
}
