package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.fagsak.FagsakRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
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
class FagsakStatusOppdatererIntegrasjonTest {

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository


    @Autowired
    private lateinit var databaseCleanupService: DatabaseCleanupService

    @AfterEach
    fun cleanUp() {
        databaseCleanupService.truncate()
    }


    @Test
    fun `ikke oppdater status på fagsaker som er løpende og har løpende utbetalinger`() {
        val forelderIdent = randomFnr()

        //GHA rydder ikke opp etter tidligere tester så postgres har mange fagsaker liggende...
        //rydder opp manuellt
        fagsakRepository.deleteAll()

        fagsakService.hentEllerOpprettFagsakForPersonIdent(forelderIdent).also {
            fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE)
        }
        val førstegangsbehandling = opprettOgLagreBehandlingMedAndeler(personIdent = forelderIdent, offsetPåAndeler = listOf(1L))

        val fagsak = fagsakService.hentLøpendeFagsaker()

        Assertions.assertEquals(1, fagsak.size)

        fagsakService.oppdaterLøpendeStatusPåFagsaker()
        val fagsak2 = fagsakService.hentLøpendeFagsaker()

        Assertions.assertEquals(1, fagsak2.size)

    }

    @Test
    fun `skal sette status til avsluttet hvis ingen løpende utbetalinger`() {
        val forelderIdent = randomFnr()


        //GHA rydder ikke opp etter tidligere tester så postgres har mange fagsaker liggende...
        //rydder opp manuellt
        fagsakRepository.deleteAll()

        fagsakService.hentEllerOpprettFagsakForPersonIdent(forelderIdent).also {
            fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE)
        }
        val førstegangsbehandling = opprettOgLagreBehandlingMedAndeler(personIdent = forelderIdent, offsetPåAndeler = listOf(1L))


        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(førstegangsbehandling.id)

        tilkjentYtelse.stønadTom = LocalDate.now().minusMonths(1).toYearMonth()
        tilkjentYtelseRepository.save(tilkjentYtelse)

        fagsakService.oppdaterLøpendeStatusPåFagsaker()
        val fagsak = fagsakService.hentLøpendeFagsaker()

        Assertions.assertEquals(0, fagsak.size)

    }


    private fun opprettOgLagreBehandlingMedAndeler(personIdent: String,
                                                   offsetPåAndeler: List<Long> = emptyList(),
                                                   erIverksatt: Boolean = true,
                                                   medStatus: BehandlingStatus = BehandlingStatus.UTREDES): Behandling {
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(personIdent))
        behandling.status = medStatus
        behandlingService.lagreEllerOppdater(behandling)
        val tilkjentYtelse = tilkjentYtelse(behandling = behandling, erIverksatt = erIverksatt)
        tilkjentYtelseRepository.save(tilkjentYtelse)
        offsetPåAndeler.forEach {
            andelTilkjentYtelseRepository.save(andelPåTilkjentYtelse(tilkjentYtelse = tilkjentYtelse,
                    periodeOffset = it))
        }
        return behandling
    }

    private fun opprettOgLagreRevurderingMedAndeler(personIdent: String,
                                                    offsetPåAndeler: List<Long> = emptyList(),
                                                    erIverksatt: Boolean = true): Behandling {
        val behandling = behandlingService.opprettBehandling(nyRevurdering(personIdent))
        val tilkjentYtelse = tilkjentYtelse(behandling = behandling, erIverksatt = erIverksatt)
        tilkjentYtelseRepository.save(tilkjentYtelse)
        offsetPåAndeler.forEach {
            andelTilkjentYtelseRepository.save(andelPåTilkjentYtelse(tilkjentYtelse = tilkjentYtelse,
                    periodeOffset = it))
        }
        return behandling
    }

    private fun tilkjentYtelse(behandling: Behandling, erIverksatt: Boolean) = TilkjentYtelse(behandling = behandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now(),
            utbetalingsoppdrag = if (erIverksatt) "Skal ikke være null" else null)

    // Kun offset og kobling til behandling/tilkjent ytelse som er relevant når man skal plukke ut til konsistensavstemming
    private fun andelPåTilkjentYtelse(tilkjentYtelse: TilkjentYtelse,
                                      periodeOffset: Long) = AndelTilkjentYtelse(personIdent = randomFnr(),
            behandlingId = tilkjentYtelse.behandling.id,
            tilkjentYtelse = tilkjentYtelse,
            beløp = 1054,
            stønadFom = LocalDate.now().minusMonths(12).toYearMonth(),
            stønadTom = LocalDate.now().plusMonths(12).toYearMonth(),
            type = YtelseType.ORDINÆR_BARNETRYGD,
            periodeOffset = periodeOffset,
            forrigePeriodeOffset = null
    )
}