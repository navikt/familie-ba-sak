package no.nav.familie.ba.sak.kjørbehandling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagSøknadDTO
import no.nav.familie.ba.sak.datagenerator.leggTilBegrunnelsePåVedtaksperiodeIBehandling
import no.nav.familie.ba.sak.datagenerator.vurderVilkårsvurderingTilInnvilget
import no.nav.familie.ba.sak.ekstern.restDomene.InstitusjonDto
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.TilbakekrevingDto
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
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
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.hentGyldigeBegrunnelserPerPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.StatusFraOppdragTask
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.prosessering.domene.Task
import java.time.LocalDate
import java.util.Properties

fun kjørStegprosessForBehandling(
    tilSteg: StegType = StegType.BEHANDLING_AVSLUTTET,
    søkerFnr: String,
    barnasIdenter: List<String>,
    vedtakService: VedtakService,
    underkategori: BehandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
    behandlingÅrsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
    overstyrendeVilkårsvurdering: Vilkårsvurdering,
    behandlingstype: BehandlingType,
    vilkårsvurderingService: VilkårsvurderingService,
    stegService: StegService,
    vedtaksperiodeService: VedtaksperiodeService,
    fagsakService: FagsakService,
    brevmalService: BrevmalService,
): Behandling {
    val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)

    val nyBehandling =
        NyBehandling(
            kategori = BehandlingKategori.NASJONAL,
            underkategori = underkategori,
            søkersIdent = søkerFnr,
            behandlingType = behandlingstype,
            behandlingÅrsak = behandlingÅrsak,
            barnasIdenter = barnasIdenter,
            søknadMottattDato = if (behandlingÅrsak == BehandlingÅrsak.SØKNAD) LocalDate.now().minusYears(18) else null,
            fagsakId = fagsak.id,
        )

    val behandling = stegService.håndterNyBehandling(nyBehandling)

    val behandlingEtterPersongrunnlagSteg =
        if (behandlingÅrsak == BehandlingÅrsak.SØKNAD || behandlingÅrsak == BehandlingÅrsak.FØDSELSHENDELSE) {
            håndterSøknadSteg(stegService, behandling, søkerFnr, barnasIdenter, underkategori)
        } else {
            behandling
        }

    if (tilSteg == StegType.REGISTRERE_PERSONGRUNNLAG || tilSteg == StegType.REGISTRERE_SØKNAD) {
        return behandlingEtterPersongrunnlagSteg
    }

    val behandlingEtterVilkårsvurderingSteg =
        håndterVilkårsvurderingSteg(
            vilkårsvurderingService = vilkårsvurderingService,
            behandling = behandlingEtterPersongrunnlagSteg,
            nyVilkårsvurdering = overstyrendeVilkårsvurdering,
            stegService = stegService,
        )
    if (tilSteg == StegType.VILKÅRSVURDERING) return behandlingEtterVilkårsvurderingSteg

    val behandlingEtterBehandlingsresultat = stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurderingSteg)
    if (tilSteg == StegType.BEHANDLINGSRESULTAT) return behandlingEtterBehandlingsresultat

    val behandlingEtterSimuleringSteg = hånderSilmuleringssteg(stegService, behandlingEtterBehandlingsresultat)
    if (tilSteg == StegType.VURDER_TILBAKEKREVING) return behandlingEtterSimuleringSteg

    val behandlingEtterSendTilBeslutter =
        håndterSendtTilBeslutterSteg(
            behandlingEtterSimuleringSteg = behandlingEtterSimuleringSteg,
            vedtakService = vedtakService,
            vedtaksperiodeService = vedtaksperiodeService,
            stegService = stegService,
        )
    if (tilSteg == StegType.SEND_TIL_BESLUTTER) return behandlingEtterSendTilBeslutter

    val behandlingEtterBeslutteVedtak =
        håndterBeslutteVedtakSteg(stegService, behandlingEtterSendTilBeslutter)
    if (tilSteg == StegType.BESLUTTE_VEDTAK) return behandlingEtterBeslutteVedtak

    val behandlingEtterIverksetteVedtak =
        håndterIverksetteVedtakSteg(stegService, behandlingEtterBeslutteVedtak, vedtakService)
    if (tilSteg == StegType.IVERKSETT_MOT_OPPDRAG) return behandlingEtterIverksetteVedtak

    val behandlingEtterStatusFraOppdrag =
        håndterStatusFraOppdragSteg(stegService, behandlingEtterIverksetteVedtak, søkerFnr, vedtakService)
    if (tilSteg == StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI) return behandlingEtterStatusFraOppdrag

    val behandlingEtterIverksetteMotTilbake =
        stegService.håndterIverksettMotFamilieTilbake(behandlingEtterStatusFraOppdrag, Properties())
    if (tilSteg == StegType.IVERKSETT_MOT_FAMILIE_TILBAKE) return behandlingEtterIverksetteMotTilbake

    val behandlingEtterJournalførtVedtak =
        håndterJournalførtVedtakSteg(stegService, behandlingEtterIverksetteMotTilbake, vedtakService)
    if (tilSteg == StegType.JOURNALFØR_VEDTAKSBREV) return behandlingEtterJournalførtVedtak

    val behandlingEtterDistribuertVedtak =
        håndterDistribuertVedtakSteg(stegService, behandlingEtterJournalførtVedtak, brevmalService)
    if (tilSteg == StegType.DISTRIBUER_VEDTAKSBREV) return behandlingEtterDistribuertVedtak

    return stegService.håndterFerdigstillBehandling(behandlingEtterDistribuertVedtak)
}

