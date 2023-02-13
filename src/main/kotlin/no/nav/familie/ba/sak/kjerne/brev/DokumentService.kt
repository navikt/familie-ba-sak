package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.DEFAULT_JOURNALFØRENDE_ENHET
import no.nav.familie.ba.sak.integrasjoner.journalføring.UtgåendeJournalføringService
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.DbJournalpost
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.DbJournalpostType
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.JournalføringRepository
import no.nav.familie.ba.sak.integrasjoner.organisasjon.OrganisasjonService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.domene.erTilInstitusjon
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.leggTilBlankAnnenVurdering
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.task.DistribuerDokumentTask
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Førsteside
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.Properties

@Service
class DokumentService(
    private val journalføringRepository: JournalføringRepository,
    private val taskRepository: TaskRepositoryWrapper,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val vilkårsvurderingForNyBehandlingService: VilkårsvurderingForNyBehandlingService,
    private val rolleConfig: RolleConfig,
    private val settPåVentService: SettPåVentService,
    private val utgåendeJournalføringService: UtgåendeJournalføringService,
    private val fagsakRepository: FagsakRepository,
    private val organisasjonService: OrganisasjonService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val dokumentGenereringService: DokumentGenereringService
) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun hentBrevForVedtak(vedtak: Vedtak): Ressurs<ByteArray> {
        if (SikkerhetContext.hentHøyesteRolletilgangForInnloggetBruker(rolleConfig) == BehandlerRolle.VEILEDER && vedtak.stønadBrevPdF == null) {
            throw FunksjonellFeil("Det finnes ikke noe vedtaksbrev.")
        } else {
            val pdf =
                vedtak.stønadBrevPdF ?: throw Feil("Klarte ikke finne vedtaksbrevbrev for vedtak med id ${vedtak.id}")
            return Ressurs.success(pdf)
        }
    }

    @Transactional
    fun sendManueltBrev(
        manueltBrevRequest: ManueltBrevRequest,
        behandling: Behandling? = null,
        fagsakId: Long
    ) {
        val generertBrev = dokumentGenereringService.genererManueltBrev(manueltBrevRequest)

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

        if (behandling != null && manueltBrevRequest.brevmal.førerTilOpplysningsplikt()) {
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
                            manuellFrist = manueltBrevRequest.antallUkerSvarfrist?.let { it * 7 }?.toLong(),
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

    companion object {
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
