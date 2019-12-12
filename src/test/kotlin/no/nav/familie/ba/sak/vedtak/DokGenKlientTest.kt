package no.nav.familie.ba.sak.vedtak

import org.junit.jupiter.api.Test

internal class DokGenKlientTest {

    private val dokgenKlient: DokGenKlient = DokGenKlient("http://localhost:8080")

    @Test
    fun `Test å hente Markdown og konvertere til html når dokgen kjører lokalt`() {
        dokgenKlient.runCatching {
            val markdownForMal = hentMarkdownForMal("Innvilget")

            val htmlResponse = lagHtmlFraMarkdown(markdownForMal)
            assert(htmlResponse.startsWith("<html>"))
        }
    }
}