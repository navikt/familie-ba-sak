package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ArrayNode
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevBegrunnelseGrunnlagMedPersoner
import no.nav.familie.ba.sak.kjerne.brev.domene.RestBehandlingsgrunnlagForBrev
import javax.persistence.AttributeConverter
import javax.persistence.Converter

interface IVedtakBegrunnelse {

    val sanityApiNavn: String
    val vedtakBegrunnelseType: VedtakBegrunnelseType
    val kanDelesOpp: Boolean

    fun delOpp(
        restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
        triggesAv: TriggesAv,
        periode: NullablePeriode
    ): List<BrevBegrunnelseGrunnlagMedPersoner>

    fun enumnavnTilString(): String
}

class IVedtakBegrunnelseDeserializer : StdDeserializer<List<IVedtakBegrunnelse>>(List::class.java) {
    override fun deserialize(jsonParser: JsonParser?, p1: DeserializationContext?): List<IVedtakBegrunnelse> {
        val node: ArrayNode = jsonParser!!.codec.readTree(jsonParser)
        return node
            .map { it.asText() }
            .map { it.fraEnumNavnTilEnumVerdi() }
    }
}

private fun String.fraEnumNavnTilEnumVerdi(): IVedtakBegrunnelse {
    val splittet = split('$')
    val type = splittet.get(0)
    val enumNavn = splittet.get(1)
    return when (type) {
        EØSStandardbegrunnelse::class.simpleName -> EØSStandardbegrunnelse.valueOf(enumNavn)
        Standardbegrunnelse::class.simpleName -> Standardbegrunnelse.valueOf(enumNavn)
        else -> throw Feil("Fikk en begrunnelse med ugyldig type: hverken EØSStandardbegrunnelse eller Standardbegrunnelse: $this")
    }
}

@Converter
class IVedtakBegrunnelseListConverter :
    AttributeConverter<List<IVedtakBegrunnelse>, String> {

    override fun convertToDatabaseColumn(vedtakbegrunnelser: List<IVedtakBegrunnelse>) =
        vedtakbegrunnelser.map { it.enumnavnTilString() }.joinToString(";")

    override fun convertToEntityAttribute(string: String?): List<IVedtakBegrunnelse> =
        if (string.isNullOrBlank()) emptyList() else string.split(";").map { it.fraEnumNavnTilEnumVerdi() }
}
