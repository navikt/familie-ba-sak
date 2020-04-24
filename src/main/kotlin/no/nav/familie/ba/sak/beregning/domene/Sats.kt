package no.nav.familie.ba.sak.beregning.domene

import org.springframework.stereotype.Service
import java.time.LocalDate
import javax.persistence.*

data class Sats(val type: SatsType, val beløp:Int, val gyldigFom:LocalDate = LocalDate.MIN, val gyldigTom: LocalDate = LocalDate.MAX)

@Service
object SatsRegister : SatsRepository {

    val satser = listOf(
            Sats(SatsType.ORBA, 1054, LocalDate.of(2019,3,1), LocalDate.MAX),
            Sats(SatsType.ORBA, 970, LocalDate.MIN, LocalDate.of(2019,2,28)),
            Sats(SatsType.SMA, 660,LocalDate.MIN,LocalDate.MAX),
            Sats(SatsType.TILLEGG_ORBA, 1354, LocalDate.of(2020,9,1),LocalDate.MAX),
            Sats(SatsType.FINN_SVAL, 1054, LocalDate.MIN, LocalDate.of(2014,3,31))
    )

    override fun finnAlleSatserFor(type: SatsType): List<Sats> =
            satser.filter { it.type==type }
}

enum class SatsType(val beskrivelse: String) {
    ORBA("Ordinær barnetrygd"),
    SMA("Småbarnstillegg"),
    TILLEGG_ORBA("Tillegg til barnetrygd for barn 0-6 år"),
    FINN_SVAL("Finnmark- og Svalbardtillegg")
}