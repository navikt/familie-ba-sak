package no.nav.familie.ba.sak.common

import java.text.NumberFormat
import java.util.*

val nbLocale = Locale("nb", "Norway")

object Utils {
    fun slåSammen(values: List<String>): String = Regex("(.*),").replace(values.joinToString(", "), "$1 og")

    fun formaterBeløp(beløp: Int): String = NumberFormat.getNumberInstance(nbLocale).format(beløp)
}