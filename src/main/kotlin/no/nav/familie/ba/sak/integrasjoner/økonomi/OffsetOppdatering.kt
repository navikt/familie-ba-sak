package no.nav.familie.ba.sak.integrasjoner.økonomi

data class OffsetOppdatering(
    val beståendeAndelSomSkalHaOppdatertOffset: AndelTilkjentYtelseForUtbetalingsoppdrag,
    val periodeOffset: Long?,
    val forrigePeriodeOffset: Long?,
    val kildeBehandlingId: Long?
) {
    fun oppdater() {
        beståendeAndelSomSkalHaOppdatertOffset.periodeOffset = periodeOffset
        beståendeAndelSomSkalHaOppdatertOffset.forrigePeriodeOffset = forrigePeriodeOffset
        beståendeAndelSomSkalHaOppdatertOffset.kildeBehandlingId = kildeBehandlingId
    }

    fun erGyldigOppdatering() =
        beståendeAndelSomSkalHaOppdatertOffset.periodeOffset != periodeOffset ||
            beståendeAndelSomSkalHaOppdatertOffset.forrigePeriodeOffset != forrigePeriodeOffset ||
            beståendeAndelSomSkalHaOppdatertOffset.kildeBehandlingId != kildeBehandlingId
}
