package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.kontrakter.felles.kodeverk.BeskrivelseDto
import no.nav.familie.kontrakter.felles.kodeverk.BetydningDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkSpråk
import java.time.LocalDate
import java.time.Month

val POL_EØS_FOM = LocalDate.of(2004, Month.MAY, 1)
val GBR_EØS_FOM = LocalDate.of(1900, Month.JANUARY, 1)
val GBR_EØS_TOM = LocalDate.of(2020, Month.DECEMBER, 31)
val DEU_EØS_FOM = LocalDate.of(1900, Month.JANUARY, 1)
val DNK_EØS_FOM = LocalDate.of(1990, Month.JANUARY, 1)
val TOM_UENDELIG = LocalDate.of(9999, Month.DECEMBER, 31)

fun lagKodeverkLand(): KodeverkDto {
    val beskrivelsePolen = BeskrivelseDto("POL", "")
    val betydningPolen =
        BetydningDto(
            POL_EØS_FOM,
            TOM_UENDELIG,
            mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelsePolen),
        )
    val beskrivelseTyskland = BeskrivelseDto("DEU", "")
    val betydningTyskland =
        BetydningDto(
            DEU_EØS_FOM,
            TOM_UENDELIG,
            mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseTyskland),
        )
    val beskrivelseDanmark = BeskrivelseDto("DNK", "")
    val betydningDanmark =
        BetydningDto(
            DNK_EØS_FOM,
            TOM_UENDELIG,
            mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseDanmark),
        )
    val beskrivelseUK = BeskrivelseDto("GBR", "")
    val betydningUK =
        BetydningDto(
            GBR_EØS_FOM,
            GBR_EØS_TOM,
            mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseUK),
        )

    return KodeverkDto(
        betydninger =
            mapOf(
                "POL" to listOf(betydningPolen),
                "DEU" to listOf(betydningTyskland),
                "DNK" to listOf(betydningDanmark),
                "GBR" to listOf(betydningUK),
            ),
    )
}
