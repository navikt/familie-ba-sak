package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse

data class OffsetOppdatering(
    val beståendeIOppdatert: AndelTilkjentYtelse,
    val periodeOffset: Long?,
    val forrigePeriodeOffset: Long?,
    val kildeBehandlingId: Long?
) {
    fun oppdater() {
        beståendeIOppdatert.periodeOffset = periodeOffset
        beståendeIOppdatert.forrigePeriodeOffset = forrigePeriodeOffset
        beståendeIOppdatert.kildeBehandlingId = kildeBehandlingId
    }
}