package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.SatsendringService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.Satskjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.task.OpprettTaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class StegServiceTest {
    private val behandlingService: BehandlingService = mockk()
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val satsendringService: SatsendringService = mockk()
    private val opprettTaskService: OpprettTaskService = mockk()
    private val satskjøringRepository: SatskjøringRepository = mockk()
    private val unleashService: UnleashNextMedContextService = mockk()
    private val tilgangService: TilgangService = mockk()

    private val stegService =
        StegService(
            steg = listOf(mockRegistrerPersongrunnlag()),
            fagsakService = mockk(),
            behandlingService = behandlingService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            beregningService = mockk(),
            søknadGrunnlagService = mockk(),
            tilgangService = tilgangService,
            infotrygdFeedService = mockk(),
            satsendringService = satsendringService,
            personopplysningerService = mockk(),
            automatiskBeslutningService = mockk(),
            opprettTaskService = opprettTaskService,
            satskjøringRepository = satskjøringRepository,
            unleashService = unleashService,
        )

    @BeforeEach
    fun setup() {
        every { tilgangService.validerTilgangTilBehandling(any(), any()) } just runs
        every { tilgangService.verifiserHarTilgangTilHandling(any(), any()) } just runs
    }

    @Nested
    inner class HåndterNyBehandlingTest {
        @Test
        fun `skal ikke feile validering av helmanuell migrering når fagsak har aktivt vedtak som er et opphør`() {
            // Arrange
            val foreldre = randomAktør()
            val barn = randomAktør()

            val forrigeBehandling = lagBehandling()

            val nyBehandling =
                NyBehandling(
                    kategori = BehandlingKategori.NASJONAL,
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                    behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                    behandlingÅrsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
                    søkersIdent = foreldre.aktivFødselsnummer(),
                    barnasIdenter = listOf(barn.aktivFødselsnummer()),
                    nyMigreringsdato = LocalDate.now().minusMonths(6),
                    fagsakId = 1L,
                )

            val opprettetBehandling =
                lagBehandling(
                    fagsak =
                        lagFagsak(
                            id = nyBehandling.fagsakId,
                            aktør = foreldre,
                        ),
                    behandlingKategori = nyBehandling.kategori!!,
                    underkategori = nyBehandling.underkategori!!,
                    behandlingType = nyBehandling.behandlingType,
                    årsak = nyBehandling.behandlingÅrsak,
                )

            every { behandlingHentOgPersisterService.hent(opprettetBehandling.id) } returns opprettetBehandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(nyBehandling.fagsakId) } returns forrigeBehandling
            every { behandlingService.erLøpende(forrigeBehandling) } returns false
            every { behandlingService.opprettBehandling(nyBehandling) } returns opprettetBehandling
            every { behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(opprettetBehandling.id, any()) } returns opprettetBehandling

            // Act
            val håndtertNyBehandling = stegService.håndterNyBehandling(nyBehandling)

            // Assert
            assertThat(håndtertNyBehandling.id).isNotNull()
            assertThat(håndtertNyBehandling.fagsak.id).isEqualTo(1L)
            assertThat(håndtertNyBehandling.behandlingStegTilstand).hasSize(1)
            assertThat(håndtertNyBehandling.resultat).isEqualTo(Behandlingsresultat.IKKE_VURDERT)
            assertThat(håndtertNyBehandling.type).isEqualTo(BehandlingType.MIGRERING_FRA_INFOTRYGD)
            assertThat(håndtertNyBehandling.opprettetÅrsak).isEqualTo(BehandlingÅrsak.HELMANUELL_MIGRERING)
            assertThat(håndtertNyBehandling.skalBehandlesAutomatisk).isFalse()
            assertThat(håndtertNyBehandling.kategori).isEqualTo(nyBehandling.kategori)
            assertThat(håndtertNyBehandling.underkategori).isEqualTo(nyBehandling.underkategori)
            assertThat(håndtertNyBehandling.aktiv).isTrue()
            assertThat(håndtertNyBehandling.status).isEqualTo(BehandlingStatus.UTREDES)
            assertThat(håndtertNyBehandling.overstyrtEndringstidspunkt).isNull()
            assertThat(håndtertNyBehandling.aktivertTidspunkt).isNotNull()
            assertThat(håndtertNyBehandling.opprettetAv).isEqualTo(SikkerhetContext.SYSTEM_FORKORTELSE)
            assertThat(håndtertNyBehandling.opprettetTidspunkt).isNotNull()
            assertThat(håndtertNyBehandling.endretAv).isEqualTo(SikkerhetContext.SYSTEM_FORKORTELSE)
            assertThat(håndtertNyBehandling.endretTidspunkt).isNotNull()
            assertThat(håndtertNyBehandling.versjon).isEqualTo(0L)
            assertThat(håndtertNyBehandling.steg).isEqualTo(StegType.REGISTRERE_PERSONGRUNNLAG)
        }

        @Test
        fun `skal feile validering av helmanuell migrering når fagsak har aktivt vedtak med løpende utbetalinger`() {
            // Arrange
            val nyBehandling =
                NyBehandling(
                    kategori = BehandlingKategori.NASJONAL,
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                    behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                    behandlingÅrsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
                    søkersIdent = randomFnr(),
                    barnasIdenter = listOf(randomFnr()),
                    nyMigreringsdato = LocalDate.now().minusMonths(6),
                    fagsakId = 1L,
                )

            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(nyBehandling.fagsakId) } returns lagBehandling()
            every { behandlingService.erLøpende(any()) } returns true

            // Act & assert
            val exception = assertThrows<FunksjonellFeil> { stegService.håndterNyBehandling(nyBehandling) }
            assertThat(exception.message).isEqualTo("Det finnes allerede en vedtatt behandling med løpende utbetalinger på fagsak 1.Behandling kan ikke opprettes med årsak Manuell migrering")
        }

        @Test
        fun `skal kaste feil dersom behandlingsårsak er IVERKSETTE_KA_VEDTAK og toggle ikke er skrudd på`() {
            // Arrange
            val nyBehandling =
                NyBehandling(
                    søkersIdent = randomFnr(),
                    behandlingType = BehandlingType.REVURDERING,
                    behandlingÅrsak = BehandlingÅrsak.IVERKSETTE_KA_VEDTAK,
                    fagsakId = 1L,
                )

            every { unleashService.isEnabled(FeatureToggle.KAN_OPPRETTE_REVURDERING_MED_ÅRSAK_IVERKSETTE_KA_VEDTAK) } returns false

            // Act & assert
            val exception = assertThrows<FunksjonellFeil> { stegService.håndterNyBehandling(nyBehandling) }
            assertThat(exception.melding).isEqualTo("Det er ikke mulig å opprette behandling med årsak Iverksette KA-vedtak")
        }
    }

    @Nested
    inner class OpprettSatsendringTaskTest {
        @Test
        fun `Skal feile og trigge satsendring dersom vi har en gammel sats på forrige iverksatte behandling på endre migreringsdato behandling`() {
            // Arrange
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

            every { satsendringService.erFagsakOppdatertMedSisteSatser(nyBehandling.fagsakId) } returns false
            every { satskjøringRepository.findByFagsakIdAndSatsTidspunkt(nyBehandling.fagsakId, any()) } returns null
            every { opprettTaskService.opprettSatsendringTask(nyBehandling.fagsakId, any()) } just runs

            // Act & assert
            val exception = assertThrows<FunksjonellFeil> { stegService.håndterNyBehandling(nyBehandling) }
            assertThat(exception.melding).isEqualTo("Fagsaken har ikke siste sats. Det har automatisk blitt opprettet en behandling for satsendring. Vent til den er ferdig behandlet før du endrer migreringsdato.")
            verify(exactly = 1) { opprettTaskService.opprettSatsendringTask(any(), any()) }
        }

        @Test
        fun `Skal ikke trigge ny satsendring dersom vi har en gammel sats på forrige iverksatte behandling på endre migreringsdato behandling, og satsendring allerede er trigget`() {
            // Arrange
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

            every { satsendringService.erFagsakOppdatertMedSisteSatser(nyBehandling.fagsakId) } returns false
            every { satskjøringRepository.findByFagsakIdAndSatsTidspunkt(nyBehandling.fagsakId, any()) } returns Satskjøring(fagsakId = 1, startTidspunkt = LocalDateTime.now(), satsTidspunkt = YearMonth.now())

            // Act & assert
            val exception = assertThrows<FunksjonellFeil> { stegService.håndterNyBehandling(nyBehandling) }
            assertThat(exception.melding).isEqualTo("Det kjøres satsendring på fagsaken. Vennligst prøv igjen senere")
            verify(exactly = 0) { opprettTaskService.opprettSatsendringTask(any(), any()) }
        }
    }

    @Nested
    inner class HåndterPersongrunnlagTest {
        @Test
        fun `skal feile dersom behandlingen er satt på vent`() {
            // Arange
            val behandling = lagBehandling(status = BehandlingStatus.SATT_PÅ_VENT)
            val grunnlag = RegistrerPersongrunnlagDTO("123", emptyList())

            // Act & assert
            val exception = assertThrows<FunksjonellFeil> { stegService.håndterPersongrunnlag(behandling, grunnlag) }
            assertThat(exception.message).isEqualTo("System prøver å utføre steg REGISTRERE_PERSONGRUNNLAG på behandling ${behandling.id} som er på vent.")
        }
    }

    private fun mockRegistrerPersongrunnlag() =
        object : RegistrerPersongrunnlag(
            behandlingHentOgPersisterService = mockk(),
            vilkårsvurderingForNyBehandlingService = mockk(),
            personopplysningGrunnlagForNyBehandlingService = mockk(),
            eøsSkjemaerForNyBehandlingService = mockk(),
        ) {
            override fun utførStegOgAngiNeste(
                behandling: Behandling,
                data: RegistrerPersongrunnlagDTO,
            ): StegType = StegType.VILKÅRSVURDERING

            override fun stegType(): StegType = StegType.REGISTRERE_PERSONGRUNNLAG
        }
}
