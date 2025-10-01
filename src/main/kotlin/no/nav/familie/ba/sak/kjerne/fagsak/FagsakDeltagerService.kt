package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.ekstern.restDomene.FagsakDeltagerRolle
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsakDeltager
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
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
    fun hentFagsakDeltager(personIdent: String): List<RestFagsakDeltager> {
        val aktør = personidentService.hentAktørOrNullHvisIkkeAktivFødselsnummer(personIdent) ?: return emptyList()

        val maskertDeltaker = hentMaskertFagsakdeltakerVedManglendeTilgang(aktør)

        if (maskertDeltaker != null) {
            return listOf(maskertDeltaker)
        }

        val personInfoMedRelasjoner = hentPersoninfoMedRelasjonerOgRegisterinformasjon(aktør)

        if (personInfoMedRelasjoner == null) {
            return emptyList()
        }

        // Finner alle fagsaker relatert til aktør og finner ut hvem som står som eier. Dette kan være aktør selv, en forelder eller en person som ikke har en direkte relasjon.
        val assosierteFagsakDeltagere = hentFagsakDeltakereSomEierFagsakerAssosiertMedAktør(aktør, personInfoMedRelasjoner)

        val erBarn = Period.between(personInfoMedRelasjoner.fødselsdato, LocalDate.now()).years < 18

        val fagsaker = fagsakRepository.finnFagsakerForAktør(aktør)

        if (fagsaker.isEmpty()) {
            assosierteFagsakDeltagere.add(
                RestFagsakDeltager(
                    navn = personInfoMedRelasjoner.navn,
                    ident = aktør.aktivFødselsnummer(),
                    // we set the role to unknown when the person is not a child because the person may not have a child
                    rolle = if (erBarn) FagsakDeltagerRolle.BARN else FagsakDeltagerRolle.UKJENT,
                    kjønn = personInfoMedRelasjoner.kjønn,
                    fagsakId = null,
                    fagsakType = null,
                    adressebeskyttelseGradering = personInfoMedRelasjoner.adressebeskyttelseGradering,
                ),
            )
        }

//        fagsaker.forEach { fagsak ->
//            if (assosierteFagsakDeltagere.find { it.ident == aktør.aktivFødselsnummer() && it.fagsakId == fagsak?.id } == null) {
//                assosierteFagsakDeltagere.add(
//                    RestFagsakDeltager(
//                        navn = personInfoMedRelasjoner.navn,
//                        ident = aktør.aktivFødselsnummer(),
//                        // we set the role to unknown when the person is not a child because the person may not have a child
//                        rolle = if (erBarn) FagsakDeltagerRolle.BARN else FagsakDeltagerRolle.UKJENT,
//                        kjønn = personInfoMedRelasjoner.kjønn,
//                        fagsakId = fagsak?.id,
//                        fagsakType = fagsak?.type,
//                        adressebeskyttelseGradering = personInfoMedRelasjoner.adressebeskyttelseGradering,
//                    ),
//                )
//            }
//        }

        if (erBarn) {
            personInfoMedRelasjoner.forelderBarnRelasjon
                .filter { relasjon ->
                    relasjon.relasjonsrolle == FORELDERBARNRELASJONROLLE.FAR ||
                        relasjon.relasjonsrolle == FORELDERBARNRELASJONROLLE.MOR ||
                        relasjon.relasjonsrolle == FORELDERBARNRELASJONROLLE.MEDMOR
                }.forEach { relasjon ->
                    if (assosierteFagsakDeltagere.find { fagsakDeltager ->
                            fagsakDeltager.ident == relasjon.aktør.aktivFødselsnummer()
                        } == null
                    ) {
                        val maskertForelder =
                            hentMaskertFagsakdeltakerVedManglendeTilgang(relasjon.aktør)
                        if (maskertForelder != null) {
                            assosierteFagsakDeltagere.add(maskertForelder.copy(rolle = FagsakDeltagerRolle.FORELDER))
                        } else {
                            val fagsakerForRelasjon = fagsakRepository.finnFagsakerForAktør(relasjon.aktør).ifEmpty { listOf(null) }
                            fagsakerForRelasjon.forEach { fagsak ->
                                assosierteFagsakDeltagere.add(
                                    RestFagsakDeltager(
                                        navn = relasjon.navn,
                                        ident = relasjon.aktør.aktivFødselsnummer(),
                                        rolle = FagsakDeltagerRolle.FORELDER,
                                        kjønn = relasjon.kjønn,
                                        fagsakId = fagsak?.id,
                                        fagsakType = fagsak?.type,
                                        adressebeskyttelseGradering = relasjon.adressebeskyttelseGradering,
                                    ),
                                )
                            }
                        }
                    }
                }
        }
        val fagsakDeltagereMedEgenAnsattStatus = settEgenAnsattStatusPåFagsakDeltagere(assosierteFagsakDeltagere)

        return fagsakDeltagereMedEgenAnsattStatus
    }

    private fun sjekkStatuskodeOgHåndterFeil(throwable: Throwable): RestFagsakDeltager? {
        val clientError = throwable as? HttpStatusCodeException?
        return if ((clientError != null && clientError.statusCode == HttpStatus.NOT_FOUND) ||
            throwable.message?.contains("Fant ikke person") == true
        ) {
            null
        } else {
            throw throwable
        }
    }

    private fun hentFagsakDeltakereSomEierFagsakerAssosiertMedAktør(
        aktør: Aktør,
        personInfoMedRelasjoner: PersonInfo,
    ): MutableList<RestFagsakDeltager> {
        val assosierteFagsakDeltagerMap = mutableMapOf<Long, RestFagsakDeltager>()

        personRepository.findByAktør(aktør).forEach { person: Person ->
            if (!person.personopplysningGrunnlag.aktiv) {
                return@forEach
            }
            val behandling = behandlingHentOgPersisterService.hent(behandlingId = person.personopplysningGrunnlag.behandlingId)
            if (!behandling.aktiv || behandling.fagsak.arkivert || assosierteFagsakDeltagerMap.containsKey(behandling.fagsak.id)) {
                return@forEach
            }

            assosierteFagsakDeltagerMap[behandling.fagsak.id] = hentFagsakEier(behandling.fagsak, aktør, personInfoMedRelasjoner)
        }

        return assosierteFagsakDeltagerMap.values.toMutableList()
    }

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

        val maskertForelder = hentMaskertFagsakdeltakerVedManglendeTilgang(fagsak.aktør)
        if (maskertForelder != null) {
            return maskertForelder.copy(
                rolle = FagsakDeltagerRolle.FORELDER,
                fagsakType = fagsak.type,
            )
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

    private fun hentMaskertFagsakdeltakerVedManglendeTilgang(aktør: Aktør): RestFagsakDeltager? =
        runCatching {
            familieIntegrasjonerTilgangskontrollService
                .hentMaskertPersonInfoVedManglendeTilgang(aktør)
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
            onFailure = { return sjekkStatuskodeOgHåndterFeil(it) },
        )

    private fun hentPersoninfoMedRelasjonerOgRegisterinformasjon(aktør: Aktør): PersonInfo? =
        runCatching {
            personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(aktør)
        }.fold(
            onSuccess = { it },
            onFailure = { return null },
        )

    private fun settEgenAnsattStatusPåFagsakDeltagere(fagsakDeltagere: List<RestFagsakDeltager>): List<RestFagsakDeltager> {
        val egenAnsattPerIdent = integrasjonClient.sjekkErEgenAnsattBulk(fagsakDeltagere.map { it.ident })
        return fagsakDeltagere.map { fagsakDeltager ->
            fagsakDeltager.copy(
                erEgenAnsatt = egenAnsattPerIdent.getOrDefault(fagsakDeltager.ident, null),
            )
        }
    }

    fun oppgiFagsakdeltagere(
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
