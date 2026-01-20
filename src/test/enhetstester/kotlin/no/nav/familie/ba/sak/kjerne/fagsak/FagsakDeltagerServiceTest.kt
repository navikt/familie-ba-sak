package no.nav.familie.ba.sak.kjerne.fagsak

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.PdlPersonKanIkkeBehandlesIFagSystemÅrsak
import no.nav.familie.ba.sak.common.PdlPersonKanIkkeBehandlesIFagsystem
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.randomBarnFnr
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.ekstern.restDomene.FagsakDeltagerRolle
import no.nav.familie.ba.sak.ekstern.restDomene.PersonInfoDto
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.FalskIdentitetPersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlPersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class FagsakDeltagerServiceTest {
    private val fagsakRepository = mockk<FagsakRepository>()
    private val personRepository = mockk<PersonRepository>()
    private val personidentService = mockk<PersonidentService>()
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val familieIntegrasjonerTilgangskontrollService = mockk<FamilieIntegrasjonerTilgangskontrollService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val integrasjonKlient = mockk<IntegrasjonKlient>()
    private val fagsakService = mockk<FagsakService>()

    private val fagsakDeltagerService =
        FagsakDeltagerService(
            fagsakService = fagsakService,
            fagsakRepository = fagsakRepository,
            personRepository = personRepository,
            personidentService = personidentService,
            personopplysningerService = personopplysningerService,
            familieIntegrasjonerTilgangskontrollService = familieIntegrasjonerTilgangskontrollService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            integrasjonKlient = integrasjonKlient,
        )

    @Test
    fun `skal returnere tom liste når person ikke finnes`() {
        // Arrange
        val ident = randomFnr()
        every { personidentService.hentAktørOrNullHvisIkkeAktivFødselsnummer(ident) } returns null

        // Act
        val resultat = fagsakDeltagerService.hentFagsakDeltagere(ident)

        // Assert
        assertThat(resultat).isEmpty()
    }

    @Test
    fun `skal returnere kun maskert deltager når søker ikke har tilgang`() {
        // Arrange
        val (barnIdent, barnAktør) = randomBarnFnr().let { it to randomAktør(it) }
        every { personidentService.hentAktørOrNullHvisIkkeAktivFødselsnummer(barnIdent) } returns barnAktør
        every {
            familieIntegrasjonerTilgangskontrollService.hentMaskertPersonInfoVedManglendeTilgang(barnAktør)
        } returns
            PersonInfoDto(
                personIdent = barnAktør.aktivFødselsnummer(),
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG,
            )

        // Act
        val resultat = fagsakDeltagerService.hentFagsakDeltagere(barnIdent)

        // Assert
        assertThat(resultat).hasSize(1)
        assertThat(resultat.single().adressebeskyttelseGradering).isEqualTo(ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG)
        assertThat(resultat.single().harTilgang).isFalse()
        assertThat(resultat.single().rolle).isEqualTo(FagsakDeltagerRolle.UKJENT)
    }

    @Test
    fun `skal returnere grunnleggende info hvis person ikke har fagsak`() {
        // Arrange
        val (ident, aktør) = randomBarnFnr().let { it to randomAktør(it) }
        val navn = "Mock Mockesen"
        val personInfo = PersonInfo(fødselsdato = LocalDate.now().minusYears(30), kjønn = Kjønn.KVINNE, navn = navn)
        every { personidentService.hentAktørOrNullHvisIkkeAktivFødselsnummer(ident) } returns aktør
        every { familieIntegrasjonerTilgangskontrollService.hentMaskertPersonInfoVedManglendeTilgang(aktør) } returns null
        every {
            personopplysningerService.hentPdlPersoninfoMedRelasjonerOgRegisterinformasjon(aktør)
        } returns PdlPersonInfo.Person(personInfo = personInfo)
        every { fagsakRepository.finnFagsakerForAktør(aktør) } returns emptyList()
        every { personRepository.findByAktør(aktør) } returns emptyList()
        every { integrasjonKlient.sjekkErEgenAnsattBulk(any()) } returns emptyMap()

        // Act
        val resultat = fagsakDeltagerService.hentFagsakDeltagere(ident)

        // Assert
        assertThat(resultat).hasSize(1)
        val deltager = resultat.single()
        assertThat(deltager.ident).isEqualTo(ident)
        assertThat(deltager.navn).isEqualTo(navn)
        assertThat(deltager.rolle).isEqualTo(FagsakDeltagerRolle.UKJENT)
        assertThat(deltager.fagsakId).isNull()
    }

    @Test
    fun `søk på barn med to foreldre skal returnere alle tre`() {
        // Arrange
        val (barnIdent, barnAktør) = randomBarnFnr().let { it to randomAktør(it) }
        val (morIdent, morAktør) = randomFnr().let { it to randomAktør(it) }
        val (farIdent, farAktør) = randomFnr().let { it to randomAktør(it) }
        val aktører = listOf(barnAktør, morAktør, farAktør)
        val barnPersonInfo =
            PersonInfo(
                fødselsdato = LocalDate.now().minusYears(5),
                forelderBarnRelasjon =
                    setOf(
                        ForelderBarnRelasjon(
                            aktør = morAktør,
                            relasjonsrolle = FORELDERBARNRELASJONROLLE.MOR,
                        ),
                        ForelderBarnRelasjon(
                            aktør = farAktør,
                            relasjonsrolle = FORELDERBARNRELASJONROLLE.FAR,
                        ),
                    ),
            )
        every { personidentService.hentAktørOrNullHvisIkkeAktivFødselsnummer(barnIdent) } returns barnAktør
        every {
            familieIntegrasjonerTilgangskontrollService.hentMaskertPersonInfoVedManglendeTilgang(match { it in aktører })
        } returns null
        every {
            personopplysningerService.hentPdlPersoninfoMedRelasjonerOgRegisterinformasjon(barnAktør)
        } returns PdlPersonInfo.Person(personInfo = barnPersonInfo)
        every { personRepository.findByAktør(barnAktør) } returns listOf()
        every {
            integrasjonKlient.sjekkErEgenAnsattBulk(match { it.containsAll(listOf(barnIdent, morIdent, farIdent)) })
        } returns emptyMap()
        every { fagsakRepository.finnFagsakerForAktør(match { it in aktører }) } returns emptyList()

        // Act
        val resultat = fagsakDeltagerService.hentFagsakDeltagere(barnIdent)

        // Assert
        assertThat(resultat).hasSize(3)
        assertThat(resultat.single { it.ident == barnIdent }.rolle).isEqualTo(FagsakDeltagerRolle.BARN)
        assertThat(resultat.single { it.ident == morIdent }.rolle).isEqualTo(FagsakDeltagerRolle.FORELDER)
        assertThat(resultat.single { it.ident == farIdent }.rolle).isEqualTo(FagsakDeltagerRolle.FORELDER)
    }

    @Test
    fun `skal sette korrekt egen ansatt status basert på respons fra integrasjoner`() {
        // Arrange
        val (barnIdent, barnAktør) = randomBarnFnr().let { it to randomAktør(it) }
        val (morIdent, morAktør) = randomFnr().let { it to randomAktør(it) }
        val (farIdent, farAktør) = randomFnr().let { it to randomAktør(it) }
        val aktører = listOf(barnAktør, morAktør, farAktør)
        val barnInfo =
            PersonInfo(
                fødselsdato = LocalDate.now().minusYears(6),
                forelderBarnRelasjon =
                    setOf(
                        ForelderBarnRelasjon(
                            aktør = morAktør,
                            relasjonsrolle = FORELDERBARNRELASJONROLLE.MOR,
                        ),
                        ForelderBarnRelasjon(
                            aktør = farAktør,
                            relasjonsrolle = FORELDERBARNRELASJONROLLE.FAR,
                        ),
                    ),
            )
        every { personidentService.hentAktørOrNullHvisIkkeAktivFødselsnummer(barnIdent) } returns barnAktør
        every {
            familieIntegrasjonerTilgangskontrollService.hentMaskertPersonInfoVedManglendeTilgang(match { it in aktører })
        } returns null
        every {
            personopplysningerService.hentPdlPersoninfoMedRelasjonerOgRegisterinformasjon(barnAktør)
        } returns PdlPersonInfo.Person(personInfo = barnInfo)
        every { fagsakRepository.finnFagsakerForAktør(match { it in aktører }) } returns emptyList()
        every { personRepository.findByAktør(match { it in aktører }) } returns emptyList()
        every {
            integrasjonKlient.sjekkErEgenAnsattBulk(match { it.containsAll(listOf(barnIdent, morIdent, farIdent)) })
        } returns mapOf(barnIdent to false, morIdent to true) // Far utelates

        // Act
        val resultat = fagsakDeltagerService.hentFagsakDeltagere(barnIdent)

        // Assert
        assertThat(resultat.single { it.ident == barnIdent }.erEgenAnsatt).isFalse()
        assertThat(resultat.single { it.ident == morIdent }.erEgenAnsatt).isTrue()
        assertThat(resultat.single { it.ident == farIdent }.erEgenAnsatt).isNull()
    }

    @Test
    fun `skal omgjøre alle exceptions unntatt Funksjonell feil til Feil`() {
        // Arrange
        val personInfo = PersonInfo(LocalDate.now())
        val person = lagPerson()
        val behandling = lagBehandling()

        every {
            personidentService.hentAktørOrNullHvisIkkeAktivFødselsnummer("a")
        } returns person.aktør
        every {
            familieIntegrasjonerTilgangskontrollService.hentMaskertPersonInfoVedManglendeTilgang(person.aktør)
        } returns null
        every {
            personopplysningerService.hentPdlPersoninfoMedRelasjonerOgRegisterinformasjon(person.aktør)
        } returns PdlPersonInfo.Person(personInfo = personInfo)
        every {
            personRepository.findByAktør(person.aktør)
        } returns listOf(person)
        every {
            behandlingHentOgPersisterService.hent(behandlingId = person.personopplysningGrunnlag.behandlingId)
        } returns behandling
        every {
            familieIntegrasjonerTilgangskontrollService.hentMaskertPersonInfoVedManglendeTilgang(any())
        } returns null
        every {
            personopplysningerService.hentPdlPersonInfoEnkel(any())
        } throws Exception("Feil ved henting av personinfo")

        // Act og Assert

        val feil = assertThrows<Feil> { fagsakDeltagerService.hentFagsakDeltagere("a") }
        assertThat { feil.message == "Feil ved henting av person fra PDL" }
    }

    @Test
    fun `skal ikke omgjøre Funksjonell feil til Feil`() {
        // Arrange
        val personInfo = PersonInfo(LocalDate.now())
        val person = lagPerson()
        val behandling = lagBehandling()

        every {
            personidentService.hentAktørOrNullHvisIkkeAktivFødselsnummer("a")
        } returns person.aktør
        every {
            familieIntegrasjonerTilgangskontrollService.hentMaskertPersonInfoVedManglendeTilgang(person.aktør)
        } returns null
        every {
            personopplysningerService.hentPdlPersoninfoMedRelasjonerOgRegisterinformasjon(person.aktør)
        } returns PdlPersonInfo.Person(personInfo = personInfo)
        every {
            personRepository.findByAktør(person.aktør)
        } returns listOf(person)
        every {
            behandlingHentOgPersisterService.hent(behandlingId = person.personopplysningGrunnlag.behandlingId)
        } returns behandling
        every {
            familieIntegrasjonerTilgangskontrollService.hentMaskertPersonInfoVedManglendeTilgang(any())
        } returns null
        every {
            personopplysningerService.hentPdlPersonInfoEnkel(any())
        } throws FunksjonellFeil("Feil ved henting av personinfo")

        // Act og Assert

        val funksjonellFeil = assertThrows<FunksjonellFeil> { fagsakDeltagerService.hentFagsakDeltagere("a") }
        assertThat { funksjonellFeil.message == "Feil ved henting av person fra PDL" }
    }

    @Test
    fun `Dersom feilen PdlPersonKanIkkeBehandlesIFagSystemÅrsak kastes ved henting av forelder uten direkte relasjon skal det logges og ikke feile søket dersom forelder ikke har falsk identitet`() {
        // Arrange
        val personInfo = PersonInfo(LocalDate.now())
        val person = lagPerson()
        val behandling = lagBehandling()

        every {
            personidentService.hentAktørOrNullHvisIkkeAktivFødselsnummer(person.aktør.aktivFødselsnummer())
        } returns person.aktør
        every {
            familieIntegrasjonerTilgangskontrollService.hentMaskertPersonInfoVedManglendeTilgang(person.aktør)
        } returns null
        every {
            personopplysningerService.hentPdlPersoninfoMedRelasjonerOgRegisterinformasjon(person.aktør)
        } returns PdlPersonInfo.Person(personInfo = personInfo)
        every {
            personRepository.findByAktør(person.aktør)
        } returns listOf(person)
        every {
            behandlingHentOgPersisterService.hent(behandlingId = person.personopplysningGrunnlag.behandlingId)
        } returns behandling
        every {
            familieIntegrasjonerTilgangskontrollService.hentMaskertPersonInfoVedManglendeTilgang(any())
        } returns null
        every { fagsakRepository.finnFagsakerForAktør(person.aktør) } returns emptyList()
        every { integrasjonKlient.sjekkErEgenAnsattBulk(any()) } returns emptyMap()
        every {
            personopplysningerService.hentPdlPersonInfoEnkel(behandling.fagsak.aktør)
        } throws PdlPersonKanIkkeBehandlesIFagsystem(årsak = PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)

        // Act og Assert
        assertThat(fagsakDeltagerService.hentFagsakDeltagere(person.aktør.aktivFødselsnummer())).hasSize(1)
    }

    @Test
    fun `Dersom feilen PdlPersonKanIkkeBehandlesIFagSystemÅrsak kastes ved henting av forelder uten direkte relasjon og forelder har falsk identitet skal person info om falsk identitet returneres`() {
        // Arrange
        val personInfo = PersonInfo(LocalDate.now())
        val person = lagPerson()
        val forelderUtenDirekteRelasjon = lagPerson()
        val behandling = lagBehandling(fagsak = lagFagsak(aktør = forelderUtenDirekteRelasjon.aktør))

        every {
            personidentService.hentAktørOrNullHvisIkkeAktivFødselsnummer(person.aktør.aktivFødselsnummer())
        } returns person.aktør
        every {
            familieIntegrasjonerTilgangskontrollService.hentMaskertPersonInfoVedManglendeTilgang(person.aktør)
        } returns null
        every {
            personopplysningerService.hentPdlPersoninfoMedRelasjonerOgRegisterinformasjon(person.aktør)
        } returns PdlPersonInfo.Person(personInfo = personInfo)
        every {
            personRepository.findByAktør(person.aktør)
        } returns listOf(person)
        every {
            behandlingHentOgPersisterService.hent(behandlingId = person.personopplysningGrunnlag.behandlingId)
        } returns behandling
        every {
            familieIntegrasjonerTilgangskontrollService.hentMaskertPersonInfoVedManglendeTilgang(any())
        } returns null
        every { fagsakRepository.finnFagsakerForAktør(person.aktør) } returns emptyList()
        every { integrasjonKlient.sjekkErEgenAnsattBulk(any()) } returns emptyMap()
        every {
            personopplysningerService.hentPdlPersonInfoEnkel(any())
        } returns
            PdlPersonInfo.FalskPerson(
                falskIdentitetPersonInfo =
                    FalskIdentitetPersonInfo(
                        navn = forelderUtenDirekteRelasjon.navn,
                        fødselsdato = forelderUtenDirekteRelasjon.fødselsdato,
                        kjønn = forelderUtenDirekteRelasjon.kjønn,
                    ),
            )

        // Act og Assert
        val fagsakDeltagere = fagsakDeltagerService.hentFagsakDeltagere(person.aktør.aktivFødselsnummer())
        assertThat(fagsakDeltagere).hasSize(2)
        val forelder = fagsakDeltagere.first { it.rolle == FagsakDeltagerRolle.FORELDER }
        assertThat(forelder.navn).isEqualTo(forelderUtenDirekteRelasjon.navn)
        assertThat(forelder.kjønn).isEqualTo(forelderUtenDirekteRelasjon.kjønn)
        assertThat(forelder.fagsakId).isEqualTo(behandling.fagsak.id)
        assertThat(forelder.fagsakType).isEqualTo(behandling.fagsak.type)
        assertThat(forelder.harTilgang).isTrue
    }
}
