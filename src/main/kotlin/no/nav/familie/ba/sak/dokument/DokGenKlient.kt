package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.behandling.restDomene.DocFormat
import no.nav.familie.ba.sak.behandling.restDomene.DocFormat.HTML
import no.nav.familie.ba.sak.behandling.restDomene.DocFormat.PDF
import no.nav.familie.ba.sak.behandling.restDomene.DokumentRequest
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakResultat
import no.nav.familie.ba.sak.behandling.vedtak.toDokGenTemplate
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.NavHttpHeaders
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.net.URI

@Service
@Profile("!mock-dokgen-java")
class DokGenKlient(
        @Value("\${FAMILIE_BA_DOKGEN_API_URL}") private val dokgenServiceUri: String,
        private val restTemplate: RestTemplate
) {

    fun hentStønadBrevMarkdown(vedtak: Vedtak): String {
        val fletteFelter = mapTilBrevfelter(vedtak)
        return hentMarkdownForMal(vedtak.resultat.toDokGenTemplate(), fletteFelter)
    }

    private fun mapTilBrevfelter(vedtak: Vedtak): String = when (vedtak.resultat) {
        VedtakResultat.INNVILGET -> mapTilInnvilgetBrevFelter(vedtak)
        VedtakResultat.AVSLÅTT -> mapTilAvslagBrevFelter(vedtak)
        else -> throw RuntimeException("Invalid/unsupported vedtak.resultat")
    }

    private fun mapTilInnvilgetBrevFelter(vedtak: Vedtak): String {

        val startDato = "februar 2020" // TODO hent fra beregningen

        // TODO hent fra dokgen (/template/{templateName}/schema)
        // TODO Bytt ut hardkodede felter med faktiske verdier
        return "{\"belop\": 123,\n" +
               "\"startDato\": \"$startDato\",\n" +
               "\"etterbetaling\": false,\n" +
               "\"enhet\": \"enhet\",\n" +
               "\"fodselsnummer\": \"${vedtak.behandling.fagsak.personIdent.ident}\",\n" +
               "\"fodselsdato\": \"24.12.19\",\n" +
               "\"saksbehandler\": \"${vedtak.ansvarligSaksbehandler}\", \n" +
               "\"fritekst\": \"${vedtak.begrunnelse}\"}"
    }

    private fun mapTilAvslagBrevFelter(vedtak: Vedtak): String {

        //TODO: sett navn, hjemmel og firtekst
        return "{\"fodselsnummer\": \"${vedtak.behandling.fagsak.personIdent.ident}\",\n" +
               "\"navn\": \"No Name\",\n" +
               "\"hjemmel\": \"\",\n" +
               "\"fritekst\": \"${vedtak.begrunnelse}\"}"
    }

    private fun hentMarkdownForMal(malNavn: String, fletteFelter: String): String {
        val url = URI.create("$dokgenServiceUri/template/$malNavn/create-markdown")
        val response = utførRequest(lagPostRequest(url, fletteFelter), String::class.java)
        return response.body.orEmpty()
    }

    fun lagHtmlFraMarkdown(template: String, markdown: String): String {
        val request = lagDokumentRequestForMarkdown(HTML, template, markdown)
        val response = utførRequest(request, String::class.java)
        return response.body.orEmpty()
    }

    fun lagPdfFraMarkdown(template: String, markdown: String): ByteArray {
        val request = lagDokumentRequestForMarkdown(PDF, template, markdown)
        val response = utførRequest(request, ByteArray::class.java)
        return response.body!!
    }

    fun lagDokumentRequestForMarkdown(format: DocFormat, template: String, markdown: String): RequestEntity<String> {
        val url = URI.create("$dokgenServiceUri/template/${template}/create-doc")
        val body = DokumentRequest(format,
                                   markdown,
                                   true,
                                   null,
                                   true,
                                   "{\"fodselsnummer\":\"12345678910\",\"navn\": \"navn\",\"adresse\": \"adresse\"," +
                                   "\"postnr\": \"1626\",\"returadresse\": \"returadresse\"," +
                                   "\"dokumentDato\": \"3. september 2019\"}")
        return lagPostRequest(url, objectMapper.writeValueAsString(body))
    }

    private fun lagPostRequest(url: URI, body: String): RequestEntity<String> {
        return RequestEntity.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .acceptCharset(Charsets.UTF_8)
                .header(NavHttpHeaders.NAV_CALL_ID.asString(), MDC.get(MDCConstants.MDC_CALL_ID))
                .body(body)
    }

    protected fun <T : Any> utførRequest(request: RequestEntity<String>, responseType: Class<T>): ResponseEntity<T> {
        return restTemplate.exchange(request, responseType)
    }
}
