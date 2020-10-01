package no.nav.familie.ba.sak.nare

import javax.persistence.AttributeConverter
import javax.persistence.Converter


interface EvalueringÅrsak {

    fun hentBeskrivelse(): String

    fun hentMetrikkBeskrivelse(): String

    fun hentIdentifikator(): String
}


data class Evaluering(
        val resultat: Resultat,
        val evalueringÅrsaker: List<EvalueringÅrsak>,
        val begrunnelse: String = "",
        val beskrivelse: String = "",
        val identifikator: String = "",
        val operator: Operator = Operator.INGEN,
        var children: List<Evaluering> = emptyList()) {

    infix fun og(other: Evaluering) = Evaluering(
            resultat = resultat og other.resultat,
            evalueringÅrsaker = evalueringÅrsaker + other.evalueringÅrsaker,
            operator = Operator.OG,
            children = this.specOrChildren() + other.specOrChildren()
    )

    infix fun eller(other: Evaluering) = Evaluering(
            resultat = resultat eller other.resultat,
            evalueringÅrsaker = evalueringÅrsaker + other.evalueringÅrsaker,
            operator = Operator.ELLER,
            children = this.specOrChildren() + other.specOrChildren()
    )

    fun ikke() = Evaluering(
            resultat = resultat.ikke(),
            evalueringÅrsaker = evalueringÅrsaker,
            operator = Operator.IKKE,
            children = listOf(this)
    )

    private fun specOrChildren(): List<Evaluering> =
            if (identifikator.isBlank() && children.isNotEmpty()) children else listOf(this)

    companion object {

        fun ja(evalueringÅrsak: EvalueringÅrsak) = Evaluering(Resultat.JA, listOf(evalueringÅrsak))

        fun nei(evalueringÅrsak: EvalueringÅrsak) = Evaluering(Resultat.NEI, listOf(evalueringÅrsak))

        fun kanskje(evalueringÅrsak: EvalueringÅrsak) = Evaluering(Resultat.KANSKJE, listOf(evalueringÅrsak))

        fun evaluer(identifikator: String,
                    beskrivelse: String,
                    eval: Evaluering) = eval.copy(identifikator = identifikator,
                                                  beskrivelse = beskrivelse)
    }

}

enum class Operator {
    OG, ELLER, IKKE, INGEN
}
