package no.nav.familie.ba.sak.common

object Utils {
    fun slåSammen(values: List<String>) = Regex("(.*),").replace(values.joinToString(", "), "$1 og")
}