package no.nav.familie.ba.sak.kjerne.porteføljejustering

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet.OSLO
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet.VADSØ

private val mappeIdSteinkjerTilOsloOgVadsø =
    mapOf(
        100027793 to mapOf(OSLO.enhetsnummer to 100012753, VADSØ.enhetsnummer to 100012691),
        100027794 to mapOf(OSLO.enhetsnummer to 100012782, VADSØ.enhetsnummer to 100012692),
        100027795 to mapOf(OSLO.enhetsnummer to 100012754, VADSØ.enhetsnummer to 100012693),
        100027796 to mapOf(OSLO.enhetsnummer to 100012783, VADSØ.enhetsnummer to 100012721),
        100027797 to mapOf(OSLO.enhetsnummer to 100012755, VADSØ.enhetsnummer to 100012694),
        100027798 to mapOf(OSLO.enhetsnummer to 100012785, VADSØ.enhetsnummer to 100012695),
        100033711 to mapOf(OSLO.enhetsnummer to 100033712, VADSØ.enhetsnummer to 100033731),
        100034010 to mapOf(OSLO.enhetsnummer to 100033910, VADSØ.enhetsnummer to 100032890),
        100034011 to mapOf(OSLO.enhetsnummer to 100032451, VADSØ.enhetsnummer to 100030295),
        100033711 to mapOf(OSLO.enhetsnummer to 100033712, VADSØ.enhetsnummer to 100033731),
    )

fun hentMappeIdHosOsloEllerVadsøSomTilsvarerMappeISteinkjer(
    mappeIdSteinkjer: Int,
    annenEnhet: String,
): Int =
    mappeIdSteinkjerTilOsloOgVadsø
        .getOrElse(mappeIdSteinkjer) { throw Feil("Finner ikke mappe id $mappeIdSteinkjer i mapping") }
        .getOrElse(annenEnhet) { throw Feil("Enhet $annenEnhet finnes ikke i mapping") }
