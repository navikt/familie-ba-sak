package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.ekstern.restDomene.FagsakDeltagerRolle
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsakDeltager
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfoBase
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import java.time.LocalDate
import java.time.Period

@Service
class FagsakDeltagerService(
    private val fagsakRepository: FagsakRepository,
    private val personRepository: PersonRepository,
    private val personidentService: PersonidentService,
    private val personopplysningerService: PersonopplysningerService,
    private val familieIntegrasjonerTilgangskontrollService: FamilieIntegrasjonerTilgangskontrollService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val fagsakService: FagsakService,
    private val integrasjonClient: IntegrasjonClient,
) {
    fun hentFagsakDeltagere(personIdent: String): List<RestFagsakDeltager> {
        val aktør = personidentService.hentAktørOrNullHvisIkkeAktivFødselsnummer(personIdent) ?: return emptyList()

        val maskertFagsakDeltager = hentMaskertPersonVedManglendeTilgang(aktør)
        if (maskertFagsakDeltager != null) {
            return listOf(maskertFagsakDeltager)
        }

        val personInfoForAktør = hentPersoninfoMedRelasjonerOgRegisterinformasjon(aktør) ?: return emptyList()

        val assosierteFagsakDeltagere = mutableListOf<RestFagsakDeltager>()

        // Legger til alle fagsak deltakere som eier fagsaker relatert til aktør. Dette kan være aktør selv, en forelder eller en person som ikke har en direkte relasjon.
        assosierteFagsakDeltagere.addAll(hentFagsakDeltagereSomEierFagsakerAssosiertMedAktør(aktør, personInfoForAktør))

        // Legger til fagsak deltagere for hver fagsak aktør selv står som eier, eller en default fagsak deltager dersom aktør ikke eier noen fagsak.
        assosierteFagsakDeltagere.addAll(
            hentFagsakDeltagerPerFagsakAktørEierEllerDefaultUtenFagsak(
                aktør = aktør,
                personInfoBase = personInfoForAktør,
                assosierteFagsakDeltagere = assosierteFagsakDeltagere,
                // Setter rolle til barn dersom aktør er barn, ellers ukjent da vi ikke kan vite om aktør har barn/er forelder.
                rolle = if (personInfoForAktør.erBarn()) FagsakDeltagerRolle.BARN else FagsakDeltagerRolle.UKJENT,
            ),
        )

        if (personInfoForAktør.erBarn()) {
            // Legger til foreldre som fagsak deltagere dersom de ikke allerede er lagt til
            assosierteFagsakDeltagere.addAll(hentForelderFagsakDeltagereSomMangler(personInfoForAktør, assosierteFagsakDeltagere))
        }

        val fagsakDeltagereMedEgenAnsattStatus = settEgenAnsattStatusPåFagsakDeltagere(assosierteFagsakDeltagere)

        return fagsakDeltagereMedEgenAnsattStatus
    }

    private fun hentFagsakDeltagerPerFagsakAktørEierEllerDefaultUtenFagsak(
        aktør: Aktør,
        personInfoBase: PersonInfoBase,
        assosierteFagsakDeltagere: List<RestFagsakDeltager>,
        rolle: FagsakDeltagerRolle,
    ): List<RestFagsakDeltager> {
        val fagsaker = fagsakRepository.finnFagsakerForAktør(aktør)
        if (fagsaker.isEmpty()) {
            return listOf(
                RestFagsakDeltager(
                    navn = personInfoBase.navn,
                    ident = aktør.aktivFødselsnummer(),
                    rolle = rolle,
                    kjønn = personInfoBase.kjønn,
                    fagsakId = null,
                    fagsakType = null,
                    adressebeskyttelseGradering = personInfoBase.adressebeskyttelseGradering,
                ),
            )
        }
        return fagsaker
            .filter { fagsak -> !assosierteFagsakDeltagere.map { it.fagsakId }.contains(fagsak.id) }
            .map {
                RestFagsakDeltager(
                    navn = personInfoBase.navn,
                    ident = aktør.aktivFødselsnummer(),
                    rolle = rolle,
                    kjønn = personInfoBase.kjønn,
                    fagsakId = it.id,
                    fagsakType = it.type,
                    adressebeskyttelseGradering = personInfoBase.adressebeskyttelseGradering,
                )
            }
    }

    private fun hentForelderFagsakDeltagereSomMangler(
        personInfo: PersonInfo,
        assosierteFagsakDeltagere: MutableList<RestFagsakDeltager>,
    ): List<RestFagsakDeltager> =
        personInfo.forelderBarnRelasjon
            .filter { relasjon ->
                assosierteFagsakDeltagere.none { it.ident == relasjon.aktør.aktivFødselsnummer() }
            }.filter { relasjon ->
                relasjon.relasjonsrolle == FORELDERBARNRELASJONROLLE.FAR ||
                    relasjon.relasjonsrolle == FORELDERBARNRELASJONROLLE.MOR ||
                    relasjon.relasjonsrolle == FORELDERBARNRELASJONROLLE.MEDMOR
            }.flatMap { relasjon ->
                val maskertPerson = hentMaskertPersonVedManglendeTilgang(relasjon.aktør)
                if (maskertPerson != null) {
                    return@flatMap listOf(maskertPerson.copy(rolle = FagsakDeltagerRolle.FORELDER))
                }
                hentFagsakDeltagerPerFagsakAktørEierEllerDefaultUtenFagsak(
                    aktør = relasjon.aktør,
                    personInfoBase = relasjon,
                    assosierteFagsakDeltagere = assosierteFagsakDeltagere,
                    rolle = FagsakDeltagerRolle.FORELDER,
                )
            }

    private fun <T> sjekkStatuskodeOgHåndterFeil(throwable: Throwable): T? {
        val clientError = throwable as? HttpStatusCodeException?
        return if ((clientError != null && clientError.statusCode == HttpStatus.NOT_FOUND) ||
            throwable.message?.contains("Fant ikke person") == true
        ) {
            null
        } else {
            throw throwable
        }
    }

    private fun hentFagsakDeltagereSomEierFagsakerAssosiertMedAktør(
        aktør: Aktør,
        personInfoForAktør: PersonInfo,
    ): Collection<RestFagsakDeltager> =
        personRepository
            .findByAktør(aktør)
            .filter { it.personopplysningGrunnlag.aktiv }
            .fold(mutableMapOf<Long, RestFagsakDeltager>()) { fagsakDeltagerMap, person: Person ->
                val behandling = behandlingHentOgPersisterService.hent(behandlingId = person.personopplysningGrunnlag.behandlingId)
                if (!behandling.aktiv || behandling.fagsak.arkivert || fagsakDeltagerMap.containsKey(behandling.fagsak.id)) {
                    return@fold fagsakDeltagerMap
                }
                fagsakDeltagerMap[behandling.fagsak.id] =
                    hentFagsakEier(
                        fagsak = behandling.fagsak,
                        aktør = aktør,
                        personInfoMedRelasjoner = personInfoForAktør,
                    )
                fagsakDeltagerMap
            }.values

    private fun hentFagsakEier(
        fagsak: Fagsak,
        aktør: Aktør,
        personInfoMedRelasjoner: PersonInfo,
    ): RestFagsakDeltager {
        if (fagsak.aktør == aktør) {
            return RestFagsakDeltager(
                navn = personInfoMedRelasjoner.navn,
                ident = fagsak.aktør.aktivFødselsnummer(),
                rolle =
                    when (fagsak.type) {
                        FagsakType.NORMAL -> FagsakDeltagerRolle.FORELDER
                        FagsakType.SKJERMET_BARN -> FagsakDeltagerRolle.BARN
                        FagsakType.BARN_ENSLIG_MINDREÅRIG -> FagsakDeltagerRolle.BARN
                        FagsakType.INSTITUSJON -> FagsakDeltagerRolle.UKJENT
                    },
                kjønn = personInfoMedRelasjoner.kjønn,
                fagsakId = fagsak.id,
                fagsakType = fagsak.type,
                adressebeskyttelseGradering = personInfoMedRelasjoner.adressebeskyttelseGradering,
            )
        }
        val maskertPerson = hentMaskertPersonVedManglendeTilgang(fagsak.aktør)
        if (maskertPerson != null) {
            return maskertPerson.copy(rolle = FagsakDeltagerRolle.FORELDER, fagsakType = fagsak.type)
        }

        val forelderInfo = personInfoMedRelasjoner.forelderBarnRelasjon.find { it.aktør.aktivFødselsnummer() == fagsak.aktør.aktivFødselsnummer() }
        if (forelderInfo != null) {
            return RestFagsakDeltager(
                navn = forelderInfo.navn,
                ident = fagsak.aktør.aktivFødselsnummer(),
                rolle = FagsakDeltagerRolle.FORELDER,
                kjønn = forelderInfo.kjønn,
                fagsakId = fagsak.id,
                fagsakType = fagsak.type,
                adressebeskyttelseGradering = forelderInfo.adressebeskyttelseGradering,
            )
        }

        // Person med forelderrolle uten direkte relasjon
        return runCatching {
            personopplysningerService.hentPersoninfoEnkel(fagsak.aktør)
        }.fold(
            onSuccess = {
                RestFagsakDeltager(
                    navn = it.navn,
                    ident = fagsak.aktør.aktivFødselsnummer(),
                    rolle = FagsakDeltagerRolle.FORELDER,
                    kjønn = it.kjønn,
                    fagsakId = fagsak.id,
                    fagsakType = fagsak.type,
                    adressebeskyttelseGradering = it.adressebeskyttelseGradering,
                )
            },
            onFailure = {
                throw Feil("Feil ved henting av person fra PDL", throwable = it)
            },
        )
    }

    private fun hentMaskertPersonVedManglendeTilgang(aktør: Aktør): RestFagsakDeltager? =
        runCatching {
            familieIntegrasjonerTilgangskontrollService.hentMaskertPersonInfoVedManglendeTilgang(aktør)
        }.fold(
            onSuccess = {
                it?.let {
                    RestFagsakDeltager(
                        rolle = FagsakDeltagerRolle.UKJENT,
                        adressebeskyttelseGradering = it.adressebeskyttelseGradering,
                        harTilgang = false,
                    )
                }
            },
            onFailure = { sjekkStatuskodeOgHåndterFeil(throwable = it) },
        )

    private fun hentPersoninfoMedRelasjonerOgRegisterinformasjon(aktør: Aktør): PersonInfo? =
        runCatching {
            personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(aktør)
        }.fold(
            onSuccess = { it },
            onFailure = { sjekkStatuskodeOgHåndterFeil(it) },
        )

    private fun settEgenAnsattStatusPåFagsakDeltagere(fagsakDeltagere: List<RestFagsakDeltager>): List<RestFagsakDeltager> {
        val egenAnsattPerIdent = integrasjonClient.sjekkErEgenAnsattBulk(fagsakDeltagere.map { it.ident })
        return fagsakDeltagere.map { fagsakDeltager ->
            fagsakDeltager.copy(
                erEgenAnsatt = egenAnsattPerIdent.getOrDefault(fagsakDeltager.ident, null),
            )
        }
    }

    fun oppgiFagsakDeltagere(
        aktør: Aktør,
        barnasAktørId: List<Aktør>,
    ): List<RestFagsakDeltager> {
        val fagsakDeltagere = mutableListOf<RestFagsakDeltager>()

        fagsakService.hentFagsakPåPerson(aktør)?.also { fagsak ->
            fagsakDeltagere.add(
                RestFagsakDeltager(
                    ident = aktør.aktivFødselsnummer(),
                    fagsakId = fagsak.id,
                    fagsakStatus = fagsak.status,
                    rolle = FagsakDeltagerRolle.FORELDER,
                ),
            )
        }

        barnasAktørId.forEach { barnsAktørId ->
            fagsakService.hentFagsakerPåPerson(barnsAktørId).toSet().forEach { fagsak ->
                fagsakDeltagere.add(
                    RestFagsakDeltager(
                        ident = barnsAktørId.aktivFødselsnummer(),
                        fagsakId = fagsak.id,
                        fagsakStatus = fagsak.status,
                        rolle = FagsakDeltagerRolle.BARN,
                    ),
                )
            }
        }

        return fagsakDeltagere
    }
}
