package no.nav.familie.ba.sak.behandling.fødselshendelse.filtreringsregler

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.fødselshendelse.EvaluerFiltreringsreglerForFødselshendelse
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.*
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.*
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import no.nav.nare.core.evaluations.Resultat
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FilteringsreglerForFlereBarnTest {
    val dnummer0 = PersonIdent("42345678910")
    val dnummer1 = PersonIdent("52345678910")
    val barnFnr0 = PersonIdent("22345678910")
    val barnFnr1 = PersonIdent("21345678910")
    val gyldigFnr = PersonIdent("12345678910")

    val personopplysningGrunnlagRepositoryMock = mockk<PersonopplysningGrunnlagRepository>()
    val personopplysningerServiceMock = mockk<PersonopplysningerService>()
    val evaluerFiltreringsreglerForFødselshendelse = EvaluerFiltreringsreglerForFødselshendelse(
            personopplysningerServiceMock, personopplysningGrunnlagRepositoryMock)

    @Test
    fun `Regelevaluering skal resultere i NEI når minst ett barn har D-nummer`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barn = listOf(
                tilfeldigPerson(LocalDate.now()).copy(personIdent = barnFnr0),
                tilfeldigPerson(LocalDate.now()).copy(personIdent = dnummer0)
        )
        val restenAvBarna: List<PersonInfo> = listOf()

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, barn, restenAvBarna, morLever = true, barnetLever = true, morHarVerge = false))

        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
        Assertions.assertThat(evaluering.children.filter { it.resultat == Resultat.NEI }.size).isEqualTo(1)
        Assertions.assertThat(evaluering.children.filter { it.resultat == Resultat.NEI }[0].identifikator).isEqualTo(
                Filtreringsregler.BARNET_HAR_GYLDIG_FOEDSELSNUMMER.spesifikasjon.identifikator)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når minst ett barn er over 6 måneder`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barn = listOf(
                tilfeldigPerson(LocalDate.now().minusMonths(1)).copy(personIdent = barnFnr0),
                tilfeldigPerson(LocalDate.now().minusYears(1)).copy(personIdent = barnFnr1)
        )

        val restenAvBarna: List<PersonInfo> = listOf()

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, barn, restenAvBarna, morLever = true, barnetLever = true, morHarVerge = false))

        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
        Assertions.assertThat(evaluering.children.filter { it.resultat == Resultat.NEI }.size).isEqualTo(1)
        Assertions.assertThat(evaluering.children.filter { it.resultat == Resultat.NEI }[0].identifikator).isEqualTo(
                Filtreringsregler.BARNET_ER_UNDER_6_MND.spesifikasjon.identifikator)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når det har gått mindre enn 5 måneder siden forrige minst ett barn ble født`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(personIdent = gyldigFnr)
        val barn = listOf(
                tilfeldigPerson(LocalDate.now().minusMonths(1)).copy(personIdent = barnFnr0),
                tilfeldigPerson(LocalDate.now().minusMonths(3)).copy(personIdent = barnFnr1)
        )

        val restenAvBarna: List<PersonInfo> = listOf(
                PersonInfo(LocalDate.now().minusMonths(7))
        )

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon()
                .evaluer(Fakta(mor, barn, restenAvBarna, morLever = true, barnetLever = true, morHarVerge = false))

        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
        Assertions.assertThat(evaluering.children.filter { it.resultat == Resultat.NEI }.size).isEqualTo(1)
        Assertions.assertThat(evaluering.children.filter { it.resultat == Resultat.NEI }[0].identifikator).isEqualTo(
                Filtreringsregler.MER_ENN_5_MND_SIDEN_FORRIGE_BARN.spesifikasjon.identifikator)
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

        every { personopplysningerServiceMock.hentPersoninfoFor(gyldigFnr.ident) } returns personInfo

        every { personopplysningerServiceMock.hentVergeData(Ident(gyldigFnr.ident)) } returns VergeData(harVerge = false)

        val evaluering = evaluerFiltreringsreglerForFødselshendelse.evaluerFiltreringsregler(behandling,
                                                                                             setOf(barnFnr0.ident,
                                                                                                   barnFnr1.ident))

        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
        Assertions.assertThat(evaluering.children.filter { it.resultat == Resultat.NEI }.size).isEqualTo(1)
        Assertions.assertThat(evaluering.children.filter { it.resultat == Resultat.NEI }[0].identifikator).isEqualTo(
                Filtreringsregler.BARNET_LEVER.spesifikasjon.identifikator)
    }

    @Test
    fun `Regelevaluering skal resultere i JA når alle filtreringsregler er oppfylt`(){
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

        every { personopplysningerServiceMock.hentPersoninfoFor(gyldigFnr.ident) } returns personInfo

        every { personopplysningerServiceMock.hentVergeData(Ident(gyldigFnr.ident)) } returns VergeData(harVerge = false)

        val evaluering = evaluerFiltreringsreglerForFødselshendelse.evaluerFiltreringsregler(behandling,
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
                                    navn = "navn ${it}")
                }?.toSet() ?: emptySet()
        )
    }
}