package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering.AutovedtakMånedligValutajusteringService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.beregning.SatsTidspunkt
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValidering
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.RegistrerPersongrunnlag
import no.nav.familie.ba.sak.kjerne.steg.RegistrerPersongrunnlagDTO
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.task.MånedligValutajusteringTaskDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.YearMonth

class AutovedtakMånedligValutajusteringServiceTest(
    @Autowired private val autovedtakMånedligValutajusteringService: AutovedtakMånedligValutajusteringService,
    @Autowired private val jdbcTemplate: JdbcTemplate,
    @Autowired private val mockLocalDateService: LocalDateService,
    @Autowired private val databaseCleanupService: DatabaseCleanupService,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingService: BehandlingService,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val autovedtakSatsendringService: AutovedtakSatsendringService,
    @Autowired private val satskjøringRepository: SatskjøringRepository,
    @Autowired private val settPåVentService: SettPåVentService,
    @Autowired private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    @Autowired private val personidentService: PersonidentService,
    @Autowired private val registrerPersongrunnlag: RegistrerPersongrunnlag,
) : AbstractSpringIntegrationTest() {
    private lateinit var fagsak: Fagsak
    private lateinit var aktørBarn: Aktør

    @BeforeEach
    fun setUp() {
        databaseCleanupService.truncate()

        // Vilkårsvurdering og andeler tilkjent ytelse blir ikke generert i disse testene. Validering av andeler ved satsendring vil derfor kaste feil. For at testene for sett på vent skal fungere skrur vi her av denne valideringen.
        mockkObject(TilkjentYtelseValidering)

        every { mockLocalDateService.now() } returns LocalDate.now().minusYears(6) andThen LocalDate.now()
        fagsak = opprettLøpendeFagsak()
        aktørBarn = personidentService.hentOgLagreAktør(randomFnr(), true)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SatsTidspunkt)
        unmockkObject(TilkjentYtelseValidering)
    }

    @Nested
    inner class ÅpenBehandling {
        @Test
        fun `skal kjøre månedlig valutajustering`() {
            val behandling = opprettBehandling()
            lagTilkjentAndelOgFerdigstillBehandling(behandling)

            val autovedtak = autovedtakMånedligValutajusteringService.utførMånedligValutajustering(MånedligValutajusteringTaskDto(behandling.id, YearMonth.of(2023, 3)))
        }
    }

    private fun opprettBehandling(): Behandling {
        val behandling =
            behandlingService.opprettBehandling(
                NyBehandling(
                    fagsakId = fagsak.id,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    behandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                    kategori = BehandlingKategori.NASJONAL,
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                    søkersIdent = fagsak.aktør.aktivFødselsnummer(),
                ),
            )
        registrerPersongrunnlag.utførStegOgAngiNeste(
            behandling,
            RegistrerPersongrunnlagDTO(fagsak.aktør.aktivFødselsnummer(), listOf(aktørBarn.aktivFødselsnummer())),
        )
        return behandling
    }

    private fun lagTilkjentAndelOgFerdigstillBehandling(behandling: Behandling) {
        behandling.status = BehandlingStatus.AVSLUTTET
        val avsluttetSteg =
            BehandlingStegTilstand(
                behandling = behandling,
                behandlingSteg = StegType.BEHANDLING_AVSLUTTET,
            )
        behandling.behandlingStegTilstand.add(avsluttetSteg)
        with(lagInitiellTilkjentYtelse(behandling, "utbetalingsoppdrag")) {
            val andel =
                lagAndelTilkjentYtelse(
                    // Tidspunkt før siste satsendring
                    fom = YearMonth.of(2021, 1),
                    tom =
                        YearMonth.of(
                            2026,
                            5,
                        ),
                    // Tidspunkt etter siste satsendring. Dersom tom er før siste satsendring vil alle testene feile.
                    behandling = behandling,
                    beløp = 10,
                    aktør = aktørBarn,
                    tilkjentYtelse = this,
                )
            andelerTilkjentYtelse.add(andel)
            tilkjentYtelseRepository.saveAndFlush(this)
        }
        behandlingRepository.saveAndFlush(behandling)
    }

    private fun opprettLøpendeFagsak(): Fagsak {
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        return fagsakService.lagre(fagsak.copy(status = FagsakStatus.LØPENDE))
    }
}
