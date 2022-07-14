package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import org.springframework.stereotype.Service

@Service
class TilkjentYtelseHentOgPersiserService(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) {

    fun hentTilkjentYtelseForBehandling(behandlingId: Long) =
        tilkjentYtelseRepository.findByBehandling(behandlingId)

    fun hentTilkjentYtelseForBehandlingThrows(behandlingId: Long) =
        tilkjentYtelseRepository.findByBehandling(behandlingId)
            ?: throw Feil("Fant ikke tilkjent ytelse for behandling=$behandlingId")

    fun lagre(tilkjentYtelse: TilkjentYtelse) =
        tilkjentYtelseRepository.save(tilkjentYtelse)

    fun slett(tilkjentYtelse: TilkjentYtelse) =
        tilkjentYtelseRepository.save(tilkjentYtelse)

    fun slettTilkjentYtelseFor(behandling: Behandling) =
        tilkjentYtelseRepository.slettTilkjentYtelseFor(behandling)

    fun lagreOgFlush(tilkjentYtelse: TilkjentYtelse) =
        tilkjentYtelseRepository.saveAndFlush(tilkjentYtelse)

    fun hentForBehandlingOgHarUtbetalingsoppdrag(behandlingId: Long) =
        tilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(behandlingId)
}
