package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.tilgangsmaskin.Avvisningskode

data class PersonTilgang(
    val personIdent: String,
    val avvisning: Avvisning?,
) {
    val harTilgang: Boolean get() = avvisning == null

    fun erAvvistGrunnetStrengtFortrolig(): Boolean = avvisning?.avvisningskode in AVVISNINGSKODER_STRENGT_FORTROLIG

    override fun toString(): String = "PersonTilgang(personIdent=${"*".repeat(personIdent.length)}, avvisning=$avvisning)"

    data class Avvisning(
        val avvisningskode: Avvisningskode,
        val begrunnelse: String,
    )

    companion object {
        private val AVVISNINGSKODER_STRENGT_FORTROLIG =
            setOf(Avvisningskode.AVVIST_STRENGT_FORTROLIG_ADRESSE, Avvisningskode.AVVIST_STRENGT_FORTROLIG_UTLAND)

        fun medTilgang(personIdent: String) = PersonTilgang(personIdent = personIdent, avvisning = null)

        fun avvist(
            personIdent: String,
            avvisningskode: Avvisningskode,
            begrunnelse: String,
        ) = PersonTilgang(personIdent = personIdent, avvisning = Avvisning(avvisningskode = avvisningskode, begrunnelse = begrunnelse))
    }
}
