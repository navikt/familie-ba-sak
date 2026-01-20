package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner

import no.nav.familie.ba.sak.config.hentCacheForSaksbehandler
import no.nav.familie.ba.sak.ekstern.restDomene.PersonInfoDto
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.tilAdressebeskyttelse
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service

@Service
class FamilieIntegrasjonerTilgangskontrollService(
    private val familieIntegrasjonerTilgangskontrollKlient: FamilieIntegrasjonerTilgangskontrollKlient,
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

    fun sjekkTilgangTilPerson(personIdent: String): Tilgang = sjekkTilgangTilPersoner(listOf(personIdent)).values.single()

    fun sjekkTilgangTilPersoner(personIdenter: List<String>): Map<String, Tilgang> =
        cacheManager.hentCacheForSaksbehandler("sjekkTilgangTilPersoner", personIdenter) {
            familieIntegrasjonerTilgangskontrollKlient.sjekkTilgangTilPersoner(it).associateBy { it.personIdent }
        }

    fun hentIdenterMedStrengtFortroligAdressebeskyttelse(personIdenter: List<String>): List<String> {
        val adresseBeskyttelseBolk = systemOnlyPdlRestKlient.hentAdressebeskyttelseBolk(personIdenter)
        return adresseBeskyttelseBolk
            .filter { (_, person) ->
                person.adressebeskyttelse.any { adressebeskyttelse ->
                    adressebeskyttelse.gradering == ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG ||
                        adressebeskyttelse.gradering == ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG_UTLAND
                }
            }.map { it.key }
    }
}
