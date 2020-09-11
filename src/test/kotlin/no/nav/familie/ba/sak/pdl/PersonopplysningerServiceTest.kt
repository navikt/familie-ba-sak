package no.nav.familie.ba.sak.pdl

import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import org.apache.commons.lang3.StringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@SpringBootTest(properties = ["PDL_URL=http://localhost:28085/api"])
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-dokgen", "mock-oauth", "mock-sts")
@Tag("integration")
@AutoConfigureWireMock(port = 28085)
class PersonopplysningerServiceTest {
    @Autowired
    lateinit var personopplysningerService: PersonopplysningerService

    @Test
    fun `hentPersoninfoFor() skal return riktig personinfo`(){
        val personInfo= personopplysningerService.hentPersoninfoMedRelasjoner(ID_MOR_MED_XXX_STATSBORGERSKAP)

        assert(LocalDate.of(1955, 9, 13).equals(personInfo.fødselsdato))
    }

    @Test
    fun `hentStatsborgerskap() skal return riktig statsborgerskap`(){
        val statsborgerskap= personopplysningerService.hentStatsborgerskap(Ident(ID_MOR_MED_XXX_STATSBORGERSKAP))
        assert(statsborgerskap.size == 1)
        assert(statsborgerskap.first().land == "XXX")
    }

    @Test
    fun `hentOpphold() skal returnere riktig opphold`(){
        val opphold = personopplysningerService.hentOpphold(ID_MOR_MED_XXX_STATSBORGERSKAP)
        assert(opphold.size == 1)
        assert(opphold.first().type == OPPHOLDSTILLATELSE.MIDLERTIDIG)
    }

    @Test
    fun `hentBostedsadresseperioder() skal returnere riktige perioder`(){
        val bostedsadresseperioder = personopplysningerService.hentBostedsadresseperioder(ID_MOR_MED_XXX_STATSBORGERSKAP)
        assert(bostedsadresseperioder.size == 1)
        assert(bostedsadresseperioder.first().periode?.fom != null)
    }

    @Test
    fun `hentLandkodeUtenlandskAdresse() skal returnere landkode `(){
        val landkode = personopplysningerService.hentLandkodeUtenlandskBostedsadresse(ID_MOR_MED_XXX_STATSBORGERSKAP)
        assertThat(landkode).isEqualTo("DK")
    }

    @Test
    fun `hentLandkodeUtenlandskAdresse() skal returnere ZZ hvis ingen landkode `(){
        val landkode = personopplysningerService.hentLandkodeUtenlandskBostedsadresse(ID_BARN)
        assertThat(landkode).isEqualTo("ZZ")
    }

    companion object{
        val ID_MOR_MED_XXX_STATSBORGERSKAP= "22345678901"
        val ID_BARN= "32345678901"

        private fun gyldigRequest(queryFilnavn: String, requestFilnavn: String): String {
            return readfile(requestFilnavn)
                    .replace(
                            "GRAPHQL-PLACEHOLDER",
                            readfile(queryFilnavn).graphqlCompatible()
                    )
        }

        private fun readfile(filnavn: String): String {
            return this::class.java.getResource("/pdl/$filnavn").readText()
        }

        private fun String.graphqlCompatible(): String {
            return StringUtils.normalizeSpace(this.replace("\n", ""))
        }

        private fun lagMockForPdl(graphqlQueryFilnavn: String, requestFilnavn: String, mockResponse: String) {
            WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/api/graphql"))
                                     .withRequestBody(WireMock.equalToJson(gyldigRequest(graphqlQueryFilnavn, requestFilnavn)))
                                     .willReturn(WireMock.aResponse()
                                                         .withHeader("Content-Type", "application/json")
                                                         .withBody(mockResponse)))
        }

        @BeforeAll
        @JvmStatic
        fun lagMockForPersoner(){
            lagMockForPdl("hentperson-med-relasjoner.graphql", "PdlIntegrasjon/gyldigRequestForMorMedXXXStatsborgerskap.json",
                          readfile("PdlIntegrasjon/personinfoResponseForMorMedXXXStatsborgerskap.json"))

            lagMockForPdl("hentperson-enkel.graphql", "PdlIntegrasjon/gyldigRequestForBarn.json",
                          readfile("PdlIntegrasjon/personinfoResponseForBarn.json"))

            lagMockForPdl("statsborgerskap.graphql", "PdlIntegrasjon/gyldigRequestForMorMedXXXStatsborgerskap.json",
                          readfile("PdlIntegrasjon/personinfoResponseForMorMedXXXStatsborgerskap.json"))

            lagMockForPdl("opphold.graphql", "PdlIntegrasjon/gyldigRequestForMorMedXXXStatsborgerskap.json",
                          readfile("PdlIntegrasjon/personinfoResponseForMorMedXXXStatsborgerskap.json"))

            lagMockForPdl("hentBostedsadresseperioder.graphql", "PdlIntegrasjon/gyldigRequestForBostedsadresseperioder.json",
                          readfile("PdlIntegrasjon/bostedsadresseperioderResponse.json"))

            lagMockForPdl("bostedsadresse-utenlandsk.graphql", "PdlIntegrasjon/gyldigRequestForBostedsadresseperioder.json",
                          readfile("PdlIntegrasjon/utenlandskAdresseResponse.json"))

            lagMockForPdl("bostedsadresse-utenlandsk.graphql", "PdlIntegrasjon/gyldigRequestForBarn.json",
                          readfile("PdlIntegrasjon/personinfoResponseForBarn.json"))
        }

    }
}