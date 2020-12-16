package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.beregning.domene.*
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate


@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-pdl", "mock-arbeidsfordeling")
@Tag("integration")
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class KonsistensavstemmingUtplukkingIntegrationTest {

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository

    @Autowired
    private lateinit var databaseCleanupService: DatabaseCleanupService

    @AfterEach
    @BeforeEach
    fun cleanUp() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Skal plukke iverksatt FGB`() {
        val forelderIdent = randomFnr()

        fagsakService.hentEllerOpprettFagsakForPersonIdent(forelderIdent).also {
            fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE)
        }
        val førstegangsbehandling =
                opprettOgLagreBehandlingMedAndeler(personIdent = forelderIdent,
                                                   kildeOgOffsetPåAndeler = listOf(KildeOgOffsetPåAndel(null, 1L)))

        val gjeldendeBehandlinger = behandlingRepository.finnBehandlingerMedLøpendeAndel()

        Assertions.assertEquals(1, gjeldendeBehandlinger.size)
        Assertions.assertEquals(førstegangsbehandling.id, gjeldendeBehandlinger[0])
    }

    @Test
    fun `Skal plukke både iverksatt FGB og revurdering når periode legges til`() {
        val forelderIdent = randomFnr()

        fagsakService.hentEllerOpprettFagsakForPersonIdent(forelderIdent).also {
            fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE)
        }

        val førstegangsbehandling =
                opprettOgLagreBehandlingMedAndeler(personIdent = forelderIdent,
                                                   kildeOgOffsetPåAndeler = listOf(KildeOgOffsetPåAndel(null, 1L)),
                                                   medStatus = BehandlingStatus.AVSLUTTET)
        val revurdering =
                opprettOgLagreRevurderingMedAndeler(personIdent = forelderIdent,
                                                    kildeOgOffsetPåAndeler = listOf(
                                                            KildeOgOffsetPåAndel(førstegangsbehandling.id, 1L),
                                                            KildeOgOffsetPåAndel(null, 2L)))

        val gjeldendeBehandlinger = behandlingRepository.finnBehandlingerMedLøpendeAndel()

        Assertions.assertEquals(2, gjeldendeBehandlinger.size)
        Assertions.assertEquals(førstegangsbehandling.id, gjeldendeBehandlinger[0])
        Assertions.assertEquals(revurdering.id, gjeldendeBehandlinger[1])
    }

    @Test
    fun `Skal kun plukke revurdering når periode på førstegangsbehandling blir erstattet`() {
        val forelderIdent = randomFnr()

        fagsakService.hentEllerOpprettFagsakForPersonIdent(forelderIdent).also {
            fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE)
        }
        opprettOgLagreBehandlingMedAndeler(personIdent = forelderIdent,
                                           kildeOgOffsetPåAndeler = listOf(KildeOgOffsetPåAndel(null, 1L)),
                                           medStatus = BehandlingStatus.AVSLUTTET)
        val revurdering =
                opprettOgLagreRevurderingMedAndeler(personIdent = forelderIdent,
                                                    kildeOgOffsetPåAndeler = listOf(KildeOgOffsetPåAndel(null, 2L)))


        val gjeldendeBehandlinger = behandlingRepository.finnBehandlingerMedLøpendeAndel()

        Assertions.assertEquals(1, gjeldendeBehandlinger.size)
        Assertions.assertEquals(revurdering.id, gjeldendeBehandlinger[0])
    }

    @Test
    fun `Skal ikke plukke noe ved opphør`() {
        val forelderIdent = randomFnr()

        fagsakService.hentEllerOpprettFagsakForPersonIdent(forelderIdent).also {
            fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE)
        }

        opprettOgLagreBehandlingMedAndeler(personIdent = forelderIdent,
                                           kildeOgOffsetPåAndeler = listOf(KildeOgOffsetPåAndel(null, 1L)),
                                           medStatus = BehandlingStatus.AVSLUTTET)
        opprettOgLagreRevurderingMedAndeler(personIdent = forelderIdent,
                                            kildeOgOffsetPåAndeler = emptyList())

        val gjeldendeBehandlinger = behandlingRepository.finnBehandlingerMedLøpendeAndel()

        Assertions.assertTrue(gjeldendeBehandlinger.isEmpty())
    }

    @Test
    fun `Skal ikke plukke behandling som ikke er iverksatt`() {
        val forelderIdent = randomFnr()

        fagsakService.hentEllerOpprettFagsakForPersonIdent(forelderIdent).also {
            fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE)
        }
        val iverksattBehandling =
                opprettOgLagreBehandlingMedAndeler(personIdent = forelderIdent,
                                                   kildeOgOffsetPåAndeler = listOf(KildeOgOffsetPåAndel(null, 1L)),
                                                   medStatus = BehandlingStatus.AVSLUTTET)

        opprettOgLagreRevurderingMedAndeler(personIdent = forelderIdent,
                                            kildeOgOffsetPåAndeler = listOf(KildeOgOffsetPåAndel(null, 2L)),
                                            erIverksatt = false)

        val gjeldendeBehandlinger = behandlingRepository.finnBehandlingerMedLøpendeAndel()

        Assertions.assertEquals(1, gjeldendeBehandlinger.size)
        Assertions.assertEquals(iverksattBehandling.id, gjeldendeBehandlinger[0])
    }

    private fun opprettOgLagreBehandlingMedAndeler(personIdent: String,
                                                   kildeOgOffsetPåAndeler: List<KildeOgOffsetPåAndel> = emptyList(),
                                                   erIverksatt: Boolean = true,
                                                   medStatus: BehandlingStatus = BehandlingStatus.UTREDES): Behandling {
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(personIdent))
        behandling.status = medStatus
        behandlingService.lagreEllerOppdater(behandling)
        val tilkjentYtelse = tilkjentYtelse(behandling = behandling, erIverksatt = erIverksatt)
        tilkjentYtelseRepository.save(tilkjentYtelse)
        kildeOgOffsetPåAndeler.forEach {
            andelTilkjentYtelseRepository.save(andelPåTilkjentYtelse(tilkjentYtelse = tilkjentYtelse,
                                                                     kildeBehandlingId = it.kilde ?: behandling.id,
                                                                     periodeOffset = it.offset))
        }
        return behandling
    }

    private fun opprettOgLagreRevurderingMedAndeler(personIdent: String,
                                                    kildeOgOffsetPåAndeler: List<KildeOgOffsetPåAndel> = emptyList(),
                                                    erIverksatt: Boolean = true): Behandling {
        val behandling = behandlingService.opprettBehandling(nyRevurdering(personIdent))
        val tilkjentYtelse = tilkjentYtelse(behandling = behandling, erIverksatt = erIverksatt)
        tilkjentYtelseRepository.save(tilkjentYtelse)
        kildeOgOffsetPåAndeler.forEach {
            andelTilkjentYtelseRepository.save(andelPåTilkjentYtelse(tilkjentYtelse = tilkjentYtelse,
                                                                     kildeBehandlingId = it.kilde ?: behandling.id,
                                                                     periodeOffset = it.offset))
        }
        return behandling
    }

    private fun tilkjentYtelse(behandling: Behandling, erIverksatt: Boolean) = TilkjentYtelse(behandling = behandling,
                                                                                              opprettetDato = LocalDate.now(),
                                                                                              endretDato = LocalDate.now(),
                                                                                              utbetalingsoppdrag = if (erIverksatt) "Skal ikke være null" else null)

    // Kun offset og kobling til behandling/tilkjent ytelse som er relevant når man skal plukke ut til konsistensavstemming
    private fun andelPåTilkjentYtelse(tilkjentYtelse: TilkjentYtelse,
                                      kildeBehandlingId: Long,
                                      periodeOffset: Long) = AndelTilkjentYtelse(personIdent = randomFnr(),
                                                                                 behandlingId = tilkjentYtelse.behandling.id,
                                                                                 tilkjentYtelse = tilkjentYtelse,
                                                                                 beløp = 1054,
                                                                                 stønadFom = LocalDate.now()
                                                                                         .minusMonths(12)
                                                                                         .toYearMonth(),
                                                                                 stønadTom = LocalDate.now()
                                                                                         .plusMonths(12)
                                                                                         .toYearMonth(),
                                                                                 type = YtelseType.ORDINÆR_BARNETRYGD,
                                                                                 kildeBehandlingId = kildeBehandlingId,
                                                                                 periodeOffset = periodeOffset,
                                                                                 forrigePeriodeOffset = null
    )
}

data class KildeOgOffsetPåAndel(
        val kilde: Long?, // Hvis denne er null setter vi til behandling som opprettes, for å unngå loop-avhengighet
        val offset: Long)