package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.domene.Ident
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.personinfo.Bostedsadresse
import no.nav.familie.kontrakter.felles.personinfo.Vegadresse
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PersongrunnlagService(
        private val personRepository: PersonRepository,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val integrasjonClient: IntegrasjonClient,
        private val environment: Environment
) {

    fun lagreOgDeaktiverGammel(personopplysningGrunnlag: PersonopplysningGrunnlag): PersonopplysningGrunnlag {
        val aktivPersongrunnlag = hentAktiv(personopplysningGrunnlag.behandlingId)

        if (aktivPersongrunnlag != null) {
            personopplysningGrunnlagRepository.saveAndFlush(aktivPersongrunnlag.also { it.aktiv = false })
        }

        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter persongrunnlag $personopplysningGrunnlag")
        return personopplysningGrunnlagRepository.save(personopplysningGrunnlag)
    }

    fun hentSøker(behandling: Behandling): Person? {
        return personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)!!.personer
                .find { person -> person.type == PersonType.SØKER }
    }

    fun hentBarna(behandling: Behandling): List<Person> {
        return personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)!!.personer
                .filter { person -> person.type == PersonType.BARN }
    }

    fun hentAktiv(behandlingId: Long): PersonopplysningGrunnlag? {
        return personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)
    }

    fun lagreSøkerOgBarnIPersonopplysningsgrunnlaget(fødselsnummer: String,
                                                     barnasFødselsnummer: List<String>,
                                                     behandling: Behandling) {
        val personopplysningGrunnlag = lagreOgDeaktiverGammel(PersonopplysningGrunnlag(behandlingId = behandling.id))

        var søker : Person?= null
        var barn : List<Person>?= null

        if(environment.activeProfiles.contains("e2e")){
            søker= e2eMockSøker(fødselsnummer, personopplysningGrunnlag)
            barn= e2eMockBarn(barnasFødselsnummer, personopplysningGrunnlag)
            LOG.info("Mock søker and barn person information for e2e: Søker $søker Barn $barn")
        }else{
            val personinfo = integrasjonClient.hentPersoninfoFor(fødselsnummer)
            val aktørId = integrasjonClient.hentAktivAktørId(Ident(fødselsnummer))
            søker = Person(personIdent = behandling.fagsak.hentAktivIdent(),
                               type = PersonType.SØKER,
                               personopplysningGrunnlag = personopplysningGrunnlag,
                               fødselsdato = personinfo.fødselsdato,
                               aktørId = aktørId,
                               navn = personinfo.navn ?: "",
                               bostedsadresse = GrBostedsadresse.fraBostedsadresse(personinfo.bostedsadresse),
                               kjønn = personinfo.kjønn ?: Kjønn.UKJENT
            )
            barn = hentBarn(barnasFødselsnummer, personopplysningGrunnlag)
        }
        personopplysningGrunnlag.personer.add(søker!!)
        personopplysningGrunnlag.personer.addAll(barn!!)

        secureLogger.info("Setter persongrunnlag med søker: ${fødselsnummer} og barn: ${barnasFødselsnummer}")
        secureLogger.info("Barna på persongrunnlaget som lagres: ${personopplysningGrunnlag.barna.map { it.personIdent.ident }}")
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)
    }

    private fun e2eMockSøker(fødselsnummer: String, personopplysningGrunnlag: PersonopplysningGrunnlag): Person {
        return Person(personIdent = PersonIdent(fødselsnummer),
                      type = PersonType.SØKER,
                      personopplysningGrunnlag = personopplysningGrunnlag,
                      fødselsdato = LocalDate.of(1990, 1, 1),
                      aktørId = AktørId("e2eaktoerid"),
                      navn = "Etoe Tester",
                      bostedsadresse = GrBostedsadresse.fraBostedsadresse(Bostedsadresse(vegadresse = Vegadresse(
                              1, "1", "B", "H101",
                              "Testerveg", "0101", "whatever", "1011"
                      ))),
                      kjønn = Kjønn.KVINNE
        )
    }

    private fun e2eMockBarn(barnasFødselsnummer: List<String>, personopplysningGrunnlag: PersonopplysningGrunnlag): List<Person> {
        return listOf(Person(personIdent = if (!barnasFødselsnummer.isEmpty()) PersonIdent(barnasFødselsnummer.first()) else PersonIdent(
                "12345678910"),
                             type = PersonType.BARN,
                             personopplysningGrunnlag = personopplysningGrunnlag,
                             fødselsdato = LocalDate.of(2009, 1, 1),
                             aktørId = AktørId("e2ebarnaktoerid"),
                             navn = "Etoe Barnet",
                             bostedsadresse = GrBostedsadresse.fraBostedsadresse(Bostedsadresse(vegadresse = Vegadresse(
                                     1, "1", "B", "H101",
                                     "Testerveg", "0101", "whatever", "1011"
                             ))),
                             kjønn = Kjønn.KVINNE
        ))
    }

    private fun tilGrBostedsadresse(bostedsadresse: Bostedsadresse?): Set<GrBostedsadresse> {
        val grBostedsadresser: Set<GrBostedsadresse> = mutableSetOf()

        bostedsadresse?.matrikkeladresse?.let { grBostedsadresser.plus(it) }
        bostedsadresse?.ukjentBosted?.let { grBostedsadresser.plus(it) }
        bostedsadresse?.vegadresse?.let { grBostedsadresser.plus(it) }

        return grBostedsadresser
    }

    private fun hentBarn(barnasFødselsnummer: List<String>,
                         personopplysningGrunnlag: PersonopplysningGrunnlag): List<Person> {
        return barnasFødselsnummer.map { nyttBarn ->
            val personinfo = integrasjonClient.hentPersoninfoFor(nyttBarn)
            personRepository.save(Person(personIdent = PersonIdent(nyttBarn),
                                         type = PersonType.BARN,
                                         personopplysningGrunnlag = personopplysningGrunnlag,
                                         fødselsdato = personinfo.fødselsdato,
                                         aktørId = integrasjonClient.hentAktivAktørId(Ident(nyttBarn)),
                                         navn = personinfo.navn ?: "",
                                         kjønn = personinfo.kjønn ?: Kjønn.UKJENT,
                                         bostedsadresse = GrBostedsadresse.fraBostedsadresse(personinfo.bostedsadresse)
            ))
        }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(this::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}