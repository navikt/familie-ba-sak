package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Utils.storForbokstav
import no.nav.familie.ba.sak.ekstern.restDomene.RestPerson
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPerson
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.filtrerUtKunNorskeBostedsadresser
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.opphold.GrOpphold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.StatsborgerskapService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PersongrunnlagService(
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val statsborgerskapService: StatsborgerskapService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val personopplysningerService: PersonopplysningerService,
    private val personidentService: PersonidentService,
    private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,
    private val behandlingRepository: BehandlingRepository,
    private val personidentRepository: PersonidentRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val loggService: LoggService,
) {

    fun mapTilRestPersonMedStatsborgerskapLand(person: Person): RestPerson {
        val restPerson = person.tilRestPerson()
        restPerson.registerhistorikk?.statsborgerskap
            ?.forEach { lagret ->
                val landkode = lagret.verdi
                val land = statsborgerskapService.hentLand(landkode)
                lagret.verdi = if (land.lowercase().contains("uoppgitt")) "$land ($landkode)" else land.storForbokstav()
            }
        return restPerson
    }

    fun hentSøker(behandlingId: Long): Person? {
        return personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)!!.personer
            .find { person -> person.type == PersonType.SØKER }
    }

    fun hentBarna(behandling: Behandling): List<Person> {
        return personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)!!.personer
            .filter { person -> person.type == PersonType.BARN }
    }

    fun hentPersonerPåBehandling(identer: List<String>, behandling: Behandling): List<Person> {
        val grunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
            ?: throw Feil("Finner ikke personopplysningsgrunnlag på behandling ${behandling.id}")
        return grunnlag.personer.filter { person -> identer.contains(person.personIdent.ident) }
    }

    fun hentAktiv(behandlingId: Long): PersonopplysningGrunnlag? {
        return personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)
    }

    fun oppdaterRegisteropplysninger(behandlingId: Long): PersonopplysningGrunnlag {
        val nåværendeGrunnlag =
            hentAktiv(behandlingId) ?: throw Feil("Ingen aktivt personopplysningsgrunnlag på behandling $behandlingId")
        val behandling = behandlingRepository.finnBehandling(behandlingId)

        if (behandling.status != BehandlingStatus.UTREDES) throw Feil("BehandlingStatus må være UTREDES for å manuelt oppdatere registeropplysninger")
        return hentOgLagreSøkerOgBarnINyttGrunnlag(
            fødselsnummer = nåværendeGrunnlag.søker.personIdent.ident,
            barnasFødselsnummer =
            nåværendeGrunnlag.barna.map { it.personIdent.ident },
            behandling = behandling,
            målform = nåværendeGrunnlag.søker.målform
        )
    }

    /**
     * Legger til barn i nytt personopplysningsgrunnlag
     */
    @Transactional
    fun leggTilBarnIPersonopplysningsgrunnlag(
        nyttBarnIdent: String,
        behandling: Behandling
    ) {
        val personopplysningGrunnlag =
            hentAktiv(behandlingId = behandling.id)
                ?: throw FunksjonellFeil(
                    melding = "Fant ikke personopplysningsgrunnlag på behandling ${behandling.id} ved oppdatering av barn",
                    frontendFeilmelding = "En feil oppsto og barn ble ikke lagt til"
                )

        val barnIGrunnlag = personopplysningGrunnlag.barna.map { it.personIdent.ident }

        if (barnIGrunnlag.contains(nyttBarnIdent)) throw FunksjonellFeil(
            melding = "Forsøker å legge til barn som allerede finnes i personopplysningsgrunnlag ${personopplysningGrunnlag.id}",
            frontendFeilmelding = "Barn finnes allerede på behandling og er derfor ikke lagt til."
        )

        val oppdatertGrunnlag = hentOgLagreSøkerOgBarnINyttGrunnlag(
            personopplysningGrunnlag.søker.personIdent.ident,
            barnIGrunnlag.plus(nyttBarnIdent).toList(),
            behandling,
            personopplysningGrunnlag.søker.målform
        )

        val barnLagtTil = oppdatertGrunnlag.barna.singleOrNull { nyttBarnIdent == it.personIdent.ident }
            ?: throw Feil("Nytt barn ikke lagt til i personopplysningsgrunnlag ${personopplysningGrunnlag.id}")
        loggService.opprettBarnLagtTilLogg(behandling, barnLagtTil)
    }

    fun finnNyeBarn(behandling: Behandling, forrigeBehandling: Behandling?): List<Person> {
        val barnIForrigeGrunnlag = forrigeBehandling?.let { hentAktiv(behandlingId = it.id)?.barna } ?: emptySet()
        val barnINyttGrunnlag =
            behandling.let { hentAktiv(behandlingId = it.id)?.barna }
                ?: throw Feil("Fant ikke personopplysningsgrunnlag")

        return barnINyttGrunnlag.filter { barn -> barnIForrigeGrunnlag.none { barn.personIdent == it.personIdent } }
    }

    /**
     * Registrerer barn valgt i søknad og barn fra forrige behandling
     */
    fun registrerBarnFraSøknad(
        søknadDTO: SøknadDTO,
        behandling: Behandling,
        forrigeBehandlingSomErVedtatt: Behandling? = null
    ) {
        val søkerIdent = søknadDTO.søkerMedOpplysninger.ident
        val valgteBarnsIdenter =
            søknadDTO.barnaMedOpplysninger.filter { it.inkludertISøknaden && it.erFolkeregistrert }
                .map { barn -> barn.ident }

        if (behandling.type == BehandlingType.REVURDERING && forrigeBehandlingSomErVedtatt != null) {
            val forrigePersongrunnlag = hentAktiv(behandlingId = forrigeBehandlingSomErVedtatt.id)
            val forrigePersongrunnlagBarna = forrigePersongrunnlag?.barna?.map { it.personIdent.ident }
                ?.filter {
                    andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(
                        forrigeBehandlingSomErVedtatt.id,
                        it
                    )
                        .isNotEmpty()
                } ?: emptyList()

            hentOgLagreSøkerOgBarnINyttGrunnlag(
                søkerIdent,
                valgteBarnsIdenter.union(forrigePersongrunnlagBarna)
                    .toList(),
                behandling,
                søknadDTO.søkerMedOpplysninger.målform
            )
        } else {
            hentOgLagreSøkerOgBarnINyttGrunnlag(
                søkerIdent,
                valgteBarnsIdenter,
                behandling,
                søknadDTO.søkerMedOpplysninger.målform
            )
        }
    }

    /**
     * Henter oppdatert registerdata og lagrer i nytt aktivt personopplysningsgrunnlag
     */
    @Transactional
    fun hentOgLagreSøkerOgBarnINyttGrunnlag(
        fødselsnummer: String,
        barnasFødselsnummer: List<String>,
        behandling: Behandling,
        målform: Målform
    ): PersonopplysningGrunnlag {
        val personopplysningGrunnlag = lagreOgDeaktiverGammel(PersonopplysningGrunnlag(behandlingId = behandling.id))

        val aktivFødselsnummer = personidentService.hentOgLagreAktivIdentMedAktørId(fødselsnummer)
        val barnasAktiveFødselsnummer = personidentService.hentOgLagreAktiveIdenterMedAktørId(barnasFødselsnummer)

        val enkelPersonInfo = behandling.erMigrering()
        personopplysningGrunnlag.personer.add(
            hentPerson(
                ident = aktivFødselsnummer,
                personopplysningGrunnlag = personopplysningGrunnlag,
                målform = målform,
                personType = PersonType.SØKER,
                enkelPersonInfo = enkelPersonInfo
            )
        )
        barnasAktiveFødselsnummer.forEach {
            personopplysningGrunnlag.personer.add(
                hentPerson(
                    ident = it,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    målform = målform,
                    personType = PersonType.BARN,
                    enkelPersonInfo = enkelPersonInfo
                )
            )
        }

        return personopplysningGrunnlagRepository.save(personopplysningGrunnlag).also {
            /**
             * For sikkerhetsskyld fastsetter vi alltid behandlende enhet når nytt personopplysningsgrunnlag opprettes.
             * Dette gjør vi fordi det kan ha blitt introdusert personer med fortrolig adresse.
             */
            arbeidsfordelingService.fastsettBehandlendeEnhet(behandling)
            saksstatistikkEventPublisher.publiserSaksstatistikk(behandling.fagsak.id)
        }
    }

    private fun hentPerson(
        ident: String,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        målform: Målform,
        personType: PersonType,
        enkelPersonInfo: Boolean = false
    ): Person {
        val personinfo =
            if (enkelPersonInfo) personopplysningerService.hentPersoninfoEnkel(ident)
            else personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(ident)

        val aktivIdent =
            personidentRepository.hentAktivIdent(ident) ?: throw Feil("Personident har ikke aktørid persistert")

        return Person(
            personIdent = PersonIdent(ident),
            type = personType,
            personopplysningGrunnlag = personopplysningGrunnlag,
            fødselsdato = personinfo.fødselsdato,
            aktørId = aktivIdent.aktørId,
            navn = personinfo.navn ?: "",
            kjønn = personinfo.kjønn ?: Kjønn.UKJENT,
            målform = målform
        ).also { person ->
            person.opphold = personinfo.opphold?.map { GrOpphold.fraOpphold(it, person) } ?: emptyList()
            person.statsborgerskap =
                personinfo.statsborgerskap?.map { GrStatsborgerskap.fraStatsborgerskap(it, person) } ?: emptyList()
            person.bostedsadresser =
                personinfo.bostedsadresser.filtrerUtKunNorskeBostedsadresser()
                    .map { GrBostedsadresse.fraBostedsadresse(it, person) }
                    .toMutableList()
            person.sivilstander = personinfo.sivilstander.map { GrSivilstand.fraSivilstand(it, person) }
        }
    }

    fun lagreOgDeaktiverGammel(personopplysningGrunnlag: PersonopplysningGrunnlag): PersonopplysningGrunnlag {
        val aktivPersongrunnlag = hentAktiv(personopplysningGrunnlag.behandlingId)

        if (aktivPersongrunnlag != null) {
            personopplysningGrunnlagRepository.saveAndFlush(aktivPersongrunnlag.also { it.aktiv = false })
        }

        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter persongrunnlag $personopplysningGrunnlag")
        return personopplysningGrunnlagRepository.save(personopplysningGrunnlag)
    }

    fun hentSøkersMålform(behandlingId: Long) =
        hentSøker(behandlingId)?.målform ?: Målform.NB

    companion object {

        private val logger = LoggerFactory.getLogger(PersongrunnlagService::class.java)
    }
}
