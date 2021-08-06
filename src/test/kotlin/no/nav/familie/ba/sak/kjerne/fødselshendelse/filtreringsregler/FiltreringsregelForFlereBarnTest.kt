package no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.VergeResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.DødsfallData
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.Personident
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.filtreringsregler.domene.FødselshendelsefiltreringResultatRepository
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.fødselshendelse.erOppfylt
import no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.domene.FødselshendelsefiltreringResultat
import no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.domene.erOppfylt
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.util.FnrGenerator
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FiltreringsregelForFlereBarnTest {

    val barnFnr0 = PersonIdent(FnrGenerator.generer())
    val barnFnr1 = PersonIdent(FnrGenerator.generer())
    val gyldigFnr = PersonIdent(FnrGenerator.generer())

    val personopplysningGrunnlagRepositoryMock = mockk<PersonopplysningGrunnlagRepository>()
    val personopplysningerServiceMock = mockk<PersonopplysningerService>()
    val localDateServiceMock = mockk<LocalDateService>()
    val fødselshendelsefiltreringResultatRepository = mockk<FødselshendelsefiltreringResultatRepository>(relaxed = true)
    val filtreringsreglerService = FiltreringsreglerService(
            personopplysningerServiceMock,
            personopplysningGrunnlagRepositoryMock,
            localDateServiceMock,
            fødselshendelsefiltreringResultatRepository)

    init {
        val fødselshendelsefiltreringResultatSlot = slot<List<FødselshendelsefiltreringResultat>>()
        every { fødselshendelsefiltreringResultatRepository.saveAll(capture(fødselshendelsefiltreringResultatSlot)) } answers {
            fødselshendelsefiltreringResultatSlot.captured
        }
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når det har gått mellom fem dager og fem måneder siden forrige minst ett barn ble født`() {
        val evalueringer = evaluerFiltreringsregler(
                genererFaktaMedTidligereBarn(1, 3, 7, 0)
        )

        Assertions.assertThat(evalueringer.erOppfylt()).isFalse
        Assertions.assertThat(evalueringer
                                      .filter { it.resultat == Resultat.IKKE_OPPFYLT }
                                      .any { it.identifikator == Filtreringsregel.MER_ENN_5_MND_SIDEN_FORRIGE_BARN.name }
        )
    }

    @Test
    fun `Regelevaluering skal resultere i JA når det har ikke gått mellom fem dager og fem måneder siden forrige minst ett barn ble født`() {
        val evalueringer = evaluerFiltreringsregler(
                genererFaktaMedTidligereBarn(0, 0, 0, 5)
        )

        Assertions.assertThat(evalueringer.erOppfylt()).isTrue
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når det er registrert dødsfall på minst ett barn`() {
        val behandling = lagBehandling()
        val personInfo = generePersonInfoMedBarn(setOf(barnFnr0.ident, barnFnr1.ident))

        every { personopplysningGrunnlagRepositoryMock.findByBehandlingAndAktiv(any()) } returns
                PersonopplysningGrunnlag(behandlingId = behandling.id, aktiv = true).apply {
                    personer.addAll(listOf(
                            genererPerson(type = PersonType.SØKER, personopplysningGrunnlag = this, ident = gyldigFnr.ident),
                            genererPerson(type = PersonType.BARN, personopplysningGrunnlag = this, ident = barnFnr0.ident,
                                          fødselsDato = LocalDate.now().minusMonths(1)),
                            genererPerson(type = PersonType.BARN, personopplysningGrunnlag = this, ident = barnFnr1.ident,
                                          fødselsDato = LocalDate.now().minusMonths(1))
                    ))
                }
        every { personopplysningerServiceMock.hentDødsfall(Ident(gyldigFnr.ident)) } returns DødsfallData(erDød = false,
                                                                                                          dødsdato = null)
        every { personopplysningerServiceMock.hentDødsfall(Ident(barnFnr0.ident)) } returns DødsfallData(erDød = false,
                                                                                                         dødsdato = null)
        every { personopplysningerServiceMock.hentDødsfall(Ident(barnFnr1.ident)) } returns DødsfallData(erDød = true,
                                                                                                         dødsdato = null)

        every { personopplysningerServiceMock.hentPersoninfoMedRelasjoner(gyldigFnr.ident) } returns personInfo

        every { personopplysningerServiceMock.harVerge(gyldigFnr.ident) } returns VergeResponse(harVerge = false)

        every { localDateServiceMock.now() } returns LocalDate.now().withDayOfMonth(15)

        val fødselshendelsefiltreringResultater = filtreringsreglerService.kjørFiltreringsregler(
                NyBehandlingHendelse(
                        morsIdent = gyldigFnr.ident,
                        barnasIdenter = listOf(barnFnr0.ident,
                                               barnFnr1.ident)),
                behandling)

        Assertions.assertThat(fødselshendelsefiltreringResultater.erOppfylt()).isFalse
        Assertions.assertThat(fødselshendelsefiltreringResultater
                                      .filter { it.resultat == Resultat.IKKE_OPPFYLT }
                                      .any { it.filtreringsregel == Filtreringsregel.BARN_LEVER }
        )
    }

    @Test
    fun `Regelevaluering skal resultere i JA når alle filtreringsregler er oppfylt`() {
        val behandling = lagBehandling()
        val personInfo = generePersonInfoMedBarn(setOf(barnFnr0.ident, barnFnr1.ident))

        every { personopplysningGrunnlagRepositoryMock.findByBehandlingAndAktiv(any()) } returns
                PersonopplysningGrunnlag(behandlingId = behandling.id, aktiv = true).apply {
                    personer.addAll(listOf(
                            genererPerson(type = PersonType.SØKER, personopplysningGrunnlag = this, ident = gyldigFnr.ident),
                            genererPerson(type = PersonType.BARN, personopplysningGrunnlag = this, ident = barnFnr0.ident,
                                          fødselsDato = LocalDate.now().minusMonths(1)),
                            genererPerson(type = PersonType.BARN, personopplysningGrunnlag = this, ident = barnFnr1.ident,
                                          fødselsDato = LocalDate.now().minusMonths(1))
                    ))
                }
        every { personopplysningerServiceMock.hentDødsfall(Ident(gyldigFnr.ident)) } returns DødsfallData(erDød = false,
                                                                                                          dødsdato = null)
        every { personopplysningerServiceMock.hentDødsfall(Ident(barnFnr0.ident)) } returns DødsfallData(erDød = false,
                                                                                                         dødsdato = null)
        every { personopplysningerServiceMock.hentDødsfall(Ident(barnFnr1.ident)) } returns DødsfallData(erDød = false,
                                                                                                         dødsdato = null)

        every { personopplysningerServiceMock.hentPersoninfoMedRelasjoner(gyldigFnr.ident) } returns personInfo

        every { personopplysningerServiceMock.harVerge(gyldigFnr.ident) } returns VergeResponse(harVerge = false)

        every { localDateServiceMock.now() } returns LocalDate.now().withDayOfMonth(20)

        val fødselshendelsefiltreringResultater = filtreringsreglerService.kjørFiltreringsregler(
                NyBehandlingHendelse(
                        morsIdent = gyldigFnr.ident,
                        barnasIdenter = listOf(barnFnr0.ident,
                                               barnFnr1.ident)),
                behandling)

        Assertions.assertThat(fødselshendelsefiltreringResultater.erOppfylt()).isTrue
    }

    private fun genererPerson(type: PersonType,
                              personopplysningGrunnlag: PersonopplysningGrunnlag,
                              ident: String,
                              fødselsDato: LocalDate? = null,
                              grBostedsadresse: GrBostedsadresse? = null,
                              kjønn: Kjønn = Kjønn.KVINNE,
                              sivilstand: SIVILSTAND = SIVILSTAND.UGIFT): Person {
        return Person(aktørId = randomAktørId(),
                      personIdent = PersonIdent(ident),
                      type = type,
                      personopplysningGrunnlag = personopplysningGrunnlag,
                      fødselsdato = fødselsDato ?: LocalDate.of(1991, 1, 1),
                      navn = "navn",
                      kjønn = kjønn,
                      bostedsadresser = grBostedsadresse?.let { mutableListOf(grBostedsadresse) } ?: mutableListOf())
                .apply { this.sivilstander = listOf(GrSivilstand(type = sivilstand, person = this)) }
    }

    private fun generePersonInfoMedBarn(barn: Set<String>? = null,
                                        navn: String = "Noname",
                                        fødselsDato: LocalDate? = null,
                                        adressebeskyttelsegradering: ADRESSEBESKYTTELSEGRADERING = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
                                        bostedsadresse: Bostedsadresse? = null,
                                        sivilstand: SIVILSTAND = SIVILSTAND.UGIFT): PersonInfo {
        return PersonInfo(
                fødselsdato = fødselsDato ?: LocalDate.now().minusYears(20),
                navn = navn,
                adressebeskyttelseGradering = adressebeskyttelsegradering,
                bostedsadresser = bostedsadresse?.let { mutableListOf(it) } ?: mutableListOf(Bostedsadresse()),
                sivilstander = listOf(Sivilstand(type = sivilstand)),
                forelderBarnRelasjon = barn?.map {
                    ForelderBarnRelasjon(personIdent = Personident(it),
                                         relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                                         navn = "navn $it")
                }?.toSet() ?: emptySet()
        )
    }

    private fun genererFaktaMedTidligereBarn(manaderFodselEtt: Long,
                                             manaderFodselTo: Long,
                                             manaderFodselForrigeFodsel: Long,
                                             dagerFodselForrigeFodsel: Long): Fakta {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barn = listOf(
                tilfeldigPerson(LocalDate.now().minusMonths(manaderFodselEtt)).copy(personIdent = barnFnr0),
                tilfeldigPerson(LocalDate.now().minusMonths(manaderFodselTo)).copy(personIdent = barnFnr1)
        )

        val restenAvBarna: List<PersonInfo> = listOf(
                PersonInfo(LocalDate.now().minusMonths(manaderFodselForrigeFodsel).minusDays(dagerFodselForrigeFodsel))
        )

        return Fakta(mor,
                     barn,
                     restenAvBarna,
                     morLever = true,
                     barnaLever = true,
                     morHarVerge = false,
                     dagensDato = LocalDate.now())

    }
}