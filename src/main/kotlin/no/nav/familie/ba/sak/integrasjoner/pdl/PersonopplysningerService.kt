package no.nav.familie.ba.sak.integrasjoner.pdl

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.Doedsfall
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.DødsfallData
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
    private val integrasjonClient: IntegrasjonClient,
) {

    fun hentPersoninfoMedRelasjonerOgRegisterinformasjon(aktør: Aktør): PersonInfo {
        val personinfo = hentPersoninfoMedQuery(aktør, PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON)
        val identerMedAdressebeskyttelse = mutableSetOf<Pair<Aktør, FORELDERBARNRELASJONROLLE>>()
        val forelderBarnRelasjon = personinfo.forelderBarnRelasjon.mapNotNull {
            val harTilgang =
                integrasjonClient.sjekkTilgangTilPersoner(listOf(it.aktør.aktivFødselsnummer()))
                    .harTilgang
            if (harTilgang) {
                val relasjonsinfo = hentPersoninfoEnkel(it.aktør)
                ForelderBarnRelasjon(
                    aktør = it.aktør,
                    relasjonsrolle = it.relasjonsrolle,
                    fødselsdato = relasjonsinfo.fødselsdato,
                    navn = relasjonsinfo.navn,
                    adressebeskyttelseGradering = relasjonsinfo.adressebeskyttelseGradering
                )
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

    private fun hentDødsfallDataFraListeMedDødsfall(doedsfall: List<Doedsfall>): DødsfallData {
        return DødsfallResponse(
            erDød = doedsfall.isNotEmpty(),
            dødsdato = doedsfall.filter { it.doedsdato != null }
                .map { it.doedsdato }
                .firstOrNull()
        )
            .let { DødsfallData(erDød = it.erDød, dødsdato = it.dødsdato) }
    }

    fun hentVergeData(aktør: Aktør): VergeData {
        return VergeData(harVerge = harVerge(aktør).harVerge)
    }

    fun harVerge(aktør: Aktør): VergeResponse {
        val harVerge = pdlRestClient.hentVergemaalEllerFremtidsfullmakt(aktør)
            .any { it.type != "stadfestetFremtidsfullmakt" }

        return VergeResponse(harVerge)
    }

    fun hentGjeldendeStatsborgerskap(aktør: Aktør): Statsborgerskap =
        pdlRestClient.hentStatsborgerskapUtenHistorikk(aktør).firstOrNull()
            ?: throw Feil(
                message = "Bruker mangler statsborgerskap",
                frontendFeilmelding = "Person (${aktør.aktivFødselsnummer()}) mangler statsborgerskap."
            )

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
    }
}

data class DødsfallResponse(
    val erDød: Boolean,
    val dødsdato: String?
)

data class VergeResponse(val harVerge: Boolean)
