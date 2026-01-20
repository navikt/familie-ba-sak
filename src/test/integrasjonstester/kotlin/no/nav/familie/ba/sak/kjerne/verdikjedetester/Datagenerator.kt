package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.ekstern.restDomene.MinimalFagsakDto
import no.nav.familie.ba.sak.ekstern.restDomene.PersonResultatDto
import no.nav.familie.ba.sak.ekstern.restDomene.PutVedtaksperiodeMedStandardbegrunnelserDto
import no.nav.familie.ba.sak.ekstern.restDomene.TilbakekrevingDto
import no.nav.familie.ba.sak.ekstern.restDomene.UtvidetBehandlingDto
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.BeslutningPåVedtakDto
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.ScenarioDto
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.springframework.http.HttpHeaders
import java.time.LocalDate

fun fullførBehandlingFraVilkårsvurderingAlleVilkårOppfylt(
    utvidetBehandlingDto: UtvidetBehandlingDto,
    personScenario: ScenarioDto,
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
        utvidetBehandlingDto = utvidetBehandlingDto,
        barnFødselsdato = personScenario.barna.maxOf { LocalDate.parse(it.fødselsdato) },
        familieBaSakKlient = familieBaSakKlient,
    )

    familieBaSakKlient.validerVilkårsvurdering(
        behandlingId = utvidetBehandlingDto.behandlingId,
    )

    val utvidetBehandlingDtoEtterBehandlingsResultat =
        familieBaSakKlient.behandlingsresultatStegOgGåVidereTilNesteSteg(
            behandlingId = utvidetBehandlingDto.behandlingId,
        )

    val utvidetBehandlingDtoEtterVurderTilbakekreving =
        familieBaSakKlient.lagreTilbakekrevingOgGåVidereTilNesteSteg(
            utvidetBehandlingDtoEtterBehandlingsResultat.data!!.behandlingId,
            TilbakekrevingDto(Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING, begrunnelse = "begrunnelse"),
        )

    val vedtaksperioderMedBegrunnelser =
        vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelserDto(
            utvidetBehandlingDtoEtterVurderTilbakekreving.data!!.behandlingId,
        )

    val utvidetVedtaksperiodeMedBegrunnelser =
        vedtaksperioderMedBegrunnelser.sortedBy { it.fom }.first()

    familieBaSakKlient.oppdaterVedtaksperiodeMedStandardbegrunnelser(
        vedtaksperiodeId = utvidetVedtaksperiodeMedBegrunnelser.id,
        putVedtaksperiodeMedStandardbegrunnelserDto =
            PutVedtaksperiodeMedStandardbegrunnelserDto(
                standardbegrunnelser = utvidetVedtaksperiodeMedBegrunnelser.gyldigeBegrunnelser.filter(String::isNotEmpty).take(5),
            ),
    )
    val utvidetBehandlingDtoEtterSendTilBeslutter =
        familieBaSakKlient.sendTilBeslutter(behandlingId = utvidetBehandlingDtoEtterVurderTilbakekreving.data!!.behandlingId)

    familieBaSakKlient.iverksettVedtak(
        behandlingId = utvidetBehandlingDtoEtterSendTilBeslutter.data!!.behandlingId,
        beslutningPåVedtakDto =
            BeslutningPåVedtakDto(
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
    utvidetBehandlingDto: UtvidetBehandlingDto,
    barnFødselsdato: LocalDate,
    familieBaSakKlient: FamilieBaSakKlient,
) {
    utvidetBehandlingDto.personResultater.forEach { personResultatDto ->
        personResultatDto.vilkårResultater.filter { it.resultat == Resultat.IKKE_VURDERT }.forEach {
            familieBaSakKlient.putVilkår(
                behandlingId = utvidetBehandlingDto.behandlingId,
                vilkårId = it.id,
                personResultatDto =
                    PersonResultatDto(
                        personIdent = personResultatDto.personIdent,
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
