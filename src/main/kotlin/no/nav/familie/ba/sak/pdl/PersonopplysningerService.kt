package no.nav.familie.ba.sak.pdl

import no.nav.familie.ba.sak.pdl.internal.IdentInformasjon
import no.nav.familie.ba.sak.pdl.internal.Person
import no.nav.familie.kontrakter.felles.personinfo.Statsborgerskap
import org.springframework.stereotype.Service
import org.springframework.web.context.annotation.ApplicationScope

@Service
@ApplicationScope
class PersonopplysningerService(private val pdlRestClient: PdlRestClient) {

    fun hentPersoninfo(personIdent: String, tema: String, personInfoQuery: PersonInfoQuery): Person {
        return pdlRestClient.hentPerson(personIdent, tema, personInfoQuery)
    }

    fun hentAktørId(personIdent: String, tema: String): List<String> {
        val hentIdenter = pdlRestClient.hentIdenter(personIdent, tema)
        return hentIdenter.data.pdlIdenter!!.identer.filter { it.gruppe == "AKTORID" && !it.historisk }.map { it.ident }
    }

    fun hentIdenter(personIdent: String, tema: String, historikk: Boolean): List<IdentInformasjon> {
        val hentIdenter = pdlRestClient.hentIdenter(personIdent, tema)

        return if (historikk) {
            hentIdenter.data.pdlIdenter!!.identer.map { it }
        } else {
            hentIdenter.data.pdlIdenter!!.identer.filter { !it.historisk }.map { it }
        }
    }

    fun hentDødsfall(personIdent: String, tema: String): DødsfallResponse {
        val doedsfall = pdlRestClient.hentDødsfall(personIdent, tema)
        return DødsfallResponse(erDød = doedsfall.isNotEmpty(),
                                dødsdato = doedsfall.filter { it.doedsdato != null }
                                        .map { it.doedsdato }
                                        .firstOrNull())
    }

    fun harVerge(personIdent: String, tema: String): VergeResponse {
        val harVerge = pdlRestClient.hentVergemaalEllerFremtidsfullmakt(personIdent, tema)
                .any { it.type != "stadfestetFremtidsfullmakt" }

        return VergeResponse(harVerge)
    }

    fun hentStatsborgerskap(ident: String, tema: String): List<Statsborgerskap> =
            pdlRestClient.hentStatsborgerskap(ident, tema)


    companion object {
        const val PERSON = "PERSON"
    }
}

data class DødsfallResponse(val erDød: Boolean,
                            val dødsdato: String?)

data class VergeResponse(val harVerge: Boolean)
