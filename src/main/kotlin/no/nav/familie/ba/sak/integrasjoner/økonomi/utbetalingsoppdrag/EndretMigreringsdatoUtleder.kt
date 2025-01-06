package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.stereotype.Component
import java.time.YearMonth

@Component
class EndretMigreringsdatoUtleder(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) {
    fun utled(
        fagsak: Fagsak,
        forrigeTilkjentYtelse: TilkjentYtelse?,
    ): YearMonth? {
        val førsteAndelFomDatoForrigeBehandling = forrigeTilkjentYtelse?.andelerTilkjentYtelse?.minOfOrNull { it.stønadFom } ?: return null

        val erMigrertSak =
            behandlingHentOgPersisterService
                .hentBehandlinger(fagsak.id)
                .any { it.type == BehandlingType.MIGRERING_FRA_INFOTRYGD }

        if (!erMigrertSak) {
            return null
        }

        val migreringsdatoPåFagsak = behandlingService.hentMigreringsdatoPåFagsak(fagsakId = fagsak.id) ?: return null

        // Plusser på 1 mnd på migreringsdato da barnetrygden kun skal løpe fra BA-sak tidligst mnd etter migrering.
        val migreringsdatoPåFagsakPlussEnMnd = migreringsdatoPåFagsak.toYearMonth().plusMonths(1)
        if (migreringsdatoPåFagsakPlussEnMnd.isAfter(førsteAndelFomDatoForrigeBehandling)) {
            throw IllegalStateException("Ny migreringsdato pluss 1 mnd kan ikke være etter første fom i forrige behandling")
        }

        val harOpphørtFraMigreringsdatoTidligere =
            tilkjentYtelseRepository
                .findByFagsak(fagsak.id)
                .map { objectMapper.readValue(it.utbetalingsoppdrag, Utbetalingsoppdrag::class.java) }
                .any { utbetalingsoppdrag -> utbetalingsoppdrag.utbetalingsperiode.any { utbetalingsperiode -> utbetalingsperiode.opphør?.opphørDatoFom?.toYearMonth() == migreringsdatoPåFagsakPlussEnMnd } }

        return if (harOpphørtFraMigreringsdatoTidligere) {
            null
        } else {
            migreringsdatoPåFagsakPlussEnMnd
        }
    }
}
