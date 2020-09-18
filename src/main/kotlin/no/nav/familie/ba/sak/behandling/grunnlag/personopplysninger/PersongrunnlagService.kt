package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger

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
                søker.also {
                    it.arbeidsforhold = arbeidsforholdService.hentArbeidsforhold(Ident(fødselsnummer), it)
                }

                if (!personHarLøpendeArbeidsforhold(søker)) {
                    leggTilFarEllerMedmor(barnasFødselsnummer.first(), personopplysningGrunnlag)
                }
            } else if (søkersMedlemskap != Medlemskap.NORDEN) {
                søker.also {
                    it.opphold = oppholdService.hentOpphold(it)
                }
            }
        }

        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)
    }

    private fun hentBarn(barnasFødselsnummer: List<String>,
                         personopplysningGrunnlag: PersonopplysningGrunnlag): List<Person> {
        return barnasFødselsnummer.map { nyttBarn ->
            val personinfo = personopplysningerService.hentPersoninfoMedRelasjoner(nyttBarn)
            Person(
                    personIdent = PersonIdent(nyttBarn),
                    type = PersonType.BARN,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    fødselsdato = personinfo.fødselsdato,
                    aktørId = personopplysningerService.hentAktivAktørId(Ident(nyttBarn)),
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
        val farEllerMedmor =
                barnPersoninfo.familierelasjoner.firstOrNull { it.relasjonsrolle == FAMILIERELASJONSROLLE.FAR || it.relasjonsrolle == FAMILIERELASJONSROLLE.MEDMOR }
        if (farEllerMedmor != null) {
            val annenPartFødselsnummer = farEllerMedmor.personIdent.id
            val personinfo = personopplysningerService.hentPersoninfoMedRelasjoner(annenPartFødselsnummer)
            val person = Person(personIdent = PersonIdent(annenPartFødselsnummer),
                                type = PersonType.ANNENPART,
                                personopplysningGrunnlag = personopplysningGrunnlag,
                                fødselsdato = personinfo.fødselsdato,
                                aktørId = personopplysningerService.hentAktivAktørId(Ident(annenPartFødselsnummer)),
                                navn = personinfo.navn ?: "",
                                kjønn = personinfo.kjønn ?: Kjønn.UKJENT,
                                bostedsadresse = GrBostedsadresse.fraBostedsadresse(personinfo.bostedsadresse),
                                sivilstand = personinfo.sivilstand ?: SIVILSTAND.UOPPGITT
            ).also {
                it.statsborgerskap =
                        statsborgerskapService.hentStatsborgerskapMedMedlemskapOgHistorikk(Ident(annenPartFødselsnummer),
                                                                                           it)
            }

            val farEllerMedmorsStatsborgerskap = finnNåværendeSterkesteMedlemskap(person.statsborgerskap)

            if (farEllerMedmorsStatsborgerskap == Medlemskap.EØS) {
                person.also {
                    it.arbeidsforhold = arbeidsforholdService.hentArbeidsforhold(Ident(annenPartFødselsnummer), it)
                }
            }

            personopplysningGrunnlag.personer.add(person)
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