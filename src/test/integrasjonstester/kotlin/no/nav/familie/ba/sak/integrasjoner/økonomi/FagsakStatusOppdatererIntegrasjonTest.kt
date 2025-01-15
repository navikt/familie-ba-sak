package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.nyOrdinærBehandling
import randomAktør
import randomFnr
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

class FagsakStatusOppdatererIntegrasjonTest : AbstractSpringIntegrationTest() {
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

    @BeforeEach
    fun cleanUp() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `ikke oppdater status på fagsaker som er løpende og har løpende utbetalinger`() {
        val forelderIdent = randomFnr()

        val fagsakOriginal =
            fagsakService.hentEllerOpprettFagsakForPersonIdent(forelderIdent).also {
                fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE)
            }
        opprettOgLagreBehandlingMedAndeler(
            personIdent = forelderIdent,
            offsetPåAndeler = listOf(1L),
            fagsakId = fagsakOriginal.id,
            andelProsent = BigDecimal(100),
            andelKalkulertUtbetalingsbeløp = 1054,
        )

        val fagsak = fagsakService.hentLøpendeFagsaker()

        Assertions.assertTrue(fagsak.any { it.id == fagsakOriginal.id })

        fagsakService.oppdaterLøpendeStatusPåFagsaker()
        val fagsak2 = fagsakService.hentLøpendeFagsaker()

        Assertions.assertTrue(fagsak2.any { it.id == fagsakOriginal.id })
    }

    @Test
    fun `skal sette status til avsluttet hvis ingen løpende utbetalinger`() {
        val forelderIdent = randomFnr()

        val fagsakOriginal =
            fagsakService.hentEllerOpprettFagsakForPersonIdent(forelderIdent).also {
                fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE)
            }
        val førstegangsbehandling =
            opprettOgLagreBehandlingMedAndeler(
                personIdent = forelderIdent,
                offsetPåAndeler = listOf(1L),
                medStatus = BehandlingStatus.AVSLUTTET,
                fagsakId = fagsakOriginal.id,
                andelProsent = BigDecimal(100),
                andelKalkulertUtbetalingsbeløp = 1054,
            )

        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(førstegangsbehandling.id)

        tilkjentYtelse.stønadTom = LocalDate.now().minusMonths(1).toYearMonth()
        tilkjentYtelseRepository.save(tilkjentYtelse)

        fagsakService.oppdaterLøpendeStatusPåFagsaker()
        val fagsak = fagsakService.hentLøpendeFagsaker()

        Assertions.assertFalse(fagsak.any { it.id == fagsakOriginal.id })
    }

    @Test
    fun `skal sette status til avsluttet hvis alle løpende andeler er satt til 0 grunnet endret utbetalingsandeler`() {
        val forelderIdent = randomFnr()

        val fagsakOriginal =
            fagsakService.hentEllerOpprettFagsakForPersonIdent(forelderIdent).also {
                fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE)
            }
        opprettOgLagreBehandlingMedAndeler(
            personIdent = forelderIdent,
            offsetPåAndeler = listOf(1L),
            medStatus = BehandlingStatus.AVSLUTTET,
            fagsakId = fagsakOriginal.id,
            andelProsent = BigDecimal(0),
            andelKalkulertUtbetalingsbeløp = 0,
        )

        fagsakService.oppdaterLøpendeStatusPåFagsaker()
        val fagsak = fagsakService.hentLøpendeFagsaker()

        Assertions.assertFalse(fagsak.any { it.id == fagsakOriginal.id })
    }

    @Test
    fun `skal ikke sette status til avsluttet hvis alle løpende andeler er satt til 0 grunnet nullutbetaling`() {
        val forelderIdent = randomFnr()

        val fagsakOriginal =
            fagsakService.hentEllerOpprettFagsakForPersonIdent(forelderIdent).also {
                fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE)
            }
        opprettOgLagreBehandlingMedAndeler(
            personIdent = forelderIdent,
            offsetPåAndeler = listOf(1L),
            medStatus = BehandlingStatus.AVSLUTTET,
            fagsakId = fagsakOriginal.id,
            andelProsent = BigDecimal(100),
            andelKalkulertUtbetalingsbeløp = 0,
        )

        fagsakService.oppdaterLøpendeStatusPåFagsaker()
        val fagsak = fagsakService.hentLøpendeFagsaker()

        Assertions.assertTrue(fagsak.any { it.id == fagsakOriginal.id })
    }

    private fun opprettOgLagreBehandlingMedAndeler(
        personIdent: String,
        offsetPåAndeler: List<Long> = emptyList(),
        erIverksatt: Boolean = true,
        medStatus: BehandlingStatus = BehandlingStatus.UTREDES,
        fagsakId: Long,
        andelProsent: BigDecimal,
        andelKalkulertUtbetalingsbeløp: Int,
    ): Behandling {
        val behandling =
            behandlingService.opprettBehandling(nyOrdinærBehandling(søkersIdent = personIdent, fagsakId = fagsakId))
        behandling.status = medStatus
        behandlingRepository.save(behandling)
        val tilkjentYtelse = tilkjentYtelse(behandling = behandling, erIverksatt = erIverksatt)
        tilkjentYtelseRepository.save(tilkjentYtelse)
        offsetPåAndeler.forEach {
            andelTilkjentYtelseRepository.save(
                andelPåTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelse,
                    periodeOffset = it,
                    aktør = behandling.fagsak.aktør,
                    andelProsent = andelProsent,
                    andelKalkulertUtbetalingsbeløp = andelKalkulertUtbetalingsbeløp,
                ),
            )
        }
        return behandling
    }

    private fun tilkjentYtelse(
        behandling: Behandling,
        erIverksatt: Boolean,
    ) = TilkjentYtelse(
        behandling = behandling,
        opprettetDato = LocalDate.now(),
        endretDato = LocalDate.now(),
        utbetalingsoppdrag = if (erIverksatt) "Skal ikke være null" else null,
    )

    // Kun offset og kobling til behandling/tilkjent ytelse som er relevant når man skal plukke ut til konsistensavstemming
    private fun andelPåTilkjentYtelse(
        tilkjentYtelse: TilkjentYtelse,
        periodeOffset: Long,
        aktør: Aktør = randomAktør(),
        andelProsent: BigDecimal,
        andelKalkulertUtbetalingsbeløp: Int,
    ) = AndelTilkjentYtelse(
        aktør = aktør,
        behandlingId = tilkjentYtelse.behandling.id,
        tilkjentYtelse = tilkjentYtelse,
        kalkulertUtbetalingsbeløp = andelKalkulertUtbetalingsbeløp,
        nasjonaltPeriodebeløp = andelKalkulertUtbetalingsbeløp,
        stønadFom =
            LocalDate
                .now()
                .minusMonths(12)
                .toYearMonth(),
        stønadTom =
            LocalDate
                .now()
                .plusMonths(12)
                .toYearMonth(),
        type = YtelseType.ORDINÆR_BARNETRYGD,
        periodeOffset = periodeOffset,
        forrigePeriodeOffset = null,
        sats = andelKalkulertUtbetalingsbeløp,
        prosent = andelProsent,
    )
}
