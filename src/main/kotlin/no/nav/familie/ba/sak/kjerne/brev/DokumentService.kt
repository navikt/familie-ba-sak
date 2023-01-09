package no.nav.familie.ba.sak.kjerne.brev

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.DEFAULT_JOURNALFØRENDE_ENHET
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.journalføring.UtgåendeJournalføringService
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.DbJournalpost
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.DbJournalpostType
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.JournalføringRepository
import no.nav.familie.ba.sak.integrasjoner.organisasjon.OrganisasjonService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.domene.erTilInstitusjon
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.domene.tilBrev
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.leggTilBlankAnnenVurdering
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.task.DistribuerDokumentTask
import no.nav.familie.ba.sak.task.DistribuerDødsfallDokumentPåFagsakTask
import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Førsteside
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.Properties

@Service
class DokumentService(
    private val persongrunnlagService: PersongrunnlagService,
    private val integrasjonClient: IntegrasjonClient,
    private val loggService: LoggService,
    private val journalføringRepository: JournalføringRepository,
    private val taskRepository: TaskRepositoryWrapper,
    private val brevKlient: BrevKlient,
    private val brevService: BrevService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val vilkårsvurderingForNyBehandlingService: VilkårsvurderingForNyBehandlingService,
    private val rolleConfig: RolleConfig,
    private val settPåVentService: SettPåVentService,
    private val utgåendeJournalføringService: UtgåendeJournalføringService,
    private val fagsakRepository: FagsakRepository,
    private val organisasjonService: OrganisasjonService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService
) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    private val antallBrevSendt: Map<Brevmal, Counter> = mutableListOf<Brevmal>().plus(Brevmal.values()).associateWith {
        Metrics.counter(
            "brev.sendt",
            "brevtype",
            it.visningsTekst
        )
    }

    private val antallBrevIkkeDistribuertUkjentAndresse: Map<Brevmal, Counter> =
        mutableListOf<Brevmal>().plus(Brevmal.values()).associateWith {
            Metrics.counter(
                "brev.ikke.sendt.ukjent.andresse",
                "brevtype",
                it.visningsTekst
            )
        }

    fun hentBrevForVedtak(vedtak: Vedtak): Ressurs<ByteArray> {
        if (SikkerhetContext.hentHøyesteRolletilgangForInnloggetBruker(rolleConfig) == BehandlerRolle.VEILEDER && vedtak.stønadBrevPdF == null) {
            throw FunksjonellFeil("Det finnes ikke noe vedtaksbrev.")
        } else {
            val pdf =
                vedtak.stønadBrevPdF ?: throw Feil("Klarte ikke finne vedtaksbrevbrev for vedtak med id ${vedtak.id}")
            return Ressurs.success(pdf)
        }
    }

    fun genererBrevForVedtak(vedtak: Vedtak): ByteArray {
        try {
            if (!vedtak.behandling.skalBehandlesAutomatisk && vedtak.behandling.steg > StegType.BESLUTTE_VEDTAK) {
                throw FunksjonellFeil("Ikke tillatt å generere brev etter at behandlingen er sendt fra beslutter")
            }

            val målform = persongrunnlagService.hentSøkersMålform(vedtak.behandling.id)
            val vedtaksbrev =
                when (vedtak.behandling.opprettetÅrsak) {
                    BehandlingÅrsak.DØDSFALL_BRUKER -> brevService.hentDødsfallbrevData(vedtak)
                    BehandlingÅrsak.KORREKSJON_VEDTAKSBREV -> brevService.hentKorreksjonbrevData(vedtak)
                    else -> brevService.hentVedtaksbrevData(vedtak)
                }
            return brevKlient.genererBrev(målform.tilSanityFormat(), vedtaksbrev)
        } catch (feil: Throwable) {
            if (feil is FunksjonellFeil) throw feil

            throw Feil(
                message = "Klarte ikke generere vedtaksbrev på behandling ${vedtak.behandling}: ${feil.message}",
                frontendFeilmelding = "Det har skjedd en feil, og brevet er ikke sendt. Prøv igjen, og ta kontakt med brukerstøtte hvis problemet vedvarer.",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                throwable = feil
            )
        }
    }

    fun genererManueltBrev(
        manueltBrevRequest: ManueltBrevRequest,
        erForhåndsvisning: Boolean = false
    ): ByteArray {
        Result.runCatching {
            val brev: Brev = manueltBrevRequest.tilBrev { integrasjonClient.hentLandkoderISO2() }
            return brevKlient.genererBrev(
                målform = manueltBrevRequest.mottakerMålform.tilSanityFormat(),
                brev = brev
            )
        }.fold(
            onSuccess = { it },
            onFailure = {
                if (it is Feil) {
                    throw it
                } else {
                    throw Feil(
                        message = "Klarte ikke generere brev for ${manueltBrevRequest.brevmal}. ${it.message}",
                        frontendFeilmelding = "${if (erForhåndsvisning) "Det har skjedd en feil" else "Det har skjedd en feil, og brevet er ikke sendt"}. Prøv igjen, og ta kontakt med brukerstøtte hvis problemet vedvarer.",
                        httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                        throwable = it
                    )
                }
            }
        )
    }

    @Transactional
    fun sendManueltBrev(
        manueltBrevRequest: ManueltBrevRequest,
        behandling: Behandling? = null,
        fagsakId: Long
    ) {
        val generertBrev = genererManueltBrev(manueltBrevRequest)

        val førsteside = if (manueltBrevRequest.brevmal.skalGenerereForside()) {
            Førsteside(
                språkkode = manueltBrevRequest.mottakerMålform.tilSpråkkode(),
                navSkjemaId = "NAV 33.00-07",
                overskriftstittel = "Ettersendelse til søknad om barnetrygd ordinær NAV 33-00.07"
            )
        } else {
            null
        }

        val fagsak = fagsakRepository.finnFagsak(fagsakId)

        val journalpostId = utgåendeJournalføringService.journalførManueltBrev(
            fnr = fagsak!!.aktør.aktivFødselsnummer(),
            fagsakId = fagsakId.toString(),
            journalførendeEnhet = manueltBrevRequest.enhet?.enhetId
                ?: DEFAULT_JOURNALFØRENDE_ENHET,
            brev = generertBrev,
            førsteside = førsteside,
            dokumenttype = manueltBrevRequest.brevmal.tilFamilieKontrakterDokumentType(),
            avsenderMottaker = utledAvsenderMottaker(manueltBrevRequest)
        )

        if (behandling != null) {
            journalføringRepository.save(
                DbJournalpost(
                    behandling = behandling,
                    journalpostId = journalpostId,
                    type = DbJournalpostType.U
                )
            )
        }

        if (
            behandling != null &&
            manueltBrevRequest.brevmal.førerTilOpplysningsplikt()
        ) {
            leggTilOpplysningspliktIVilkårsvurdering(behandling)
        }

        DistribuerDokumentTask.opprettDistribuerDokumentTask(
            distribuerDokumentDTO = DistribuerDokumentDTO(
                personEllerInstitusjonIdent = manueltBrevRequest.mottakerIdent,
                behandlingId = behandling?.id,
                journalpostId = journalpostId,
                brevmal = manueltBrevRequest.brevmal,
                erManueltSendt = true
            ),
            properties = Properties().apply {
                this["fagsakIdent"] = behandling?.fagsak?.aktør?.aktivFødselsnummer() ?: ""
                this["mottakerIdent"] = manueltBrevRequest.mottakerIdent
                this["journalpostId"] = journalpostId
                this["behandlingId"] = behandling?.id.toString()
                this["fagsakId"] = fagsakId.toString()
            }
        ).also {
            taskRepository.save(it)
        }

        if (
            behandling != null &&
            manueltBrevRequest.brevmal.setterBehandlingPåVent()
        ) {
            settPåVentService.settBehandlingPåVent(
                behandlingId = behandling.id,
                frist = LocalDate.now()
                    .plusDays(
                        manueltBrevRequest.brevmal.ventefristDager(
                            manuellFrist = manueltBrevRequest.antallUkerSvarfrist?.toLong(),
                            behandlingKategori = behandling.kategori
                        )
                    ),
                årsak = manueltBrevRequest.brevmal.venteårsak()
            )
        }
    }

    private fun utledAvsenderMottaker(manueltBrevRequest: ManueltBrevRequest): AvsenderMottaker? {
        return if (manueltBrevRequest.erTilInstitusjon) {
            AvsenderMottaker(
                idType = BrukerIdType.ORGNR,
                id = manueltBrevRequest.mottakerIdent,
                navn = utledInstitusjonNavn(manueltBrevRequest)
            )
        } else {
            null
        }
    }

    private fun utledInstitusjonNavn(manueltBrevRequest: ManueltBrevRequest): String {
        return manueltBrevRequest.mottakerNavn.ifBlank {
            organisasjonService.hentOrganisasjon(manueltBrevRequest.mottakerIdent).navn
        }
    }

    private fun leggTilOpplysningspliktIVilkårsvurdering(behandling: Behandling) {
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandling.id)
            ?: vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
                behandling = behandling,
                bekreftEndringerViaFrontend = false,
                forrigeBehandlingSomErVedtatt = behandlingHentOgPersisterService
                    .hentForrigeBehandlingSomErVedtatt(behandling)
            )
        vilkårsvurdering.personResultater.single { it.erSøkersResultater() }
            .leggTilBlankAnnenVurdering(AnnenVurderingType.OPPLYSNINGSPLIKT)
    }

    fun prøvDistribuerBrevOgLoggHendelse(
        journalpostId: String,
        behandlingId: Long?,
        loggBehandlerRolle: BehandlerRolle,
        brevmal: Brevmal
    ) = try {
        distribuerBrevOgLoggHendlese(journalpostId, behandlingId, brevmal, loggBehandlerRolle)
    } catch (ressursException: RessursException) {
        logger.info("Klarte ikke å distribuere brev til journalpost $journalpostId. Httpstatus ${ressursException.httpStatus}")

        when {
            mottakerErIkkeDigitalOgHarUkjentAdresse(ressursException) && behandlingId != null ->
                loggBrevIkkeDistribuertUkjentAdresse(journalpostId, behandlingId, brevmal)

            mottakerErDødUtenDødsboadresse(ressursException) && behandlingId != null ->
                håndterMottakerDødIngenAdressePåBehandling(journalpostId, brevmal, behandlingId)

            dokumentetErAlleredeDistribuert(ressursException) ->
                logger.warn(alleredeDistribuertMelding(journalpostId, behandlingId))

            else -> throw ressursException
        }
    }

    internal fun håndterMottakerDødIngenAdressePåBehandling(
        journalpostId: String,
        brevmal: Brevmal,
        behandlingId: Long
    ) {
        val task = DistribuerDødsfallDokumentPåFagsakTask.opprettTask(journalpostId = journalpostId, brevmal = brevmal)
        taskRepository.save(task)
        logger.info("Klarte ikke å distribuere brev for journalpostId $journalpostId på behandling $behandlingId. Bruker har ukjent dødsboadresse.")
        loggService.opprettBrevIkkeDistribuertUkjentDødsboadresseLogg(
            behandlingId = behandlingId,
            brevnavn = brevmal.visningsTekst
        )
    }

    internal fun loggBrevIkkeDistribuertUkjentAdresse(
        journalpostId: String,
        behandlingId: Long,
        brevMal: Brevmal
    ) {
        logger.info("Klarte ikke å distribuere brev for journalpostId $journalpostId på behandling $behandlingId. Bruker har ukjent adresse.")
        loggService.opprettBrevIkkeDistribuertUkjentAdresseLogg(
            behandlingId = behandlingId,
            brevnavn = brevMal.visningsTekst
        )
        antallBrevIkkeDistribuertUkjentAndresse[brevMal]?.increment()
    }

    private fun distribuerBrevOgLoggHendlese(
        journalpostId: String,
        behandlingId: Long?,
        brevMal: Brevmal,
        loggBehandlerRolle: BehandlerRolle
    ) {
        integrasjonClient.distribuerBrev(journalpostId = journalpostId, distribusjonstype = brevMal.distribusjonstype)

        if (behandlingId != null) {
            loggService.opprettDistribuertBrevLogg(
                behandlingId = behandlingId,
                tekst = brevMal.visningsTekst,
                rolle = loggBehandlerRolle
            )
        }

        antallBrevSendt[brevMal]?.increment()
    }

    companion object {

        fun alleredeDistribuertMelding(journalpostId: String, behandlingId: Long?) =
            "Journalpost med Id=$journalpostId er allerede distiribuert. Hopper over distribuering." +
                if (behandlingId != null) " BehandlingId=$behandlingId." else ""
    }
}
