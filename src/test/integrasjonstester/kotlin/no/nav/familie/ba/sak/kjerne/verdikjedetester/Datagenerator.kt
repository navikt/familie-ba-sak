package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.ekstern.restDomene.MinimalFagsakDto
import no.nav.familie.ba.sak.ekstern.restDomene.PersonResultatDto
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.TilbakekrevingDto
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.RestScenario
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.springframework.http.HttpHeaders
import java.time.LocalDate

fun fullførBehandlingFraVilkårsvurderingAlleVilkårOppfylt(
    restUtvidetBehandling: RestUtvidetBehandling,
    personScenario: RestScenario,
    fagsak: MinimalFagsakDto,
    familieBaSakKlient: FamilieBaSakKlient,
    lagToken: (Map<String, Any>) -> String,
    behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    fagsakService: FagsakService,
    vedtakService: VedtakService,
    stegService: StegService,
    brevmalService: BrevmalService,
    vedtaksperiodeService: VedtaksperiodeService,
): Behandling {
    settAlleVilkårTilOppfylt(
        restUtvidetBehandling = restUtvidetBehandling,
        barnFødselsdato = personScenario.barna.maxOf { LocalDate.parse(it.fødselsdato) },
        familieBaSakKlient = familieBaSakKlient,
    )

    familieBaSakKlient.validerVilkårsvurdering(
        behandlingId = restUtvidetBehandling.behandlingId,
    )

    val restUtvidetBehandlingEtterBehandlingsResultat =
        familieBaSakKlient.behandlingsresultatStegOgGåVidereTilNesteSteg(
            behandlingId = restUtvidetBehandling.behandlingId,
        )

    val restUtvidetBehandlingEtterVurderTilbakekreving =
        familieBaSakKlient.lagreTilbakekrevingOgGåVidereTilNesteSteg(
            restUtvidetBehandlingEtterBehandlingsResultat.data!!.behandlingId,
            TilbakekrevingDto(Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING, begrunnelse = "begrunnelse"),
        )

    val vedtaksperioderMedBegrunnelser =
        vedtaksperiodeService.hentRestUtvidetVedtaksperiodeMedBegrunnelser(
            restUtvidetBehandlingEtterVurderTilbakekreving.data!!.behandlingId,
        )

    val utvidetVedtaksperiodeMedBegrunnelser =
        vedtaksperioderMedBegrunnelser.sortedBy { it.fom }.first()

    familieBaSakKlient.oppdaterVedtaksperiodeMedStandardbegrunnelser(
        vedtaksperiodeId = utvidetVedtaksperiodeMedBegrunnelser.id,
        restPutVedtaksperiodeMedStandardbegrunnelser =
            RestPutVedtaksperiodeMedStandardbegrunnelser(
                standardbegrunnelser = utvidetVedtaksperiodeMedBegrunnelser.gyldigeBegrunnelser.filter(String::isNotEmpty).take(5),
            ),
    )
    val restUtvidetBehandlingEtterSendTilBeslutter =
        familieBaSakKlient.sendTilBeslutter(behandlingId = restUtvidetBehandlingEtterVurderTilbakekreving.data!!.behandlingId)

    familieBaSakKlient.iverksettVedtak(
        behandlingId = restUtvidetBehandlingEtterSendTilBeslutter.data!!.behandlingId,
        restBeslutningPåVedtak =
            RestBeslutningPåVedtak(
                Beslutning.GODKJENT,
            ),
        beslutterHeaders =
            HttpHeaders().apply {
                setBearerAuth(
                    lagToken(
                        mapOf(
                            "groups" to listOf("SAKSBEHANDLER", "BESLUTTER"),
                            "azp" to "azp-test",
                            "name" to "Mock McMockface Beslutter",
                            "NAVident" to "Z0000",
                        ),
                    ),
                )
            },
    )
    return håndterIverksettingAvBehandling(
        behandlingEtterVurdering = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId = fagsak.id)!!,
        søkerFnr = personScenario.søker.ident,
        fagsakService = fagsakService,
        vedtakService = vedtakService,
        stegService = stegService,
        brevmalService = brevmalService,
    )
}

fun settAlleVilkårTilOppfylt(
    restUtvidetBehandling: RestUtvidetBehandling,
    barnFødselsdato: LocalDate,
    familieBaSakKlient: FamilieBaSakKlient,
) {
    restUtvidetBehandling.personResultater.forEach { restPersonResultat ->
        restPersonResultat.vilkårResultater.filter { it.resultat == Resultat.IKKE_VURDERT }.forEach {
            familieBaSakKlient.putVilkår(
                behandlingId = restUtvidetBehandling.behandlingId,
                vilkårId = it.id,
                personResultatDto =
                    PersonResultatDto(
                        personIdent = restPersonResultat.personIdent,
                        vilkårResultater =
                            listOf(
                                it.copy(
                                    resultat = Resultat.OPPFYLT,
                                    periodeFom = barnFødselsdato,
                                ),
                            ),
                    ),
            )
        }
    }
}
