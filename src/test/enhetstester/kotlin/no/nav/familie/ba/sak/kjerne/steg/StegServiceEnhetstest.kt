package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.SatsendringService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class StegServiceEnhetstest {

    private val behandlingService: BehandlingService = mockk()
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val settPåVentService: SettPåVentService = mockk()
    private val satsendringService: SatsendringService = mockk()
    private val simuleringService: SimuleringService = mockk()

    private val stegService = StegService(
        steg = listOf(mockRegistrerPersongrunnlag()),
        fagsakService = mockk(),
        behandlingService = behandlingService,
        behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        beregningService = mockk(),
        søknadGrunnlagService = mockk(),
        tilgangService = mockk(relaxed = true),
        infotrygdFeedService = mockk(),
        settPåVentService = settPåVentService,
        satsendringService = satsendringService,
        simuleringService = simuleringService
    )

    @BeforeEach
    fun setup() {
        val behandling = lagBehandling()
        every { behandlingService.opprettBehandling(any()) } returns behandling
        every { behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(any(), any()) } returns behandling
        every { behandlingHentOgPersisterService.hent(any()) } returns behandling
        every { settPåVentService.finnAktivSettPåVentPåBehandling(any()) } returns null
    }

    @Test
    fun `skal feile validering av helmanuell migrering når fagsak har aktivt vedtak som IKKE var teknisk endring med opphør`() {
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = any()) } returns
            lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE, resultat = Behandlingsresultat.INNVILGET)

        assertThrows<FunksjonellFeil>(message = "Det finnes allerede en vedtatt behandling på fagsak") {
            stegService.håndterNyBehandling(
                NyBehandling(
                    kategori = BehandlingKategori.NASJONAL,
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                    behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                    behandlingÅrsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
                    søkersIdent = randomFnr(),
                    barnasIdenter = listOf(randomFnr()),
                    nyMigreringsdato = LocalDate.now().minusMonths(6),
                    fagsakId = 1L
                )
            )
        }
    }

    @Test
    fun `skal IKKE feile validering av helmanuell migrering når fagsak har aktivt vedtak som var teknisk endring med opphør`() {
        listOf(Behandlingsresultat.OPPHØRT, Behandlingsresultat.ENDRET_OG_OPPHØRT).forEach {
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = any()) } returns
                lagBehandling(årsak = BehandlingÅrsak.TEKNISK_ENDRING, resultat = it)

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
                        fagsakId = 1L
                    )
                )
            }
        }
    }

    @Test
    fun `Skal feile dersom vi har en gammel sats på forrige iverksatte behandling på endre migreringsdato behandling`() {
        every { satsendringService.erFagsakOppdatertMedSisteSatser(any()) } returns false

        val nyBehandling = NyBehandling(
            kategori = BehandlingKategori.NASJONAL,
            underkategori = BehandlingUnderkategori.ORDINÆR,
            behandlingType = BehandlingType.REVURDERING,
            behandlingÅrsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
            søkersIdent = randomFnr(),
            barnasIdenter = listOf(randomFnr()),
            nyMigreringsdato = LocalDate.now().minusMonths(6),
            fagsakId = 1L
        )

        assertThrows<FunksjonellFeil> { stegService.håndterNyBehandling(nyBehandling) }
    }

    private fun mockRegistrerPersongrunnlag() = object : RegistrerPersongrunnlag(mockk(), mockk(), mockk(), mockk()) {
        override fun utførStegOgAngiNeste(behandling: Behandling, data: RegistrerPersongrunnlagDTO): StegType {
            return StegType.VILKÅRSVURDERING
        }

        override fun stegType(): StegType {
            return StegType.REGISTRERE_PERSONGRUNNLAG
        }
    }
}
