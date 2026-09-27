package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.config.hentCacheForSaksbehandler
import no.nav.familie.ba.sak.ekstern.restDomene.PersonInfoDto
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.tilAdressebeskyttelse
import no.nav.familie.ba.sak.integrasjoner.tilgangsmaskin.TilgangsmaskinTilgangskontrollKlient
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.erStrengtFortrolig
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.tilgangsmaskin.Avvisningskode
import org.springframework.cache.CacheManager
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class PersonTilgangService(
    private val tilgangsmaskinTilgangskontrollKlient: TilgangsmaskinTilgangskontrollKlient,
    private val familieIntegrasjonerTilgangskontrollKlient: FamilieIntegrasjonerTilgangskontrollKlient,
    private val featureToggleService: FeatureToggleService,
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

    fun sjekkTilgangTilPersoner(personIdenter: List<String>): Map<String, PersonTilgang> {
        // Tilgangene caches per kilde, slik at en endring av togglen tar effekt med en gang.
        val skalBrukeTilgangsmaskinen = featureToggleService.isEnabled(FeatureToggle.SKAL_BRUKE_TILGANGSMASKINEN)
        val cacheNavn = if (skalBrukeTilgangsmaskinen) TILGANGSMASKINEN_CACHE else FAMILIE_INTEGRASJONER_CACHE
        return cacheManager.hentCacheForSaksbehandler(cacheNavn, personIdenter) { identerUtenCache ->
            val tilganger =
                if (skalBrukeTilgangsmaskinen) {
                    tilgangsmaskinTilgangskontrollKlient.sjekkTilgangTilPersoner(identerUtenCache.toSet())
                } else {
                    sjekkTilgangTilPersonerIFamilieIntegrasjoner(identerUtenCache)
                }
            tilganger.associateBy { it.personIdent }
        }
    }

    private fun sjekkTilgangTilPersonerIFamilieIntegrasjoner(personIdenter: List<String>): List<PersonTilgang> {
        val tilgangPerIdent =
            familieIntegrasjonerTilgangskontrollKlient
                .sjekkTilgangTilPersoner(personIdenter)
                .associateBy { it.personIdent }
        val identerUtenSvar = personIdenter.filterNot { it in tilgangPerIdent }
        if (identerUtenSvar.isNotEmpty()) {
            throw Feil(
                message = "Fikk ikke svar fra familie-integrasjoner for ${identerUtenSvar.size} av ${personIdenter.size} identer.",
                frontendFeilmelding = "Klarte ikke å sjekke tilgang til personene. Prøv igjen senere.",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            )
        }
        return personIdenter.map { tilgangPerIdent.getValue(it).tilPersonTilgang() }
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

    // familie-integrasjoner gir ingen avvisningskode. Strengt fortrolig adresse gjenkjennes på begrunnelsen,
    // alle andre avvisninger får avvisningskoden UKJENT.
    private fun Tilgang.tilPersonTilgang(): PersonTilgang =
        if (harTilgang) {
            PersonTilgang.medTilgang(personIdent)
        } else {
            PersonTilgang.avvist(
                personIdent = personIdent,
                avvisningskode =
                    if (begrunnelse?.contains(BEGRUNNELSE_STRENGT_FORTROLIG_FRA_FAMILIE_INTEGRASJONER) == true) {
                        Avvisningskode.AVVIST_STRENGT_FORTROLIG_ADRESSE
                    } else {
                        Avvisningskode.UKJENT
                    },
                begrunnelse = begrunnelse ?: "familie-integrasjoner ga ikke tilgang til personen",
            )
        }

    companion object {
        const val TILGANGSMASKINEN_CACHE = "sjekkTilgangTilPersonerITilgangsmaskinen"
        const val FAMILIE_INTEGRASJONER_CACHE = "sjekkTilgangTilPersonerIFamilieIntegrasjoner"
        private const val BEGRUNNELSE_STRENGT_FORTROLIG_FRA_FAMILIE_INTEGRASJONER = "Bruker mangler rollen '0000-GA-Strengt_Fortrolig_Adresse'"
    }
}
