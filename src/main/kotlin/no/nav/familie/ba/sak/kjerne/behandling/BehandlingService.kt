package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils.bestemKategori
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils.bestemUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils.utledLøpendeKategori
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils.utledLøpendeUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus.AVSLUTTET
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus.FATTER_VEDTAK
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.initStatus
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatUtils
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakPersonRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.steg.FØRSTE_STEG
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class BehandlingService(
    private val behandlingRepository: BehandlingRepository,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val behandlingMetrikker: BehandlingMetrikker,
    private val fagsakPersonRepository: FagsakPersonRepository,
    private val vedtakRepository: VedtakRepository,
    private val loggService: LoggService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,
    private val oppgaveService: OppgaveService,
    private val infotrygdService: InfotrygdService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val featureToggleService: FeatureToggleService
) {

    @Transactional
    fun opprettBehandling(nyBehandling: NyBehandling): Behandling {
        val fagsak = fagsakPersonRepository.finnFagsak(setOf(PersonIdent(nyBehandling.søkersIdent)))
            ?: throw FunksjonellFeil(
                melding = "Kan ikke lage behandling på person uten tilknyttet fagsak",
                frontendFeilmelding = "Kan ikke lage behandling på person uten tilknyttet fagsak"
            )

        val aktivBehandling = hentAktivForFagsak(fagsak.id)

        return if (aktivBehandling == null || aktivBehandling.status == AVSLUTTET) {

            val kategori = bestemKategori(
                behandlingÅrsak = nyBehandling.behandlingÅrsak,
                nyBehandlingKategori = nyBehandling.kategori,
                løpendeBehandlingKategori = hentLøpendeKategori(fagsakId = fagsak.id)
            )

            val underkategori = bestemUnderkategori(
                nyUnderkategori = nyBehandling.underkategori,
                nyBehandlingType = nyBehandling.behandlingType,
                løpendeUnderkategori = hentLøpendeUnderkategori(fagsakId = fagsak.id)
            )

            sjekkToggleOgThrowHvisBrudd(kategori, underkategori)

            val behandling = Behandling(
                fagsak = fagsak,
                opprettetÅrsak = nyBehandling.behandlingÅrsak,
                type = nyBehandling.behandlingType,
                kategori = kategori,
                underkategori = underkategori,
                skalBehandlesAutomatisk = nyBehandling.skalBehandlesAutomatisk
            )
                .initBehandlingStegTilstand()

            behandling.validerBehandling()

            val lagretBehandling = lagreNyOgDeaktiverGammelBehandling(behandling)
            opprettOgInitierNyttVedtakForBehandling(behandling = lagretBehandling)

            loggService.opprettBehandlingLogg(lagretBehandling)
            if (lagretBehandling.opprettBehandleSakOppgave()) {
                oppgaveService.opprettOppgave(
                    behandlingId = lagretBehandling.id,
                    oppgavetype = Oppgavetype.BehandleSak,
                    fristForFerdigstillelse = LocalDate.now(),
                    tilordnetNavIdent = nyBehandling.navIdent
                )
            }

            lagretBehandling
        } else if (aktivBehandling.steg < StegType.BESLUTTE_VEDTAK) {
            aktivBehandling.leggTilBehandlingStegTilstand(FØRSTE_STEG)
            aktivBehandling.status = initStatus()

            lagreEllerOppdater(aktivBehandling)
        } else {
            throw FunksjonellFeil(
                melding = "Kan ikke lage ny behandling. Fagsaken har en aktiv behandling som ikke er ferdigstilt.",
                frontendFeilmelding = "Kan ikke lage ny behandling. Fagsaken har en aktiv behandling som ikke er ferdigstilt."
            )
        }
    }

    fun oppdaterBehandlingstema(
        behandling: Behandling,
        nyKategori: BehandlingKategori,
        nyUnderkategori: BehandlingUnderkategori,
        manueltOppdatert: Boolean = false
    ): Behandling {

        val nyKategori = bestemKategori(
            behandlingÅrsak = behandling.opprettetÅrsak,
            nyBehandlingKategori = nyKategori,
            løpendeBehandlingKategori = hentLøpendeKategori(fagsakId = behandling.fagsak.id)
        )

        val nyUnderkategori: BehandlingUnderkategori =
            if (manueltOppdatert) nyUnderkategori
            else bestemUnderkategori(
                nyUnderkategori = nyUnderkategori,
                nyBehandlingType = behandling.type,
                løpendeUnderkategori = hentLøpendeUnderkategori(fagsakId = behandling.fagsak.id)
            )

        sjekkToggleOgThrowHvisBrudd(nyKategori, nyUnderkategori)

        val forrigeUnderkategori = behandling.underkategori
        val forrigeKategori = behandling.kategori
        val skalOppdatereKategori = nyKategori != forrigeKategori
        val skalOppdatereUnderkategori = nyUnderkategori != forrigeUnderkategori

        if (skalOppdatereKategori) {
            behandling.apply { kategori = nyKategori }
        }
        if (skalOppdatereUnderkategori) {
            behandling.apply { underkategori = nyUnderkategori }
        }

        return lagreEllerOppdater(behandling).also { lagretBehandling ->
            oppgaveService.patchOppgaverForBehandling(lagretBehandling) {
                if (it.behandlingstema != lagretBehandling.underkategori.tilBehandlingstema().value || it.behandlingstype != lagretBehandling.kategori.tilBehandlingstype().value) {
                    it.copy(
                        behandlingstema = lagretBehandling.underkategori.tilBehandlingstema().value,
                        behandlingstype = lagretBehandling.kategori.tilBehandlingstype().value
                    )
                } else null
            }

            if (manueltOppdatert && (skalOppdatereKategori || skalOppdatereUnderkategori)) {
                loggService.opprettEndretBehandlingstema(
                    behandling = lagretBehandling,
                    forrigeKategori = forrigeKategori,
                    forrigeUnderkategori = forrigeUnderkategori,
                    nyKategori = nyKategori,
                    nyUnderkategori = nyUnderkategori
                )
            }
        }
    }

    private fun sjekkToggleOgThrowHvisBrudd(
        kategori: BehandlingKategori,
        underkategori: BehandlingUnderkategori
    ) {
        if (kategori == BehandlingKategori.EØS && !featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS)) {
            throw FunksjonellFeil(
                melding = "EØS er ikke påskrudd",
                frontendFeilmelding = "Det er ikke støtte for å behandle EØS søknad."
            )
        }

        if (underkategori == BehandlingUnderkategori.UTVIDET && !featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_UTVIDET)) {
            throw FunksjonellFeil(
                melding = "Utvidet er ikke påskrudd",
                frontendFeilmelding = "Det er ikke støtte for å behandle utvidet søknad og du må fjerne tilknytningen til behandling."
            )
        }
    }

    @Transactional
    fun opprettOgInitierNyttVedtakForBehandling(
        behandling: Behandling,
        kopierVedtakBegrunnelser: Boolean = false,
        begrunnelseVilkårPekere: List<OriginalOgKopiertVilkårResultat> = emptyList()
    ) {
        behandling.steg.takeUnless { it !== StegType.BESLUTTE_VEDTAK && it !== StegType.REGISTRERE_PERSONGRUNNLAG }
            ?: throw error("Forsøker å initiere vedtak på steg ${behandling.steg}")

        val deaktivertVedtak =
            vedtakRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
                ?.let { vedtakRepository.saveAndFlush(it.also { it.aktiv = false }) }

        val nyttVedtak = Vedtak(
            behandling = behandling,
            vedtaksdato = if (behandling.skalBehandlesAutomatisk) LocalDateTime.now() else null
        )

        if (kopierVedtakBegrunnelser && deaktivertVedtak != null) {
            vedtaksperiodeService.kopierOverVedtaksperioder(
                deaktivertVedtak = deaktivertVedtak,
                aktivtVedtak = nyttVedtak
            )
        }

        vedtakRepository.save(nyttVedtak)
    }

    private fun sendTilDvh(behandling: Behandling) {
        saksstatistikkEventPublisher.publiserBehandlingsstatistikk(behandling.id)
    }

    fun hentAktivForFagsak(fagsakId: Long): Behandling? {
        return behandlingRepository.findByFagsakAndAktiv(fagsakId)
    }

    fun hentAktivOgÅpenForFagsak(fagsakId: Long): Behandling? {
        return behandlingRepository.findByFagsakAndAktivAndOpen(fagsakId)
    }

    fun hent(behandlingId: Long): Behandling {
        return behandlingRepository.finnBehandling(behandlingId)
    }

    fun hentSisteIverksatteBehandlingerFraLøpendeFagsaker(): List<Long> =
        behandlingRepository.finnSisteIverksatteBehandlingFraLøpendeFagsaker()

    fun hentBehandlinger(fagsakId: Long): List<Behandling> {
        return behandlingRepository.finnBehandlinger(fagsakId)
    }

    private fun hentIverksatteBehandlinger(fagsakId: Long): List<Behandling> {
        return behandlingRepository.finnIverksatteBehandlinger(fagsakId = fagsakId)
    }

    fun hentSisteBehandlingSomIkkeErHenlagt(fagsakId: Long): Behandling? {
        return behandlingRepository.finnBehandlinger(fagsakId)
            .filter { !it.erHenlagt() }
            .maxByOrNull { it.opprettetTidspunkt }
    }

    /**
     * Henter siste iverksatte behandling på fagsak
     */
    fun hentSisteBehandlingSomErIverksatt(fagsakId: Long): Behandling? {
        val iverksatteBehandlinger = hentIverksatteBehandlinger(fagsakId)
        return Behandlingutils.hentSisteBehandlingSomErIverksatt(iverksatteBehandlinger)
    }

    /**
     * Henter alle barn på behandlingen som har minst en periode med tilkjentytelse.
     */
    fun finnBarnFraBehandlingMedTilkjentYtsele(behandlingId: Long): List<String> =
        personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)?.barna?.map { it.personIdent.ident }
            ?.filter {
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(
                    behandlingId,
                    it
                )
                    .isNotEmpty()
            } ?: emptyList()

    /**
     * Henter siste iverksatte behandling FØR en gitt behandling
     */
    fun hentForrigeBehandlingSomErIverksatt(behandling: Behandling): Behandling? {
        val iverksatteBehandlinger = hentIverksatteBehandlinger(behandling.fagsak.id)
        return Behandlingutils.hentForrigeIverksatteBehandling(iverksatteBehandlinger, behandling)
    }

    fun lagreEllerOppdater(behandling: Behandling, sendTilDvh: Boolean = true): Behandling {
        return behandlingRepository.save(behandling).also {
            if (sendTilDvh) {
                sendTilDvh(it)
            }
        }
    }

    fun hentDagensFødselshendelser(): List<Behandling> {
        return behandlingRepository.finnFødselshendelserOpprettetIdag()
    }

    fun lagreNyOgDeaktiverGammelBehandling(behandling: Behandling): Behandling {
        val aktivBehandling = hentAktivForFagsak(behandling.fagsak.id)

        if (aktivBehandling != null) {
            behandlingRepository.saveAndFlush(aktivBehandling.also { it.aktiv = false })
            sendTilDvh(aktivBehandling)
        } else if (harAktivInfotrygdSak(behandling)) {
            throw FunksjonellFeil(
                "Kan ikke lage behandling på person med aktiv sak i Infotrygd",
                "Kan ikke lage behandling på person med aktiv sak i Infotrygd"
            )
        }

        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter behandling $behandling")
        return lagreEllerOppdater(behandling, false).also {
            arbeidsfordelingService.fastsettBehandlendeEnhet(it)
            sendTilDvh(it)
            if (it.versjon == 0L) {
                behandlingMetrikker.tellNøkkelTallVedOpprettelseAvBehandling(it)
            }
        }
    }

    private fun harAktivInfotrygdSak(behandling: Behandling): Boolean {
        val søkerIdenter = behandling.fagsak.søkerIdenter.map { it.personIdent.ident }
        return infotrygdService.harÅpenSakIInfotrygd(søkerIdenter) ||
            !behandling.erMigrering() && infotrygdService.harLøpendeSakIInfotrygd(søkerIdenter)
    }

    fun sendBehandlingTilBeslutter(behandling: Behandling) {
        oppdaterStatusPåBehandling(behandlingId = behandling.id, status = FATTER_VEDTAK)
    }

    fun oppdaterStatusPåBehandling(behandlingId: Long, status: BehandlingStatus): Behandling {
        val behandling = hent(behandlingId)
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} endrer status på behandling $behandlingId fra ${behandling.status} til $status")

        behandling.status = status
        return lagreEllerOppdater(behandling)
    }

    fun oppdaterResultatPåBehandling(behandlingId: Long, resultat: BehandlingResultat): Behandling {
        val behandling = hent(behandlingId)
        BehandlingsresultatUtils.validerBehandlingsresultat(behandling, resultat)

        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} endrer resultat på behandling $behandlingId fra ${behandling.resultat} til $resultat")
        loggService.opprettVilkårsvurderingLogg(
            behandling = behandling,
            forrigeBehandlingResultat = behandling.resultat,
            nyttBehandlingResultat = resultat
        )

        behandling.resultat = resultat
        return lagreEllerOppdater(behandling)
    }

    fun leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
        behandlingId: Long,
        steg: StegType
    ): Behandling {
        val behandling = hent(behandlingId)
        behandling.leggTilBehandlingStegTilstand(steg)

        return lagreEllerOppdater(behandling)
    }

    fun hentForrigeAndeler(fagsakId: Long): List<AndelTilkjentYtelse>? {
        val forrigeIverksattBehandling = hentSisteBehandlingSomErIverksatt(fagsakId = fagsakId) ?: return null
        return andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = forrigeIverksattBehandling.id)
    }

    fun hentLøpendeKategori(fagsakId: Long): BehandlingKategori? {
        val forrigeAndeler = hentForrigeAndeler(fagsakId)
        return if (forrigeAndeler != null) utledLøpendeKategori(forrigeAndeler) else null
    }

    fun hentLøpendeUnderkategori(fagsakId: Long): BehandlingUnderkategori? {
        val forrigeAndeler = hentForrigeAndeler(fagsakId)
        return if (forrigeAndeler != null) utledLøpendeUnderkategori(forrigeAndeler) else null
    }

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(BehandlingService::class.java)
    }
}

typealias OriginalOgKopiertVilkårResultat = Pair<VilkårResultat?, VilkårResultat?>
