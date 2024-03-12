package no.nav.familie.ba.sak.common

const val ALFANUMERISKE_TEGN = "a-zæøåA-ZÆØÅ0-9"

fun String.saniter(): String = Regex("[^$ALFANUMERISKE_TEGN]+").replace(this, "")
