package no.nav.familie.ba.sak.behandling.fagsak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.beregning.domene.*
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
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
    fun cleanUp() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Skal plukke iverksatt FGB`() {
        val forelderIdent = randomFnr()

        fagsakService.hentEllerOpprettFagsakForPersonIdent(forelderIdent).also { fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE) }
        val behandling = lagBehandlingMedAndeler(personIdent = forelderIdent, offsetPåAndeler = listOf(1L))

        val gjeldendeBehandlinger =
                behandlingRepository.finnBehandlingerMedLøpendeAndel() // TODO: Begynte å teste på servicenivå, men da må man holde personer i sync også for å generere oppdragsid. For å slippe å dra inne personopplysningsgrunnlagrepo og personrepo valideres det mot repo og det er også nærmest det vi vil teste.

        Assertions.assertEquals(1, gjeldendeBehandlinger.size)
        Assertions.assertEquals(behandling.id, gjeldendeBehandlinger[0])
    }

    @Test
    fun `Skal plukke både iverksatt FGB og revurdering når periode legges til`() {
        val forelderIdent = randomFnr()

        fagsakService.hentEllerOpprettFagsakForPersonIdent(forelderIdent).also { fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE) }
        val behandling1 = lagBehandlingMedAndeler(personIdent = forelderIdent, offsetPåAndeler = listOf(1L))
        val behandling2 = lagBehandlingMedAndeler(personIdent = forelderIdent, offsetPåAndeler = listOf(1L, 2L))

        val gjeldendeBehandlinger =
                behandlingRepository.finnBehandlingerMedLøpendeAndel()

        Assertions.assertEquals(2, gjeldendeBehandlinger.size)
        Assertions.assertEquals(behandling1.id, gjeldendeBehandlinger[0])
        Assertions.assertEquals(behandling2.id, gjeldendeBehandlinger[1])
    }

    @Test
    fun `Skal kun plukke revurdering når periode på førstegangsbehandling blir erstattet`() {
        val forelderIdent = randomFnr()

        fagsakService.hentEllerOpprettFagsakForPersonIdent(forelderIdent).also { fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE) }
        val behandling1 = lagBehandlingMedAndeler(personIdent = forelderIdent, offsetPåAndeler = listOf(1L))
        val behandling2 = lagBehandlingMedAndeler(personIdent = forelderIdent, offsetPåAndeler = listOf(2L))

        val gjeldendeBehandlinger =
                behandlingRepository.finnBehandlingerMedLøpendeAndel()

        Assertions.assertEquals(1, gjeldendeBehandlinger.size)
        Assertions.assertEquals(behandling2.id, gjeldendeBehandlinger[0])
    }

    @Test
    fun `Skal ikke plukke noe ved opphør`() {
        val forelderIdent = randomFnr()

        fagsakService.hentEllerOpprettFagsakForPersonIdent(forelderIdent).also { fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE) }
        val behandling1 = lagBehandlingMedAndeler(personIdent = forelderIdent, offsetPåAndeler = listOf(1L))
        val behandling2 = lagBehandlingMedAndeler(personIdent = forelderIdent, offsetPåAndeler = emptyList())

        val gjeldendeBehandlinger =
                behandlingRepository.finnBehandlingerMedLøpendeAndel()

        Assertions.assertTrue(gjeldendeBehandlinger.isEmpty()) // TODO: Fix, finnes en andel er
    }

    @Test
    fun `Skal ikke plukke behandling som ikke er iverksatt`() {
        val forelderIdent = randomFnr()

        fagsakService.hentEllerOpprettFagsakForPersonIdent(forelderIdent).also { fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE) }
        val behandling1 = lagBehandlingMedAndeler(personIdent = forelderIdent, offsetPåAndeler = listOf(1L))
        val behandling2 = lagBehandlingMedAndeler(personIdent = forelderIdent, offsetPåAndeler = listOf(2L), erIverksatt = false)

        val gjeldendeBehandlinger =
                behandlingRepository.finnBehandlingerMedLøpendeAndel()

        Assertions.assertEquals(1, gjeldendeBehandlinger.size)
        Assertions.assertEquals(behandling1.id, gjeldendeBehandlinger[0])
    }

    private fun lagBehandlingMedAndeler(personIdent: String,
                                        offsetPåAndeler: List<Long> = emptyList(),
                                        erIverksatt: Boolean = true): Behandling {
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(personIdent))
        val tilkjentYtelse = tilkjentYtelse(behandling = behandling, erIverksatt = erIverksatt)
        tilkjentYtelseRepository.save(tilkjentYtelse)
        offsetPåAndeler.forEach {
            andelTilkjentYtelseRepository.save(andelPåTilkjentYtelse(tilkjentYtelse = tilkjentYtelse,
                                                                     periodeOffset = it))
        }
        return behandling
    }

    // TODO: Datoer vil være relevant å sette når vi skal teste toggling av LØPENDE-flagg, men mulig dette bør gjøres i en egen?
    private fun tilkjentYtelse(behandling: Behandling, erIverksatt: Boolean) = TilkjentYtelse(behandling = behandling,
                                                                                              opprettetDato = LocalDate.now(),
                                                                                              endretDato = LocalDate.now(),
                                                                                              utbetalingsoppdrag = if (erIverksatt) "Skal ikke være null" else null)

    // Kun offset og kobling til behandling og tilkjent ytelse som er relevant når man skal plukke ut til konsistensavstemming
    private fun andelPåTilkjentYtelse(tilkjentYtelse: TilkjentYtelse,
                                      periodeOffset: Long) = AndelTilkjentYtelse(personIdent = randomFnr(),
                                                                                 behandlingId = tilkjentYtelse.behandling.id,
                                                                                 tilkjentYtelse = tilkjentYtelse,
                                                                                 beløp = 1054,
                                                                                 stønadFom = LocalDate.now(),
                                                                                 stønadTom = LocalDate.now(),
                                                                                 type = YtelseType.ORDINÆR_BARNETRYGD,
                                                                                 periodeOffset = periodeOffset,
                                                                                 forrigePeriodeOffset = null
    )

}