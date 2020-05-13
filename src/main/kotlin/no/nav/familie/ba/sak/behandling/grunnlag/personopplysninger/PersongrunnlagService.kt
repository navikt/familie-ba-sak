package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PersongrunnlagService(
        private val personRepository: PersonRepository,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val integrasjonClient: IntegrasjonClient
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
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
                                       ?: personopplysningGrunnlagRepository.save(PersonopplysningGrunnlag(behandlingId = behandling.id))

        if (personopplysningGrunnlag.personer.none { it.personIdent == behandling.fagsak.personIdent }) {
            val personinfo = integrasjonClient.hentPersoninfoFor(fødselsnummer)
            val aktørId = integrasjonClient.hentAktivAktørId(fødselsnummer)
            val søker = Person(personIdent = behandling.fagsak.personIdent,
                               type = PersonType.SØKER,
                               personopplysningGrunnlag = personopplysningGrunnlag,
                               fødselsdato = personinfo.fødselsdato,
                               aktørId = aktørId,
                               navn = personinfo.navn ?: "",
                               kjønn = personinfo.kjønn ?: Kjønn.UKJENT
            )

            personopplysningGrunnlag.personer.add(søker)
        }

        personopplysningGrunnlag.personer.addAll(hentNyeBarn(barnasFødselsnummer, personopplysningGrunnlag))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)
    }

    private fun hentNyeBarn(barnasFødselsnummer: List<String>,
                            personopplysningGrunnlag: PersonopplysningGrunnlag): List<Person> {
        return barnasFødselsnummer.filter { barn ->
            personopplysningGrunnlag.barna.none { eksisterendeBarn -> barn == eksisterendeBarn.personIdent.ident }
        }.map { nyttBarn ->
            val personinfo = integrasjonClient.hentPersoninfoFor(nyttBarn)
            personRepository.save(Person(personIdent = PersonIdent(nyttBarn),
                                         type = PersonType.BARN,
                                         personopplysningGrunnlag = personopplysningGrunnlag,
                                         fødselsdato = personinfo.fødselsdato,
                                         aktørId = integrasjonClient.hentAktørId(nyttBarn),
                                         navn = personinfo.navn ?: "",
                                         kjønn = personinfo.kjønn ?: Kjønn.UKJENT
            ))
        }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(this::class.java)
    }
}