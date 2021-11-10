package no.nav.familie.ba.sak.scripts

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.familie.ba.sak.kjerne.dokument.domene.finnEnumverdi
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

printUtAlleSanitybegrunnelserViIkkeHarIBaSak()

fun printUtAlleSanitybegrunnelserViIkkeHarIBaSak() {
    val sanitybegrunnelser = hentSanityBegrunnelser()
    val begrunnelser = VedtakBegrunnelseSpesifikasjon.values()

    sanitybegrunnelser.filter { !finnesIBaSak(begrunnelser, it) }
        .groupBy { tilVedtakBegrunnelseType(it.begrunnelsetype!!) }
        .forEach { (key, values) ->
            println(key.name + "-begrunnelser som ikke finnes i ba-sak:")
            println("––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––")
            values.forEach {
                println(
                    "${
                    sentenceTilEnum(
                        it.begrunnelsetype + Regex("[^A-Za-z0-9ÆØÅæøå\\s]").replace(
                            " ${it.navnISystem}",
                            ""
                        )
                    )
                    }{"
                )
                println(
                    "override val vedtakBegrunnelseType = ${
                    tilVedtakBegrunnelseType(it.begrunnelsetype!!).vedtaksBegrtekst()
                    }"
                )
                println("override val sanityApiNavn = \"${it.apiNavn}\"")
                println("},")
            }
            println("")
        }
}

fun hentSanityBegrunnelser(): List<SanityBegrunnelse> {
    val sanityUrl = "https://xsrv1mh6.apicdn.sanity.io/v2021-06-07/data/query/ba-brev"
    val query = "*[_type == \"begrunnelse\"]{\n" +
        "     apiNavn,\n" +
        "     navnISystem,\n" +
        "     begrunnelsetype,\n" +
        " }"

    val parameters = java.net.URLEncoder.encode(query, "utf-8")

    val client = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder()
        .uri(URI.create("$sanityUrl?query=$parameters"))
        .build()

    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    val json = ObjectMapper().readValue(
        response.body(),
        SanityRespons::class.java
    )
    return json.result
}

fun tilVedtakBegrunnelseType(tekst: String): VedtakBegrunnelseType {
    val vedtakBegrunnelseType = when (tekst) {
        "ENDRET_UTBETALINGSPERIODE" -> VedtakBegrunnelseType.ENDRET_UTBETALING
        "ETTER_ENDRET_UTBETALINGSPERIODE" -> VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING
        "OPPHOR" -> VedtakBegrunnelseType.OPPHØR
        else -> finnEnumverdi(tekst, VedtakBegrunnelseType.values(), "")
    }

    if (vedtakBegrunnelseType == null) {
        error(tekst)
    }

    return vedtakBegrunnelseType
}

fun finnesIBaSak(
    begrunnelser: Array<VedtakBegrunnelseSpesifikasjon>,
    it: SanityBegrunnelse
) = begrunnelser.find { begrunnelse -> begrunnelse.sanityApiNavn == it.apiNavn } != null

fun sentenceTilEnum(tekst: String) = tekst.split(" ").joinToString("_").uppercase()

fun VedtakBegrunnelseType.vedtaksBegrtekst(): String {
    return when (this) {
        VedtakBegrunnelseType.INNVILGET -> "VedtakBegrunnelseType.INNVILGET"
        VedtakBegrunnelseType.REDUKSJON -> "VedtakBegrunnelseType.REDUKSJON"
        VedtakBegrunnelseType.AVSLAG -> "VedtakBegrunnelseType.AVSLAG"
        VedtakBegrunnelseType.OPPHØR -> "VedtakBegrunnelseType.OPPHØR"
        VedtakBegrunnelseType.FORTSATT_INNVILGET -> "VedtakBegrunnelseType.FORTSATT_INNVILGET"
        VedtakBegrunnelseType.ENDRET_UTBETALING -> "VedtakBegrunnelseType.ENDRET_UTBETALING"
        VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING -> "VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING"
    }
}
