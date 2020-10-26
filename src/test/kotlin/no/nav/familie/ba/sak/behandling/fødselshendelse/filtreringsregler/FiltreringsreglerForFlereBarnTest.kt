package no.nav.familie.ba.sak.behandling.fødselshendelse.filtreringsregler

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.fødselshendelse.EvaluerFiltreringsreglerForFødselshendelse
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.*
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.*
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.util.FnrGenerator
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FiltreringsreglerForFlereBarnTest {

    val barnFnr0 = PersonIdent(FnrGenerator.generer())
    val barnFnr1 = PersonIdent(FnrGenerator.generer())
    val gyldigFnr = PersonIdent(FnrGenerator.generer())

    val personopplysningGrunnlagRepositoryMock = mockk<PersonopplysningGrunnlagRepository>()
    val personopplysningerServiceMock = mockk<PersonopplysningerService>()
    val localDateServiceMock = mockk<LocalDateService>()
    val evaluerFiltreringsreglerForFødselshendelse = EvaluerFiltreringsreglerForFødselshendelse(
            personopplysningerServiceMock, personopplysningGrunnlagRepositoryMock, localDateServiceMock)

    @Test
    fun `Regelevaluering skal resultere i NEI når det har gått mellom fem dager og fem måneder siden forrige minst ett barn ble født`() {
        val evaluering = Filtreringsregler.hentSamletSpesifikasjon().evaluer(
                genererFaktaMedTidligereBarn(1, 3, 7,0)
        )

        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
        Assertions.assertThat(evaluering.children
                                      .filter { it.resultat == Resultat.NEI }
                                      .any { it.identifikator == Filtreringsregler.MER_ENN_5_MND_SIDEN_FORRIGE_BARN.spesifikasjon.identifikator }
        )
    }

    @Test
    fun `Regelevaluering skal resultere i JA når det har ikke gått mellom fem dager og fem måneder siden forrige minst ett barn ble født`() {
        val evaluering = Filtreringsregler.hentSamletSpesifikasjon().evaluer(
                genererFaktaMedTidligereBarn(0, 0, 0, 5)
        )

        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.JA)
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

        every { personopplysningerServiceMock.hentVergeData(Ident(gyldigFnr.ident)) } returns VergeData(harVerge = false)

        every { localDateServiceMock.now() } returns LocalDate.now().withDayOfMonth(15)

        val (_, evaluering) = evaluerFiltreringsreglerForFødselshendelse.evaluerFiltreringsregler(behandling,
                                                                                                  setOf(barnFnr0.ident,
                                                                                                        barnFnr1.ident))

        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
        Assertions.assertThat(evaluering.children
                                      .filter { it.resultat == Resultat.NEI }
                                      .any { it.identifikator == Filtreringsregler.BARNET_LEVER.spesifikasjon.identifikator }
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

        every { personopplysningerServiceMock.hentVergeData(Ident(gyldigFnr.ident)) } returns VergeData(harVerge = false)

        every { localDateServiceMock.now() } returns LocalDate.now().withDayOfMonth(20)

        val (_, evaluering) = evaluerFiltreringsreglerForFødselshendelse.evaluerFiltreringsregler(behandling,
                                                                                                  setOf(barnFnr0.ident,
                                                                                                        barnFnr1.ident))

        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.JA)
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
                      bostedsadresse = grBostedsadresse,
                      sivilstand = sivilstand)
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
                bostedsadresse = bostedsadresse ?: Bostedsadresse(),
                sivilstand = sivilstand,
                familierelasjoner = barn?.map {
                    Familierelasjon(personIdent = Personident(it),
                                    relasjonsrolle = FAMILIERELASJONSROLLE.BARN,
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
                     barnetLever = true,
                     morHarVerge = false,
                     dagensDato = LocalDate.now())

    }
}