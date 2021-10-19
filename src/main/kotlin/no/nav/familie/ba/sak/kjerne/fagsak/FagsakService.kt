package no.nav.familie.ba.sak.kjerne.fagsak

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.ekstern.restDomene.FagsakDeltagerRolle
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsakDeltager
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestBehandlingStegTilstand
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestFødselshendelsefiltreringResultat
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPersonerMedAndeler
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestTotrinnskontroll
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestVedtak
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.integrasjoner.skyggesak.SkyggesakService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.tilRestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.domene.FødselshendelsefiltreringResultatRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingsbehandlingService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.sikkerhet.validering.FagsaktilgangConstraint
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.ba.sak.task.BehandleAnnullertFødselTask
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Ident
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
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val fagsakRepository: FagsakRepository,
    private val fagsakPersonRepository: FagsakPersonRepository,
    private val persongrunnlagService: PersongrunnlagService,
    private val personRepository: PersonRepository,
    private val behandlingRepository: BehandlingRepository,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val vedtakRepository: VedtakRepository,
    private val totrinnskontrollRepository: TotrinnskontrollRepository,
    private val personopplysningerService: PersonopplysningerService,
    private val integrasjonClient: IntegrasjonClient,
    private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,
    private val skyggesakService: SkyggesakService,
    private val tilgangService: TilgangService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val tilbakekrevingRepository: TilbakekrevingRepository,
    private val tilbakekrevingsbehandlingService: TilbakekrevingsbehandlingService,
    private val fødselshendelsefiltreringResultatRepository: FødselshendelsefiltreringResultatRepository,
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
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
            val fagsak = fagsakRepository.getOne(fagsakId)
            oppdaterStatus(fagsak, FagsakStatus.AVSLUTTET)
        }
    }

    @Transactional
    fun hentEllerOpprettFagsak(fagsakRequest: FagsakRequest): Ressurs<RestFagsak> {
        val personIdent = when {
            fagsakRequest.personIdent !== null -> PersonIdent(fagsakRequest.personIdent)
            fagsakRequest.aktørId !== null -> personopplysningerService.hentAktivPersonIdent(Ident(fagsakRequest.aktørId))
            else -> throw Feil(
                "Hverken aktørid eller personident er satt på fagsak-requesten. Klarer ikke opprette eller hente fagsak.",
                "Fagsak er forsøkt opprettet uten ident. Dette er en systemfeil, vennligst ta kontakt med systemansvarlig.",
                HttpStatus.BAD_REQUEST
            )
        }
        val fagsak = hentEllerOpprettFagsak(personIdent)
        return hentRestFagsak(fagsakId = fagsak.id).also {
            skyggesakService.opprettSkyggesak(personIdent.ident, fagsak.id)
        }
    }

    @Transactional
    fun hentEllerOpprettFagsak(personIdent: PersonIdent, fraAutomatiskBehandling: Boolean = false): Fagsak {
        val identer =
            personopplysningerService.hentIdenter(Ident(personIdent.ident)).map { PersonIdent(it.ident) }.toSet()
        var fagsak = fagsakPersonRepository.finnFagsak(personIdenter = identer)
        if (fagsak == null) {
            tilgangService.verifiserHarTilgangTilHandling(BehandlerRolle.SAKSBEHANDLER, "opprette fagsak")

            fagsak = Fagsak().also {
                it.søkerIdenter = setOf(FagsakPerson(personIdent = personIdent, fagsak = it))
                lagre(it)
            }
            if (fraAutomatiskBehandling) {
                antallFagsakerOpprettetFraAutomatisk.increment()
            } else {
                antallFagsakerOpprettetFraManuell.increment()
            }
        } else if (fagsak.søkerIdenter.none { fagsakPerson -> fagsakPerson.personIdent == personIdent }) {
            fagsak.also {
                it.søkerIdenter += FagsakPerson(personIdent = personIdent, fagsak = it)
                lagre(it)
            }
        }
        return fagsak
    }

    fun hentFagsakerPåPerson(personIdent: PersonIdent): List<Fagsak> {
        val versjonerAvBarn = personRepository.findByPersonIdent(personIdent)
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

    fun hentRestFagsak(fagsakId: Long): Ressurs<RestFagsak> = Ressurs.success(data = lagRestFagsak(fagsakId))

    fun hentRestFagsakForPerson(personIdent: PersonIdent): Ressurs<RestFagsak> {
        val fagsak = fagsakRepository.finnFagsakForPersonIdent(personIdent)
        return if (fagsak != null) Ressurs.success(data = lagRestFagsak(fagsakId = fagsak.id)) else Ressurs.failure(
            errorMessage = "Fant ikke fagsak på person"
        )
    }

    private fun lagRestFagsak(@FagsaktilgangConstraint fagsakId: Long): RestFagsak {
        val fagsak = fagsakRepository.finnFagsak(fagsakId)
            ?: throw FunksjonellFeil(
                melding = "Finner ikke fagsak med id $fagsakId",
                frontendFeilmelding = "Finner ikke fagsak med id $fagsakId"
            )
        val behandlinger = behandlingRepository.finnBehandlinger(fagsakId)
        val tilbakekrevingsbehandlinger =
            tilbakekrevingsbehandlingService.hentRestTilbakekrevingsbehandlinger((fagsakId))
        val utvidedeBehandlinger = behandlinger.map { lagRestUtvidetBehandling(it) }

        val sistIverksatteBehandling =
            Behandlingutils.hentSisteBehandlingSomErIverksatt(
                iverksatteBehandlinger = behandlingRepository.finnIverksatteBehandlinger(
                    fagsakId = fagsakId
                )
            )
        val gjeldendeUtbetalingsperioder =
            if (sistIverksatteBehandling != null) vedtaksperiodeService.hentUtbetalingsperioder(behandling = sistIverksatteBehandling) else emptyList()

        return fagsak.tilRestFagsak(utvidedeBehandlinger, gjeldendeUtbetalingsperioder, tilbakekrevingsbehandlinger)
    }

    fun lagRestUtvidetBehandling(behandling: Behandling): RestUtvidetBehandling {

        val søknadsgrunnlag = søknadGrunnlagService.hentAktiv(behandlingId = behandling.id)
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandling.id)
        val personer = personopplysningGrunnlag?.personer

        val arbeidsfordeling = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)

        val vedtak = vedtakRepository.finnVedtakForBehandling(behandling.id)

        val personResultater = vilkårsvurderingService.hentAktivForBehandling(behandling.id)?.personResultater

        val andelerTilkjentYtelse =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(listOf(behandling.id))

        val totrinnskontroll =
            totrinnskontrollRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)

        val tilbakekreving = tilbakekrevingRepository.findByBehandlingId(behandling.id)

        return RestUtvidetBehandling(
            behandlingId = behandling.id,
            aktiv = behandling.aktiv,
            steg = behandling.steg,
            stegTilstand = behandling.behandlingStegTilstand.map { it.tilRestBehandlingStegTilstand() },
            status = behandling.status,
            resultat = behandling.resultat,
            skalBehandlesAutomatisk = behandling.skalBehandlesAutomatisk,
            type = behandling.type,
            kategori = behandling.kategori,
            underkategori = behandling.underkategori,
            årsak = behandling.opprettetÅrsak,
            opprettetTidspunkt = behandling.opprettetTidspunkt,
            endretAv = behandling.endretAv,
            arbeidsfordelingPåBehandling = arbeidsfordeling.tilRestArbeidsfordelingPåBehandling(),
            søknadsgrunnlag = søknadsgrunnlag?.hentSøknadDto(),
            personer = personer?.map { persongrunnlagService.mapTilRestPersonMedStatsborgerskapLand(it) }
                ?: emptyList(),
            personResultater = personResultater?.map { it.tilRestPersonResultat() } ?: emptyList(),
            fødselshendelsefiltreringResultater = fødselshendelsefiltreringResultatRepository.finnFødselshendelsefiltreringResultater(
                behandlingId = behandling.id
            ).map { it.tilRestFødselshendelsefiltreringResultat() },
            utbetalingsperioder = vedtaksperiodeService.hentUtbetalingsperioder(behandling),
            personerMedAndelerTilkjentYtelse = personopplysningGrunnlag?.tilRestPersonerMedAndeler(andelerTilkjentYtelse)
                ?: emptyList(),
            endretUtbetalingAndeler = endretUtbetalingAndelRepository.findByBehandlingId(behandling.id)
                .map { it.tilRestEndretUtbetalingAndel() },
            tilbakekreving = tilbakekreving?.tilRestTilbakekreving(),
            vedtakForBehandling = vedtak.filter { it.aktiv }.map {
                it.tilRestVedtak(
                    vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelser(vedtak = it)
                )
            },
            totrinnskontroll = totrinnskontroll?.tilRestTotrinnskontroll(),
        )
    }

    fun hentEllerOpprettFagsakForPersonIdent(fødselsnummer: String, fraAutomatiskBehandling: Boolean = false): Fagsak {
        val personIdent = PersonIdent(fødselsnummer)
        return hentEllerOpprettFagsak(personIdent, fraAutomatiskBehandling)
    }

    fun hent(personIdent: PersonIdent): Fagsak? {
        val identer =
            personopplysningerService.hentIdenter(Ident(personIdent.ident)).map { PersonIdent(it.ident) }.toSet()
        return fagsakPersonRepository.finnFagsak(identer)
    }

    fun hentPåFagsakId(fagsakId: Long): Fagsak {
        return fagsakRepository.finnFagsak(fagsakId) ?: error("Finner ikke fagsak med id $fagsakId")
    }

    fun hentFagsakPåPerson(identer: Set<PersonIdent>): Fagsak? {
        return fagsakPersonRepository.finnFagsak(identer)
    }

    fun hentLøpendeFagsaker(): List<Fagsak> {
        return fagsakRepository.finnLøpendeFagsaker()
    }

    fun hentFagsakDeltager(personIdent: String): List<RestFagsakDeltager> {
        val maskertDeltaker = runCatching {
            hentMaskertFagsakdeltakerVedManglendeTilgang(personIdent)
        }.fold(
            onSuccess = { it },
            onFailure = { return sjekkStatuskodeOgHåndterFeil(it) }
        )

        if (maskertDeltaker != null) {
            return listOf(maskertDeltaker)
        }

        val personInfoMedRelasjoner = runCatching {
            personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(personIdent)
        }.fold(
            onSuccess = { it },
            onFailure = { return sjekkStatuskodeOgHåndterFeil(it) }
        )
        val assosierteFagsakDeltagere = hentAssosierteFagsakdeltagere(personIdent, personInfoMedRelasjoner)

        val erBarn = Period.between(personInfoMedRelasjoner.fødselsdato, LocalDate.now()).years < 18

        if (assosierteFagsakDeltagere.find { it.ident == personIdent } == null) {
            val fagsakId =
                if (!erBarn) fagsakRepository.finnFagsakForPersonIdent(PersonIdent(personIdent))?.id else null
            assosierteFagsakDeltagere.add(
                RestFagsakDeltager(
                    navn = personInfoMedRelasjoner.navn,
                    ident = personIdent,
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
                    fagsakDeltager.ident == relasjon.personIdent.id
                } == null ) {
                    val maskertForelder = hentMaskertFagsakdeltakerVedManglendeTilgang(relasjon.personIdent.id)
                    if (maskertForelder != null) {
                        assosierteFagsakDeltagere.add(maskertForelder.copy(rolle = FagsakDeltagerRolle.FORELDER))
                    } else {

                        val forelderInfo = runCatching {
                            personopplysningerService.hentPersoninfoEnkel(relasjon.personIdent.id)
                        }.fold(
                            onSuccess = { it },
                            onFailure = {
                                throw IllegalStateException("Feil ved henting av person fra PDL", it)
                            }
                        )

                        val fagsak = fagsakRepository.finnFagsakForPersonIdent(PersonIdent(relasjon.personIdent.id))
                        assosierteFagsakDeltagere.add(
                            RestFagsakDeltager(
                                navn = forelderInfo.navn,
                                ident = relasjon.personIdent.id,
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
        personIdent: String,
        personInfoMedRelasjoner: PersonInfo
    ): MutableList<RestFagsakDeltager> {
        val assosierteFagsakDeltagerMap = mutableMapOf<Long, RestFagsakDeltager>()

        personRepository.findByPersonIdent(PersonIdent(personIdent)).forEach { person: Person ->
            if (person.personopplysningGrunnlag.aktiv) {
                val behandling = behandlingRepository.finnBehandling(person.personopplysningGrunnlag.behandlingId)
                if (behandling.aktiv && !behandling.fagsak.arkivert && !assosierteFagsakDeltagerMap.containsKey(
                        behandling.fagsak.id
                    )
                ) {
                    // get applicant info from PDL. we assume that the applicant is always a person whose info is stored in PDL.
                    if (behandling.fagsak.hentAktivIdent().ident == personIdent) {
                        assosierteFagsakDeltagerMap[behandling.fagsak.id] = RestFagsakDeltager(
                            navn = personInfoMedRelasjoner.navn,
                            ident = behandling.fagsak.hentAktivIdent().ident,
                            rolle = FagsakDeltagerRolle.FORELDER,
                            kjønn = personInfoMedRelasjoner.kjønn,
                            fagsakId = behandling.fagsak.id
                        )
                    } else {
                        val maskertForelder =
                            hentMaskertFagsakdeltakerVedManglendeTilgang(behandling.fagsak.hentAktivIdent().ident)
                        if (maskertForelder != null) {
                            assosierteFagsakDeltagerMap[behandling.fagsak.id] =
                                maskertForelder.copy(rolle = FagsakDeltagerRolle.FORELDER)
                        } else {
                            val personinfo =
                                runCatching {
                                    personopplysningerService.hentPersoninfoEnkel(behandling.fagsak.hentAktivIdent().ident)
                                }.fold(
                                    onSuccess = { it },
                                    onFailure = {
                                        throw IllegalStateException("Feil ved henting av person fra PDL", it)
                                    }
                                )

                            assosierteFagsakDeltagerMap[behandling.fagsak.id] = RestFagsakDeltager(
                                navn = personinfo.navn,
                                ident = behandling.fagsak.hentAktivIdent().ident,
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

    private fun hentMaskertFagsakdeltakerVedManglendeTilgang(personIdent: String): RestFagsakDeltager? {
        val harTilgang = integrasjonClient.sjekkTilgangTilPersoner(listOf(personIdent)).first().harTilgang
        return if (!harTilgang) {
            val adressebeskyttelse = personopplysningerService.hentAdressebeskyttelseSomSystembruker(personIdent)
            RestFagsakDeltager(
                rolle = FagsakDeltagerRolle.UKJENT,
                adressebeskyttelseGradering = adressebeskyttelse,
                harTilgang = false
            )
        } else null
    }

    fun oppgiFagsakdeltagere(personIdent: String, barnasIdenter: List<String>): List<RestFagsakDeltager> {
        val fagsakDeltagere = mutableListOf<RestFagsakDeltager>()

        val personIdenter = personopplysningerService.hentIdenter(Ident(personIdent))
        hentFagsakPåPerson(personIdenter.map { PersonIdent(it.ident) }.toSet())?.also { fagsak ->
            fagsakDeltagere.add(
                RestFagsakDeltager(
                    ident = personIdent,
                    fagsakId = fagsak.id,
                    fagsakStatus = fagsak.status,
                    rolle = FagsakDeltagerRolle.FORELDER
                )
            )
        }

        barnasIdenter.forEach { barnIdent ->
            personopplysningerService.hentIdenter(Ident(barnIdent)).filter { it.gruppe == "FOLKEREGISTERIDENT" }
                .flatMap { hentFagsakerPåPerson(PersonIdent(it.ident)) }.toSet().forEach { fagsak ->
                    fagsakDeltagere.add(
                        RestFagsakDeltager(
                            ident = barnIdent,
                            fagsakId = fagsak.id,
                            fagsakStatus = fagsak.status,
                            rolle = FagsakDeltagerRolle.BARN
                        )
                    )
                }
        }

        return fagsakDeltagere
    }

    fun behandleAnnullertFødsel(barnasIdenter: List<String>) {
        taskRepository.save(BehandleAnnullertFødselTask.opprettTask(barnasIdenter))
    }

    companion object {

        private val logger = LoggerFactory.getLogger(FagsakService::class.java)
    }
}
