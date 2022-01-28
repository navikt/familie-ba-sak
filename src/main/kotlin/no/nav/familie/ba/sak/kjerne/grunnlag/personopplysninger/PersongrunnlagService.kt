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
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.opphold.GrOpphold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.StatsborgerskapService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class PersongrunnlagService(
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val statsborgerskapService: StatsborgerskapService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val personopplysningerService: PersonopplysningerService,
    private val personidentService: PersonidentService,
    private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,
    private val behandlingRepository: BehandlingRepository,
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
        val aktørIder = personidentService.hentOgLagreAktørIder(identer)

        val grunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
            ?: throw Feil("Finner ikke personopplysningsgrunnlag på behandling ${behandling.id}")
        return grunnlag.personer.filter { person -> aktørIder.contains(person.aktør) }
    }

    fun hentAktiv(behandlingId: Long): PersonopplysningGrunnlag? {
        return personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)
    }

    fun hentAktivThrows(behandlingId: Long): PersonopplysningGrunnlag {
        return hentAktiv(behandlingId = behandlingId)
            ?: throw Feil("Finner ikke personopplysningsgrunnlag på behandling $behandlingId")
    }

    fun oppdaterRegisteropplysninger(behandlingId: Long): PersonopplysningGrunnlag {
        val nåværendeGrunnlag = hentAktivThrows(behandlingId)
        val behandling = behandlingRepository.finnBehandling(behandlingId)

        if (behandling.status != BehandlingStatus.UTREDES) throw Feil("BehandlingStatus må være UTREDES for å manuelt oppdatere registeropplysninger")
        return hentOgLagreSøkerOgBarnINyttGrunnlag(
            aktør = nåværendeGrunnlag.søker.aktør,
            barnasAktør = nåværendeGrunnlag.barna.map { it.aktør },
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
        val nyttbarnAktør = personidentService.hentOgLagreAktør(nyttBarnIdent, true)

        val personopplysningGrunnlag = hentAktivThrows(behandlingId = behandling.id)

        val barnIGrunnlag = personopplysningGrunnlag.barna.map { it.aktør }

        if (barnIGrunnlag.contains(nyttbarnAktør)) throw FunksjonellFeil(
            melding = "Forsøker å legge til barn som allerede finnes i personopplysningsgrunnlag ${personopplysningGrunnlag.id}",
            frontendFeilmelding = "Barn finnes allerede på behandling og er derfor ikke lagt til."
        )

        val oppdatertGrunnlag = hentOgLagreSøkerOgBarnINyttGrunnlag(
            personopplysningGrunnlag.søker.aktør,
            barnIGrunnlag.plus(nyttbarnAktør).toList(),
            behandling,
            personopplysningGrunnlag.søker.målform
        )

        val barnLagtTil = oppdatertGrunnlag.barna.singleOrNull { nyttbarnAktør == it.aktør }
            ?: throw Feil("Nytt barn ikke lagt til i personopplysningsgrunnlag ${personopplysningGrunnlag.id}")
        loggService.opprettBarnLagtTilLogg(behandling, barnLagtTil)
    }

    fun finnNyeBarn(behandling: Behandling, forrigeBehandling: Behandling?): List<Person> {
        val barnIForrigeGrunnlag = forrigeBehandling?.let { hentAktiv(behandlingId = it.id)?.barna } ?: emptySet()
        val barnINyttGrunnlag = behandling.let { hentAktivThrows(behandlingId = it.id).barna }

        return barnINyttGrunnlag.filter { barn -> barnIForrigeGrunnlag.none { barn.aktør == it.aktør } }
    }

    /**
     * Registrerer barn valgt i søknad og barn fra forrige behandling
     */
    fun registrerBarnFraSøknad(
        søknadDTO: SøknadDTO,
        behandling: Behandling,
        forrigeBehandlingSomErVedtatt: Behandling? = null
    ) {
        val søkerAktør = personidentService.hentOgLagreAktør(søknadDTO.søkerMedOpplysninger.ident, true)
        val valgteBarnsAktør =
            søknadDTO.barnaMedOpplysninger.filter { it.inkludertISøknaden && it.erFolkeregistrert }
                .map { barn -> personidentService.hentOgLagreAktør(barn.ident, true) }

        if (behandling.type == BehandlingType.REVURDERING && forrigeBehandlingSomErVedtatt != null) {
            val forrigePersongrunnlag = hentAktiv(behandlingId = forrigeBehandlingSomErVedtatt.id)
            val forrigePersongrunnlagBarna = forrigePersongrunnlag?.barna?.map { it.aktør }
                ?.filter {
                    andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(
                        forrigeBehandlingSomErVedtatt.id,
                        it
                    )
                        .isNotEmpty()
                } ?: emptyList()

            hentOgLagreSøkerOgBarnINyttGrunnlag(
                søkerAktør,
                valgteBarnsAktør.union(forrigePersongrunnlagBarna)
                    .toList(),
                behandling,
                søknadDTO.søkerMedOpplysninger.målform
            )
        } else {
            hentOgLagreSøkerOgBarnINyttGrunnlag(
                søkerAktør,
                valgteBarnsAktør,
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
        aktør: Aktør,
        barnasAktør: List<Aktør>,
        behandling: Behandling,
        målform: Målform
    ): PersonopplysningGrunnlag {
        val personopplysningGrunnlag = lagreOgDeaktiverGammel(PersonopplysningGrunnlag(behandlingId = behandling.id))

        val enkelPersonInfo = behandling.erMigrering() || behandling.erSatsendring()
        personopplysningGrunnlag.personer.add(
            hentPerson(
                aktør = aktør,
                personopplysningGrunnlag = personopplysningGrunnlag,
                målform = målform,
                personType = PersonType.SØKER,
                enkelPersonInfo = enkelPersonInfo
            )
        )
        barnasAktør.forEach { barnsAktør ->

            personopplysningGrunnlag.personer.add(
                hentPerson(
                    aktør = barnsAktør,
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
        aktør: Aktør,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        målform: Målform,
        personType: PersonType,
        enkelPersonInfo: Boolean = false
    ): Person {
        val personinfo =
            if (enkelPersonInfo) personopplysningerService.hentPersoninfoEnkel(aktør)
            else personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(aktør)

        return Person(
            type = personType,
            personopplysningGrunnlag = personopplysningGrunnlag,
            fødselsdato = personinfo.fødselsdato,
            aktør = aktør,
            navn = personinfo.navn ?: "",
            kjønn = personinfo.kjønn ?: Kjønn.UKJENT,
            målform = målform,
            dødsfallDato = LocalDate.parse(personinfo.dødsfall?.dødsdato) // TODO kanskje fikse på
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
