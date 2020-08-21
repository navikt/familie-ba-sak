package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import java.text.NumberFormat
import java.util.*

val nbLocale = Locale("nb", "Norway")

object Utils {
    fun slåSammen(values: List<String>): String = Regex("(.*),").replace(values.joinToString(", "), "$1 og")

    fun formaterBeløp(beløp: Int): String = NumberFormat.getNumberInstance(nbLocale).format(beløp)

    // TODO: Tilpasset fastsettelse av BehandlingResultatType inntil støtte for delvis innvilgelse.
    //  Fastsettelse nedenfor løser enkelte steder generering av utbetalingsoppdrag til økonomi, men det vil fortsatt se rart ut
    //  frontend og i database vil det bli satt opphørsdato på TilkjentYtelse-nivå frem til støtte for delvis.
    fun midlertidigUtledBehandlingResultatType(hentetBehandlingResultatType: BehandlingResultatType) =
            when {
                (hentetBehandlingResultatType == BehandlingResultatType.OPPHØRT) -> BehandlingResultatType.DELVIS_INNVILGET
                else -> hentetBehandlingResultatType
            }
}

fun <E> List<E>.slåSammenMedKommaOgOg(): String {
    return when (this.size > 1) {
        false -> this.firstOrNull()?.toString() ?: ""
        true -> this.subList(0, this.size - 1)
                .joinToString(postfix = " og ")
                .plus(this.last())
    }
}