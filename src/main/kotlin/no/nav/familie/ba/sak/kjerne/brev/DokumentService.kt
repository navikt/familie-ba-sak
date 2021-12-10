package no.nav.familie.ba.sak.kjerne.brev

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.DEFAULT_JOURNALFØRENDE_ENHET
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.DbJournalpost
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.DbJournalpostType
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.JournalføringRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevType.INNHENTE_OPPLYSNINGER
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevType.VARSEL_OM_REVURDERING
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.domene.tilBrevmal
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.task.DistribuerDokumentTask
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Førsteside
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
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
    private val environment: Environment,
    private val rolleConfig: RolleConfig,
    private val envService: EnvService
) {

    private val antallBrevSendt: Map<Brevmal, Counter> = mutableListOf<Brevmal>().plus(Brevmal.values()).associateWith {
        Metrics.counter(
            "brev.sendt",
            "brevtype", it.visningsTekst
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
                throw Feil("Ikke tillatt å generere brev etter at behandlingen er sendt fra beslutter")
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
                message = "Klarte ikke generere vedtaksbrev: ${feil.message}",
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
            val brev: Brev = manueltBrevRequest.tilBrevmal()
            return brevKlient.genererBrev(
                målform = manueltBrevRequest.mottakerMålform.tilSanityFormat(),
                brev = brev
            )
        }.fold(
            onSuccess = { it },
            onFailure = {
                if (it is Feil) {
                    throw it
                } else throw Feil(
                    message = "Klarte ikke generere brev for ${manueltBrevRequest.brevmal.visningsTekst}. ${it.message}",
                    frontendFeilmelding = "${if (erForhåndsvisning) "Det har skjedd en feil" else "Det har skjedd en feil, og brevet er ikke sendt"}. Prøv igjen, og ta kontakt med brukerstøtte hvis problemet vedvarer.",
                    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                    throwable = it
                )
            }
        )
    }

    fun sendManueltBrev(
        manueltBrevRequest: ManueltBrevRequest,
        behandling: Behandling? = null,
        fagsakId: Long
    ) {

        val generertBrev = genererManueltBrev(manueltBrevRequest)

        val førsteside = if (manueltBrevRequest.brevmal.genererForside) {
            Førsteside(
                språkkode = manueltBrevRequest.mottakerMålform.tilSpråkkode(),
                navSkjemaId = "NAV 33.00-07",
                overskriftstittel = "Ettersendelse til søknad om barnetrygd ordinær NAV 33-00.07"
            )
        } else null

        val journalpostId = integrasjonClient.journalførManueltBrev(
            fnr = manueltBrevRequest.mottakerIdent,
            fagsakId = fagsakId.toString(),
            journalførendeEnhet = manueltBrevRequest.enhet?.enhetId
                ?: DEFAULT_JOURNALFØRENDE_ENHET,
            brev = generertBrev,
            førsteside = førsteside,
            dokumenttype = manueltBrevRequest.brevmal.dokumenttype
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

        if ((
                manueltBrevRequest.brevmal == INNHENTE_OPPLYSNINGER ||
                    manueltBrevRequest.brevmal == VARSEL_OM_REVURDERING
                ) && behandling != null
        ) {
            vilkårsvurderingService.opprettOglagreBlankAnnenVurdering(
                annenVurderingType = AnnenVurderingType.OPPLYSNINGSPLIKT,
                behandlingId = behandling.id
            )
        }

        DistribuerDokumentTask.opprettDistribuerDokumentTask(
            distribuerDokumentDTO = DistribuerDokumentDTO(
                personIdent = manueltBrevRequest.mottakerIdent,
                behandlingId = behandling?.id,
                journalpostId = journalpostId,
                brevmal = manueltBrevRequest.brevmal.tilSanityBrevtype(),
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
    }

    fun distribuerBrevOgLoggHendelse(
        journalpostId: String,
        behandlingId: Long?,
        loggBehandlerRolle: BehandlerRolle,
        brevMal: Brevmal,
    ) {
        integrasjonClient.distribuerBrev(journalpostId)
        if (behandlingId != null) {
            loggService.opprettDistribuertBrevLogg(
                behandlingId = behandlingId,
                tekst = brevMal.visningsTekst.replaceFirstChar { it.uppercase() },
                rolle = loggBehandlerRolle
            )
        }
        antallBrevSendt[brevMal]?.increment()
    }
}
