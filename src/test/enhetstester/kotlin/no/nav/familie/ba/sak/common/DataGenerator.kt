package no.nav.familie.ba.sak.common

import lagSøknadDTO
import leggTilBegrunnelsePåVedtaksperiodeIBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.RestInstitusjon
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerDb
import no.nav.familie.ba.sak.kjerne.brev.mottaker.MottakerType
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StatusFraOppdragMedTask
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.steg.domene.JournalførVedtaksbrevDTO
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.StatusFraOppdragTask
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.prosessering.domene.Task
import randomFnr
import vurderVilkårsvurderingTilInnvilget
import java.time.LocalDate
import java.time.YearMonth
import java.util.Properties

fun dato(s: String) = LocalDate.parse(s)

fun årMnd(s: String) = YearMonth.parse(s)

/**
 * Dette er en funksjon for å få en førstegangsbehandling til en ønsket tilstand ved test.
 * Man sender inn steg man ønsker å komme til (tilSteg), personer på behandlingen (søkerFnr og barnasIdenter),
 * og serviceinstanser som brukes i testen.
 */
fun kjørStegprosessForFGB(
    tilSteg: StegType,
    barnasIdenter: List<String>,
    søkerFnr: String = randomFnr(),
    fagsakService: FagsakService,
    vedtakService: VedtakService,
    persongrunnlagService: PersongrunnlagService,
    vilkårsvurderingService: VilkårsvurderingService,
    stegService: StegService,
    vedtaksperiodeService: VedtaksperiodeService,
    behandlingUnderkategori: BehandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
    institusjon: RestInstitusjon? = null,
    brevmalService: BrevmalService,
    behandlingKategori: BehandlingKategori = BehandlingKategori.NASJONAL,
    vilkårInnvilgetFom: LocalDate? = null,
): Behandling {
    val fagsakType = utledFagsaktype(institusjon)
    val fagsak =
        fagsakService.hentEllerOpprettFagsakForPersonIdent(
            fødselsnummer = søkerFnr,
            institusjon = institusjon,
            fagsakType = fagsakType,
        )
    val behandling =
        stegService.håndterNyBehandling(
            NyBehandling(
                kategori = behandlingKategori,
                underkategori = behandlingUnderkategori,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                behandlingÅrsak = BehandlingÅrsak.SØKNAD,
                søkersIdent = søkerFnr,
                barnasIdenter = barnasIdenter,
                søknadMottattDato = LocalDate.now(),
                fagsakId = fagsak.id,
            ),
        )

    val behandlingEtterPersongrunnlagSteg =
        stegService.håndterSøknad(
            behandling = behandling,
            restRegistrerSøknad =
                RestRegistrerSøknad(
                    søknad =
                        lagSøknadDTO(
                            søkerIdent = søkerFnr,
                            barnasIdenter = barnasIdenter,
                            underkategori = behandlingUnderkategori,
                        ),
                    bekreftEndringerViaFrontend = true,
                ),
        )

    if (tilSteg == StegType.REGISTRERE_PERSONGRUNNLAG || tilSteg == StegType.REGISTRERE_SØKNAD) {
        return behandlingEtterPersongrunnlagSteg
    }

    val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)!!
    persongrunnlagService.hentAktivThrows(behandlingId = behandling.id).personer.forEach { barn ->
        vurderVilkårsvurderingTilInnvilget(vilkårsvurdering, barn, vilkårInnvilgetFom)
    }
    vilkårsvurderingService.oppdater(vilkårsvurdering)

    val behandlingEtterVilkårsvurderingSteg = stegService.håndterVilkårsvurdering(behandlingEtterPersongrunnlagSteg)

    if (tilSteg == StegType.VILKÅRSVURDERING) return behandlingEtterVilkårsvurderingSteg

    val behandlingEtterBehandlingsresultat = stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurderingSteg)

    if (tilSteg == StegType.BEHANDLINGSRESULTAT) return behandlingEtterBehandlingsresultat

    val behandlingEtterVurderTilbakekrevingSteg =
        stegService.håndterVurderTilbakekreving(
            behandlingEtterBehandlingsresultat,
            RestTilbakekreving(
                valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                begrunnelse = "Begrunnelse",
            ),
        )

    leggTilBegrunnelsePåVedtaksperiodeIBehandling(
        behandling = behandlingEtterVurderTilbakekrevingSteg,
        vedtakService = vedtakService,
        vedtaksperiodeService = vedtaksperiodeService,
    )

    if (tilSteg == StegType.VURDER_TILBAKEKREVING) return behandlingEtterVurderTilbakekrevingSteg

    val behandlingEtterSendTilBeslutter =
        stegService.håndterSendTilBeslutter(behandlingEtterVurderTilbakekrevingSteg, "1234")
    if (tilSteg == StegType.SEND_TIL_BESLUTTER) return behandlingEtterSendTilBeslutter

    val behandlingEtterBeslutteVedtak =
        stegService.håndterBeslutningForVedtak(
            behandlingEtterSendTilBeslutter,
            RestBeslutningPåVedtak(beslutning = Beslutning.GODKJENT),
        )
    if (tilSteg == StegType.BESLUTTE_VEDTAK) return behandlingEtterBeslutteVedtak

    val vedtak = vedtakService.hentAktivForBehandling(behandlingEtterBeslutteVedtak.id)
    val behandlingEtterIverksetteVedtak =
        stegService.håndterIverksettMotØkonomi(
            behandlingEtterBeslutteVedtak,
            IverksettingTaskDTO(
                behandlingsId = behandlingEtterBeslutteVedtak.id,
                vedtaksId = vedtak!!.id,
                saksbehandlerId = "System",
                personIdent = behandlingEtterBeslutteVedtak.fagsak.aktør.aktivFødselsnummer(),
            ),
        )
    if (tilSteg == StegType.IVERKSETT_MOT_OPPDRAG) return behandlingEtterIverksetteVedtak

    val behandlingEtterStatusFraOppdrag =
        stegService.håndterStatusFraØkonomi(
            behandlingEtterIverksetteVedtak,
            StatusFraOppdragMedTask(
                statusFraOppdragDTO =
                    StatusFraOppdragDTO(
                        fagsystem = FAGSYSTEM,
                        personIdent = søkerFnr,
                        aktørId = behandlingEtterIverksetteVedtak.fagsak.aktør.aktivFødselsnummer(),
                        behandlingsId = behandlingEtterIverksetteVedtak.id,
                        vedtaksId = vedtak.id,
                    ),
                task = Task(type = StatusFraOppdragTask.TASK_STEP_TYPE, payload = ""),
            ),
        )
    if (tilSteg == StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI) return behandlingEtterStatusFraOppdrag

    val behandlingEtterIverksetteMotTilbake =
        stegService.håndterIverksettMotFamilieTilbake(behandlingEtterStatusFraOppdrag, Properties())
    if (tilSteg == StegType.IVERKSETT_MOT_FAMILIE_TILBAKE) return behandlingEtterIverksetteMotTilbake

    val behandlingEtterJournalførtVedtak =
        stegService.håndterJournalførVedtaksbrev(
            behandlingEtterIverksetteMotTilbake,
            JournalførVedtaksbrevDTO(
                vedtakId = vedtak.id,
                task = Task(type = JournalførVedtaksbrevTask.TASK_STEP_TYPE, payload = ""),
            ),
        )
    if (tilSteg == StegType.JOURNALFØR_VEDTAKSBREV) return behandlingEtterJournalførtVedtak

    val behandlingEtterDistribuertVedtak =
        stegService.håndterDistribuerVedtaksbrev(
            behandlingEtterJournalførtVedtak,
            DistribuerDokumentDTO(
                behandlingId = behandlingEtterJournalførtVedtak.id,
                journalpostId = "1234",
                fagsakId = behandlingEtterJournalførtVedtak.fagsak.id,
                brevmal =
                    brevmalService.hentBrevmal(
                        behandlingEtterJournalførtVedtak,
                    ),
                erManueltSendt = false,
            ),
        )
    if (tilSteg == StegType.DISTRIBUER_VEDTAKSBREV) return behandlingEtterDistribuertVedtak

    return stegService.håndterFerdigstillBehandling(behandlingEtterDistribuertVedtak)
}

