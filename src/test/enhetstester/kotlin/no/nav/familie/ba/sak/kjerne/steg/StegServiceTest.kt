package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.SatsendringService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.task.OpprettTaskService
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class StegServiceTest {
    private val behandlingService: BehandlingService = mockk()
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val satsendringService: SatsendringService = mockk()
    private val opprettTaskService: OpprettTaskService = mockk(relaxed = true)

    private val stegService =
        StegService(
            steg = listOf(mockRegistrerPersongrunnlag()),
            fagsakService = mockk(),
            behandlingService = behandlingService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            beregningService = mockk(),
            søknadGrunnlagService = mockk(),
            tilgangService = mockk(relaxed = true),
            infotrygdFeedService = mockk(),
            satsendringService = satsendringService,
            personopplysningerService = mockk(),
            automatiskBeslutningService = mockk(),
            opprettTaskService = opprettTaskService,
            satskjøringRepository = mockk(relaxed = true),
        )

    @BeforeEach
    fun setup() {
        val behandling = lagBehandling()
        every { behandlingService.opprettBehandling(any()) } returns behandling
        every { behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(any(), any()) } returns behandling
        every { behandlingHentOgPersisterService.hent(any()) } returns behandling
    }

    @Test
    fun `skal IKKE feile validering av helmanuell migrering når fagsak har aktivt vedtak som er et opphør`() {
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = any()) } returns
            lagBehandling()

        every { behandlingService.erLøpende(any()) } returns false

        assertDoesNotThrow {
            stegService.håndterNyBehandling(
                NyBehandling(
                    kategori = BehandlingKategori.NASJONAL,
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                    behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                    behandlingÅrsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
                    søkersIdent = randomFnr(),
                    barnasIdenter = listOf(randomFnr()),
                    nyMigreringsdato = LocalDate.now().minusMonths(6),
                    fagsakId = 1L,
                ),
            )
        }
    }

    @Test
    fun `skal feile validering av helmanuell migrering når fagsak har aktivt vedtak med løpende utbetalinger`() {
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = any()) } returns
            lagBehandling()

        every { behandlingService.erLøpende(any()) } returns true

        assertThrows<FunksjonellFeil> {
            stegService.håndterNyBehandling(
                NyBehandling(
                    kategori = BehandlingKategori.NASJONAL,
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                    behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                    behandlingÅrsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
                    søkersIdent = randomFnr(),
                    barnasIdenter = listOf(randomFnr()),
                    nyMigreringsdato = LocalDate.now().minusMonths(6),
                    fagsakId = 1L,
                ),
            )
        }
    }

    @Test
    fun `Skal feile dersom vi har en gammel sats på forrige iverksatte behandling på endre migreringsdato behandling`() {
        every { satsendringService.erFagsakOppdatertMedSisteSatser(any()) } returns false

        val nyBehandling =
            NyBehandling(
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
                søkersIdent = randomFnr(),
                barnasIdenter = listOf(randomFnr()),
                nyMigreringsdato = LocalDate.now().minusMonths(6),
                fagsakId = 1L,
            )

        assertThrows<FunksjonellFeil> { stegService.håndterNyBehandling(nyBehandling) }
        verify(exactly = 1) { opprettTaskService.opprettSatsendringTask(any(), any()) }
    }

    @Test
    fun `Skal feile dersom behandlingen er satt på vent`() {
        val behandling = lagBehandling(status = BehandlingStatus.SATT_PÅ_VENT)
        val grunnlag = RegistrerPersongrunnlagDTO("", emptyList())
        assertThatThrownBy { stegService.håndterPersongrunnlag(behandling, grunnlag) }
            .hasMessageContaining("er på vent")
    }

    private fun mockRegistrerPersongrunnlag() =
        object : RegistrerPersongrunnlag(mockk(), mockk(), mockk(), mockk()) {
            override fun utførStegOgAngiNeste(
                behandling: Behandling,
                data: RegistrerPersongrunnlagDTO,
            ): StegType {
                return StegType.VILKÅRSVURDERING
            }

            override fun stegType(): StegType {
                return StegType.REGISTRERE_PERSONGRUNNLAG
            }
        }
}
