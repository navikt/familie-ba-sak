package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.config.hentCacheForSaksbehandler
import no.nav.familie.ba.sak.ekstern.restDomene.PersonInfoDto
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.tilAdressebeskyttelse
import no.nav.familie.ba.sak.integrasjoner.tilgangsmaskin.TilgangsmaskinTilgangskontrollKlient
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.erStrengtFortrolig
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service

@Service
class PersonTilgangService(
    private val tilgangsmaskinTilgangskontrollKlient: TilgangsmaskinTilgangskontrollKlient,
    private val cacheManager: CacheManager,
    private val systemOnlyPdlRestKlient: SystemOnlyPdlRestKlient,
) {
    fun hentMaskertPersonInfoVedManglendeTilgang(aktør: Aktør): PersonInfoDto? {
        val harTilgang = sjekkTilgangTilPerson(personIdent = aktør.aktivFødselsnummer()).harTilgang
        return if (!harTilgang) {
            val adressebeskyttelse = systemOnlyPdlRestKlient.hentAdressebeskyttelse(aktør).tilAdressebeskyttelse()
            PersonInfoDto(
                personIdent = aktør.aktivFødselsnummer(),
                adressebeskyttelseGradering = adressebeskyttelse,
                harTilgang = false,
            )
        } else {
            null
        }
    }

    fun sjekkTilgangTilPerson(personIdent: String): PersonTilgang = sjekkTilgangTilPersoner(listOf(personIdent)).values.single()

    fun sjekkTilgangTilPersoner(personIdenter: List<String>): Map<String, PersonTilgang> =
        cacheManager.hentCacheForSaksbehandler(TILGANG_TIL_PERSONER_CACHE, personIdenter) { identerUtenCache ->
            tilgangsmaskinTilgangskontrollKlient
                .sjekkTilgangTilPersoner(identerUtenCache.toSet())
                .associateBy { it.personIdent }
        }

    fun hentIdenterMedStrengtFortroligAdressebeskyttelse(personIdenter: List<String>): List<String> {
        val adresseBeskyttelseBolk = systemOnlyPdlRestKlient.hentAdressebeskyttelseBolk(personIdenter)
        return adresseBeskyttelseBolk
            .filter { (_, person) ->
                person.adressebeskyttelse.any { adressebeskyttelse ->
                    adressebeskyttelse.gradering.erStrengtFortrolig()
                }
            }.map { it.key }
    }

    companion object {
        const val TILGANG_TIL_PERSONER_CACHE = "sjekkTilgangTilPersoner"
    }
}