private fun håndterSøknadSteg(
    stegService: StegService,
    behandling: Behandling,
    søkerFnr: String,
    barnasIdenter: List<String>,
    behandlingUnderkategori: BehandlingUnderkategori,
) = stegService.håndterSøknad(
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

private fun håndterSendtTilBeslutterSteg(
    behandlingEtterSimuleringSteg: Behandling,
    vedtakService: VedtakService,
    vedtaksperiodeService: VedtaksperiodeService,
    stegService: StegService,
): Behandling {
    leggTilAlleGyldigeBegrunnelserPåVedtaksperiodeIBehandling(
        behandling = behandlingEtterSimuleringSteg,
        vedtakService = vedtakService,
        vedtaksperiodeService = vedtaksperiodeService,
    )
    val behandlingEtterSendTilBeslutter = stegService.håndterSendTilBeslutter(behandlingEtterSimuleringSteg, "1234")
    return behandlingEtterSendTilBeslutter
}

private fun håndterDistribuertVedtakSteg(
    stegService: StegService,
    behandling: Behandling,
    brevmalService: BrevmalService,
): Behandling {
    val behandlingEtterDistribuertVedtak =
        stegService.håndterDistribuerVedtaksbrev(
            behandling,
            DistribuerDokumentDTO(
                behandlingId = behandling.id,
                journalpostId = "1234",
                fagsakId = behandling.fagsak.id,
                brevmal = brevmalService.hentBrevmal(behandling),
                erManueltSendt = false,
            ),
        )
    return behandlingEtterDistribuertVedtak
}

private fun håndterJournalførtVedtakSteg(
    stegService: StegService,
    behandlingEtterIverksetteMotTilbake: Behandling,
    vedtakService: VedtakService,
): Behandling {
    val vedtak = vedtakService.hentAktivForBehandling(behandlingEtterIverksetteMotTilbake.id)
    return stegService.håndterJournalførVedtaksbrev(
        behandlingEtterIverksetteMotTilbake,
        JournalførVedtaksbrevDTO(
            vedtakId = vedtak!!.id,
            task = Task(type = JournalførVedtaksbrevTask.TASK_STEP_TYPE, payload = ""),
        ),
    )
}

private fun håndterStatusFraOppdragSteg(
    stegService: StegService,
    behandlingEtterIverksetteVedtak: Behandling,
    søkerFnr: String,
    vedtakService: VedtakService,
): Behandling {
    val vedtak = vedtakService.hentAktivForBehandling(behandlingEtterIverksetteVedtak.id)
    return stegService.håndterStatusFraØkonomi(
        behandlingEtterIverksetteVedtak,
        StatusFraOppdragMedTask(
            statusFraOppdragDTO =
                StatusFraOppdragDTO(
                    fagsystem = FAGSYSTEM,
                    personIdent = søkerFnr,
                    behandlingsId = behandlingEtterIverksetteVedtak.id,
                    vedtaksId = vedtak!!.id,
                ),
            task = Task(type = StatusFraOppdragTask.TASK_STEP_TYPE, payload = ""),
        ),
    )
}

private fun håndterIverksetteVedtakSteg(
    stegService: StegService,
    behandlingEtterBeslutteVedtak: Behandling,
    vedtakService: VedtakService,
): Behandling {
    val vedtak = vedtakService.hentAktivForBehandling(behandlingEtterBeslutteVedtak.id)
    return stegService.håndterIverksettMotØkonomi(
        behandlingEtterBeslutteVedtak,
        IverksettingTaskDTO(
            behandlingsId = behandlingEtterBeslutteVedtak.id,
            vedtaksId = vedtak!!.id,
            saksbehandlerId = "System",
            personIdent = behandlingEtterBeslutteVedtak.fagsak.aktør.aktivFødselsnummer(),
        ),
    )
}

private fun håndterBeslutteVedtakSteg(
    stegService: StegService,
    behandlingEtterSendTilBeslutter: Behandling,
): Behandling {
    val behandlingEtterBeslutteVedtak =
        stegService.håndterBeslutningForVedtak(
            behandlingEtterSendTilBeslutter,
            RestBeslutningPåVedtak(beslutning = Beslutning.GODKJENT),
        )
    return behandlingEtterBeslutteVedtak
}

private fun hånderSilmuleringssteg(
    stegService: StegService,
    behandlingEtterBehandlingsresultat: Behandling,
): Behandling =
    stegService.håndterVurderTilbakekreving(
        behandlingEtterBehandlingsresultat,
        if (behandlingEtterBehandlingsresultat.resultat != Behandlingsresultat.FORTSATT_INNVILGET) {
            TilbakekrevingDto(
                valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                begrunnelse = "Begrunnelse",
            )
        } else {
            null
        },
    )

private fun håndterVilkårsvurderingSteg(
    vilkårsvurderingService: VilkårsvurderingService,
    behandling: Behandling,
    nyVilkårsvurdering: Vilkårsvurdering,
    stegService: StegService,
): Behandling {
    val vilkårsvurderingForBehandling = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)!!
    vilkårsvurderingForBehandling.oppdaterMedDataFra(nyVilkårsvurdering)

    vilkårsvurderingService.oppdater(vilkårsvurderingForBehandling)

    return stegService.håndterVilkårsvurdering(behandling)
}

