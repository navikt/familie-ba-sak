package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.arbeidsforhold.ArbeidsforholdService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.opphold.OppholdService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.statsborgerskap.StatsborgerskapService
import no.nav.familie.ba.sak.behandling.vilkår.finnNåværendeMedlemskap
import no.nav.familie.ba.sak.behandling.vilkår.finnSterkesteMedlemskap
import no.nav.familie.ba.sak.behandling.vilkår.personHarLøpendeArbeidsforhold
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.FAMILIERELASJONSROLLE
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PersongrunnlagService(
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val statsborgerskapService: StatsborgerskapService,
        private val oppholdService: OppholdService,
        private val arbeidsforholdService: ArbeidsforholdService,
        private val arbeidsfordelingService: ArbeidsfordelingService,
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
                                                     behandling: Behandling,
                                                     målform: Målform) {
        val personopplysningGrunnlag = lagreOgDeaktiverGammel(PersonopplysningGrunnlag(behandlingId = behandling.id))

        val personinfo = personopplysningerService.hentPersoninfoMedRelasjoner(fødselsnummer)
        val aktørId = personopplysningerService.hentAktivAktørId(Ident(fødselsnummer))

        val søker = Person(personIdent = behandling.fagsak.hentAktivIdent(),
                           type = PersonType.SØKER,
                           personopplysningGrunnlag = personopplysningGrunnlag,
                           fødselsdato = personinfo.fødselsdato,
                           aktørId = aktørId,
                           navn = personinfo.navn ?: "",
                           bostedsadresse = GrBostedsadresse.fraBostedsadresse(personinfo.bostedsadresse),
                           kjønn = personinfo.kjønn ?: Kjønn.UKJENT,
                           sivilstand = personinfo.sivilstand ?: SIVILSTAND.UOPPGITT,
                           målform = målform
        )

        personopplysningGrunnlag.personer.add(søker)
        personopplysningGrunnlag.personer.addAll(hentBarn(barnasFødselsnummer, personopplysningGrunnlag))

        if (behandling.opprinnelse == BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE) {
            søker.also {
                it.statsborgerskap = statsborgerskapService.hentStatsborgerskapMedMedlemskapOgHistorikk(Ident(fødselsnummer), it)
                it.bostedsadresseperiode = personopplysningerService.hentBostedsadresseperioder(it.personIdent.ident)
            }

            val søkersMedlemskap = finnNåværendeSterkesteMedlemskap(søker.statsborgerskap)
            if (søkersMedlemskap == Medlemskap.EØS) {
                søker.arbeidsforhold = arbeidsforholdService.hentArbeidsforhold(Ident(fødselsnummer), søker)

                if (!personHarLøpendeArbeidsforhold(søker)) {
                    leggTilFarEllerMedmor(barnasFødselsnummer.first(), personopplysningGrunnlag)
                }
            } else if (søkersMedlemskap != Medlemskap.NORDEN) {
                søker.opphold = oppholdService.hentOpphold(søker)
            }
        }

        personopplysningGrunnlagRepository.save(personopplysningGrunnlag).also {
            /**
             * For sikkerhetsskyld fastsetter vi alltid behandlende enhet når nytt personopplysningsgrunnlag opprettes.
             * Dette gjør vi fordi det kan ha blitt introdusert personer med fortrolig adresse.
             */
            arbeidsfordelingService.fastsettBehandlendeEnhet(behandling)
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
                    bostedsadresse = GrBostedsadresse.fraBostedsadresse(personinfo.bostedsadresse),
                    sivilstand = personinfo.sivilstand ?: SIVILSTAND.UOPPGITT,
            )
        }
    }

    private fun leggTilFarEllerMedmor(barnetsFødselsnummer: String,
                                      personopplysningGrunnlag: PersonopplysningGrunnlag) {
        val barnPersoninfo = personopplysningerService.hentPersoninfoMedRelasjoner(barnetsFødselsnummer)
        val farEllerMedmorRelasjon =
                barnPersoninfo.familierelasjoner.singleOrNull { it.relasjonsrolle == FAMILIERELASJONSROLLE.FAR || it.relasjonsrolle == FAMILIERELASJONSROLLE.MEDMOR }
        if (farEllerMedmorRelasjon != null) {
            val farEllerMedmorPersonIdent = farEllerMedmorRelasjon.personIdent.id
            val personinfo = personopplysningerService.hentPersoninfoMedRelasjoner(farEllerMedmorPersonIdent)
            val farEllerMedmor = Person(personIdent = PersonIdent(farEllerMedmorPersonIdent),
                                        type = PersonType.ANNENPART,
                                        personopplysningGrunnlag = personopplysningGrunnlag,
                                        fødselsdato = personinfo.fødselsdato,
                                        aktørId = personopplysningerService.hentAktivAktørId(Ident(farEllerMedmorPersonIdent)),
                                        navn = personinfo.navn ?: "",
                                        kjønn = personinfo.kjønn ?: Kjønn.UKJENT,
                                        bostedsadresse = GrBostedsadresse.fraBostedsadresse(personinfo.bostedsadresse),
                                        sivilstand = personinfo.sivilstand ?: SIVILSTAND.UOPPGITT
            ).also {
                it.statsborgerskap =
                        statsborgerskapService.hentStatsborgerskapMedMedlemskapOgHistorikk(Ident(farEllerMedmorPersonIdent),
                                                                                           it)
            }

            val farEllerMedmorsStatsborgerskap = finnNåværendeSterkesteMedlemskap(farEllerMedmor.statsborgerskap)

            if (farEllerMedmorsStatsborgerskap == Medlemskap.EØS) {
                farEllerMedmor.arbeidsforhold =
                        arbeidsforholdService.hentArbeidsforhold(Ident(farEllerMedmorPersonIdent), farEllerMedmor)
            }

            personopplysningGrunnlag.personer.add(farEllerMedmor)
        }
    }

    private fun finnNåværendeSterkesteMedlemskap(statsborgerskap: List<GrStatsborgerskap>?): Medlemskap? {
        val nåværendeMedlemskap = finnNåværendeMedlemskap(statsborgerskap)

        return finnSterkesteMedlemskap(nåværendeMedlemskap)
    }

    companion object {

        val LOG = LoggerFactory.getLogger(this::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}