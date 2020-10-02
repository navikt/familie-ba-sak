package no.nav.familie.ba.sak.nare

enum class Resultat {
    JA {

        override infix fun og(other: Resultat): Resultat = other
        override infix fun eller(other: Resultat): Resultat = JA
        override fun ikke(): Resultat = NEI
    },

    NEI {

        override infix fun og(other: Resultat): Resultat = NEI
        override infix fun eller(other: Resultat): Resultat = other
        override fun ikke(): Resultat = JA
    },

    KANSKJE {

        override infix fun og(other: Resultat): Resultat = if (other == JA) KANSKJE else other
        override infix fun eller(other: Resultat): Resultat = if (other == NEI) KANSKJE else other
        override fun ikke(): Resultat = KANSKJE
    };

    abstract infix fun og(other: Resultat): Resultat
    abstract infix fun eller(other: Resultat): Resultat
    abstract fun ikke(): Resultat
}