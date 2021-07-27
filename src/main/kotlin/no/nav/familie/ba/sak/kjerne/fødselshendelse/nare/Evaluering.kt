package no.nav.familie.ba.sak.kjerne.fødselshendelse.nare

import no.nav.familie.kontrakter.felles.objectMapper


interface EvalueringÅrsak {

    fun hentBeskrivelse(): String

    fun hentMetrikkBeskrivelse(): String

    fun hentIdentifikator(): String
}


data class Evaluering(
        val resultat: Resultat,
        val evalueringÅrsaker: List<EvalueringÅrsak>,
        val begrunnelse: String,
        val beskrivelse: String = "",
        val identifikator: String = "",
        val operator: Operator = Operator.INGEN,
        var children: List<Evaluering> = emptyList()) {

    fun toJson(): String =
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)

    infix fun og(other: Evaluering) = Evaluering(
            resultat = resultat og other.resultat,
            evalueringÅrsaker = evalueringÅrsaker + other.evalueringÅrsaker,
            begrunnelse = "($begrunnelse OG ${other.begrunnelse})",
            operator = Operator.OG,
            children = this.specOrChildren() + other.specOrChildren()
    )

    infix fun eller(other: Evaluering) = Evaluering(
            resultat = resultat eller other.resultat,
            evalueringÅrsaker = evalueringÅrsaker + other.evalueringÅrsaker,
            begrunnelse = "($begrunnelse ELLER ${other.begrunnelse})",
            operator = Operator.ELLER,
            children = this.specOrChildren() + other.specOrChildren()
    )

    fun ikke() = Evaluering(
            resultat = resultat.ikke(),
            evalueringÅrsaker = evalueringÅrsaker,
            begrunnelse = "(IKKE $begrunnelse)",
            operator = Operator.IKKE,
            children = listOf(this)
    )

    private fun specOrChildren(): List<Evaluering> =
            if (identifikator.isBlank() && children.isNotEmpty()) children else listOf(this)

    companion object {

        fun oppfylt(evalueringÅrsak: EvalueringÅrsak) = Evaluering(Resultat.OPPFYLT,
                                                                   listOf(evalueringÅrsak),
                                                                   evalueringÅrsak.hentBeskrivelse())

        fun ikkeOppfylt(evalueringÅrsak: EvalueringÅrsak) = Evaluering(Resultat.IKKE_OPPFYLT,
                                                                       listOf(evalueringÅrsak),
                                                                       evalueringÅrsak.hentBeskrivelse())

        fun ikkeVurdert(evalueringÅrsak: EvalueringÅrsak) = Evaluering(Resultat.IKKE_VURDERT,
                                                                       listOf(evalueringÅrsak),
                                                                       evalueringÅrsak.hentBeskrivelse())

        fun evaluer(identifikator: String,
                    beskrivelse: String,
                    eval: Evaluering) = eval.copy(identifikator = identifikator,
                                                  beskrivelse = beskrivelse)
    }

}

enum class Operator {
    OG, ELLER, IKKE, INGEN
}
