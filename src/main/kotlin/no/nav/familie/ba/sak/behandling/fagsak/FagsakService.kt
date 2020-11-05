package no.nav.familie.ba.sak.behandling.fagsak

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.arbeidsfordeling.domene.toRestArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.*
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.VedtakRepository
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.TilkjentYtelseUtils
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.opplysningsplikt.OpplysningspliktRepository
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.FAMILIERELASJONSROLLE
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollRepository
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException
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
        private val behandlingResultatService: BehandlingResultatService,
        private val vedtakRepository: VedtakRepository,
        private val totrinnskontrollRepository: TotrinnskontrollRepository,
        private val tilkjentYtelseRepository: TilkjentYtelseRepository,
        private val personopplysningerService: PersonopplysningerService,
        private val integrasjonClient: IntegrasjonClient,
        private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,
        private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
        private val opplysningspliktRepository: OpplysningspliktRepository) {


    private val antallFagsakerOpprettet = Metrics.counter("familie.ba.sak.fagsak.opprettet")

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
        return hentRestFagsak(fagsakId = fagsak.id)
    }

    @Transactional
    fun hentEllerOpprettFagsak(personIdent: PersonIdent): Fagsak {
        val identer = personopplysningerService.hentIdenter(Ident(personIdent.ident)).map { PersonIdent(it.ident) }.toSet()
        var fagsak = fagsakPersonRepository.finnFagsak(personIdenter = identer)
        if (fagsak == null) {
            fagsak = Fagsak().also {
                it.søkerIdenter = setOf(FagsakPerson(personIdent = personIdent, fagsak = it))
                lagre(it)
            }
            saksstatistikkEventPublisher.publish(fagsak.id)
            antallFagsakerOpprettet.increment()
        } else if (fagsak.søkerIdenter.none { fagsakPerson -> fagsakPerson.personIdent == personIdent }) {
            fagsak.also {
                it.søkerIdenter += FagsakPerson(personIdent = personIdent, fagsak = it)
                lagre(it)
            }
            saksstatistikkEventPublisher.publish(fagsak.id)
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
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter fagsak $fagsak")
        return fagsakRepository.save(fagsak)
    }

    fun oppdaterStatus(fagsak: Fagsak, nyStatus: FagsakStatus) {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} endrer status på fagsak ${fagsak.id} fra ${fagsak.status} til $nyStatus")
        fagsak.status = nyStatus

        lagre(fagsak)
        saksstatistikkEventPublisher.publish(fagsak.id)
    }

    fun hentRestFagsak(fagsakId: Long): Ressurs<RestFagsak> {
        val fagsak = fagsakRepository.finnFagsak(fagsakId)
                     ?: throw FunksjonellFeil(melding = "Finner ikke fagsak med id $fagsakId",
                                              frontendFeilmelding = "Finner ikke fagsak med id $fagsakId")

        val restBehandlinger: List<RestBehandling> = lagRestBehandlinger(fagsak)
        return Ressurs.success(data = fagsak.toRestFagsak(restBehandlinger))
    }

    fun hentRestFagsakForPerson(personIdent: PersonIdent): Ressurs<RestFagsak?> {
        val fagsak = fagsakRepository.finnFagsakForPersonIdent(personIdent)
        if (fagsak != null) {
            val restBehandlinger: List<RestBehandling> = lagRestBehandlinger(fagsak)
            return Ressurs.success(data = fagsak.toRestFagsak(restBehandlinger))
        }
        return Ressurs.success(data = null)
    }

    private fun lagRestBehandlinger(fagsak: Fagsak): List<RestBehandling> {
        val behandlinger = behandlingRepository.finnBehandlinger(fagsak.id)

        return behandlinger.map { behandling ->
            val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandling.id)

            val arbeidsfordelingPåBehandling =
                    arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)

            val restVedtakForBehandling = vedtakRepository.finnVedtakForBehandling(behandling.id).map { vedtak ->
                val andelerTilkjentYtelse =
                        andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(listOf(behandling.id))
                val restVedtakPerson = lagRestVedtakPerson(andelerTilkjentYtelse, personopplysningGrunnlag)
                vedtak.toRestVedtak(restVedtakPerson)
            }

            val totrinnskontroll = totrinnskontrollRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)

            val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingOptional(behandlingId = behandling.id)

            val forrigeBehandling = behandlinger
                    .filter { it.opprettetTidspunkt.isBefore(behandling.opprettetTidspunkt) }
                    .sortedBy { it.opprettetTidspunkt }
                    .findLast {
                        it.type != BehandlingType.TEKNISK_OPPHØR &&
                        it.stegTemp == StegType.BEHANDLING_AVSLUTTET &&
                        !erBehandlingHenlagt(it)
                    }

            val opplysningsplikt = opplysningspliktRepository.findByBehandlingId(behandlingId = behandling.id)

            RestBehandling(
                    aktiv = behandling.aktiv,
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling.toRestArbeidsfordelingPåBehandling(),
                    årsak = behandling.opprettetÅrsak,
                    skalBehandlesAutomatisk = behandling.skalBehandlesAutomatisk,
                    behandlingId = behandling.id,
                    vedtakForBehandling = restVedtakForBehandling,
                    personer = personopplysningGrunnlag?.personer?.map { it.toRestPerson() } ?: emptyList(),
                    type = behandling.type,
                    status = behandling.status,
                    steg = behandling.stegTemp,
                    stegTilstand = behandling.behandlingStegTilstand.map { it.toRestBehandlingStegTilstand() },
                    personResultater = behandlingResultatService.hentAktivForBehandling(behandling.id)
                                               ?.personResultater?.map { it.tilRestPersonResultat() } ?: emptyList(),
                    samletResultat =
                    if (personopplysningGrunnlag == null)
                        BehandlingResultatType.IKKE_VURDERT
                    else
                        behandlingResultatService.hentAktivForBehandling(
                                behandling.id)?.samletResultat
                        ?: BehandlingResultatType.IKKE_VURDERT,
                    opprettetTidspunkt = behandling.opprettetTidspunkt,
                    kategori = behandling.kategori,
                    underkategori = behandling.underkategori,
                    endretAv = behandling.endretAv,
                    totrinnskontroll = totrinnskontroll?.toRestTotrinnskontroll(),
                    beregningOversikt = if (tilkjentYtelse == null || personopplysningGrunnlag == null) emptyList() else
                        TilkjentYtelseUtils.hentBeregningOversikt(
                                tilkjentYtelseForBehandling = tilkjentYtelse,
                                personopplysningGrunnlag = personopplysningGrunnlag,
                                tilkjentYtelseForForrigeBehandling = if (forrigeBehandling != null) tilkjentYtelseRepository.findByBehandling(
                                        behandlingId = forrigeBehandling.id) else null),
                    gjeldendeForUtbetaling = behandling.gjeldendeForFremtidigUtbetaling,
                    opplysningsplikt = opplysningsplikt?.toRestOpplysningsplikt()
            )
        }
    }

    fun erBehandlingHenlagt(behandling: Behandling): Boolean {
        return behandlingResultatService.hentAktivForBehandling(behandling.id)?.erHenlagt() == true
    }

    fun hentEllerOpprettFagsakForPersonIdent(fødselsnummer: String): Fagsak {
        val personIdent = PersonIdent(fødselsnummer)
        return hentEllerOpprettFagsak(personIdent)
    }

    fun hent(personIdent: PersonIdent): Fagsak? {
        val identer = personopplysningerService.hentIdenter(Ident(personIdent.ident)).map { PersonIdent(it.ident) }.toSet()
        return fagsakPersonRepository.finnFagsak(identer)
    }

    fun hentLøpendeFagsaker(): List<Fagsak> {
        return fagsakRepository.finnLøpendeFagsaker()
    }

    fun hentFagsakDeltager(personIdent: String): List<RestFagsakDeltager> {
        val maskertDeltaker = hentMaskertFagsakdeltakerVedManglendeTilgang(personIdent)
        if (maskertDeltaker != null) {
            return listOf(maskertDeltaker)
        }
        val personInfoMedRelasjoner = runCatching {
            personopplysningerService.hentPersoninfoMedRelasjoner(personIdent)
        }.fold(
                onSuccess = { it },
                onFailure = {
                    val clientError = it as? HttpClientErrorException?
                    if (clientError != null && clientError.statusCode == HttpStatus.NOT_FOUND) {
                        throw clientError
                    } else {
                        throw IllegalStateException("Feil ved henting av person fra PDL", it)
                    }
                }
        )

        //We find all cases that either have the given person as applicant, or have it as a child
        val assosierteFagsakDeltagerMap = mutableMapOf<Long, RestFagsakDeltager>()

        personRepository.findByPersonIdent(PersonIdent(personIdent)).forEach {
            if (it.personopplysningGrunnlag.aktiv) {
                val behandling = behandlingRepository.finnBehandling(it.personopplysningGrunnlag.behandlingId)
                if (behandling.aktiv && !assosierteFagsakDeltagerMap.containsKey(behandling.fagsak.id)) {
                    //get applicant info from PDL. we assume that the applicant is always a person whose info is stored in PDL.
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
                                        personopplysningerService.hentPersoninfo(behandling.fagsak.hentAktivIdent().ident)
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

        //The given person and its parents may be included in the result, no matter whether they have a case.
        val assosierteFagsakDeltager = assosierteFagsakDeltagerMap.values.toMutableList()
        val erBarn = Period.between(personInfoMedRelasjoner.fødselsdato, LocalDate.now()).years < 18

        if (assosierteFagsakDeltager.find { it.ident == personIdent } == null) {
            val fagsakId = if (!erBarn) fagsakRepository.finnFagsakForPersonIdent(PersonIdent(personIdent))?.id else null
            assosierteFagsakDeltager.add(RestFagsakDeltager(
                    navn = personInfoMedRelasjoner.navn,
                    ident = personIdent,
                    //we set the role to unknown when the person is not a child because the person may not have a child
                    rolle = if (erBarn) FagsakDeltagerRolle.BARN else FagsakDeltagerRolle.UKJENT,
                    kjønn = personInfoMedRelasjoner.kjønn,
                    fagsakId = fagsakId
            ))
        }

        if (erBarn) {
            personInfoMedRelasjoner.familierelasjoner.filter { familierelasjon ->
                familierelasjon.relasjonsrolle == FAMILIERELASJONSROLLE.FAR ||
                familierelasjon.relasjonsrolle == FAMILIERELASJONSROLLE.MOR ||
                familierelasjon.relasjonsrolle == FAMILIERELASJONSROLLE.MEDMOR
            }.forEach { familierelasjon ->
                if (assosierteFagsakDeltager.find { fagsakDeltager -> fagsakDeltager.ident == familierelasjon.personIdent.id } == null) {

                    val maskertForelder = hentMaskertFagsakdeltakerVedManglendeTilgang(familierelasjon.personIdent.id)
                    if (maskertForelder != null) {
                        assosierteFagsakDeltager.add(maskertForelder.copy(rolle = FagsakDeltagerRolle.FORELDER))
                    } else {

                        val forelderInfo = runCatching {
                            personopplysningerService.hentPersoninfo(familierelasjon.personIdent.id)
                        }.fold(
                                onSuccess = { it },
                                onFailure = {
                                    throw IllegalStateException("Feil ved henting av person fra PDL", it)
                                }
                        )

                        val fagsak = fagsakRepository.finnFagsakForPersonIdent(PersonIdent(familierelasjon.personIdent.id))
                        assosierteFagsakDeltager.add(RestFagsakDeltager(
                                navn = forelderInfo.navn,
                                ident = familierelasjon.personIdent.id,
                                rolle = FagsakDeltagerRolle.FORELDER,
                                kjønn = forelderInfo.kjønn,
                                fagsakId = fagsak?.id
                        ))
                    }
                }
            }
        }
        return assosierteFagsakDeltager
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

    fun hentPågåendeSakStatus(personIdent: String): RestPågåendeSakSøk {
        val fagsak = hent(PersonIdent(personIdent))
        val behandling = fagsak?.let { behandlingRepository.findByFagsakAndAktiv(it.id) }

        return RestPågåendeSakSøk(
                harPågåendeSakIBaSak = fagsak?.status.let { it == FagsakStatus.LØPENDE } ||
                                       behandling?.status.let { it != null && it != BehandlingStatus.AVSLUTTET },
                harPågåendeSakIInfotrygd = harLøpendeSakIInfotrygd(personIdent)
        )
    }

    private fun harLøpendeSakIInfotrygd(personIdent: String): Boolean {
        val identer = personopplysningerService.hentIdenter(Ident(personIdent)).filter { it.gruppe == "FOLKEREGISTERIDENT" }
                .map { it.ident }
        return infotrygdBarnetrygdClient.harLøpendeSakIInfotrygd(søkersIdenter = identer)
    }

    companion object {

        val LOG = LoggerFactory.getLogger(FagsakService::class.java)
    }
}
