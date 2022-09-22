package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse

data class OffsetOppdatering(
    val beståendeAndelSomSkalHaOppdatertOffset: AndelTilkjentYtelse,
    val periodeOffset: Long?,
    val forrigePeriodeOffset: Long?,
    val kildeBehandlingId: Long?
) {
    fun oppdater() {
        beståendeAndelSomSkalHaOppdatertOffset.periodeOffset = periodeOffset
        beståendeAndelSomSkalHaOppdatertOffset.forrigePeriodeOffset = forrigePeriodeOffset
        beståendeAndelSomSkalHaOppdatertOffset.kildeBehandlingId = kildeBehandlingId
    }
}
