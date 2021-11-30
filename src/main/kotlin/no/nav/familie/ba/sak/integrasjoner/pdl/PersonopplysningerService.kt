package no.nav.familie.ba.sak.integrasjoner.pdl

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.DødsfallData
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.ForelderBarnRelasjonMaskert
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.VergeData
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.springframework.stereotype.Service
import org.springframework.web.context.annotation.ApplicationScope

@Service
@ApplicationScope
class PersonopplysningerService(
    val pdlRestClient: PdlRestClient,
    val systemOnlyPdlRestClient: SystemOnlyPdlRestClient,
    val integrasjonClient: IntegrasjonClient,
    val personidentService: PersonidentService,
) {

    fun hentPersoninfoMedRelasjonerOgRegisterinformasjon(aktør: Aktør): PersonInfo {
        val personinfo = hentPersoninfoMedQuery(aktør, PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON)
        val identerMedAdressebeskyttelse = mutableSetOf<Pair<String, FORELDERBARNRELASJONROLLE>>()
        val forelderBarnRelasjon = personinfo.forelderBarnRelasjon.mapNotNull {
            val harTilgang =
                integrasjonClient.sjekkTilgangTilPersoner(listOf(it.aktør.aktivIdent()))
                    .firstOrNull()?.harTilgang
                    ?: error("Fikk ikke svar på tilgang til person.")
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
                identerMedAdressebeskyttelse.add(Pair(it.aktør.aktivIdent(), it.relasjonsrolle))
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
            forelderBarnRelasjonMaskert = forelderBarnRelasjonMaskert
        )
    }

    fun hentPersoninfoEnkel(aktør: Aktør): PersonInfo {
        return hentPersoninfoMedQuery(aktør, PersonInfoQuery.ENKEL)
    }

    private fun hentPersoninfoMedQuery(aktør: Aktør, personInfoQuery: PersonInfoQuery): PersonInfo {
        return pdlRestClient.hentPerson(aktør, personInfoQuery)
    }

    fun hentAktivPersonIdent(ident: Ident): PersonIdent {
        val identer = hentIdenter(ident).filter { !it.historisk && it.gruppe == "FOLKEREGISTERIDENT" }.map { it.ident }
        if (identer.isEmpty()) error("Finner ingen aktiv personIdent for ident")
        return PersonIdent(identer.first())
    }

    fun hentIdenter(ident: Ident): List<IdentInformasjon> {
        return hentIdenter(ident.ident, true)
    }

    fun hentIdenter(personIdent: String, historikk: Boolean): List<IdentInformasjon> {
        val hentIdenter = pdlRestClient.hentIdenter(personIdent)

        return if (historikk) {
            hentIdenter.data.pdlIdenter!!.identer.map { it }
        } else {
            hentIdenter.data.pdlIdenter!!.identer.filter { !it.historisk }.map { it }
        }
    }

    fun hentDødsfall(aktør: Aktør): DødsfallData {
        val doedsfall = pdlRestClient.hentDødsfall(aktør)
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

    fun hentGjeldendeStatsborgerskap(ident: Ident): Statsborgerskap =
        pdlRestClient.hentStatsborgerskapUtenHistorikk(ident.ident).firstOrNull()
            ?: throw Feil(
                message = "Bruker mangler statsborgerskap",
                frontendFeilmelding = "Person ($ident) mangler statsborgerskap."
            )

    fun hentGjeldendeOpphold(ident: String): Opphold = pdlRestClient.hentOppholdUtenHistorikk(ident).firstOrNull()
        ?: throw Feil(
            message = "Bruker mangler opphold",
            frontendFeilmelding = "Person ($ident) mangler opphold."
        )

    fun hentLandkodeUtenlandskBostedsadresse(aktør: Aktør): String {
        val landkode = pdlRestClient.hentUtenlandskBostedsadresse(aktør.aktivIdent())?.landkode
        return if (landkode.isNullOrEmpty()) UKJENT_LANDKODE else landkode
    }

    fun hentAdressebeskyttelseSomSystembruker(ident: String): ADRESSEBESKYTTELSEGRADERING =
        systemOnlyPdlRestClient.hentAdressebeskyttelse(ident).tilAdressebeskyttelse()

    companion object {

        const val UKJENT_LANDKODE = "ZZ"
    }
}

data class DødsfallResponse(
    val erDød: Boolean,
    val dødsdato: String?
)

data class VergeResponse(val harVerge: Boolean)
