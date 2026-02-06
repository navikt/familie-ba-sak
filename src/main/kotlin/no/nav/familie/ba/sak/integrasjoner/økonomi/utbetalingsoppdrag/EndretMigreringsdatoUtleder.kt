package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingMigreringsinfoRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.jsonMapper
import org.springframework.stereotype.Component
import java.time.YearMonth

@Component
class EndretMigreringsdatoUtleder(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingMigreringsinfoRepository: BehandlingMigreringsinfoRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) {
    fun utled(
        fagsak: Fagsak,
        forrigeTilkjentYtelse: TilkjentYtelse?,
        erSatsendring: Boolean = false,
    ): YearMonth? {
        val førsteAndelFomDatoForrigeBehandling = forrigeTilkjentYtelse?.andelerTilkjentYtelse?.minOfOrNull { it.stønadFom } ?: return null

        val behandlingerIFagsak =
            behandlingHentOgPersisterService
                .hentBehandlinger(fagsak.id)
                .filter { !it.erHenlagt() }

        val migreringsBehandlingerIFagsak = behandlingerIFagsak.filter { it.type == BehandlingType.MIGRERING_FRA_INFOTRYGD }

        val førsteBehandlingErMigrering = behandlingerIFagsak.minBy { it.aktivertTidspunkt }.type == BehandlingType.MIGRERING_FRA_INFOTRYGD

        // Dersom det det ikke finnes noen migreringsbehandlinger i fagsak eller det kun finnes 1, og dette er den første behandlingen, vil det ikke være behov for å opphøre fra migreringsdato.
        if (migreringsBehandlingerIFagsak.isEmpty() || (migreringsBehandlingerIFagsak.size <= 1 && førsteBehandlingErMigrering)) {
            return null
        }

        // Dersom det ikke finnes noen lagret BehandlingMigreringsInfo tilknyttet fagsak har vi ingen dato å opphøre fra
        val behandlingMigreringsinfo = behandlingMigreringsinfoRepository.finnSisteBehandlingMigreringsInfoPåFagsak(fagsakId = fagsak.id) ?: return null

        // Plusser på 1 mnd på migreringsdato da barnetrygden kun skal løpe fra BA-sak tidligst mnd etter migrering.
        val migreringsdatoPåFagsakPlussEnMnd = behandlingMigreringsinfo.migreringsdato.plusMonths(1)
        if (!erSatsendring && migreringsdatoPåFagsakPlussEnMnd.toYearMonth().isAfter(førsteAndelFomDatoForrigeBehandling)) {
            throw FunksjonellFeil(
                "Ny migreringsdato pluss 1 mnd kan ikke være etter første fom i forrige behandling",
                "Migreringsdato i fagsak er lagt til å være etter en måned med utbetaling fra en behandling som ikke kommer fra infotrygd.",
            )
        }

        // Sjekker om vi har opphørt fra migreringsdato pluss 1 mnd i en av behandlingene etter at migreringsdato sist ble endret.
        // Har vi opphørt fra denne datoen tidligere trenger vi ikke å gjøre det igjen.
        val fagsakOpphørtFraMigreringsdatoIEnAvBehandlingeneEtterMigreringsdatoBleEndret =
            tilkjentYtelseRepository
                .findByFagsak(fagsakId = fagsak.id)
                .filter { it.behandling.aktivertTidspunkt > behandlingMigreringsinfo.endretTidspunkt && it.utbetalingsoppdrag != null }
                .map { jsonMapper.readValue(it.utbetalingsoppdrag, Utbetalingsoppdrag::class.java) }
                // Viktig at vi omgjør til YearMonth før sammenligning her da vi alltid bruker YearMonth for endretMigreringsdato inn i utbetalingsgenerator
                .any { utbetalingsoppdrag -> utbetalingsoppdrag.utbetalingsperiode.any { utbetalingsperiode -> utbetalingsperiode.opphør?.opphørDatoFom?.toYearMonth() == migreringsdatoPåFagsakPlussEnMnd.toYearMonth() } }

        return if (fagsakOpphørtFraMigreringsdatoIEnAvBehandlingeneEtterMigreringsdatoBleEndret) {
            null
        } else if (førsteAndelFomDatoForrigeBehandling.isAfter(migreringsdatoPåFagsakPlussEnMnd.toYearMonth())) {
            migreringsdatoPåFagsakPlussEnMnd.toYearMonth()
        } else {
            null
        }
    }
}
