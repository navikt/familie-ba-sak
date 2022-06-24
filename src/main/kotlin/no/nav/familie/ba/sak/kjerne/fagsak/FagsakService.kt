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
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.integrasjoner.skyggesak.SkyggesakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakEier.OMSORGSPERSON
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingsbehandlingService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
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
    private val behandlingstemaService: BehandlingstemaService,
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val behandlingService: BehandlingService,
    private val vedtakRepository: VedtakRepository,
    private val personopplysningerService: PersonopplysningerService,
    private val familieIntegrasjonerTilgangskontrollClient: FamilieIntegrasjonerTilgangskontrollClient,
    private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,
    private val skyggesakService: SkyggesakService,
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
        val fagsak = hentEllerOpprettFagsak(personident, eier = fagsakRequest.fagsakEier ?: OMSORGSPERSON)
        return hentRestMinimalFagsak(fagsakId = fagsak.id)
    }

    @Transactional
    fun hentEllerOpprettFagsak(
        personIdent: String,
        fraAutomatiskBehandling: Boolean = false,
        eier: FagsakEier = OMSORGSPERSON,
        type: FagsakType = FagsakType.NORMAL,
    ): Fagsak {
        val aktør = personidentService.hentOgLagreAktør(personIdent, true)
        var fagsak = fagsakRepository.finnFagsakForAktør(aktør, eier)
        if (fagsak == null) {

            fagsak = lagre(Fagsak(aktør = aktør, eier = eier, type = type))
            if (fraAutomatiskBehandling) {
                antallFagsakerOpprettetFraAutomatisk.increment()
            } else {
                antallFagsakerOpprettetFraManuell.increment()
            }
            skyggesakService.opprettSkyggesak(fagsak)
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

    fun hentMinimalFagsakForPerson(aktør: Aktør, fagsakEier: FagsakEier = OMSORGSPERSON): Ressurs<RestMinimalFagsak> {
        val fagsak = fagsakRepository.finnFagsakForAktør(aktør, fagsakEier)
        return if (fagsak != null) Ressurs.success(data = lagRestMinimalFagsak(fagsakId = fagsak.id)) else Ressurs.failure(
            errorMessage = "Fant ikke fagsak på person"
        )
    }

    fun hentMinimalFagsakerForPerson(aktør: Aktør): Ressurs<List<RestMinimalFagsak>> {
        val fagsaker = fagsakRepository.finnFagsakerForAktør(aktør)
        return if (!fagsaker.isEmpty()) Ressurs.success(data = lagRestMinimalFagsaker(fagsaker)) else Ressurs.failure(
            errorMessage = "Fant ikke fagsaker på person"
        )
    }

    fun hentRestFagsak(fagsakId: Long): Ressurs<RestFagsak> = Ressurs.success(data = lagRestFagsak(fagsakId))

    fun hentRestMinimalFagsak(fagsakId: Long): Ressurs<RestMinimalFagsak> =
        Ressurs.success(data = lagRestMinimalFagsak(fagsakId))

    fun lagRestMinimalFagsaker(fagsaker: List<Fagsak>): List<RestMinimalFagsak> {
        return fagsaker.map { lagRestMinimalFagsak(it.id) }
    }

    fun lagRestMinimalFagsak(fagsakId: Long): RestMinimalFagsak {
        val restBaseFagsak = lagRestBaseFagsak(fagsakId)

        val tilbakekrevingsbehandlinger =
            tilbakekrevingsbehandlingService.hentRestTilbakekrevingsbehandlinger((fagsakId))
        val visningsbehandlinger = behandlingRepository.finnBehandlinger(fagsakId).map {
            it.tilRestVisningBehandling(
                vedtaksdato = vedtakRepository.findByBehandlingAndAktivOptional(it.id)?.vedtaksdato
            )
        }
        val migreringsdato = behandlingService.hentMigreringsdatoPåFagsak(fagsakId)
        return restBaseFagsak.tilRestMinimalFagsak(
            restVisningBehandlinger = visningsbehandlinger,
            tilbakekrevingsbehandlinger = tilbakekrevingsbehandlinger,
            migreringsdato = migreringsdato
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

    private fun lagRestBaseFagsak(fagsakId: Long): RestBaseFagsak {
        val fagsak = hentPåFagsakId(fagsakId = fagsakId)

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
            løpendeKategori = behandlingstemaService.hentLøpendeKategori(fagsakId = fagsakId),
            løpendeUnderkategori = behandlingstemaService.hentLøpendeUnderkategori(fagsakId = fagsakId),
            gjeldendeUtbetalingsperioder = gjeldendeUtbetalingsperioder,
            fagsakEier = fagsak.eier
        )
    }

    fun hentEllerOpprettFagsakForPersonIdent(
        fødselsnummer: String,
        fraAutomatiskBehandling: Boolean = false,
        fagsakEier: FagsakEier = OMSORGSPERSON
    ): Fagsak {
        return hentEllerOpprettFagsak(fødselsnummer, fraAutomatiskBehandling, fagsakEier)
    }

    fun hent(aktør: Aktør, fagsakEier: FagsakEier = OMSORGSPERSON): Fagsak? =
        fagsakRepository.finnFagsakForAktør(aktør, fagsakEier)

    fun hentPåFagsakId(fagsakId: Long): Fagsak {
        return fagsakRepository.finnFagsak(fagsakId) ?: throw FunksjonellFeil(
            melding = "Finner ikke fagsak med id $fagsakId",
            frontendFeilmelding = "Finner ikke fagsak med id $fagsakId"
        )
    }

    fun hentAktør(fagsakId: Long): Aktør {
        return hentPåFagsakId(fagsakId).aktør
    }

    fun hentFagsakPåPerson(aktør: Aktør, fagsakEier: FagsakEier = OMSORGSPERSON): Fagsak? {
        return fagsakRepository.finnFagsakForAktør(aktør, fagsakEier)
    }

    fun hentLøpendeFagsaker(): List<Fagsak> {
        return fagsakRepository.finnLøpendeFagsaker()
    }

    fun hentFagsakDeltager(personIdent: String): List<RestFagsakDeltager> {
        val aktør = personidentService.hentAktør(personIdent)

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
            val fagsaker = fagsakRepository.finnFagsakerForAktør(aktør).ifEmpty { listOf(null) }
            fagsaker.forEach { fagsak ->
                assosierteFagsakDeltagere.add(
                    RestFagsakDeltager(
                        navn = personInfoMedRelasjoner.navn,
                        ident = aktør.aktivFødselsnummer(),
                        // we set the role to unknown when the person is not a child because the person may not have a child
                        rolle = if (erBarn) FagsakDeltagerRolle.BARN else FagsakDeltagerRolle.UKJENT,
                        kjønn = personInfoMedRelasjoner.kjønn,
                        fagsakId = fagsak?.id,
                        fagsakEier = fagsak?.eier
                    )
                )
            }
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

                        val fagsaker = fagsakRepository.finnFagsakerForAktør(relasjon.aktør).ifEmpty { listOf(null) }
                        fagsaker.forEach { fagsak ->
                            assosierteFagsakDeltagere.add(
                                RestFagsakDeltager(
                                    navn = forelderInfo.navn,
                                    ident = relasjon.aktør.aktivFødselsnummer(),
                                    rolle = FagsakDeltagerRolle.FORELDER,
                                    kjønn = forelderInfo.kjønn,
                                    fagsakId = fagsak?.id,
                                    fagsakEier = fagsak?.eier
                                )
                            )
                        }
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
                            fagsakId = behandling.fagsak.id,
                            fagsakEier = behandling.fagsak.eier
                        )
                    } else {
                        val maskertForelder =
                            hentMaskertFagsakdeltakerVedManglendeTilgang(behandling.fagsak.aktør)
                        if (maskertForelder != null) {
                            assosierteFagsakDeltagerMap[behandling.fagsak.id] =
                                maskertForelder.copy(rolle = FagsakDeltagerRolle.FORELDER, fagsakEier = behandling.fagsak.eier)
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
                                fagsakId = behandling.fagsak.id,
                                fagsakEier = behandling.fagsak.eier
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
            familieIntegrasjonerTilgangskontrollClient.sjekkTilgangTilPersoner(listOf(aktør.aktivFødselsnummer())).harTilgang
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
