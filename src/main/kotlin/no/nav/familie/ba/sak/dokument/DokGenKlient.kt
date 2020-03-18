package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BrevType
import no.nav.familie.ba.sak.behandling.domene.toDokGenTemplate
import no.nav.familie.ba.sak.behandling.restDomene.DocFormat
import no.nav.familie.ba.sak.behandling.restDomene.DocFormat.HTML
import no.nav.familie.ba.sak.behandling.restDomene.DocFormat.PDF
import no.nav.familie.ba.sak.behandling.restDomene.DokumentRequest
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.NavHttpHeaders
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.LoggerFactory
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

    fun hentStønadBrevMarkdown(behandling: Behandling,
                               ansvarligSaksbehandler: String): String {
        val fletteFelter = mapTilBrevfelter(behandling, ansvarligSaksbehandler)
        return hentMarkdownForMal(behandling.brev.toDokGenTemplate(), fletteFelter)
    }

    private fun mapTilBrevfelter(behandling: Behandling,
                                 ansvarligSaksbehandler: String): String = when (behandling.brev) {
        BrevType.INNVILGET -> mapTilInnvilgetBrevFelter(behandling, ansvarligSaksbehandler)
        BrevType.AVSLÅTT -> mapTilAvslagBrevFelter(behandling)
        BrevType.OPPHØRT -> mapTilOpphørtBrevFelter(behandling)
        else -> error("Invalid/unsupported vedtak.resultat")
    }

    private fun mapTilOpphørtBrevFelter(behandling: Behandling): String {
        return "{\"fodselsnummer\": \"${behandling.fagsak.personIdent.ident}\",\n" +
               "\"navn\": \"No Name\",\n" +
               "\"tdato\": \"01.01.01\",\n" +
               "\"hjemmel\": \"\",\n" +
               "\"fritekst\": \"${behandling.begrunnelse}\"}"
    }

    private fun mapTilInnvilgetBrevFelter(behandling: Behandling,
                                          ansvarligSaksbehandler: String): String {
        val startDato = "februar 2020" // TODO hent fra beregningen

        // TODO hent fra dokgen (/template/{templateName}/schema)
        // TODO Bytt ut hardkodede felter med faktiske verdier
        return "{\"belop\": 123,\n" +
               "\"startDato\": \"$startDato\",\n" +
               "\"etterbetaling\": false,\n" +
               "\"enhet\": \"enhet\",\n" +
               "\"fodselsnummer\": \"${behandling.fagsak.personIdent.ident}\",\n" +
               "\"fodselsdato\": \"24.12.19\",\n" +
               "\"saksbehandler\": \"${ansvarligSaksbehandler}\", \n" +
               "\"fritekst\": \"${behandling.begrunnelse}\"}"
    }

    private fun mapTilAvslagBrevFelter(behandling: Behandling): String {

        //TODO: sett navn, hjemmel og firtekst
        return "{\"fodselsnummer\": \"${behandling.fagsak.personIdent.ident}\",\n" +
               "\"navn\": \"No Name\",\n" +
               "\"hjemmel\": \"\",\n" +
               "\"fritekst\": \"${behandling.begrunnelse}\"}"
    }

    private fun hentMarkdownForMal(malNavn: String, fletteFelter: String): String {
        val url = URI.create("$dokgenServiceUri/template/$malNavn/create-markdown")
        LOG.info("hent markdown fra: "+ url)
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

    companion object {
        val LOG = LoggerFactory.getLogger(this::class.java)
    }
}
