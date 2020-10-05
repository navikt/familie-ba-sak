package no.nav.familie.ba.sak.nare

import no.nav.familie.ba.sak.nare.Evaluering.Companion.evaluer


data class Spesifikasjon<T>(
        val beskrivelse: String,
        val identifikator: String = "",
        val children: List<Spesifikasjon<T>> = emptyList(),
        val implementasjon: T.() -> Evaluering) {

    fun evaluer(t: T): Evaluering {
        return evaluer(
                beskrivelse = beskrivelse,
                identifikator = identifikator,
                eval = t.implementasjon())
    }

    infix fun og(other: Spesifikasjon<T>): Spesifikasjon<T> {
        return Spesifikasjon(
                beskrivelse = "$beskrivelse OG ${other.beskrivelse}",
                children = this.specOrChildren() + other.specOrChildren(),
                implementasjon = { evaluer(this) og other.evaluer(this) }
        )
    }

    infix fun eller(other: Spesifikasjon<T>): Spesifikasjon<T> {
        return Spesifikasjon(
                beskrivelse = "$beskrivelse ELLER ${other.beskrivelse}",
                children = this.specOrChildren() + other.specOrChildren(),
                implementasjon = { evaluer(this) eller other.evaluer(this) }
        )
    }

    fun ikke(): Spesifikasjon<T> {
        return Spesifikasjon(
                beskrivelse = "!$beskrivelse",
                identifikator = "!$identifikator",
                children = listOf(this),
                implementasjon = { evaluer(this).ikke() }
        )
    }

    fun med(identifikator: String, beskrivelse: String): Spesifikasjon<T> {
        return this.copy(identifikator = identifikator, beskrivelse = beskrivelse)
    }

    private fun specOrChildren(): List<Spesifikasjon<T>> =
            if (identifikator.isBlank() && children.isNotEmpty()) children else listOf(this)

}

fun <T> ikke(spec: Spesifikasjon<T>) = spec.ikke()