package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.lang.Integer.min
import java.net.URI

@Service
@Profile("mock-ident-client")
@Primary
class MockPdlIdentRestClient(
    restOperations: RestOperations,
) : PdlIdentRestClient(URI("dummy_uri"), restOperations) {
    private val identMap = mutableMapOf<String, List<IdentInformasjon>>()

    override fun hentIdenter(
        personIdent: String,
        historikk: Boolean,
    ): List<IdentInformasjon> {
        // If we have stored identities for this person, return them
        identMap[personIdent]?.let { return it }

        return when {
            historikk ->
                listOf(
                    IdentInformasjon(personIdent, historisk = false, gruppe = "FOLKEREGISTERIDENT"),
                    IdentInformasjon(randomFnr(), historisk = true, gruppe = "FOLKEREGISTERIDENT"),
                )

            else ->
                listOf(
                    IdentInformasjon(
                        ident = personIdent.substring(0, min(11, personIdent.length)),
                        historisk = false,
                        gruppe = "FOLKEREGISTERIDENT",
                    ),
                    IdentInformasjon(
                        ident = personIdent.substring(0, min(11, personIdent.length)) + "00",
                        historisk = false,
                        gruppe = "AKTORID",
                    ),
                )
        }
    }

    fun leggTilIdent(
        personIdent: String,
        identInformasjon: List<IdentInformasjon>,
    ) {
        identMap[personIdent] = identInformasjon
    }

    fun reset() {
        identMap.clear()
    }
}
