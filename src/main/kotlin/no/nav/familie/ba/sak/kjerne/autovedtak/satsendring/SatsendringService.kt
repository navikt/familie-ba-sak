package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class SatsendringService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val satskjøringRepository: SatskjøringRepository
) {
    fun erFagsakOppdatertMedSisteSatser(fagsakId: Long): Boolean {
        // Må se på siste iverksatte og ikke siste vedtatte siden vi ønsker å se på
        // den forrige behandlingen som sendte noe til økonomi
        val sisteIverksatteBehandlingId =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId)?.id

        return sisteIverksatteBehandlingId == null ||
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(sisteIverksatteBehandlingId)
                .erOppdatertMedSisteSatser()
    }

    fun finnSatskjøringerSomHarStoppetPgaÅpenBehandling(antall: Int): List<Pair<Long, Long>> =
        satskjøringRepository.finnSatskjøringerSomHarStoppetPgaÅpenBehandling(Pageable.ofSize(antall))
}
