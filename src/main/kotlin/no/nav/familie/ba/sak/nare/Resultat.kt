package no.nav.familie.ba.sak.nare

enum class Resultat {
    OPPFYLT {

        override infix fun og(other: Resultat): Resultat = other
        override infix fun eller(other: Resultat): Resultat = OPPFYLT
        override fun ikke(): Resultat = IKKE_OPPFYLT
    },

    IKKE_OPPFYLT {

        override infix fun og(other: Resultat): Resultat = IKKE_OPPFYLT
        override infix fun eller(other: Resultat): Resultat = other
        override fun ikke(): Resultat = OPPFYLT
    },

    IKKE_VURDERT {

        override infix fun og(other: Resultat): Resultat = if (other == OPPFYLT) IKKE_VURDERT else other
        override infix fun eller(other: Resultat): Resultat = if (other == IKKE_OPPFYLT) IKKE_VURDERT else other
        override fun ikke(): Resultat = IKKE_VURDERT
    };

    abstract infix fun og(other: Resultat): Resultat
    abstract infix fun eller(other: Resultat): Resultat
    abstract fun ikke(): Resultat
}

enum class RestResultat {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_VURDERT,
    JA,
    NEI,
    KANSKJE
}

fun Resultat.tilRestResultat(): RestResultat {
    return when(this) {
        Resultat.OPPFYLT -> RestResultat.JA
        Resultat.IKKE_OPPFYLT -> RestResultat.NEI
        Resultat.IKKE_VURDERT -> RestResultat.KANSKJE
    }
}

fun RestResultat.tilResultat(): Resultat {
    return when(this) {
        RestResultat.OPPFYLT -> Resultat.OPPFYLT
        RestResultat.JA -> Resultat.OPPFYLT
        RestResultat.IKKE_OPPFYLT -> Resultat.IKKE_OPPFYLT
        RestResultat.NEI -> Resultat.IKKE_OPPFYLT
        RestResultat.KANSKJE -> Resultat.IKKE_VURDERT
        RestResultat.IKKE_VURDERT -> Resultat.IKKE_VURDERT
    }
}