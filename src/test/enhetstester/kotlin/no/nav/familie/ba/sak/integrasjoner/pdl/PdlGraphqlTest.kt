package no.nav.familie.ba.sak.integrasjoner.pdl

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlAdressebeskyttelseResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlBaseResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlHentPersonRelasjonerResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlHentPersonResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlMetadata
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlNavn
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.filtrerKjønnPåKilde
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.filtrerNavnPåKilde
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue
import java.io.File

class PdlGraphqlTest {
    private val mapper = jsonMapper

    @Test
    fun testDeserialization() {
        val resp =
            mapper.readValue<PdlBaseResponse<PdlHentPersonResponse>>(File(getFile("pdl/pdlOkResponse.json")))
        assertThat(
            resp.data.person!!
                .foedselsdato
                .first()
                .foedselsdato,
        ).isEqualTo("1955-09-13")
        assertThat(
            resp.data.person!!
                .navn
                .first()
                .fornavn,
        ).isEqualTo("ENGASJERT")
        assertThat(
            resp.data.person!!
                .navn
                .first()
                .metadata
                .master
                .uppercase(),
        ).isEqualTo("PDL")
        assertThat(
            resp.data.person!!
                .kjoenn
                .first()
                .kjoenn
                .toString(),
        ).isEqualTo("MANN")
        assertThat(
            resp.data.person!!
                .forelderBarnRelasjon
                .first()
                .relatertPersonsIdent,
        ).isEqualTo("12345678910")
        assertThat(
            resp.data.person!!
                .forelderBarnRelasjon
                .first()
                .relatertPersonsRolle
                .toString(),
        ).isEqualTo("BARN")
        assertThat(
            resp.data.person!!
                .sivilstand
                .first()
                .type,
        ).isEqualTo(SIVILSTANDTYPE.UGIFT)
        assertThat(
            resp.data.person!!
                .bostedsadresse
                .first()
                .vegadresse
                ?.husnummer,
        ).isEqualTo("3")
        assertThat(
            resp.data.person!!
                .bostedsadresse
                .first()
                .vegadresse
                ?.matrikkelId,
        ).isEqualTo(1234)
        assertNull(
            resp.data.person!!
                .bostedsadresse
                .first()
                .matrikkeladresse,
        )
        assertNull(
            resp.data.person!!
                .bostedsadresse
                .first()
                .ukjentBosted,
        )
        assertThat(resp.errorMessages()).isEqualTo("")
    }

    @Test
    fun testTomAdresse() {
        val resp =
            mapper.readValue<PdlBaseResponse<PdlHentPersonResponse>>(File(getFile("pdl/pdlTomAdresseOkResponse.json")))
        assertTrue(
            resp.data.person!!
                .bostedsadresse
                .isEmpty(),
        )
    }

    @Test
    fun testForelderBarnRelasjon() {
        val resp =
            mapper.readValue<PdlBaseResponse<PdlHentPersonRelasjonerResponse>>(
                File(getFile("pdl/pdlForelderBarnRelasjonResponse.json")),
            )
        assertThat(
            resp.data.person!!
                .forelderBarnRelasjon
                .first()
                .relatertPersonsRolle,
        ).isEqualTo(
            FORELDERBARNRELASJONROLLE.BARN,
        )
        assertThat(
            resp.data.person!!
                .forelderBarnRelasjon
                .first()
                .relatertPersonsIdent,
        ).isEqualTo("32345678901")
    }

    @Test
    fun testMatrikkelAdresse() {
        val resp =
            mapper.readValue<PdlBaseResponse<PdlHentPersonResponse>>(File(getFile("pdl/pdlMatrikkelAdresseOkResponse.json")))
        assertThat(
            resp.data.person!!
                .bostedsadresse
                .first()
                .matrikkeladresse
                ?.postnummer,
        ).isEqualTo("0274")
        assertThat(
            resp.data.person!!
                .bostedsadresse
                .first()
                .matrikkeladresse
                ?.matrikkelId,
        ).isEqualTo(2147483649)
    }