private fun utledFagsaktype(
    institusjon: RestInstitusjon?,
): FagsakType =
    if (institusjon != null) {
        FagsakType.INSTITUSJON
    } else {
        FagsakType.NORMAL
    }

/**
 * Dette er en funksjon for å få en førstegangsbehandling til en ønsket tilstand ved test.
 * Man sender inn steg man ønsker å komme til (tilSteg), personer på behandlingen (søkerFnr og barnasIdenter),
 * og serviceinstanser som brukes i testen.
 */
fun kjørStegprosessForRevurderingÅrligKontroll(
    tilSteg: StegType,
    søkerFnr: String,
    barnasIdenter: List<String>,
    vedtakService: VedtakService,
    stegService: StegService,
    fagsakId: Long,
    brevmalService: BrevmalService,
    vedtaksperiodeService: VedtaksperiodeService,
): Behandling {
    val behandling =
        stegService.håndterNyBehandling(
            NyBehandling(
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.ÅRLIG_KONTROLL,
                søkersIdent = søkerFnr,
                barnasIdenter = barnasIdenter,
                fagsakId = fagsakId,
            ),
        )

    val behandlingEtterVilkårsvurderingSteg = stegService.håndterVilkårsvurdering(behandling)

    if (tilSteg == StegType.VILKÅRSVURDERING) return behandlingEtterVilkårsvurderingSteg

    val behandlingEtterBehandlingsresultat = stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurderingSteg)

    if (tilSteg == StegType.BEHANDLINGSRESULTAT) return behandlingEtterBehandlingsresultat

    val behandlingEtterSimuleringSteg =
        stegService.håndterVurderTilbakekreving(
            behandlingEtterBehandlingsresultat,
            if (behandlingEtterBehandlingsresultat.resultat != Behandlingsresultat.FORTSATT_INNVILGET) {
                RestTilbakekreving(
                    valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                    begrunnelse = "Begrunnelse",
                )
            } else {
                null
            },
        )
    if (tilSteg == StegType.VURDER_TILBAKEKREVING) return behandlingEtterSimuleringSteg

    leggTilBegrunnelsePåVedtaksperiodeIBehandling(
        behandling = behandlingEtterSimuleringSteg,
        vedtakService = vedtakService,
        vedtaksperiodeService = vedtaksperiodeService,
    )

    val behandlingEtterSendTilBeslutter = stegService.håndterSendTilBeslutter(behandlingEtterSimuleringSteg, "1234")
    if (tilSteg == StegType.SEND_TIL_BESLUTTER) return behandlingEtterSendTilBeslutter

    val behandlingEtterBeslutteVedtak =
        stegService.håndterBeslutningForVedtak(
            behandlingEtterSendTilBeslutter,
            RestBeslutningPåVedtak(beslutning = Beslutning.GODKJENT),
        )
    if (tilSteg == StegType.BESLUTTE_VEDTAK) return behandlingEtterBeslutteVedtak

    val vedtak = vedtakService.hentAktivForBehandling(behandlingEtterBeslutteVedtak.id)
    val behandlingEtterIverksetteVedtak =
        stegService.håndterIverksettMotØkonomi(
            behandlingEtterBeslutteVedtak,
            IverksettingTaskDTO(
                behandlingsId = behandlingEtterBeslutteVedtak.id,
                vedtaksId = vedtak!!.id,
                saksbehandlerId = "System",
                personIdent = behandlingEtterBeslutteVedtak.fagsak.aktør.aktivFødselsnummer(),
            ),
        )
    if (tilSteg == StegType.IVERKSETT_MOT_OPPDRAG) return behandlingEtterIverksetteVedtak

    val behandlingEtterStatusFraOppdrag =
        stegService.håndterStatusFraØkonomi(
            behandlingEtterIverksetteVedtak,
            StatusFraOppdragMedTask(
                statusFraOppdragDTO =
                    StatusFraOppdragDTO(
                        fagsystem = FAGSYSTEM,
                        personIdent = søkerFnr,
                        aktørId = behandlingEtterIverksetteVedtak.fagsak.aktør.aktørId,
                        behandlingsId = behandlingEtterIverksetteVedtak.id,
                        vedtaksId = vedtak.id,
                    ),
                task = Task(type = StatusFraOppdragTask.TASK_STEP_TYPE, payload = ""),
            ),
        )
    if (tilSteg == StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI) return behandlingEtterStatusFraOppdrag

    val behandlingEtterIverksetteMotTilbake =
        stegService.håndterIverksettMotFamilieTilbake(behandlingEtterStatusFraOppdrag, Properties())
    if (tilSteg == StegType.IVERKSETT_MOT_FAMILIE_TILBAKE) return behandlingEtterIverksetteMotTilbake

    val behandlingEtterJournalførtVedtak =
        stegService.håndterJournalførVedtaksbrev(
            behandlingEtterIverksetteMotTilbake,
            JournalførVedtaksbrevDTO(
                vedtakId = vedtak.id,
                task = Task(type = JournalførVedtaksbrevTask.TASK_STEP_TYPE, payload = ""),
            ),
        )
    if (tilSteg == StegType.JOURNALFØR_VEDTAKSBREV) return behandlingEtterJournalførtVedtak

    val behandlingEtterDistribuertVedtak =
        stegService.håndterDistribuerVedtaksbrev(
            behandlingEtterJournalførtVedtak,
            DistribuerDokumentDTO(
                behandlingId = behandling.id,
                journalpostId = "1234",
                fagsakId = behandling.fagsak.id,
                brevmal = brevmalService.hentBrevmal(behandling),
                erManueltSendt = false,
            ),
        )
    if (tilSteg == StegType.DISTRIBUER_VEDTAKSBREV) return behandlingEtterDistribuertVedtak

    return stegService.håndterFerdigstillBehandling(behandlingEtterDistribuertVedtak)
}

fun lagBrevmottakerDb(
    behandlingId: Long,
    type: MottakerType = MottakerType.FULLMEKTIG,
    navn: String = "Test Testesen",
    adresselinje1: String = "En adresse her",
    adresselinje2: String? = null,
    postnummer: String = "0661",
    poststed: String = "Oslo",
    landkode: String = "NO",
) = BrevmottakerDb(
    behandlingId = behandlingId,
    type = type,
    navn = navn,
    adresselinje1 = adresselinje1,
    adresselinje2 = adresselinje2,
    postnummer = postnummer,
    poststed = poststed,
    landkode = landkode,
)

val Number.årSiden: LocalDate get() = LocalDate.now().minusYears(this.toLong())
