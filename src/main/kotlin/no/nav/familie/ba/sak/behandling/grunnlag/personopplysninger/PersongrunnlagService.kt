package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.opphold.OppholdService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.statsborgerskap.StatsborgerskapService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service

@Service
class PersongrunnlagService(
        private val personRepository: PersonRepository,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val integrasjonClient: IntegrasjonClient,
        private val statsborgerskapService: StatsborgerskapService,
        private val oppholdService: OppholdService,
        private val personopplysningerService: PersonopplysningerService
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

        val personinfo = personopplysningerService.hentPersoninfoFor(fødselsnummer)
        val aktørId = personopplysningerService.hentAktivAktørId(Ident(fødselsnummer))

        val søker = Person(personIdent = behandling.fagsak.hentAktivIdent(),
                           type = PersonType.SØKER,
                           personopplysningGrunnlag = personopplysningGrunnlag,
                           fødselsdato = personinfo.fødselsdato,
                           aktørId = aktørId,
                           navn = personinfo.navn ?: "",
                           bostedsadresse = GrBostedsadresse.fraBostedsadresse(personinfo.bostedsadresse),
                           kjønn = personinfo.kjønn ?: Kjønn.UKJENT,
                           sivilstand = personinfo.sivilstand ?: SIVILSTAND.UOPPGITT
        ).also {
            it.statsborgerskap =  statsborgerskapService.hentStatsborgerskapMedMedlemskapOgHistorikk(Ident(fødselsnummer), it)
            it.opphold = oppholdService.hentOpphold(it)
        }

        personopplysningGrunnlag.personer.add(søker)
        personopplysningGrunnlag.personer.addAll(hentBarn(barnasFødselsnummer, personopplysningGrunnlag))
        secureLogger.info("Setter persongrunnlag med søker: ${fødselsnummer} og barn: ${barnasFødselsnummer}")
        secureLogger.info("Barna på persongrunnlaget som lagres: ${personopplysningGrunnlag.barna.map { it.personIdent.ident }}")
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)
    }

    private fun hentBarn(barnasFødselsnummer: List<String>,
                         personopplysningGrunnlag: PersonopplysningGrunnlag): List<Person> {
        return barnasFødselsnummer.map { nyttBarn ->
            val personinfo = personopplysningerService.hentPersoninfoFor(nyttBarn)
            Person(personIdent = PersonIdent(nyttBarn),
                                         type = PersonType.BARN,
                                         personopplysningGrunnlag = personopplysningGrunnlag,
                                         fødselsdato = personinfo.fødselsdato,
                                         aktørId = personopplysningerService.hentAktivAktørId(Ident(nyttBarn)),
                                         navn = personinfo.navn ?: "",
                                         kjønn = personinfo.kjønn ?: Kjønn.UKJENT,
                                         bostedsadresse = GrBostedsadresse.fraBostedsadresse(personinfo.bostedsadresse),
                                         sivilstand = personinfo.sivilstand ?: SIVILSTAND.UOPPGITT
            ).also {
                it.statsborgerskap =  statsborgerskapService.hentStatsborgerskapMedMedlemskapOgHistorikk(Ident(nyttBarn), it)
                it.opphold = oppholdService.hentOpphold(it)
            }
        }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(this::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}