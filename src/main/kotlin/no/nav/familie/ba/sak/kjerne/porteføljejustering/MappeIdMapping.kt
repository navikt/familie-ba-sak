package no.nav.familie.ba.sak.kjerne.porteføljejustering

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet.OSLO
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet.VADSØ

private val mappeIdSteinkjerTilOsloOgVadsø =
    mapOf(
        100027793L to mapOf(OSLO.enhetsnummer to 100012753L, VADSØ.enhetsnummer to 100012691L),
        100027794L to mapOf(OSLO.enhetsnummer to 100012782L, VADSØ.enhetsnummer to 100012692L),
        100027795L to mapOf(OSLO.enhetsnummer to 100012754L, VADSØ.enhetsnummer to 100012693L),
        100027796L to mapOf(OSLO.enhetsnummer to 100012783L, VADSØ.enhetsnummer to 100012721L),
        100027797L to mapOf(OSLO.enhetsnummer to 100012755L, VADSØ.enhetsnummer to 100012694L),
        100027798L to mapOf(OSLO.enhetsnummer to 100012785L, VADSØ.enhetsnummer to 100012695L),
        100033711L to mapOf(OSLO.enhetsnummer to 100033712L, VADSØ.enhetsnummer to 100033731L),
        100034010L to mapOf(OSLO.enhetsnummer to 100033910L, VADSØ.enhetsnummer to 100032890L),
        100034011L to mapOf(OSLO.enhetsnummer to 100032451L, VADSØ.enhetsnummer to 100030295L),
        100033711L to mapOf(OSLO.enhetsnummer to 100033712L, VADSØ.enhetsnummer to 100033731L),
    )

fun hentMappeIdHosOsloEllerVadsøSomTilsvarerMappeISteinkjer(
    mappeIdSteinkjer: Long?,
    annenEnhet: String,
): Long? =
    mappeIdSteinkjer?.let {
        mappeIdSteinkjerTilOsloOgVadsø
            .getOrElse(mappeIdSteinkjer) { throw Feil("Finner ikke mappe id $mappeIdSteinkjer i mapping") }
            .getOrElse(annenEnhet) { throw Feil("Enhet $annenEnhet finnes ikke i mapping") }
    }
