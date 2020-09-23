package no.nav.familie.ba.sak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vedtak.Beslutning
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultat
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatService
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPersonResultaterForSøkerOgToBarn
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.ApplicationConfig
import no.nav.familie.ba.sak.dokument.DokumentService
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

object DevLauncherDokGen {

    @JvmStatic
    fun main(args: Array<String>) {
        val app = SpringApplicationBuilder(ApplicationConfig::class.java)
                .profiles("dev",
                          "mock-iverksett",
                          "mock-infotrygd-feed",
                          "mock-infotrygd-barnetrygd",
                          "mock-sts",
                          "mock-pdl"
                )
        app.run(*args)
    }
}

@RestController
@Unprotected
@RequestMapping("/debugger")
class DebugController(
        val dokumentService: DokumentService,
        val fagsakService: FagsakService,
        val behandlingService: BehandlingService,
        val persongrunnlagService: PersongrunnlagService,
        val vedtakService: VedtakService,
        val behandlingResultatService: BehandlingResultatService,
        val beregningService: BeregningService,
        val totrinnskontrollService: TotrinnskontrollService,
) {

    @GetMapping(path = ["generatePdf"], produces = [MediaType.APPLICATION_PDF_VALUE])
    fun generatePdf(): ByteArray {

        val fnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak = fagsak,
                                                                                            behandlingType = BehandlingType.REVURDERING))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barn1Fnr, barn2Fnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)
        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                personopplysningGrunnlag = personopplysningGrunnlag,
                behandling = behandling
        )
        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)

        val dato_2020_01_01 = LocalDate.of(2020, 1, 1)
        val stønadTom = dato_2020_01_01.plusYears(17)
        val behandlingResultat1 =
                BehandlingResultat(behandling = behandling)
        behandlingResultat1.personResultater = lagPersonResultaterForSøkerOgToBarn(behandlingResultat1,
                                                                                   fnr,
                                                                                   barn1Fnr,
                                                                                   barn2Fnr,
                                                                                   dato_2020_01_01.minusMonths(1),
                                                                                   stønadTom)
        behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat = behandlingResultat1, loggHendelse = true)

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)

        totrinnskontrollService.opprettEllerHentTotrinnskontroll(behandling, "ansvarligSaksbehandler")
        totrinnskontrollService.besluttTotrinnskontroll(behandling, "ansvarligBeslutter", Beslutning.GODKJENT)

        vedtakService.oppdaterVedtakMedStønadsbrev(vedtak!!)

        return dokumentService.genererBrevForVedtak(vedtak)
    }
}