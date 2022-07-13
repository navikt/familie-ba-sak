package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import org.springframework.stereotype.Service

@Service
class TilkjentYtelseHentOgPersiserService(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) {

    fun hentTilkjentYtelseForBehandling(behandlingId: Long) =
        tilkjentYtelseRepository.findByBehandlingOptional(behandlingId)

    fun hentTilkjentYtelseForBehandlingThrows(behandlingId: Long) =
        tilkjentYtelseRepository.findByBehandlingOptional(behandlingId)
            ?: throw Feil("Fant ikke tilkjent ytelse for behandling=$behandlingId")
}