fun Vilkårsvurdering.oppdaterMedDataFra(vilkårsvurdering: Vilkårsvurdering) {
    this.personResultater.forEach { personResultatSomSkalOppdateres ->
        val nyttPersonresultat =
            vilkårsvurdering.personResultater.find { it.aktør.aktørId == personResultatSomSkalOppdateres.aktør.aktørId }!!

        personResultatSomSkalOppdateres.vilkårResultater.forEach { vilkårResultatSomSkalOppdateres ->
            val nyttVilkårResultat =
                nyttPersonresultat.vilkårResultater
                    .find { it.vilkårType == vilkårResultatSomSkalOppdateres.vilkårType }
                    ?: throw Feil(
                        "Fant ikke ${vilkårResultatSomSkalOppdateres.vilkårType} i vilkårene som ble sendt med for " +
                            "${personResultatSomSkalOppdateres.aktør.aktivFødselsnummer()}.",
                    )

            vilkårResultatSomSkalOppdateres.resultat = nyttVilkårResultat.resultat
            vilkårResultatSomSkalOppdateres.periodeFom = nyttVilkårResultat.periodeFom
            vilkårResultatSomSkalOppdateres.periodeTom = nyttVilkårResultat.periodeTom
        }
    }
}

fun leggTilAlleGyldigeBegrunnelserPåVedtaksperiodeIBehandling(
    behandling: Behandling,
    vedtakService: VedtakService,
    vedtaksperiodeService: VedtaksperiodeService,
) {
    val aktivtVedtak = vedtakService.hentAktivForBehandling(behandling.id)!!

    val perisisterteVedtaksperioder =
        vedtaksperiodeService.hentPersisterteVedtaksperioder(aktivtVedtak)

    val vedtaksperiode = perisisterteVedtaksperioder.first()

    val grunnlagForBegrunnelse = vedtaksperiodeService.hentGrunnlagForBegrunnelse(behandling)
    val begrunnelserPerPerson =
        vedtaksperiode.hentGyldigeBegrunnelserPerPerson(grunnlagForBegrunnelse).values.flatten()

    val begrunnelserPerPersonSomPasserVedtaksperiode = begrunnelserPerPerson.filter { it.vedtakBegrunnelseType in vedtaksperiode.type.tillatteBegrunnelsestyper }

    vedtaksperiodeService.oppdaterVedtaksperiodeMedStandardbegrunnelser(
        vedtaksperiodeId = vedtaksperiode.id,
        standardbegrunnelserFraFrontend = begrunnelserPerPersonSomPasserVedtaksperiode.filterIsInstance<Standardbegrunnelse>().take(5),
        eøsStandardbegrunnelserFraFrontend = begrunnelserPerPersonSomPasserVedtaksperiode.filterIsInstance<EØSStandardbegrunnelse>().take(5),
    )
}

// Følgende funksjoner kan muligens erstattes av kjørStegprosessForBehandling()

/**
 * Dette er en funksjon for å få en førstegangsbehandling til en ønsket tilstand ved test.
 * Man sender inn steg man ønsker å komme til (tilSteg), personer på behandlingen (søkerFnr og barnasIdenter),
 * og serviceinstanser som brukes i testen.
 */
fun kjørStegprosessForFGB(
    tilSteg: StegType,
    barnasIdenter: List<String>,
    søkerFnr: String,
    fagsakService: FagsakService,
    vedtakService: VedtakService,
    persongrunnlagService: PersongrunnlagService,
    vilkårsvurderingService: VilkårsvurderingService,
    stegService: StegService,
    vedtaksperiodeService: VedtaksperiodeService,
    behandlingUnderkategori: BehandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
    institusjon: InstitusjonDto? = null,
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
                søknadMottattDato = LocalDate.now().minusYears(18),
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
            TilbakekrevingDto(
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
    institusjon: InstitusjonDto?,
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
                TilbakekrevingDto(
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
