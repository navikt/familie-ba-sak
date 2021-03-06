package no.nav.familie.ba.sak.integrasjoner.pdl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PdlAdressebeskyttelseResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PdlHentPersonResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PdlNavn
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File

class PdlGraphqlTest {

    private val mapper = ObjectMapper()
            .registerKotlinModule()

    @Test
    fun testDeserialization() {
        val resp = mapper.readValue(File(getFile("pdl/pdlOkResponse.json")), PdlHentPersonResponse::class.java)
        assertThat(resp.data.person!!.foedsel.first().foedselsdato).isEqualTo("1955-09-13")
        assertThat(resp.data.person!!.navn.first().fornavn).isEqualTo("ENGASJERT")
        assertThat(resp.data.person!!.kjoenn.first().kjoenn.toString()).isEqualTo("MANN")
        assertThat(resp.data.person!!.forelderBarnRelasjon.first().relatertPersonsIdent).isEqualTo("12345678910")
        assertThat(resp.data.person!!.forelderBarnRelasjon.first().relatertPersonsRolle.toString()).isEqualTo("BARN")
        assertThat(resp.data.person!!.sivilstand.first().type).isEqualTo(SIVILSTAND.UGIFT)
        assertThat(resp.data.person!!.bostedsadresse.first().vegadresse?.husnummer).isEqualTo("3")
        assertThat(resp.data.person!!.bostedsadresse.first().vegadresse?.matrikkelId).isEqualTo(1234)
        assertNull(resp.data.person!!.bostedsadresse.first().matrikkeladresse)
        assertNull(resp.data.person!!.bostedsadresse.first().ukjentBosted)
        assertThat(resp.errorMessages()).isEqualTo("")
    }

    @Test
    fun testTomAdresse() {
        val resp = mapper.readValue(File(getFile("pdl/pdlTomAdresseOkResponse.json")), PdlHentPersonResponse::class.java)
        assertTrue(resp.data.person!!.bostedsadresse.isEmpty())
    }

    @Test
    fun testMatrikkelAdresse() {
        val resp = mapper.readValue(File(getFile("pdl/pdlMatrikkelAdresseOkResponse.json")), PdlHentPersonResponse::class.java)
        assertThat(resp.data.person!!.bostedsadresse.first().matrikkeladresse?.postnummer).isEqualTo("0274")
        assertThat(resp.data.person!!.bostedsadresse.first().matrikkeladresse?.matrikkelId).isEqualTo(2147483649)
    }

    @Test
    fun testUkjentBostedAdresse() {
        val resp = mapper.readValue(File(getFile("pdl/pdlUkjentBostedAdresseOkResponse.json")), PdlHentPersonResponse::class.java)
        assertThat(resp.data.person!!.bostedsadresse.first().ukjentBosted?.bostedskommune).isEqualTo("Oslo")
    }

    @Test
    fun testAdressebeskyttelse() {
        val resp = mapper.readValue(File(getFile("pdl/pdlAdressebeskyttelseResponse.json")),
                                    PdlAdressebeskyttelseResponse::class.java)
        assertThat(resp.data.person!!.adressebeskyttelse.first().gradering).isEqualTo(ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG)
    }

    @Test
    fun testDeserializationOfResponseWithErrors() {
        val resp = mapper.readValue(File(getFile("pdl/pdlPersonIkkeFunnetResponse.json")), PdlHentPersonResponse::class.java)
        assertThat(resp.harFeil()).isTrue
        assertThat(resp.errorMessages()).contains("Fant ikke person", "Ikke tilgang")
    }

    @Test
    fun testDeserializationOfResponseWithoutFødselsdato() {
        val resp = mapper.readValue(File(getFile("pdl/pdlManglerFoedselResponse.json")), PdlHentPersonResponse::class.java)
        assertThat(resp.data.person!!.foedsel.first().foedselsdato).isNull()
    }

    @Test
    fun testFulltNavn() {
        assertThat(PdlNavn(fornavn = "For", mellomnavn = "Mellom", etternavn = "Etter").fulltNavn())
                .isEqualTo("For Mellom Etter")
        assertThat(PdlNavn(fornavn = "For", etternavn = "Etter").fulltNavn())
                .isEqualTo("For Etter")
    }

    private fun getFile(name: String): String {
        return javaClass.classLoader?.getResource(name)?.file ?: error("Testkonfigurasjon feil")
    }

}