package no.nav.familie.ba.sak.kjerne.fagsak

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestBaseFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestInstitusjon
import no.nav.familie.ba.sak.ekstern.restDomene.RestMinimalFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestSkjermetBarnSøker
import no.nav.familie.ba.sak.ekstern.restDomene.RestVisningBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestMinimalFagsak
import no.nav.familie.ba.sak.integrasjoner.organisasjon.OrganisasjonService
import no.nav.familie.ba.sak.integrasjoner.skyggesak.SkyggesakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.kjerne.institusjon.InstitusjonService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.skjermetbarnsøker.SkjermetBarnSøker
import no.nav.familie.ba.sak.kjerne.skjermetbarnsøker.SkjermetBarnSøkerRepository
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.kontrakter.felles.Ressurs
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FagsakService(
    private val fagsakRepository: FagsakRepository,
    private val personRepository: PersonRepository,
    private val andelerTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val personidentService: PersonidentService,
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val behandlingService: BehandlingService,
    private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,
    private val skyggesakService: SkyggesakService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val institusjonService: InstitusjonService,
    private val organisasjonService: OrganisasjonService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val skjermetBarnSøkerRepository: SkjermetBarnSøkerRepository,
    private val featureToggleService: FeatureToggleService,
) {
    private val antallFagsakerOpprettetFraManuell =
        Metrics.counter("familie.ba.sak.fagsak.opprettet", "saksbehandling", "manuell")
    private val antallFagsakerOpprettetFraAutomatisk =
        Metrics.counter("familie.ba.sak.fagsak.opprettet", "saksbehandling", "automatisk")

    @Transactional
    fun oppdaterLøpendeStatusPåFagsaker(): Int {
        val fagsaker = fagsakRepository.finnFagsakerSomSkalAvsluttes()
        for (fagsakId in fagsaker) {
            val fagsak = fagsakRepository.getReferenceById(fagsakId)
            oppdaterStatus(fagsak, FagsakStatus.AVSLUTTET)
        }
        return fagsaker.size
    }

    @Transactional
    fun hentEllerOpprettFagsak(fagsakRequest: FagsakRequest): Ressurs<RestMinimalFagsak> {
        val fagsak =
            hentEllerOpprettFagsak(
                personIdent = fagsakRequest.personIdent,
                type = fagsakRequest.fagsakType ?: FagsakType.NORMAL,
                institusjon = fagsakRequest.institusjon,
                skjermetBarnSøker = fagsakRequest.skjermetBarnSøker,
            )
        return hentRestMinimalFagsak(fagsakId = fagsak.id)
    }

    @Transactional
    fun hentEllerOpprettFagsak(
        personIdent: String,
        fraAutomatiskBehandling: Boolean = false,
        type: FagsakType = FagsakType.NORMAL,
        institusjon: RestInstitusjon? = null,
        skjermetBarnSøker: RestSkjermetBarnSøker? = null,
    ): Fagsak {
        if (type == FagsakType.SKJERMET_BARN) {
            when {
                !featureToggleService.isEnabled(FeatureToggle.SKAL_BRUKE_FAGSAKTYPE_SKJERMET_BARN) -> {
                    throw FunksjonellFeil(
                        melding = "Fagsaktype SKJERMET_BARN er ikke støttet i denne versjonen av tjenesten.",
                        frontendFeilmelding = "Fagsaktype SKJERMET_BARN er ikke støttet i denne versjonen av tjenesten.",
                    )
                }

                fraAutomatiskBehandling -> {
                    throw FunksjonellFeil(
                        melding = "Kan ikke opprette fagsak med fagsaktype SKJERMET_BARN automatisk",
                        frontendFeilmelding = "Kan ikke opprette fagsak med fagsaktype SKJERMET_BARN automatisk",
                    )
                }
            }
        }

        val aktør = personidentService.hentOgLagreAktør(personIdent, true)

        val eksisterendeFagsak =
            when (type) {
                FagsakType.INSTITUSJON -> {
                    val orgnummer = institusjon?.orgNummer ?: throw FunksjonellFeil("Mangler påkrevd variabel orgnummer for institusjon")

                    fagsakRepository.finnFagsakForInstitusjonOgOrgnummer(aktør, orgnummer)
                }

                FagsakType.SKJERMET_BARN -> {
                    val søkersIdent = skjermetBarnSøker?.søkersIdent ?: throw FunksjonellFeil("Mangler påkrevd variabel søkersident for skjermet barn søker")

                    if (søkersIdent == personIdent) {
                        throw FunksjonellFeil("Søker og barn søkt for kan ikke være lik for fagsak type skjermet barn")
                    }

                    val søkersAktør = personidentService.hentOgLagreAktør(søkersIdent, true)

                    fagsakRepository.finnFagsakForSkjermetBarnSøker(aktør, søkersAktør)
                }

                else -> {
                    fagsakRepository.finnFagsakForAktør(aktør, type)
                }
            }
        if (eksisterendeFagsak != null) return eksisterendeFagsak

        val nyFagsak = Fagsak(aktør = aktør, type = type)

        if (type == FagsakType.INSTITUSJON) {
            nyFagsak.institusjon = institusjonService.hentEllerOpprettInstitusjon(institusjon!!.orgNummer!!, institusjon.tssEksternId)
        }

        if (type == FagsakType.SKJERMET_BARN) {
            val søkersAktør = personidentService.hentOgLagreAktør(skjermetBarnSøker!!.søkersIdent, true)
            nyFagsak.skjermetBarnSøker = skjermetBarnSøkerRepository.findByAktør(søkersAktør) ?: skjermetBarnSøkerRepository.saveAndFlush(SkjermetBarnSøker(aktør = søkersAktør))
        }

        if (fraAutomatiskBehandling) {
            antallFagsakerOpprettetFraAutomatisk.increment()
        } else {
            antallFagsakerOpprettetFraManuell.increment()
        }

        return lagre(nyFagsak).also { skyggesakService.opprettSkyggesak(it) }
    }

    fun hentFagsakerPåPerson(aktør: Aktør): List<Fagsak> = personRepository.findFagsakerByAktør(aktør)

    @Transactional
    fun lagre(fagsak: Fagsak): Fagsak {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter fagsak $fagsak")
        return fagsakRepository.save(fagsak).also { saksstatistikkEventPublisher.publiserSaksstatistikk(it.id) }
    }

    fun oppdaterStatus(
        fagsak: Fagsak,
        nyStatus: FagsakStatus,
    ): Fagsak {
        logger.info(
            "${SikkerhetContext.hentSaksbehandlerNavn()} endrer status på fagsak ${fagsak.id} fra ${fagsak.status}" +
                " til $nyStatus",
        )
        fagsak.status = nyStatus

        return lagre(fagsak)
    }

    fun hentMinimalFagsakForPerson(
        aktør: Aktør,
        fagsakType: FagsakType = FagsakType.NORMAL,
    ): Ressurs<RestMinimalFagsak> {
        val fagsak = fagsakRepository.finnFagsakForAktør(aktør, fagsakType)
        return if (fagsak != null) {
            Ressurs.success(data = lagRestMinimalFagsak(fagsakId = fagsak.id))
        } else {
            Ressurs.failure(
                errorMessage = "Fant ikke fagsak på person",
            )
        }
    }

    fun hentMinimalFagsakerForPerson(
        aktør: Aktør,
        fagsakTyper: List<FagsakType> = FagsakType.entries.toList(),
    ): Ressurs<List<RestMinimalFagsak>> {
        val fagsaker = fagsakRepository.finnFagsakerForAktør(aktør).filter { fagsakTyper.contains(it.type) }
        return Ressurs.success(data = lagRestMinimalFagsaker(fagsaker))
    }

    fun hentRestFagsak(fagsakId: Long): Ressurs<RestFagsak> = Ressurs.success(data = lagRestFagsak(fagsakId))

    fun hentRestMinimalFagsak(fagsakId: Long): Ressurs<RestMinimalFagsak> = Ressurs.success(data = lagRestMinimalFagsak(fagsakId))

    fun lagRestMinimalFagsaker(fagsaker: List<Fagsak>): List<RestMinimalFagsak> = fagsaker.map { lagRestMinimalFagsak(it.id) }

    fun lagRestMinimalFagsak(fagsakId: Long): RestMinimalFagsak {
        val restBaseFagsak = lagRestBaseFagsak(fagsakId)
        val visningsbehandlinger = behandlingHentOgPersisterService.hentVisningsbehandlinger(fagsakId)
        val migreringsdato = behandlingService.hentMigreringsdatoPåFagsak(fagsakId)
        return restBaseFagsak.tilRestMinimalFagsak(
            restVisningBehandlinger = visningsbehandlinger.map { RestVisningBehandling.opprettFraVisningsbehandling(it) },
            migreringsdato = migreringsdato,
        )
    }

    private fun lagRestFagsak(fagsakId: Long): RestFagsak {
        val restBaseFagsak = lagRestBaseFagsak(fagsakId)

        val utvidedeBehandlinger =
            behandlingHentOgPersisterService
                .hentBehandlinger(fagsakId = fagsakId)
                .map { utvidetBehandlingService.lagRestUtvidetBehandling(it.id) }

        return restBaseFagsak.tilRestFagsak(utvidedeBehandlinger)
    }

    private fun lagRestBaseFagsak(fagsakId: Long): RestBaseFagsak {
        val fagsak = hentPåFagsakId(fagsakId = fagsakId)

        val aktivBehandling = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId = fagsakId)

        val sistVedtatteBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = fagsakId)

        val gjeldendeUtbetalingsperioder =
            if (sistVedtatteBehandling != null) vedtaksperiodeService.hentUtbetalingsperioder(behandling = sistVedtatteBehandling) else emptyList()

        return RestBaseFagsak(
            opprettetTidspunkt = fagsak.opprettetTidspunkt,
            id = fagsak.id,
            fagsakeier = fagsak.aktør.aktivFødselsnummer(),
            søkerFødselsnummer = hentSøkersFødselsnummer(fagsak),
            status = fagsak.status,
            underBehandling =
                if (aktivBehandling == null) {
                    false
                } else {
                    aktivBehandling.status == BehandlingStatus.UTREDES || (aktivBehandling.steg >= StegType.BESLUTTE_VEDTAK && aktivBehandling.steg != StegType.BEHANDLING_AVSLUTTET)
                },
            løpendeKategori = (aktivBehandling ?: sistVedtatteBehandling)?.kategori,
            løpendeUnderkategori = (aktivBehandling ?: sistVedtatteBehandling)?.underkategori,
            gjeldendeUtbetalingsperioder = gjeldendeUtbetalingsperioder,
            fagsakType = fagsak.type,
            institusjon =
                fagsak.institusjon?.let {
                    RestInstitusjon(
                        orgNummer = it.orgNummer,
                        tssEksternId = it.tssEksternId,
                        navn = organisasjonService.hentOrganisasjon(it.orgNummer).navn,
                    )
                },
        )
    }

    private fun hentSøkersFødselsnummer(fagsak: Fagsak): String {
        val aktør =
            if (fagsak.type == FagsakType.SKJERMET_BARN) {
                fagsak.skjermetBarnSøker?.aktør ?: throw Feil("Søker er ikke lagret på fagsaken")
            } else {
                fagsak.aktør
            }
        return aktør.aktivFødselsnummer()
    }

    @Transactional
    fun hentEllerOpprettFagsakForPersonIdent(
        fødselsnummer: String,
        fraAutomatiskBehandling: Boolean = false,
        fagsakType: FagsakType = FagsakType.NORMAL,
        institusjon: RestInstitusjon? = null,
        skjermetBarnSøker: RestSkjermetBarnSøker? = null,
    ): Fagsak = hentEllerOpprettFagsak(fødselsnummer, fraAutomatiskBehandling, fagsakType, institusjon, skjermetBarnSøker)

    fun hentNormalFagsak(aktør: Aktør): Fagsak? =
        fagsakRepository.finnFagsakForAktør(
            aktør,
            FagsakType.NORMAL,
        )

    fun hentPåFagsakId(fagsakId: Long): Fagsak =
        fagsakRepository.finnFagsak(fagsakId) ?: throw FunksjonellFeil(
            melding = "Finner ikke fagsak med id $fagsakId",
            frontendFeilmelding = "Finner ikke fagsak med id $fagsakId",
        )

    fun hentAktør(fagsakId: Long): Aktør = hentPåFagsakId(fagsakId).aktør

    fun hentFagsakPåPerson(
        aktør: Aktør,
        fagsakType: FagsakType = FagsakType.NORMAL,
    ): Fagsak? = fagsakRepository.finnFagsakForAktør(aktør, fagsakType)

    fun hentAlleFagsakerForAktør(aktør: Aktør): List<Fagsak> = fagsakRepository.finnFagsakerForAktør(aktør)

    fun hentLøpendeFagsaker(): List<Fagsak> = fagsakRepository.finnLøpendeFagsaker()

    fun hentIdPåLøpendeFagsaker(): List<Long> = fagsakRepository.finnIdPåLøpendeFagsaker()

    fun finnAlleFagsakerHvorAktørHarLøpendeYtelseAvType(
        aktør: Aktør,
        ytelseTyper: List<YtelseType>,
    ): List<Fagsak> {
        val ordinæreAndelerPåAktør =
            andelerTilkjentYtelseRepository
                .finnAndelerTilkjentYtelseForAktør(aktør = aktør)
                .filter { it.type in ytelseTyper }

        val løpendeAndeler = ordinæreAndelerPåAktør.filter { it.erLøpende() }

        val behandlingerMedLøpendeAndeler =
            løpendeAndeler
                .map { it.behandlingId }
                .toSet()
                .map { behandlingHentOgPersisterService.hent(behandlingId = it) }

        val behandlingerSomErSisteIverksattePåFagsak = behandlingerMedLøpendeAndeler.filter { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(it.fagsak.id) == it }

        return behandlingerSomErSisteIverksattePåFagsak.map { it.fagsak }
    }

    fun finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(aktør: Aktør): List<Fagsak> {
        val alleLøpendeFagsakerPåAktør = hentAlleFagsakerForAktør(aktør).filter { it.status == FagsakStatus.LØPENDE }

        val fagsakerHvorAktørHarLøpendeOrdinærBarnetrygd = finnAlleFagsakerHvorAktørHarLøpendeYtelseAvType(aktør = aktør, ytelseTyper = listOf(YtelseType.ORDINÆR_BARNETRYGD))

        return (alleLøpendeFagsakerPåAktør + fagsakerHvorAktørHarLøpendeOrdinærBarnetrygd).distinct()
    }

    fun finnOrgnummerForLøpendeFagsaker(): List<String> = fagsakRepository.finnOrgnummerForLøpendeFagsaker()

    fun finnIdenterForLøpendeFagsaker(): List<String> = fagsakRepository.finnIdenterForLøpendeFagsaker()

    companion object {
        private val logger = LoggerFactory.getLogger(FagsakService::class.java)
    }
}
