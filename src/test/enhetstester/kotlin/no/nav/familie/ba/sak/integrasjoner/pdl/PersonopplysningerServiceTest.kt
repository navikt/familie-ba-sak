import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.PdlPersonKanIkkeBehandlesIFagSystemÅrsak
import no.nav.familie.ba.sak.common.PdlPersonKanIkkeBehandlesIFagsystem
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonInfoQuery
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService.Companion.PERSON_HAR_FALSK_IDENTITET
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.FalskIdentitetPersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlPersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.falskidentitet.FalskIdentitetService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PersonopplysningerServiceTest {
    private val pdlRestKlient: PdlRestKlient = mockk()
    private val systemOnlyPdlRestKlient: SystemOnlyPdlRestKlient = mockk()
    private val familieIntegrasjonerTilgangskontrollService: FamilieIntegrasjonerTilgangskontrollService = mockk()
    private val integrasjonKlient: IntegrasjonKlient = mockk()
    private val falskIdentitetService: FalskIdentitetService = mockk()
    private val personopplysningerService: PersonopplysningerService =
        PersonopplysningerService(
            pdlRestKlient = pdlRestKlient,
            systemOnlyPdlRestKlient = systemOnlyPdlRestKlient,
            familieIntegrasjonerTilgangskontrollService = familieIntegrasjonerTilgangskontrollService,
            integrasjonKlient = integrasjonKlient,
            falskIdentitetService = falskIdentitetService,
        )

    @Test
    fun `hentPdlPersoninfoMedRelasjonerOgRegisterinformasjon skal returnere PdlPersonInfo av typen Person dersom aktør ikke har falsk identitet`() {
        // Arrange
        val person = lagPerson()
        val barn = lagPerson()
        val personInfo =
            PersonInfo(
                fødselsdato = person.fødselsdato,
                navn = person.navn,
                kjønn = person.kjønn,
                forelderBarnRelasjon = setOf(ForelderBarnRelasjon(aktør = barn.aktør, relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN)),
            )

        val personInfoBarn =
            PersonInfo(
                fødselsdato = barn.fødselsdato,
                navn = barn.navn,
                kjønn = barn.kjønn,
            )

        val tilganger =
            mapOf(
                Pair(person.aktør.aktivFødselsnummer(), Tilgang(personIdent = person.aktør.aktivFødselsnummer(), harTilgang = true)),
                Pair(barn.aktør.aktivFødselsnummer(), Tilgang(personIdent = barn.aktør.aktivFødselsnummer(), harTilgang = true)),
            )
        every { pdlRestKlient.hentPerson(person.aktør, PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON) } returns personInfo
        every { pdlRestKlient.hentPerson(barn.aktør, PersonInfoQuery.ENKEL) } returns personInfoBarn
        every { familieIntegrasjonerTilgangskontrollService.sjekkTilgangTilPersoner(any()) } returns tilganger
        every { integrasjonKlient.sjekkErEgenAnsattBulk(any()) } returns emptyMap()

        // Act
        val result = personopplysningerService.hentPdlPersoninfoMedRelasjonerOgRegisterinformasjon(person.aktør)

        // Assert
        assertThat(result is PdlPersonInfo.Person)
        val personInfoResult = (result as PdlPersonInfo.Person).personInfo
        assertThat(personInfoResult.navn).isEqualTo(personInfo.navn)
        assertThat(personInfoResult.fødselsdato).isEqualTo(personInfo.fødselsdato)
        assertThat(personInfoResult.kjønn).isEqualTo(personInfo.kjønn)
        assertThat(personInfoResult.forelderBarnRelasjon.size).isEqualTo(1)
        val barnRelasjon = personInfoResult.forelderBarnRelasjon.first()
        assertThat(barnRelasjon.aktør).isEqualTo(barn.aktør)
        assertThat(barnRelasjon.navn).isEqualTo(barn.navn)
        assertThat(barnRelasjon.fødselsdato).isEqualTo(barn.fødselsdato)
        assertThat(barnRelasjon.kjønn).isEqualTo(barn.kjønn)
    }

    @Test
    fun `hentPersoninfoMedRelasjonerOgRegisterinformasjon skal returnere PersonInfo dersom aktør ikke har falsk identitet`() {
        // Arrange
        val person = lagPerson()
        val barn = lagPerson()
        val personInfo =
            PersonInfo(
                fødselsdato = person.fødselsdato,
                navn = person.navn,
                kjønn = person.kjønn,
                forelderBarnRelasjon = setOf(ForelderBarnRelasjon(aktør = barn.aktør, relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN)),
            )

        val personInfoBarn =
            PersonInfo(
                fødselsdato = barn.fødselsdato,
                navn = barn.navn,
                kjønn = barn.kjønn,
            )

        val tilganger =
            mapOf(
                Pair(person.aktør.aktivFødselsnummer(), Tilgang(personIdent = person.aktør.aktivFødselsnummer(), harTilgang = true)),
                Pair(barn.aktør.aktivFødselsnummer(), Tilgang(personIdent = barn.aktør.aktivFødselsnummer(), harTilgang = true)),
            )
        every { pdlRestKlient.hentPerson(person.aktør, PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON) } returns personInfo
        every { pdlRestKlient.hentPerson(barn.aktør, PersonInfoQuery.ENKEL) } returns personInfoBarn
        every { familieIntegrasjonerTilgangskontrollService.sjekkTilgangTilPersoner(any()) } returns tilganger
        every { integrasjonKlient.sjekkErEgenAnsattBulk(any()) } returns emptyMap()

        // Act
        val result = personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(person.aktør)

        // Assert
        assertThat(result.navn).isEqualTo(personInfo.navn)
        assertThat(result.fødselsdato).isEqualTo(personInfo.fødselsdato)
        assertThat(result.kjønn).isEqualTo(personInfo.kjønn)
        assertThat(result.forelderBarnRelasjon.size).isEqualTo(1)
        val barnRelasjon = result.forelderBarnRelasjon.first()
        assertThat(barnRelasjon.aktør).isEqualTo(barn.aktør)
        assertThat(barnRelasjon.navn).isEqualTo(barn.navn)
        assertThat(barnRelasjon.fødselsdato).isEqualTo(barn.fødselsdato)
        assertThat(barnRelasjon.kjønn).isEqualTo(barn.kjønn)
    }

    @Test
    fun `hentPdlPersoninfoMedRelasjonerOgRegisterinformasjon skal returnere PdlPersonInfo av typen FalskPerson dersom aktør har falsk identitet`() {
        // Arrange
        val person = lagPerson()

        every { pdlRestKlient.hentPerson(person.aktør, PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON) } throws PdlPersonKanIkkeBehandlesIFagsystem(årsak = PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
        every { falskIdentitetService.hentFalskIdentitet(person.aktør) } returns FalskIdentitetPersonInfo()

        // Act
        val result = personopplysningerService.hentPdlPersoninfoMedRelasjonerOgRegisterinformasjon(person.aktør)

        // Assert
        assertThat(result is PdlPersonInfo.FalskPerson)
        val falskIdentitetPersonInfo = (result as PdlPersonInfo.FalskPerson).falskIdentitetPersonInfo
        assertThat(falskIdentitetPersonInfo.navn).isEqualTo("Ukjent navn")
        assertThat(falskIdentitetPersonInfo.fødselsdato).isNull()
        assertThat(falskIdentitetPersonInfo.kjønn).isEqualTo(Kjønn.UKJENT)
    }

    @Test
    fun `hentPdlPersoninfoMedRelasjonerOgRegisterinformasjon skal kaste feil dersom pdl respons mangler nødvendige felter og aktør ikke har falsk identitet`() {
        // Arrange
        val person = lagPerson()

        every { pdlRestKlient.hentPerson(person.aktør, PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON) } throws PdlPersonKanIkkeBehandlesIFagsystem(årsak = PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
        every { falskIdentitetService.hentFalskIdentitet(person.aktør) } returns null

        // Act & Assert
        val result = assertThrows<PdlPersonKanIkkeBehandlesIFagsystem> { personopplysningerService.hentPdlPersoninfoMedRelasjonerOgRegisterinformasjon(person.aktør) }
        assertThat(result.årsak).isEqualTo(PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
    }

    @Test
    fun `hentPersoninfoMedRelasjonerOgRegisterinformasjon skal kaste feil dersom aktør har falsk identitet`() {
        // Arrange
        val person = lagPerson()
        every { pdlRestKlient.hentPerson(person.aktør, PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON) } throws PdlPersonKanIkkeBehandlesIFagsystem(årsak = PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
        every { falskIdentitetService.hentFalskIdentitet(person.aktør) } returns FalskIdentitetPersonInfo()

        // Act & Assert
        val funksjonellFeil = assertThrows<FunksjonellFeil> { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(person.aktør) }
        assertThat(funksjonellFeil.message).isEqualTo(PERSON_HAR_FALSK_IDENTITET)
    }

    @Test
    fun `hentPersoninfoMedRelasjonerOgRegisterinformasjon skal kaste feil dersom pdl respons mangler nødvendige felter og aktør ikke har falsk identitet`() {
        // Arrange
        val person = lagPerson()
        every { pdlRestKlient.hentPerson(person.aktør, PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON) } throws PdlPersonKanIkkeBehandlesIFagsystem(årsak = PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
        every { falskIdentitetService.hentFalskIdentitet(person.aktør) } returns null

        // Act & Assert
        val pdlFeil = assertThrows<PdlPersonKanIkkeBehandlesIFagsystem> { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(person.aktør) }
        assertThat(pdlFeil.årsak).isEqualTo(PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
    }

    @Test
    fun `hentPdlPersoninfoEnkel skal returnere PdlPersonInfo av typen Person dersom aktør ikke har falsk identitet`() {
        // Arrange
        val person = lagPerson()
        val personInfo =
            PersonInfo(
                fødselsdato = person.fødselsdato,
                navn = person.navn,
                kjønn = person.kjønn,
            )

        every { pdlRestKlient.hentPerson(person.aktør, PersonInfoQuery.ENKEL) } returns personInfo

        // Act
        val result = personopplysningerService.hentPdlPersonInfoEnkel(person.aktør)

        // Assert
        assertThat(result is PdlPersonInfo.Person)
        val personInfoResult = (result as PdlPersonInfo.Person).personInfo
        assertThat(personInfoResult.navn).isEqualTo(personInfo.navn)
        assertThat(personInfoResult.fødselsdato).isEqualTo(personInfo.fødselsdato)
        assertThat(personInfoResult.kjønn).isEqualTo(personInfo.kjønn)
    }

    @Test
    fun `hentPersoninfoEnkel skal returnere PersonInfo dersom aktør ikke har falsk identitet`() {
        // Arrange
        val person = lagPerson()
        val personInfo =
            PersonInfo(
                fødselsdato = person.fødselsdato,
                navn = person.navn,
                kjønn = person.kjønn,
            )

        every { pdlRestKlient.hentPerson(person.aktør, PersonInfoQuery.ENKEL) } returns personInfo

        // Act
        val result = personopplysningerService.hentPersoninfoEnkel(person.aktør)

        // Assert
        assertThat(result.navn).isEqualTo(personInfo.navn)
        assertThat(result.fødselsdato).isEqualTo(personInfo.fødselsdato)
        assertThat(result.kjønn).isEqualTo(personInfo.kjønn)
    }

    @Test
    fun `hentPdlPersoninfoEnkel skal returnere PdlPersonInfo av typen FalskPerson dersom aktør har falsk identitet`() {
        // Arrange
        val person = lagPerson()

        every { pdlRestKlient.hentPerson(person.aktør, PersonInfoQuery.ENKEL) } throws PdlPersonKanIkkeBehandlesIFagsystem(årsak = PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
        every { falskIdentitetService.hentFalskIdentitet(person.aktør) } returns FalskIdentitetPersonInfo()

        // Act
        val result = personopplysningerService.hentPdlPersonInfoEnkel(person.aktør)

        // Assert
        assertThat(result is PdlPersonInfo.FalskPerson)
        val falskPersonInfoResult = (result as PdlPersonInfo.FalskPerson).falskIdentitetPersonInfo
        assertThat(falskPersonInfoResult.navn).isEqualTo("Ukjent navn")
        assertThat(falskPersonInfoResult.fødselsdato).isNull()
        assertThat(falskPersonInfoResult.kjønn).isEqualTo(Kjønn.UKJENT)
    }

    @Test
    fun `hentPdlPersoninfoEnkel skal kaste feil dersom pdl respons mangler fnr og aktør ikke har falsk identitet`() {
        // Arrange
        val person = lagPerson()

        every { pdlRestKlient.hentPerson(person.aktør, PersonInfoQuery.ENKEL) } throws PdlPersonKanIkkeBehandlesIFagsystem(årsak = PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
        every { falskIdentitetService.hentFalskIdentitet(person.aktør) } returns null

        // Act & Assert
        val result = assertThrows<PdlPersonKanIkkeBehandlesIFagsystem> { personopplysningerService.hentPdlPersonInfoEnkel(person.aktør) }
        assertThat(result.årsak).isEqualTo(PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
    }

    @Test
    fun `hentPersoninfoEnkel skal kaste feil dersom pdl respons mangler fnr og aktør har falsk identitet`() {
        // Arrange
        val person = lagPerson()

        every { pdlRestKlient.hentPerson(person.aktør, PersonInfoQuery.ENKEL) } throws PdlPersonKanIkkeBehandlesIFagsystem(årsak = PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
        every { falskIdentitetService.hentFalskIdentitet(person.aktør) } returns FalskIdentitetPersonInfo()

        // Act & Assert
        val funksjonellFeil = assertThrows<FunksjonellFeil> { personopplysningerService.hentPersoninfoEnkel(person.aktør) }
        assertThat(funksjonellFeil.message).isEqualTo(PERSON_HAR_FALSK_IDENTITET)
    }

    @Test
    fun `hentPersoninfoEnkel skal kaste feil dersom pdl respons mangler fnr og aktør ikke har falsk identitet`() {
        // Arrange
        val person = lagPerson()

        every { pdlRestKlient.hentPerson(person.aktør, PersonInfoQuery.ENKEL) } throws PdlPersonKanIkkeBehandlesIFagsystem(årsak = PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
        every { falskIdentitetService.hentFalskIdentitet(person.aktør) } returns null

        // Act & Assert
        val pdlFeil = assertThrows<PdlPersonKanIkkeBehandlesIFagsystem> { personopplysningerService.hentPersoninfoEnkel(person.aktør) }
        assertThat(pdlFeil.årsak).isEqualTo(PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
    }

    //
    @Test
    fun `hentPdlPersoninfoNavnOgAdresse skal returnere PdlPersonInfo av typen Person dersom PDL respons ikke mangler nødvendige felter`() {
        // Arrange
        val person = lagPerson()
        val personInfo =
            PersonInfo(
                fødselsdato = person.fødselsdato,
                navn = person.navn,
                kjønn = person.kjønn,
            )
        every { pdlRestKlient.hentPerson(person.aktør, PersonInfoQuery.NAVN_OG_ADRESSE) } returns personInfo

        // Act
        val result = personopplysningerService.hentPdlPersoninfoNavnOgAdresse(person.aktør)

        // Assert
        assertThat(result is PdlPersonInfo.Person)
        val personInfoResult = (result as PdlPersonInfo.Person).personInfo
        assertThat(personInfoResult.navn).isEqualTo(personInfo.navn)
        assertThat(personInfoResult.fødselsdato).isEqualTo(personInfo.fødselsdato)
        assertThat(personInfoResult.kjønn).isEqualTo(personInfo.kjønn)
    }

    @Test
    fun `hentPersoninfoNavnOgAdresse skal returnere PersonInfo dersom PDL respons ikke mangler nødvendige felter`() {
        // Arrange
        val person = lagPerson()
        val personInfo =
            PersonInfo(
                fødselsdato = person.fødselsdato,
                navn = person.navn,
                kjønn = person.kjønn,
            )
        every { pdlRestKlient.hentPerson(person.aktør, PersonInfoQuery.NAVN_OG_ADRESSE) } returns personInfo

        // Act
        val result = personopplysningerService.hentPersoninfoNavnOgAdresse(person.aktør)

        // Assert
        assertThat(result.navn).isEqualTo(personInfo.navn)
        assertThat(result.fødselsdato).isEqualTo(personInfo.fødselsdato)
        assertThat(result.kjønn).isEqualTo(personInfo.kjønn)
    }

    @Test
    fun `hentPdlPersoninfoNavnOgAdresse skal returnere PdlPersonInfo av typen FalskPerson dersom PDL respons mangler nødvendige felter og aktør har flask identitet`() {
        // Arrange
        val person = lagPerson()

        every { pdlRestKlient.hentPerson(person.aktør, PersonInfoQuery.NAVN_OG_ADRESSE) } throws PdlPersonKanIkkeBehandlesIFagsystem(årsak = PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
        every { falskIdentitetService.hentFalskIdentitet(person.aktør) } returns FalskIdentitetPersonInfo()

        // Act
        val result = personopplysningerService.hentPdlPersoninfoNavnOgAdresse(person.aktør)

        // Assert
        assertThat(result is PdlPersonInfo.FalskPerson)
        val personInfoResult = (result as PdlPersonInfo.FalskPerson).falskIdentitetPersonInfo
        assertThat(personInfoResult.navn).isEqualTo("Ukjent navn")
        assertThat(personInfoResult.fødselsdato).isNull()
        assertThat(personInfoResult.kjønn).isEqualTo(Kjønn.UKJENT)
    }

    @Test
    fun `hentPersoninfoNavnOgAdresse skal kaste feil dersom PDL respons mangler nødvendige felter og aktør har falsk identitet`() {
        // Arrange
        val person = lagPerson()

        every { pdlRestKlient.hentPerson(person.aktør, PersonInfoQuery.NAVN_OG_ADRESSE) } throws PdlPersonKanIkkeBehandlesIFagsystem(årsak = PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
        every { falskIdentitetService.hentFalskIdentitet(person.aktør) } returns FalskIdentitetPersonInfo()

        // Act & Assert
        val result = assertThrows<FunksjonellFeil> { personopplysningerService.hentPersoninfoNavnOgAdresse(person.aktør) }
        assertThat(result.message).isEqualTo(PERSON_HAR_FALSK_IDENTITET)
    }

    @Test
    fun `hentPdlPersoninfoNavnOgAdresse skal returnere PdlPersonInfo av typen FalskPerson dersom PDL respons mangler nødvendige felter og aktør ikke har flask identitet`() {
        // Arrange
        val person = lagPerson()

        every { pdlRestKlient.hentPerson(person.aktør, PersonInfoQuery.NAVN_OG_ADRESSE) } throws PdlPersonKanIkkeBehandlesIFagsystem(årsak = PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
        every { falskIdentitetService.hentFalskIdentitet(person.aktør) } returns null

        // Act & Assert
        val result = assertThrows<PdlPersonKanIkkeBehandlesIFagsystem> { personopplysningerService.hentPdlPersoninfoNavnOgAdresse(person.aktør) }
        assertThat(result.årsak).isEqualTo(PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
    }

    @Test
    fun `hentPersoninfoNavnOgAdresse skal kaste feil dersom PDL respons mangler nødvendige felter og aktør ikke har falsk identitet`() {
        // Arrange
        val person = lagPerson()

        every { pdlRestKlient.hentPerson(person.aktør, PersonInfoQuery.NAVN_OG_ADRESSE) } throws PdlPersonKanIkkeBehandlesIFagsystem(årsak = PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
        every { falskIdentitetService.hentFalskIdentitet(person.aktør) } returns null

        // Act & Assert
        val result = assertThrows<PdlPersonKanIkkeBehandlesIFagsystem> { personopplysningerService.hentPersoninfoNavnOgAdresse(person.aktør) }
        assertThat(result.årsak).isEqualTo(PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
    }
}
