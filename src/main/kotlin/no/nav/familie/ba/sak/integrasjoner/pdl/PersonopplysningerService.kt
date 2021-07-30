package no.nav.familie.ba.sak.integrasjoner.pdl

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.GrBostedsadresseperiode
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonInfo
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.DødsfallData
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.ForelderBarnRelasjonMaskert
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.VergeData
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
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
        val integrasjonClient: IntegrasjonClient) {

    fun hentPersoninfoMedRelasjoner(personIdent: String): PersonInfo {
        val personinfo = hentPersoninfoMedQuery(personIdent, PersonInfoQuery.MED_RELASJONER)
        val identerMedAdressebeskyttelse = mutableSetOf<Pair<String, FORELDERBARNRELASJONROLLE>>()
        val forelderBarnRelasjon = personinfo.forelderBarnRelasjon.mapNotNull {
            val harTilgang = integrasjonClient.sjekkTilgangTilPersoner(listOf(it.personIdent.id)).firstOrNull()?.harTilgang
                             ?: error("Fikk ikke svar på tilgang til person.")
            if (harTilgang) {
                val relasjonsinfo = hentPersoninfo(it.personIdent.id)
                ForelderBarnRelasjon(personIdent = it.personIdent,
                                     relasjonsrolle = it.relasjonsrolle,
                                     fødselsdato = relasjonsinfo.fødselsdato,
                                     navn = relasjonsinfo.navn,
                                     adressebeskyttelseGradering = relasjonsinfo.adressebeskyttelseGradering)
            } else {
                identerMedAdressebeskyttelse.add(Pair(it.personIdent.id, it.relasjonsrolle))
                null
            }

        }.toSet()
        val forelderBarnRelasjonMaskert = identerMedAdressebeskyttelse.map {
            ForelderBarnRelasjonMaskert(relasjonsrolle = it.second,
                                        adressebeskyttelseGradering = hentAdressebeskyttelseSomSystembruker(it.first)
            )
        }.toSet()
        return personinfo.copy(forelderBarnRelasjon = forelderBarnRelasjon,
                               forelderBarnRelasjonMaskert = forelderBarnRelasjonMaskert)
    }

    fun hentPersoninfo(personIdent: String): PersonInfo {
        return hentPersoninfoMedQuery(personIdent, PersonInfoQuery.ENKEL)
    }

    fun hentHistoriskPersoninfoManuell(personIdent: String): PersonInfo {
        return hentPersoninfoMedQuery(personIdent, PersonInfoQuery.ENKEL_MANUELL_BEHANDLING)
    }

    private fun hentPersoninfoMedQuery(personIdent: String, personInfoQuery: PersonInfoQuery): PersonInfo {
        return pdlRestClient.hentPerson(personIdent, personInfoQuery)
    }

    fun hentAktivAktørId(ident: Ident): AktørId {
        val aktørId = hentAktørId(ident.ident)
        if (aktørId.isEmpty()) error("Finner ingen aktiv aktørId for ident")
        return AktørId(aktørId.first())
    }

    fun hentAktørId(personIdent: String): List<String> {
        val hentIdenter = pdlRestClient.hentIdenter(personIdent)
        return hentIdenter.data.pdlIdenter!!.identer.filter { it.gruppe == "AKTORID" && !it.historisk }.map { it.ident }
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

    fun hentDødsfall(ident: Ident): DødsfallData {
        val doedsfall = pdlRestClient.hentDødsfall(ident.ident)
        return DødsfallResponse(erDød = doedsfall.isNotEmpty(),
                                dødsdato = doedsfall.filter { it.doedsdato != null }
                                        .map { it.doedsdato }
                                        .firstOrNull())
                .let { DødsfallData(erDød = it.erDød, dødsdato = it.dødsdato) }
    }

    fun hentVergeData(ident: Ident): VergeData {
        return VergeData(harVerge = harVerge(ident.ident).harVerge)
    }

    fun harVerge(personIdent: String): VergeResponse {
        val harVerge = pdlRestClient.hentVergemaalEllerFremtidsfullmakt(personIdent)
                .any { it.type != "stadfestetFremtidsfullmakt" }

        return VergeResponse(harVerge)
    }

    fun hentStatsborgerskap(ident: Ident): List<Statsborgerskap> =
            pdlRestClient.hentStatsborgerskap(ident.ident)

    fun hentOpphold(ident: String): List<Opphold> = pdlRestClient.hentOpphold(ident)

    fun hentBostedsadresseperioder(ident: String): List<GrBostedsadresseperiode> =
            pdlRestClient.hentBostedsadresseperioder(ident).map {
                GrBostedsadresseperiode(
                        periode = DatoIntervallEntitet(
                                fom = it.angittFlyttedato,
                                tom = it.gyldigTilOgMed?.toLocalDate()
                        ))
            }

    fun hentLandkodeUtenlandskBostedsadresse(ident: String): String {
        val landkode = pdlRestClient.hentUtenlandskBostedsadresse(ident)?.landkode
        return if (landkode.isNullOrEmpty()) UKJENT_LANDKODE else landkode
    }

    fun hentAdressebeskyttelseSomSystembruker(ident: String): ADRESSEBESKYTTELSEGRADERING =
            systemOnlyPdlRestClient.hentAdressebeskyttelse(ident).firstOrNull()?.gradering ?: ADRESSEBESKYTTELSEGRADERING.UGRADERT

    companion object {

        const val UKJENT_LANDKODE = "ZZ"
    }
}

data class DødsfallResponse(val erDød: Boolean,
                            val dødsdato: String?)

data class VergeResponse(val harVerge: Boolean)
