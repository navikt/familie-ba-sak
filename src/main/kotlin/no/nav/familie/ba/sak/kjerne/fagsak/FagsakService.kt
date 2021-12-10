package no.nav.familie.ba.sak.kjerne.fagsak

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.ekstern.restDomene.FagsakDeltagerRolle
import no.nav.familie.ba.sak.ekstern.restDomene.RestBaseFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsakDeltager
import no.nav.familie.ba.sak.ekstern.restDomene.RestMinimalFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestMinimalFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestVisningBehandling
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.integrasjoner.skyggesak.SkyggesakService
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingsbehandlingService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.sikkerhet.validering.FagsaktilgangConstraint
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.ba.sak.task.BehandleAnnullerFødselDto
import no.nav.familie.ba.sak.task.BehandleAnnullertFødselTask
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpStatusCodeException
import java.time.LocalDate
import java.time.Period

@Service
class FagsakService(
    private val fagsakRepository: FagsakRepository,
    private val personRepository: PersonRepository,
    private val personidentService: PersonidentService,
    private val behandlingRepository: BehandlingRepository,
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val vedtakRepository: VedtakRepository,
    private val personopplysningerService: PersonopplysningerService,
    private val integrasjonClient: IntegrasjonClient,
    private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,
    private val skyggesakService: SkyggesakService,
    private val tilgangService: TilgangService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val tilbakekrevingsbehandlingService: TilbakekrevingsbehandlingService,
    private val taskRepository: TaskRepository,
) {

    private val antallFagsakerOpprettetFraManuell =
        Metrics.counter("familie.ba.sak.fagsak.opprettet", "saksbehandling", "manuell")
    private val antallFagsakerOpprettetFraAutomatisk =
        Metrics.counter("familie.ba.sak.fagsak.opprettet", "saksbehandling", "automatisk")

    @Transactional
    fun oppdaterLøpendeStatusPåFagsaker() {
        val fagsaker = fagsakRepository.finnFagsakerSomSkalAvsluttes()
        for (fagsakId in fagsaker) {
            val fagsak = fagsakRepository.getById(fagsakId)
            oppdaterStatus(fagsak, FagsakStatus.AVSLUTTET)
        }
    }

    @Transactional
    fun hentEllerOpprettFagsak(fagsakRequest: FagsakRequest): Ressurs<RestMinimalFagsak> {
        val personident = when {
            fagsakRequest.personIdent !== null -> fagsakRequest.personIdent
            fagsakRequest.aktørId !== null -> fagsakRequest.aktørId
            else -> throw Feil(
                "Hverken aktørid eller personident er satt på fagsak-requesten. Klarer ikke opprette eller hente fagsak.",
                "Fagsak er forsøkt opprettet uten ident. Dette er en systemfeil, vennligst ta kontakt med systemansvarlig.",
                HttpStatus.BAD_REQUEST
            )
        }
        val fagsak = hentEllerOpprettFagsak(personident)
        return hentRestMinimalFagsak(fagsakId = fagsak.id).also {
            skyggesakService.opprettSkyggesak(fagsak.aktør, fagsak.id)
        }
    }

    @Transactional
    fun hentEllerOpprettFagsak(personIdent: String, fraAutomatiskBehandling: Boolean = false): Fagsak {
        val aktør = personidentService.hentOgLagreAktør(personIdent)
        var fagsak = fagsakRepository.finnFagsakForAktør(aktør)
        if (fagsak == null) {
            tilgangService.verifiserHarTilgangTilHandling(BehandlerRolle.SAKSBEHANDLER, "opprette fagsak")

            // TODO: robustgjøring dnr/fnr fjern opprettelse av fagsak person ved contract.
            fagsak = Fagsak(aktør = aktør).also {
                it.søkerIdenter =
                    setOf(FagsakPerson(personIdent = PersonIdent(aktør.aktivFødselsnummer()), fagsak = it))
                lagre(it)
            }
            if (fraAutomatiskBehandling) {
                antallFagsakerOpprettetFraAutomatisk.increment()
            } else {
                antallFagsakerOpprettetFraManuell.increment()
            }
        }
        return fagsak
    }

    fun hentFagsakerPåPerson(aktør: Aktør): List<Fagsak> {
        val versjonerAvBarn = personRepository.findByAktør(aktør)
        return versjonerAvBarn.map {
            it.personopplysningGrunnlag.behandlingId
        }.map {
            behandlingRepository.finnBehandling(it).fagsak
        }.distinct()
    }

    @Transactional
    fun lagre(fagsak: Fagsak): Fagsak {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter fagsak $fagsak")
        return fagsakRepository.save(fagsak).also { saksstatistikkEventPublisher.publiserSaksstatistikk(it.id) }
    }

    fun oppdaterStatus(fagsak: Fagsak, nyStatus: FagsakStatus) {
        logger.info(
            "${SikkerhetContext.hentSaksbehandlerNavn()} endrer status på fagsak ${fagsak.id} fra ${fagsak.status}" +
                " til $nyStatus"
        )
        fagsak.status = nyStatus

        lagre(fagsak)
    }

    fun hentMinimalFagsakForPerson(aktør: Aktør): Ressurs<RestMinimalFagsak> {
        val fagsak = fagsakRepository.finnFagsakForAktør(aktør)
        return if (fagsak != null) Ressurs.success(data = lagRestMinimalFagsak(fagsakId = fagsak.id)) else Ressurs.failure(
            errorMessage = "Fant ikke fagsak på person"
        )
    }

    fun hentRestFagsak(fagsakId: Long): Ressurs<RestFagsak> = Ressurs.success(data = lagRestFagsak(fagsakId))

    fun hentRestMinimalFagsak(fagsakId: Long): Ressurs<RestMinimalFagsak> =
        Ressurs.success(data = lagRestMinimalFagsak(fagsakId))

    fun lagRestMinimalFagsak(fagsakId: Long): RestMinimalFagsak {
        val restBaseFagsak = lagRestBaseFagsak(fagsakId)

        val tilbakekrevingsbehandlinger =
            tilbakekrevingsbehandlingService.hentRestTilbakekrevingsbehandlinger((fagsakId))
        val visningsbehandlinger = behandlingRepository.finnBehandlinger(fagsakId).map {
            it.tilRestVisningBehandling(
                vedtaksdato = vedtakRepository.findByBehandlingAndAktiv(it.id)?.vedtaksdato
            )
        }

        return restBaseFagsak.tilRestMinimalFagsak(
            restVisningBehandlinger = visningsbehandlinger,
            tilbakekrevingsbehandlinger = tilbakekrevingsbehandlinger,
        )
    }

    private fun lagRestFagsak(fagsakId: Long): RestFagsak {
        val restBaseFagsak = lagRestBaseFagsak(fagsakId)

        val tilbakekrevingsbehandlinger =
            tilbakekrevingsbehandlingService.hentRestTilbakekrevingsbehandlinger((fagsakId))
        val utvidedeBehandlinger =
            behandlingRepository.finnBehandlinger(fagsakId)
                .map { utvidetBehandlingService.lagRestUtvidetBehandling(it.id) }

        return restBaseFagsak.tilRestFagsak(utvidedeBehandlinger, tilbakekrevingsbehandlinger)
    }

    private fun lagRestBaseFagsak(@FagsaktilgangConstraint fagsakId: Long): RestBaseFagsak {
        val fagsak = fagsakRepository.finnFagsak(fagsakId)
            ?: throw FunksjonellFeil(
                melding = "Finner ikke fagsak med id $fagsakId",
                frontendFeilmelding = "Finner ikke fagsak med id $fagsakId"
            )
        val aktivBehandling = behandlingRepository.findByFagsakAndAktiv(fagsakId)

        val sistIverksatteBehandling =
            Behandlingutils.hentSisteBehandlingSomErIverksatt(
                iverksatteBehandlinger = behandlingRepository.finnIverksatteBehandlinger(
                    fagsakId = fagsakId
                )
            )
        val gjeldendeUtbetalingsperioder =
            if (sistIverksatteBehandling != null) vedtaksperiodeService.hentUtbetalingsperioder(behandling = sistIverksatteBehandling) else emptyList()

        return RestBaseFagsak(
            opprettetTidspunkt = fagsak.opprettetTidspunkt,
            id = fagsak.id,
            søkerFødselsnummer = fagsak.aktør.aktivFødselsnummer(),
            status = fagsak.status,
            underBehandling =
            if (aktivBehandling == null)
                false
            else
                aktivBehandling.status == BehandlingStatus.UTREDES || (aktivBehandling.steg >= StegType.BESLUTTE_VEDTAK && aktivBehandling.steg != StegType.BEHANDLING_AVSLUTTET),
            gjeldendeUtbetalingsperioder = gjeldendeUtbetalingsperioder,
        )
    }

    fun hentEllerOpprettFagsakForPersonIdent(fødselsnummer: String, fraAutomatiskBehandling: Boolean = false): Fagsak {
        return hentEllerOpprettFagsak(fødselsnummer, fraAutomatiskBehandling)
    }

    fun hent(aktør: Aktør): Fagsak? = fagsakRepository.finnFagsakForAktør(aktør)

    fun hentPåFagsakId(fagsakId: Long): Fagsak {
        return fagsakRepository.finnFagsak(fagsakId) ?: error("Finner ikke fagsak med id $fagsakId")
    }

    fun hentFagsakPåPerson(aktør: Aktør): Fagsak? {
        return fagsakRepository.finnFagsakForAktør(aktør)
    }

    fun hentLøpendeFagsaker(): List<Fagsak> {
        return fagsakRepository.finnLøpendeFagsaker()
    }

    fun hentFagsakDeltager(aktør: Aktør): List<RestFagsakDeltager> {
        val maskertDeltaker = runCatching {
            hentMaskertFagsakdeltakerVedManglendeTilgang(aktør)
        }.fold(
            onSuccess = { it },
            onFailure = { return sjekkStatuskodeOgHåndterFeil(it) }
        )

        if (maskertDeltaker != null) {
            return listOf(maskertDeltaker)
        }

        val personInfoMedRelasjoner = runCatching {
            personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(aktør)
        }.fold(
            onSuccess = { it },
            onFailure = { return sjekkStatuskodeOgHåndterFeil(it) }
        )
        val assosierteFagsakDeltagere = hentAssosierteFagsakdeltagere(aktør, personInfoMedRelasjoner)

        val erBarn = Period.between(personInfoMedRelasjoner.fødselsdato, LocalDate.now()).years < 18

        if (assosierteFagsakDeltagere.find { it.ident == aktør.aktivFødselsnummer() } == null) {
            val fagsakId =
                if (!erBarn) fagsakRepository.finnFagsakForAktør(aktør)?.id else null
            assosierteFagsakDeltagere.add(
                RestFagsakDeltager(
                    navn = personInfoMedRelasjoner.navn,
                    ident = aktør.aktivFødselsnummer(),
                    // we set the role to unknown when the person is not a child because the person may not have a child
                    rolle = if (erBarn) FagsakDeltagerRolle.BARN else FagsakDeltagerRolle.UKJENT,
                    kjønn = personInfoMedRelasjoner.kjønn,
                    fagsakId = fagsakId
                )
            )
        }

        if (erBarn) {
            personInfoMedRelasjoner.forelderBarnRelasjon.filter { relasjon ->
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

                        val forelderInfo = runCatching {
                            personopplysningerService.hentPersoninfoEnkel(relasjon.aktør)
                        }.fold(
                            onSuccess = { it },
                            onFailure = {
                                throw IllegalStateException("Feil ved henting av person fra PDL", it)
                            }
                        )

                        val fagsak = fagsakRepository.finnFagsakForAktør(relasjon.aktør)
                        assosierteFagsakDeltagere.add(
                            RestFagsakDeltager(
                                navn = forelderInfo.navn,
                                ident = relasjon.aktør.aktivFødselsnummer(),
                                rolle = FagsakDeltagerRolle.FORELDER,
                                kjønn = forelderInfo.kjønn,
                                fagsakId = fagsak?.id
                            )
                        )
                    }
                }
            }
        }
        return assosierteFagsakDeltagere
    }

    private fun sjekkStatuskodeOgHåndterFeil(throwable: Throwable): List<RestFagsakDeltager> {
        val clientError = throwable as? HttpStatusCodeException?
        return if ((clientError != null && clientError.statusCode == HttpStatus.NOT_FOUND) ||
            throwable.message?.contains("Fant ikke person") == true
        ) {
            emptyList()
        } else {
            throw throwable
        }
    }

    // We find all cases that either have the given person as applicant, or have it as a child
    private fun hentAssosierteFagsakdeltagere(
        aktør: Aktør,
        personInfoMedRelasjoner: PersonInfo
    ): MutableList<RestFagsakDeltager> {
        val assosierteFagsakDeltagerMap = mutableMapOf<Long, RestFagsakDeltager>()

        personRepository.findByAktør(aktør).forEach { person: Person ->
            if (person.personopplysningGrunnlag.aktiv) {
                val behandling = behandlingRepository.finnBehandling(person.personopplysningGrunnlag.behandlingId)
                if (behandling.aktiv && !behandling.fagsak.arkivert && !assosierteFagsakDeltagerMap.containsKey(
                        behandling.fagsak.id
                    )
                ) {
                    // get applicant info from PDL. we assume that the applicant is always a person whose info is stored in PDL.
                    if (behandling.fagsak.aktør == aktør) {
                        assosierteFagsakDeltagerMap[behandling.fagsak.id] = RestFagsakDeltager(
                            navn = personInfoMedRelasjoner.navn,
                            ident = behandling.fagsak.aktør.aktivFødselsnummer(),
                            rolle = FagsakDeltagerRolle.FORELDER,
                            kjønn = personInfoMedRelasjoner.kjønn,
                            fagsakId = behandling.fagsak.id
                        )
                    } else {
                        val maskertForelder =
                            hentMaskertFagsakdeltakerVedManglendeTilgang(behandling.fagsak.aktør)
                        if (maskertForelder != null) {
                            assosierteFagsakDeltagerMap[behandling.fagsak.id] =
                                maskertForelder.copy(rolle = FagsakDeltagerRolle.FORELDER)
                        } else {
                            val personinfo =
                                runCatching {
                                    personopplysningerService.hentPersoninfoEnkel(behandling.fagsak.aktør)
                                }.fold(
                                    onSuccess = { it },
                                    onFailure = {
                                        throw IllegalStateException("Feil ved henting av person fra PDL", it)
                                    }
                                )

                            assosierteFagsakDeltagerMap[behandling.fagsak.id] = RestFagsakDeltager(
                                navn = personinfo.navn,
                                ident = behandling.fagsak.aktør.aktivFødselsnummer(),
                                rolle = FagsakDeltagerRolle.FORELDER,
                                kjønn = personinfo.kjønn,
                                fagsakId = behandling.fagsak.id
                            )
                        }
                    }
                }
            }
        }

        // The given person and its parents may be included in the result, no matter whether they have a case.
        return assosierteFagsakDeltagerMap.values.toMutableList()
    }

    private fun hentMaskertFagsakdeltakerVedManglendeTilgang(aktør: Aktør): RestFagsakDeltager? {
        val harTilgang =
            integrasjonClient.sjekkTilgangTilPersoner(listOf(aktør.aktivFødselsnummer())).first().harTilgang
        return if (!harTilgang) {
            val adressebeskyttelse = personopplysningerService.hentAdressebeskyttelseSomSystembruker(aktør)
            RestFagsakDeltager(
                rolle = FagsakDeltagerRolle.UKJENT,
                adressebeskyttelseGradering = adressebeskyttelse,
                harTilgang = false
            )
        } else null
    }

    fun oppgiFagsakdeltagere(aktør: Aktør, barnasAktørId: List<Aktør>): List<RestFagsakDeltager> {
        val fagsakDeltagere = mutableListOf<RestFagsakDeltager>()

        hentFagsakPåPerson(aktør)?.also { fagsak ->
            fagsakDeltagere.add(
                RestFagsakDeltager(
                    ident = aktør.aktivFødselsnummer(),
                    fagsakId = fagsak.id,
                    fagsakStatus = fagsak.status,
                    rolle = FagsakDeltagerRolle.FORELDER
                )
            )
        }

        barnasAktørId.forEach { barnsAktørId ->
            hentFagsakerPåPerson(barnsAktørId).toSet().forEach { fagsak ->
                fagsakDeltagere.add(
                    RestFagsakDeltager(
                        ident = barnsAktørId.aktivFødselsnummer(),
                        fagsakId = fagsak.id,
                        fagsakStatus = fagsak.status,
                        rolle = FagsakDeltagerRolle.BARN
                    )
                )
            }
        }

        return fagsakDeltagere
    }

    fun behandleAnnullertFødsel(behandleAnnullerFødselDto: BehandleAnnullerFødselDto) {
        taskRepository.save(BehandleAnnullertFødselTask.opprettTask(behandleAnnullerFødselDto))
    }

    companion object {

        private val logger = LoggerFactory.getLogger(FagsakService::class.java)
    }
}
