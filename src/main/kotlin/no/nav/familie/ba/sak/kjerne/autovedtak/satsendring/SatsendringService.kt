package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import org.springframework.stereotype.Service

@Service
class SatsendringService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService
) {
    fun erFagsakOppdatertMedSisteSats(fagsakId: Long): Boolean {
        // Må se på siste iverksatte og ikke siste vedtatte siden vi ønsker å se på
        // den forrige behandlingen som sendte noe til økonomi
        val sisteIverksatteBehandlingId =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId)?.id

        return sisteIverksatteBehandlingId == null ||
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(sisteIverksatteBehandlingId)
                .erOppdatertMedSisteSatsForAlleSatstyper()
    }
}
