package no.nav.familie.ba.sak.integrasjoner.pdl

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.PdlPersonKanIkkeBehandlesIFagsystem
import no.nav.familie.ba.sak.common.logger
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjonMaskert
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.VergeData
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.springframework.stereotype.Service

@Service
class PersonopplysningerService(
    private val pdlRestClient: PdlRestClient,
    private val systemOnlyPdlRestClient: SystemOnlyPdlRestClient,
    private val familieIntegrasjonerTilgangskontrollClient: FamilieIntegrasjonerTilgangskontrollClient,
) {

    fun hentPersoninfoMedRelasjonerOgRegisterinformasjon(aktør: Aktør): PersonInfo {
        val personinfo = hentPersoninfoMedQuery(aktør, PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON)
        val identerMedAdressebeskyttelse = mutableSetOf<Pair<Aktør, FORELDERBARNRELASJONROLLE>>()
        val forelderBarnRelasjon = personinfo.forelderBarnRelasjon.mapNotNull {
            val harTilgang =
                familieIntegrasjonerTilgangskontrollClient.sjekkTilgangTilPersoner(listOf(it.aktør.aktivFødselsnummer()))
                    .harTilgang
            if (harTilgang) {
                try {
                    val relasjonsinfo = hentPersoninfoEnkel(it.aktør)
                    ForelderBarnRelasjon(
                        aktør = it.aktør,
                        relasjonsrolle = it.relasjonsrolle,
                        fødselsdato = relasjonsinfo.fødselsdato,
                        navn = relasjonsinfo.navn,
                        adressebeskyttelseGradering = relasjonsinfo.adressebeskyttelseGradering
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

        val forelderBarnRelasjonMaskert = identerMedAdressebeskyttelse.map {
            ForelderBarnRelasjonMaskert(
                relasjonsrolle = it.second,
                adressebeskyttelseGradering = hentAdressebeskyttelseSomSystembruker(it.first)
            )
        }.toSet()
        return personinfo.copy(
            forelderBarnRelasjon = forelderBarnRelasjon,
            forelderBarnRelasjonMaskert = forelderBarnRelasjonMaskert,
        )
    }

    fun hentPersoninfoEnkel(aktør: Aktør): PersonInfo {
        return hentPersoninfoMedQuery(aktør, PersonInfoQuery.ENKEL)
    }

    private fun hentPersoninfoMedQuery(aktør: Aktør, personInfoQuery: PersonInfoQuery): PersonInfo {
        return pdlRestClient.hentPerson(aktør, personInfoQuery)
    }

    fun hentVergeData(aktør: Aktør): VergeData {
        return VergeData(harVerge = harVerge(aktør).harVerge)
    }

    fun harVerge(aktør: Aktør): VergeResponse {
        val harVerge = pdlRestClient.hentVergemaalEllerFremtidsfullmakt(aktør)
            .any { it.type != "stadfestetFremtidsfullmakt" }

        return VergeResponse(harVerge)
    }

    fun hentGjeldendeStatsborgerskap(aktør: Aktør): Statsborgerskap {
        return pdlRestClient.hentStatsborgerskapUtenHistorikk(aktør).firstOrNull() ?: UKJENT_STATSBORGERSKAP
    }

    fun hentGjeldendeOpphold(aktør: Aktør): Opphold = pdlRestClient.hentOppholdUtenHistorikk(aktør).firstOrNull()
        ?: throw Feil(
            message = "Bruker mangler opphold",
            frontendFeilmelding = "Person (${aktør.aktivFødselsnummer()}) mangler opphold."
        )

    fun hentLandkodeUtenlandskBostedsadresse(aktør: Aktør): String {
        val landkode = pdlRestClient.hentUtenlandskBostedsadresse(aktør)?.landkode
        return if (landkode.isNullOrEmpty()) UKJENT_LANDKODE else landkode
    }

    fun hentAdressebeskyttelseSomSystembruker(aktør: Aktør): ADRESSEBESKYTTELSEGRADERING =
        systemOnlyPdlRestClient.hentAdressebeskyttelse(aktør).tilAdressebeskyttelse()

    companion object {

        const val UKJENT_LANDKODE = "ZZ"
        val UKJENT_STATSBORGERSKAP =
            Statsborgerskap(land = "XUK", bekreftelsesdato = null, gyldigFraOgMed = null, gyldigTilOgMed = null)
    }
}

data class VergeResponse(val harVerge: Boolean)