    @Test
    fun testUkjentBostedAdresse() {
        val resp =
            mapper.readValue<PdlBaseResponse<PdlHentPersonResponse>>(
                File(getFile("pdl/pdlUkjentBostedAdresseOkResponse.json")),
            )
        assertThat(
            resp.data.person!!
                .bostedsadresse
                .first()
                .ukjentBosted
                ?.bostedskommune,
        ).isEqualTo("Oslo")
    }

    @Test
    fun testDoedtfodtBarn() {
        val resp =
            mapper.readValue<PdlBaseResponse<PdlHentPersonResponse>>(
                File(getFile("pdl/pdlDoedfoedtBarnOkResponse.json")),
            )
        assertThat(
            resp.data.person!!
                .doedfoedtBarn
                .first()
                .dato,
        ).isEqualTo("2024-09-13")
    }

    @Test
    fun testAdressebeskyttelse() {
        val resp =
            mapper.readValue<PdlBaseResponse<PdlAdressebeskyttelseResponse>>(
                File(getFile("pdl/pdlAdressebeskyttelseResponse.json")),
            )
        assertThat(
            resp.data.person!!
                .adressebeskyttelse
                .first()
                .gradering,
        ).isEqualTo(ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG)
    }

    @Test
    fun testDeserializationOfResponseWithErrors() {
        val resp =
            mapper.readValue<PdlBaseResponse<PdlHentPersonResponse>>(File(getFile("pdl/pdlPersonIkkeFunnetResponse.json")))
        assertThat(resp.harFeil()).isTrue
        assertThat(resp.errorMessages()).contains("Fant ikke person", "Ikke tilgang")
        assertThat(resp.errors!!.any { it.extensions?.notFound() == true }).isTrue
    }

    @Test
    fun testDeserializationOfResponseWithoutFødselsdato() {
        val resp =
            mapper.readValue<PdlBaseResponse<PdlHentPersonResponse>>(File(getFile("pdl/pdlManglerFoedselResponse.json")))
        assertThat(
            resp.data.person!!
                .foedselsdato
                .first()
                .foedselsdato,
        ).isNull()
    }

    @Test
    fun testFulltNavn() {
        assertThat(
            PdlNavn(
                fornavn = "For",
                mellomnavn = "Mellom",
                etternavn = "Etter",
                metadata = PdlMetadata(master = "kilde", historisk = false),
            ).fulltNavn(),
        ).isEqualTo("For Mellom Etter")
        assertThat(
            PdlNavn(
                fornavn = "For",
                etternavn = "Etter",
                metadata = PdlMetadata(master = "kilde", historisk = false),
            ).fulltNavn(),
        ).isEqualTo("For Etter")
    }

    @Test
    fun testDeserialiseringAvResponsMedToNavn() {
        val resp =
            mapper.readValue<PdlBaseResponse<PdlHentPersonResponse>>(File(getFile("pdl/pdlMedToNavnOgToKjonn.json")))
        assertThat(
            resp.data.person!!
                .navn
                .filtrerNavnPåKilde()
                ?.etternavn,
        ).isEqualTo("NATUR")
    }

    @Test
    fun testDeserialiseringAvResponsMedToKjønn() {
        val resp =
            mapper.readValue<PdlBaseResponse<PdlHentPersonResponse>>(File(getFile("pdl/pdlMedToNavnOgToKjonn.json")))
        assertThat(
            resp.data.person!!
                .kjoenn
                .filtrerKjønnPåKilde()
                ?.kjoenn,
        ).isEqualTo(Kjønn.KVINNE)
    }

    @Test
    fun testDeserialiseringAvResponsMedUgyldigeKilder() {
        val resp =
            mapper.readValue<PdlBaseResponse<PdlHentPersonResponse>>(File(getFile("pdl/pdlMedToNavnOgToKjonn.json")))
        assertThat(
            resp.data.person!!
                .kjoenn
                .filtrerKjønnPåKilde()
                ?.kjoenn,
        ).isEqualTo(Kjønn.KVINNE)
    }

    private fun getFile(name: String): String = javaClass.classLoader?.getResource(name)?.file ?: throw Feil("Testkonfigurasjon feil")
}
