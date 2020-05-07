package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadDTO
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.dokument.DokumentService
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.Ressurs
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class VedtakService(private val behandlingService: BehandlingService,
                    private val behandlingRepository: BehandlingRepository,
                    private val behandlingResultatService: BehandlingResultatService,
                    private val søknadGrunnlagService: SøknadGrunnlagService,
                    private val loggService: LoggService,
                    private val vedtakRepository: VedtakRepository,
                    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
                    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
                    private val dokumentService: DokumentService,
                    private val fagsakService: FagsakService) {

    @Transactional
    fun opphørVedtak(saksbehandler: String,
                     gjeldendeBehandlingsId: Long,
                     nyBehandlingType: BehandlingType,
                     opphørsdato: LocalDate,
                     postProsessor: (Vedtak) -> Unit): Ressurs<Vedtak> {

        val gjeldendeVedtak = vedtakRepository.findByBehandlingAndAktiv(gjeldendeBehandlingsId)
                              ?: return Ressurs.failure("Fant ikke aktivt vedtak tilknyttet behandling $gjeldendeBehandlingsId")

        val gjeldendeAndelerTilkjentYtelse =
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(gjeldendeBehandlingsId)
        if (gjeldendeAndelerTilkjentYtelse.isEmpty()) {
            return Ressurs.failure(
                    "Fant ikke andeler tilkjent ytelse tilknyttet behandling $gjeldendeBehandlingsId")
        }

        val gjeldendeBehandling = gjeldendeVedtak.behandling
        if (!gjeldendeBehandling.aktiv) {
            return Ressurs.failure("Aktivt vedtak er tilknyttet behandling $gjeldendeBehandlingsId som IKKE er aktivt")
        }

        val nyBehandling = Behandling(fagsak = gjeldendeBehandling.fagsak,
                                      journalpostID = null,
                                      type = nyBehandlingType,
                                      kategori = gjeldendeBehandling.kategori,
                                      underkategori = gjeldendeBehandling.underkategori)

        // Må flushe denne til databasen for å sørge å opprettholde unikhet på (fagsakid,aktiv)
        behandlingRepository.saveAndFlush(gjeldendeBehandling.also { it.aktiv = false })
        behandlingRepository.save(nyBehandling)
        loggService.opprettBehandlingLogg(nyBehandling)

        val nyttVedtak = Vedtak(
                ansvarligSaksbehandler = saksbehandler,
                behandling = nyBehandling,
                vedtaksdato = LocalDate.now(),
                forrigeVedtakId = gjeldendeVedtak.id,
                opphørsdato = opphørsdato
        )

        // Trenger ikke flush her fordi det kreves unikhet på (behandlingid,aktiv) og det er ny behandlingsid
        vedtakRepository.save(gjeldendeVedtak.also { it.aktiv = false })
        vedtakRepository.save(nyttVedtak)

        val nyTilkjentYtelse = TilkjentYtelse(
                behandling = nyBehandling,
                opprettetDato = LocalDate.now(),
                endretDato = LocalDate.now()
        )
        tilkjentYtelseRepository.save(nyTilkjentYtelse)
        behandlingRepository.save(nyBehandling.also { it.steg = StegType.FERDIGSTILLE_BEHANDLING })

        postProsessor(nyttVedtak)

        return Ressurs.success(nyttVedtak)
    }

    @Transactional
    fun lagreEllerOppdaterVedtakForAktivBehandling(behandling: Behandling,
                                                   personopplysningGrunnlag: PersonopplysningGrunnlag,
                                                   ansvarligSaksbehandler: String): Vedtak {
        val forrigeVedtak = hentForrigeVedtak(behandling = behandling)
        val behandlingResultatType = behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandling.id)

        val vedtak = Vedtak(
                behandling = behandling,
                ansvarligSaksbehandler = ansvarligSaksbehandler,
                vedtaksdato = LocalDate.now(),
                forrigeVedtakId = forrigeVedtak?.id,
                opphørsdato = if (behandlingResultatType == BehandlingResultatType.OPPHØRT) LocalDate.now()
                        .førsteDagINesteMåned() else null
        )

        vedtak.stønadBrevMarkdown =
                if (behandlingResultatType != BehandlingResultatType.INNVILGET) hentVedtaksbrevMarkdown(vedtak) else ""

        return lagreOgDeaktiverGammel(vedtak)
    }


    @Transactional
    fun oppdaterVedtakMedStønadsbrev(vedtak: Vedtak): Ressurs<RestFagsak> {
        vedtak.stønadBrevMarkdown = hentVedtaksbrevMarkdown(vedtak)

        lagreOgDeaktiverGammel(vedtak)

        return fagsakService.hentRestFagsak(vedtak.behandling.fagsak.id)
    }

    fun hentVedtaksbrevMarkdown(vedtak: Vedtak): String {
        val behandlingResultatType = behandlingResultatService.hentBehandlingResultatTypeFraBehandling(vedtak.behandling.id)

        return Result.runCatching {
            val søknad: SøknadDTO? = søknadGrunnlagService.hentAktiv(vedtak.behandling.id)?.hentSøknadDto()

            dokumentService.hentStønadBrevMarkdown(
                    vedtak = vedtak,
                    behandlingResultatType = behandlingResultatType,
                    søknad = søknad
            )
        }
                .fold(
                        onSuccess = { it },
                        onFailure = { e ->
                            secureLogger.info("Klart ikke å oppdatere vedtak med vedtaksbrev: ${e.message}", e)
                            error("Klart ikke å oppdatere vedtak med vedtaksbrev: ${e.message}")
                        }
                )
    }

    fun hentForrigeVedtak(behandling: Behandling): Vedtak? {
        val behandlinger = behandlingService.hentBehandlinger(behandling.fagsak.id)


        return when (val forrigeBehandling = behandlinger.filter { it.id != behandling.id }.maxBy { it.opprettetTidspunkt }) {
            null -> null
            else -> hentAktivForBehandling(behandlingId = forrigeBehandling.id)
        }
    }

    fun hent(vedtakId: Long): Vedtak {
        return vedtakRepository.getOne(vedtakId)
    }

    fun hentAktivForBehandling(behandlingId: Long): Vedtak? {
        return vedtakRepository.findByBehandlingAndAktiv(behandlingId)
    }

    fun lagreOgDeaktiverGammel(vedtak: Vedtak): Vedtak {
        val aktivVedtak = hentAktivForBehandling(vedtak.behandling.id)

        if (aktivVedtak != null && aktivVedtak.id != vedtak.id) {
            vedtakRepository.saveAndFlush(aktivVedtak.also { it.aktiv = false })
        }

        LOG.info("${SikkerhetContext.hentSaksbehandler()} oppretter vedtak $vedtak")
        return vedtakRepository.save(vedtak)
    }

    fun lagreEllerOppdater(vedtak: Vedtak): Vedtak {
        return vedtakRepository.save(vedtak)
    }

    fun godkjennVedtak(vedtak: Vedtak) {
        vedtak.ansvarligBeslutter = SikkerhetContext.hentSaksbehandlerNavn()
        vedtak.vedtaksdato = LocalDate.now()
        oppdaterVedtakMedStønadsbrev(vedtak)

        LOG.info("${SikkerhetContext.hentSaksbehandler()} godkjenner vedtak $vedtak")
        lagreEllerOppdater(vedtak)
    }

    companion object {
        val LOG = LoggerFactory.getLogger(this::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}


