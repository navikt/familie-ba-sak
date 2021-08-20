package no.nav.familie.ba.sak.kjerne.dokument

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.DbJournalpost
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.DbJournalpostType
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.JournalføringRepository
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.dokument.DokumentController.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.dokument.domene.BrevType.INNHENTE_OPPLYSNINGER
import no.nav.familie.ba.sak.kjerne.dokument.domene.BrevType.VARSEL_OM_REVURDERING
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Brev
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.BrevType
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.EnkelBrevtype
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Vedtaksbrevtype
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.tilBrevmal
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
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
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.Properties

@Service
class DokumentService(
        private val persongrunnlagService: PersongrunnlagService,
        private val integrasjonClient: IntegrasjonClient,
        private val arbeidsfordelingService: ArbeidsfordelingService,
        private val loggService: LoggService,
        private val journalføringRepository: JournalføringRepository,
        private val taskRepository: TaskRepository,
        private val brevKlient: BrevKlient,
        private val brevService: BrevService,
        private val vilkårsvurderingService: VilkårsvurderingService,
        private val environment: Environment,
        private val rolleConfig: RolleConfig
) {

    private val antallBrevSendt: Map<BrevType, Counter> = mutableListOf<BrevType>().plus(EnkelBrevtype.values()).plus(
            Vedtaksbrevtype.values()).map {
        it to Metrics.counter("brev.sendt",
                              "brevtype", it.visningsTekst)
    }.toMap()

    fun hentBrevForVedtak(vedtak: Vedtak): Ressurs<ByteArray> {
        if (SikkerhetContext.hentHøyesteRolletilgangForInnloggetBruker(rolleConfig) == BehandlerRolle.VEILEDER && vedtak.stønadBrevPdF == null) {
            throw FunksjonellFeil("Det finnes ikke noe vedtaksbrev.")
        } else {
            val pdf = vedtak.stønadBrevPdF ?: throw Feil("Klarte ikke finne vedtaksbrevbrev for vetak med id ${vedtak.id}")
            return Ressurs.success(pdf)
        }
    }

    fun genererBrevForVedtak(vedtak: Vedtak): ByteArray {
        if (environment.activeProfiles.contains("e2e") && vedtak.behandling.skalBehandlesAutomatisk) return ByteArray(1)
        try {
            if (!vedtak.behandling.skalBehandlesAutomatisk && vedtak.behandling.steg > StegType.BESLUTTE_VEDTAK) {
                throw Feil("Ikke tillatt å generere brev etter at behandlingen er sendt fra beslutter")
            }

            val målform = persongrunnlagService.hentSøkersMålform(vedtak.behandling.id)
            val vedtaksbrev =
                    if (vedtak.behandling.opprettetÅrsak == BehandlingÅrsak.DØDSFALL_BRUKER)
                        brevService.hentDødsfallbrevData(vedtak)
                    else
                        brevService.hentVedtaksbrevData(vedtak)
            return brevKlient.genererBrev(målform.tilSanityFormat(), vedtaksbrev)
        } catch (funksjonellFeil: FunksjonellFeil) {
            throw funksjonellFeil
        } catch (feil: Throwable) {
            throw Feil(message = "Klarte ikke generere vedtaksbrev: ${feil.message}",
                       frontendFeilmelding = "Det har skjedd en feil, og brevet er ikke sendt. Prøv igjen, og ta kontakt med brukerstøtte hvis problemet vedvarer.",
                       httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                       throwable = feil)
        }
    }

    fun genererManueltBrev(behandling: Behandling,
                           manueltBrevRequest: ManueltBrevRequest,
                           erForhåndsvisning: Boolean = false): ByteArray {
        if (environment.activeProfiles.contains("e2e") && behandling.skalBehandlesAutomatisk) return ByteArray(1)
        Result.runCatching {
            val mottaker =
                    persongrunnlagService.hentPersonPåBehandling(PersonIdent(manueltBrevRequest.mottakerIdent), behandling)
                    ?: error("Finner ikke mottaker på vedtaket")

            val (enhetNavn, målform) = hentEnhetNavnOgMålform(behandling)

            val brev: Brev = manueltBrevRequest.tilBrevmal(enhetNavn, mottaker)
            return brevKlient.genererBrev(målform = målform.tilSanityFormat(),
                                          brev = brev)
        }.fold(
                onSuccess = { it },
                onFailure = {
                    if (it is Feil) {
                        throw it
                    } else throw Feil(message = "Klarte ikke generere brev for ${manueltBrevRequest.brevmal.visningsTekst}. ${it.message}",
                                      frontendFeilmelding = "${if (erForhåndsvisning) "Det har skjedd en feil" else "Det har skjedd en feil, og brevet er ikke sendt"}. Prøv igjen, og ta kontakt med brukerstøtte hvis problemet vedvarer.",
                                      httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                                      throwable = it)
                }
        )
    }


    fun sendManueltBrev(behandling: Behandling,
                        manueltBrevRequest: ManueltBrevRequest) {

        val mottaker =
                persongrunnlagService.hentPersonPåBehandling(PersonIdent(manueltBrevRequest.mottakerIdent), behandling)
                ?: error("Finner ikke mottaker på behandlingen")

        val generertBrev = genererManueltBrev(behandling, manueltBrevRequest)

        val enhet = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandling.id).behandlendeEnhetId

        val førsteside = if (manueltBrevRequest.brevmal.genererForside) {
            Førsteside(språkkode = mottaker.målform.tilSpråkkode(),
                       navSkjemaId = "NAV 33.00-07",
                       overskriftstittel = "Ettersendelse til søknad om barnetrygd ordinær NAV 33-00.07")
        } else null

        val journalpostId = integrasjonClient.journalførManueltBrev(fnr = manueltBrevRequest.mottakerIdent,
                                                                    fagsakId = behandling.fagsak.id.toString(),
                                                                    journalførendeEnhet = enhet,
                                                                    brev = generertBrev,
                                                                    førsteside = førsteside,
                                                                    dokumenttype = manueltBrevRequest.brevmal.dokumenttype)

        journalføringRepository.save(
                DbJournalpost(
                        behandling = behandling,
                        journalpostId = journalpostId,
                        type = DbJournalpostType.U
                )
        )

        if (manueltBrevRequest.brevmal == INNHENTE_OPPLYSNINGER ||
            manueltBrevRequest.brevmal == VARSEL_OM_REVURDERING) {
            vilkårsvurderingService.opprettOglagreBlankAnnenVurdering(annenVurderingType = AnnenVurderingType.OPPLYSNINGSPLIKT,
                                                                      behandlingId = behandling.id)
        }

        DistribuerDokumentTask.opprettDistribuerDokumentTask(
                distribuerDokumentDTO = DistribuerDokumentDTO(
                        personIdent = behandling.fagsak.hentAktivIdent().ident,
                        behandlingId = behandling.id,
                        journalpostId = journalpostId,
                        brevType = manueltBrevRequest.brevmal.tilSanityBrevtype(),
                ),
                properties = Properties().apply {
                    this["fagsakIdent"] = behandling.fagsak.hentAktivIdent().ident
                    this["mottakerIdent"] = manueltBrevRequest.mottakerIdent
                    this["journalpostId"] = journalpostId
                    this["behandlingId"] = behandling.id.toString()
                }
        ).also {
            taskRepository.save(it)
        }
    }

    fun distribuerBrevOgLoggHendelse(
            journalpostId: String,
            behandlingId: Long,
            loggBehandlerRolle: BehandlerRolle,
            brevType: BrevType,
    ) {
        integrasjonClient.distribuerBrev(journalpostId)
        loggService.opprettDistribuertBrevLogg(behandlingId = behandlingId,
                                               tekst = brevType.visningsTekst.replaceFirstChar { it.uppercase() },
                                               rolle = loggBehandlerRolle)
        antallBrevSendt[brevType]?.increment()
    }

    private fun hentEnhetNavnOgMålform(behandling: Behandling): Pair<String, Målform> {
        return Pair(arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandling.id).behandlendeEnhetNavn,
                    persongrunnlagService.hentSøker(behandling.id)?.målform ?: Målform.NB)
    }

}
