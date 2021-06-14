package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.arbeidsforhold.ArbeidsforholdService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.opphold.GrOpphold
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.opphold.OppholdService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.statsborgerskap.StatsborgerskapService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.restDomene.RestPerson
import no.nav.familie.ba.sak.behandling.restDomene.SøknadDTO
import no.nav.familie.ba.sak.behandling.restDomene.tilRestPerson
import no.nav.familie.ba.sak.behandling.vilkår.finnNåværendeMedlemskap
import no.nav.familie.ba.sak.behandling.vilkår.finnSterkesteMedlemskap
import no.nav.familie.ba.sak.behandling.vilkår.personHarLøpendeArbeidsforhold
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Utils.storForbokstav
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PersongrunnlagService(
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val statsborgerskapService: StatsborgerskapService,
        private val oppholdService: OppholdService,
        private val arbeidsforholdService: ArbeidsforholdService,
        private val arbeidsfordelingService: ArbeidsfordelingService,
        private val personopplysningerService: PersonopplysningerService,
        private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,
        private val featureToggleService: FeatureToggleService,
        private val behandlingRepository: BehandlingRepository,
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

    fun hentPersonPåBehandling(personIdent: PersonIdent, behandling: Behandling): Person? {
        return personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)!!.personer
                .find { person -> person.personIdent == personIdent }
    }

    fun hentAktiv(behandlingId: Long): PersonopplysningGrunnlag? {
        return personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)
    }

    fun oppdaterRegisteropplysninger(behandlingId: Long): PersonopplysningGrunnlag {
        val nåværendeGrunnlag =
                hentAktiv(behandlingId) ?: throw Feil("Ingen aktivt personopplysningsgrunnlag på behandling $behandlingId")
        val behandling = behandlingRepository.finnBehandling(behandlingId)

        if (behandling.status != BehandlingStatus.UTREDES) throw Feil("BehandlingStatus må være UTREDES for å manuelt oppdatere registeropplysninger")
        return hentOgLagreSøkerOgBarnINyttGrunnlag(fødselsnummer = nåværendeGrunnlag.søker.personIdent.ident,
                                                   barnasFødselsnummer =
                                                   nåværendeGrunnlag.barna.map { it.personIdent.ident },
                                                   behandling = behandling,
                                                   målform = nåværendeGrunnlag.søker.målform)
    }

    /**
     * Registrerer barn valgt i søknad og barn fra forrige behandling
     */
    fun registrerBarnFraSøknad(søknadDTO: SøknadDTO, behandling: Behandling, forrigeBehandling: Behandling? = null) {
        val søkerIdent = søknadDTO.søkerMedOpplysninger.ident
        val valgteBarnsIdenter =
                søknadDTO.barnaMedOpplysninger.filter { it.inkludertISøknaden && it.erFolkeregistrert }.map { barn -> barn.ident }

        if (behandling.type == BehandlingType.REVURDERING && forrigeBehandling != null) {
            val forrigePersongrunnlag = hentAktiv(behandlingId = forrigeBehandling.id)
            val forrigePersongrunnlagBarna = forrigePersongrunnlag?.barna?.map { it.personIdent.ident }!!

            hentOgLagreSøkerOgBarnINyttGrunnlag(søkerIdent,
                                                valgteBarnsIdenter.union(forrigePersongrunnlagBarna)
                                                        .toList(),
                                                behandling,
                                                søknadDTO.søkerMedOpplysninger.målform)
        } else {
            hentOgLagreSøkerOgBarnINyttGrunnlag(søkerIdent,
                                                valgteBarnsIdenter,
                                                behandling,
                                                søknadDTO.søkerMedOpplysninger.målform)
        }
    }

    /**
     * Henter oppdatert registerdata og lagrer i nytt aktivt personopplysningsgrunnlag
     */
    @Transactional
    fun hentOgLagreSøkerOgBarnINyttGrunnlag(fødselsnummer: String,
                                            barnasFødselsnummer: List<String>,
                                            behandling: Behandling,
                                            målform: Målform): PersonopplysningGrunnlag {
        val personopplysningGrunnlag = lagreOgDeaktiverGammel(PersonopplysningGrunnlag(behandlingId = behandling.id))

        val personinfo = personopplysningerService.hentPersoninfoMedRelasjoner(fødselsnummer)
        val aktørId = personopplysningerService.hentAktivAktørId(Ident(fødselsnummer))

        val søker = Person(personIdent = behandling.fagsak.hentAktivIdent(),
                           type = PersonType.SØKER,
                           personopplysningGrunnlag = personopplysningGrunnlag,
                           fødselsdato = personinfo.fødselsdato,
                           aktørId = aktørId,
                           navn = personinfo.navn ?: "",
                           kjønn = personinfo.kjønn ?: Kjønn.UKJENT,
                           målform = målform
        ).also { person ->
            person.bostedsadresser =
                    personinfo.bostedsadresser.map { GrBostedsadresse.fraBostedsadresse(it, person) }.toMutableList()
            person.sivilstander = if (personinfo.sivilstander.isEmpty()) {
                listOf(GrSivilstand(type = SIVILSTAND.UOPPGITT, person = person))
            } else {
                personinfo.sivilstander.map { GrSivilstand.fraSivilstand(it, person) }
            }
        }

        personopplysningGrunnlag.personer.add(søker)
        personopplysningGrunnlag.personer.addAll(hentBarn(barnasFødselsnummer, personopplysningGrunnlag))

        val brukRegisteropplysningerIManuellBehandling =
                featureToggleService.isEnabled(FeatureToggleConfig.SKJØNNSMESSIGVURDERING)

        if (behandling.skalBehandlesAutomatisk && !behandling.erMigrering()) {
            søker.also {
                it.statsborgerskap = statsborgerskapService.hentStatsborgerskapMedMedlemskapOgHistorikk(Ident(fødselsnummer), it)
                it.bostedsadresseperiode = personopplysningerService.hentBostedsadresseperioder(it.personIdent.ident)
            }

            val søkersMedlemskap = finnNåværendeSterkesteMedlemskap(søker.statsborgerskap)
            if (søkersMedlemskap == Medlemskap.EØS) {
                søker.arbeidsforhold = arbeidsforholdService.hentArbeidsforhold(Ident(fødselsnummer), søker)

                if (!personHarLøpendeArbeidsforhold(søker)) {
                    hentFarEllerMedmor(barnasFødselsnummer.first(), personopplysningGrunnlag)
                            ?.let { personopplysningGrunnlag.personer.add(it) }
                }
            } else if (søkersMedlemskap != Medlemskap.NORDEN) {
                søker.opphold = oppholdService.hentOpphold(søker)
            }
        } else if (!behandling.erMigrering() && brukRegisteropplysningerIManuellBehandling) {
            personopplysningGrunnlag.personer.forEach { person ->

                val personinfoManuell = personopplysningerService.hentHistoriskPersoninfoManuell(person.personIdent.ident)

                person.opphold = personinfoManuell.opphold?.map { GrOpphold.fraOpphold(it, person) } ?: emptyList()
                person.statsborgerskap =
                        personinfoManuell.statsborgerskap?.map { GrStatsborgerskap.fraStatsborgerskap(it, person) } ?: emptyList()
                person.bostedsadresser =
                        personinfoManuell.bostedsadresser.map { GrBostedsadresse.fraBostedsadresse(it, person) }.toMutableList()
                person.sivilstander = personinfoManuell.sivilstander.map { GrSivilstand.fraSivilstand(it, person) }
            }
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

    private fun hentBarn(barnasFødselsnummer: List<String>,
                         personopplysningGrunnlag: PersonopplysningGrunnlag): List<Person> {
        return barnasFødselsnummer.map { barn ->
            val personinfo = personopplysningerService.hentPersoninfoMedRelasjoner(barn)
            Person(
                    personIdent = PersonIdent(barn),
                    type = PersonType.BARN,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    fødselsdato = personinfo.fødselsdato,
                    aktørId = personopplysningerService.hentAktivAktørId(Ident(barn)),
                    navn = personinfo.navn ?: "",
                    kjønn = personinfo.kjønn ?: Kjønn.UKJENT,
            ).also { person ->
                person.bostedsadresser =
                        personinfo.bostedsadresser.map { GrBostedsadresse.fraBostedsadresse(it, person) }.toMutableList()
                person.sivilstander = if (personinfo.sivilstander.isEmpty()) {
                    listOf(GrSivilstand(type = SIVILSTAND.UOPPGITT, person = person))
                } else {
                    personinfo.sivilstander.map { GrSivilstand.fraSivilstand(it, person) }
                }
            }
        }
    }

    private fun hentFarEllerMedmor(barnetsFødselsnummer: String,
                                   personopplysningGrunnlag: PersonopplysningGrunnlag): Person? {
        val barnPersoninfo = personopplysningerService.hentPersoninfoMedRelasjoner(barnetsFødselsnummer)
        val farEllerMedmorRelasjon =
                barnPersoninfo.forelderBarnRelasjon.singleOrNull { it.relasjonsrolle == FORELDERBARNRELASJONROLLE.FAR || it.relasjonsrolle == FORELDERBARNRELASJONROLLE.MEDMOR }
        return if (farEllerMedmorRelasjon != null) {
            val farEllerMedmorPersonIdent = farEllerMedmorRelasjon.personIdent.id
            val personinfo = personopplysningerService.hentPersoninfoMedRelasjoner(farEllerMedmorPersonIdent)
            val farEllerMedmor = Person(personIdent = PersonIdent(farEllerMedmorPersonIdent),
                                        type = PersonType.ANNENPART,
                                        personopplysningGrunnlag = personopplysningGrunnlag,
                                        fødselsdato = personinfo.fødselsdato,
                                        aktørId = personopplysningerService.hentAktivAktørId(Ident(farEllerMedmorPersonIdent)),
                                        navn = personinfo.navn ?: "",
                                        kjønn = personinfo.kjønn ?: Kjønn.UKJENT,
            ).also { person ->
                person.statsborgerskap =
                        statsborgerskapService.hentStatsborgerskapMedMedlemskapOgHistorikk(Ident(farEllerMedmorPersonIdent),
                                                                                           person)
                person.bostedsadresser =
                        personinfo.bostedsadresser.map { GrBostedsadresse.fraBostedsadresse(it, person) }.toMutableList()

                person.sivilstander =
                        personinfo.sivilstander.map { GrSivilstand.fraSivilstand(it, person) }
            }

            val farEllerMedmorsStatsborgerskap = finnNåværendeSterkesteMedlemskap(farEllerMedmor.statsborgerskap)

            if (farEllerMedmorsStatsborgerskap == Medlemskap.EØS) {
                farEllerMedmor.arbeidsforhold =
                        arbeidsforholdService.hentArbeidsforhold(Ident(farEllerMedmorPersonIdent), farEllerMedmor)
            }
            farEllerMedmor
        } else null
    }

    fun lagreOgDeaktiverGammel(personopplysningGrunnlag: PersonopplysningGrunnlag): PersonopplysningGrunnlag {
        val aktivPersongrunnlag = hentAktiv(personopplysningGrunnlag.behandlingId)

        if (aktivPersongrunnlag != null) {
            personopplysningGrunnlagRepository.saveAndFlush(aktivPersongrunnlag.also { it.aktiv = false })
        }

        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter persongrunnlag $personopplysningGrunnlag")
        return personopplysningGrunnlagRepository.save(personopplysningGrunnlag)
    }

    private fun finnNåværendeSterkesteMedlemskap(statsborgerskap: List<GrStatsborgerskap>?): Medlemskap? {
        val nåværendeMedlemskap = finnNåværendeMedlemskap(statsborgerskap)

        return finnSterkesteMedlemskap(nåværendeMedlemskap)
    }

    fun hentSøkersMålform(behandlingId: Long) =
            hentSøker(behandlingId)?.målform ?: Målform.NB

    companion object {

        private val logger = LoggerFactory.getLogger(PersongrunnlagService::class.java)
    }
}
