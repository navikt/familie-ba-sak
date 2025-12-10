package no.nav.familie.ba.sak.integrasjoner.pdl

import com.neovisionaries.i18n.CountryCode
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.PdlPersonKanIkkeBehandlesIFagsystem
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.FalskIdentitet
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjonMaskert
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlPersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.VergeData
import no.nav.familie.ba.sak.kjerne.falskidentitet.FalskIdentitetService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.collections.getOrDefault

@Service
class PersonopplysningerService(
    private val pdlRestKlient: PdlRestKlient,
    private val systemOnlyPdlRestKlient: SystemOnlyPdlRestKlient,
    private val familieIntegrasjonerTilgangskontrollService: FamilieIntegrasjonerTilgangskontrollService,
    private val integrasjonKlient: IntegrasjonKlient,
    private val falskIdentitetService: FalskIdentitetService,
    private val featureToggleService: FeatureToggleService,
) {
    fun hentPdlPersoninfoMedRelasjonerOgRegisterinformasjon(aktør: Aktør): PdlPersonInfo {
        val pdlPersoninfo = hentPersoninfoMedQuery(aktør, PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON)
        val personinfo =
            when (pdlPersoninfo) {
                is PdlPersonInfo.Person -> pdlPersoninfo.personInfo
                is PdlPersonInfo.Falsk -> return PdlPersonInfo.Falsk(pdlPersoninfo.falskIdentitet)
            }
        return PdlPersonInfo.Person(personinfo.medRelasjonerOgEgenAnsattInfo(aktør))
    }

    fun hentPersoninfoMedRelasjonerOgRegisterinformasjon(aktør: Aktør): PersonInfo {
        val pdlPersoninfo = hentPersoninfoMedQuery(aktør, PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON)
        val personInfo =
            when (pdlPersoninfo) {
                is PdlPersonInfo.Person -> pdlPersoninfo.personInfo
                is PdlPersonInfo.Falsk -> throw FunksjonellFeil("Person har falsk identitet")
            }
        return personInfo.medRelasjonerOgEgenAnsattInfo(aktør)
    }

    private fun PersonInfo.medRelasjonerOgEgenAnsattInfo(aktør: Aktør): PersonInfo {
        val identerMedAdressebeskyttelse = mutableSetOf<Pair<Aktør, FORELDERBARNRELASJONROLLE>>()
        val relasjonsidenter = this.forelderBarnRelasjon.map { it.aktør.aktivFødselsnummer() }
        val tilgangPerIdent = familieIntegrasjonerTilgangskontrollService.sjekkTilgangTilPersoner(relasjonsidenter)
        val egenAnsattPerIdent = integrasjonKlient.sjekkErEgenAnsattBulk(listOf(aktør.aktivFødselsnummer()) + relasjonsidenter)
        val forelderBarnRelasjon =
            this.forelderBarnRelasjon
                .mapNotNull {
                    if (tilgangPerIdent.getValue(it.aktør.aktivFødselsnummer()).harTilgang) {
                        try {
                            val relasjonsinfo = hentPersoninfoEnkel(it.aktør)
                            ForelderBarnRelasjon(
                                aktør = it.aktør,
                                relasjonsrolle = it.relasjonsrolle,
                                fødselsdato = relasjonsinfo.fødselsdato,
                                navn = relasjonsinfo.navn,
                                kjønn = relasjonsinfo.kjønn,
                                adressebeskyttelseGradering = relasjonsinfo.adressebeskyttelseGradering,
                                erEgenAnsatt = egenAnsattPerIdent.getOrDefault(it.aktør.aktivFødselsnummer(), null),
                            )
                        } catch (pdlPersonKanIkkeBehandlesIFagsystem: PdlPersonKanIkkeBehandlesIFagsystem) {
                            logger.warn("Ignorerer relasjon: ${pdlPersonKanIkkeBehandlesIFagsystem.årsak}")
                            secureLogger.warn("Ignorerer relasjon ${it.aktør.aktivFødselsnummer()} til ${aktør.aktivFødselsnummer()}: ${pdlPersonKanIkkeBehandlesIFagsystem.årsak}")
                            null
                        }
                    } else {
                        identerMedAdressebeskyttelse.add(Pair(it.aktør, it.relasjonsrolle))
                        null
                    }
                }.toSet()

        val forelderBarnRelasjonMaskert =
            identerMedAdressebeskyttelse
                .map {
                    ForelderBarnRelasjonMaskert(
                        relasjonsrolle = it.second,
                        adressebeskyttelseGradering = hentAdressebeskyttelseSomSystembruker(it.first),
                    )
                }.toSet()
        return this.copy(
            erEgenAnsatt = egenAnsattPerIdent.getOrDefault(aktør.aktivFødselsnummer(), null),
            forelderBarnRelasjon = forelderBarnRelasjon,
            forelderBarnRelasjonMaskert = forelderBarnRelasjonMaskert,
        )
    }

    fun hentPdlPersonInfoEnkel(aktør: Aktør): PdlPersonInfo = hentPersoninfoMedQuery(aktør, PersonInfoQuery.ENKEL)

    fun hentPersoninfoEnkel(aktør: Aktør): PersonInfo {
        val pdlPersonInfo = hentPersoninfoMedQuery(aktør, PersonInfoQuery.ENKEL)
        when (pdlPersonInfo) {
            is PdlPersonInfo.Person -> return pdlPersonInfo.personInfo
            else -> throw FunksjonellFeil("Person har falsk identitet")
        }
    }

    fun hentPdlPersoninfoNavnOgAdresse(aktør: Aktør): PdlPersonInfo = hentPersoninfoMedQuery(aktør, PersonInfoQuery.NAVN_OG_ADRESSE)

    fun hentPersoninfoNavnOgAdresse(aktør: Aktør): PersonInfo {
        val pdlPersonInfo = hentPersoninfoMedQuery(aktør, PersonInfoQuery.NAVN_OG_ADRESSE)
        when (pdlPersonInfo) {
            is PdlPersonInfo.Person -> return pdlPersonInfo.personInfo
            else -> throw FunksjonellFeil("Person har falsk identitet")
        }
    }

    private fun hentPersoninfoMedQuery(
        aktør: Aktør,
        personInfoQuery: PersonInfoQuery,
    ): PdlPersonInfo =
        try {
            PdlPersonInfo.Person(pdlRestKlient.hentPerson(aktør, personInfoQuery))
        } catch (e: PdlPersonKanIkkeBehandlesIFagsystem) {
            if (!featureToggleService.isEnabled(FeatureToggle.SKAL_HÅNDTERE_FALSK_IDENTITET)) {
                throw e
            }
            val falskIdentitet = falskIdentitetService.hentFalskIdentitet(aktør)
            if (falskIdentitet != null) {
                PdlPersonInfo.Falsk(
                    falskIdentitet = falskIdentitet,
                )
            } else {
                throw e
            }
        }

    fun hentVergeData(aktør: Aktør): VergeData = VergeData(harVerge = harVerge(aktør).harVerge)

    fun harVerge(aktør: Aktør): VergeResponse {
        val harVerge =
            pdlRestKlient
                .hentVergemaalEllerFremtidsfullmakt(aktør)
                .any { it.type != "stadfestetFremtidsfullmakt" }

        return VergeResponse(harVerge)
    }

    fun hentGjeldendeStatsborgerskap(aktør: Aktør): Statsborgerskap = pdlRestKlient.hentStatsborgerskap(aktør).firstOrNull() ?: UKJENT_STATSBORGERSKAP

    fun hentHistoriskStatsborgerskap(aktør: Aktør) = systemOnlyPdlRestKlient.hentStatsborgerskap(aktør, historikk = true)

    fun hentGjeldendeOpphold(aktør: Aktør): Opphold =
        pdlRestKlient.hentOppholdstillatelse(aktør).firstOrNull()
            ?: throw Feil(
                message = "Bruker mangler opphold",
                frontendFeilmelding = "Person (${aktør.aktivFødselsnummer()}) mangler opphold.",
            )

    fun hentLandkodeAlpha2UtenlandskBostedsadresse(aktør: Aktør): String {
        val landkode = pdlRestKlient.hentUtenlandskBostedsadresse(aktør)?.landkode

        if (landkode.isNullOrEmpty()) return UKJENT_LANDKODE

        return if (landkode.length == 3) {
            if (landkode == PDL_UKJENT_LANDKODE) {
                UKJENT_LANDKODE
            } else {
                CountryCode.getByAlpha3Code(landkode.uppercase()).alpha2
            }
        } else {
            landkode
        }
    }

    fun hentAdresserForPersoner(identer: List<String>) = pdlRestKlient.hentAdresser(identer)

    fun hentAdressebeskyttelseSomSystembruker(aktør: Aktør): ADRESSEBESKYTTELSEGRADERING = systemOnlyPdlRestKlient.hentAdressebeskyttelse(aktør).tilAdressebeskyttelse()

    companion object {
        const val UKJENT_LANDKODE = "ZZ"
        const val PDL_UKJENT_LANDKODE = "XUK"
        val UKJENT_STATSBORGERSKAP =
            Statsborgerskap(
                land = PDL_UKJENT_LANDKODE,
                bekreftelsesdato = null,
                gyldigFraOgMed = null,
                gyldigTilOgMed = null,
            )
        private val logger: Logger =
            LoggerFactory.getLogger(PersonopplysningerService::class.java)
    }
}

data class VergeResponse(
    val harVerge: Boolean,
)
