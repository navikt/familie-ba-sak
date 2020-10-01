package no.nav.familie.ba.sak.beregning.domene

import java.time.LocalDate

data class Sats(val type: SatsType, val beløp:Int, val gyldigFom:LocalDate = LocalDate.MIN, val gyldigTom: LocalDate = LocalDate.MAX)

enum class SatsType(val beskrivelse: String) {
    ORBA("Ordinær barnetrygd"),
    SMA("Småbarnstillegg"),
    TILLEGG_ORBA("Tillegg til barnetrygd for barn 0-6 år"),
    FINN_SVAL("Finnmark- og Svalbardtillegg")
}