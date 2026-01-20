package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.ekstern.restDomene.PutVedtaksperiodeMedStandardbegrunnelserDto
import no.nav.familie.ba.sak.ekstern.restDomene.TilbakekrevingDto
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.brev.BrevService
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.brev.DokumentService
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Førstegangsvedtak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.ScenarioDto
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.ScenarioPersonDto
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.stubScenario
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ba.sak.kjørbehandling.kjørStegprosessForFGB
import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class OpplysningspliktTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val vilkårsvurderingService: VilkårsvurderingService,
    @Autowired private val persongrunnlagService: PersongrunnlagService,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
    @Autowired private val dokumentService: DokumentService,
    @Autowired private val brevService: BrevService,
    @Autowired private val settPåVentService: SettPåVentService,
    @Autowired private val brevmalService: BrevmalService,
) : AbstractVerdikjedetest() {
    @Test
    fun `Skal opprette opplysningsplikt-vilkår på søker når 'innhente opplysninger'-brev sendes ut og ta med hjemmel 17 og 18 i vedtaksbrev når opplysningsplikt-vilkåret ikke er oppfylt`() {
        val scenario =
            ScenarioDto(
                søker = ScenarioPersonDto(fødselsdato = "1990-04-20", fornavn = "Mor", etternavn = "Søker"),
                barna =
                    listOf(
                        ScenarioPersonDto(
                            fødselsdato = LocalDate.now().minusMonths(2).toString(),
                            fornavn = "Barn",
                            etternavn = "Barnesen",
                        ),
                    ),
            ).also { stubScenario(it) }

        val behandling =
            kjørStegprosessForFGB(
                tilSteg = StegType.VILKÅRSVURDERING,
                søkerFnr = scenario.søker.ident,
                barnasIdenter = scenario.barna.map { it.ident },
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                brevmalService = brevmalService,
            )

        // Send "innhente opplysninger"-brev og sjekk at opplysningsplikt vilkåret dukker opp på _kun_ søker
        dokumentService.sendManueltBrev(
            fagsakId = behandling.fagsak.id,
            manueltBrevRequest =
                ManueltBrevRequest(
                    brevmal = Brevmal.INNHENTE_OPPLYSNINGER,
                    enhet = Enhet(enhetId = "1234", enhetNavn = "Enhet Enhetesen"),
                ),
            behandling = behandling,
        )

        settPåVentService.gjenopptaBehandling(behandling.id)

        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)

        val opplysningspliktVilkårPåSøker =
            vilkårsvurdering
                ?.personResultater
                ?.single { it.erSøkersResultater() }
                ?.andreVurderinger
                ?.singleOrNull { it.type == AnnenVurderingType.OPPLYSNINGSPLIKT }

        val opplysningspliktVilkårPåBarna =
            vilkårsvurdering
                ?.personResultater
                ?.filter { !it.erSøkersResultater() }
                ?.flatMap { it.andreVurderinger.filter { it.type == AnnenVurderingType.OPPLYSNINGSPLIKT } } ?: emptyList()

        Assertions.assertTrue(opplysningspliktVilkårPåSøker != null)
        Assertions.assertTrue(opplysningspliktVilkårPåBarna.isEmpty())

        // Sette opplysningsplikt vilkåret til ikke oppfylt og sjekke at hjemlene blir riktig
        opplysningspliktVilkårPåSøker?.resultat = Resultat.IKKE_OPPFYLT
        vilkårsvurderingService.oppdater(vilkårsvurdering = vilkårsvurdering!!)

        val vilkårsvurderingOppdatert = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)

        Assertions.assertTrue(
            vilkårsvurderingOppdatert
                ?.personResultater
                ?.single { it.erSøkersResultater() }
                ?.andreVurderinger
                ?.single { it.type == AnnenVurderingType.OPPLYSNINGSPLIKT }
                ?.resultat == Resultat.IKKE_OPPFYLT,
        )

        familieBaSakKlient().validerVilkårsvurdering(
            behandlingId = behandling.id,
        )

        val behandlingEtterBehandlingsResultat =
            familieBaSakKlient().behandlingsresultatStegOgGåVidereTilNesteSteg(
                behandlingId = behandling.id,
            )

        val behandlingEtterVurderTilbakekreving =
            familieBaSakKlient().lagreTilbakekrevingOgGåVidereTilNesteSteg(
                behandlingEtterBehandlingsResultat.data!!.behandlingId,
                TilbakekrevingDto(Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING, begrunnelse = "begrunnelse"),
            )

        val vedtaksperioderMedBegrunnelser =
            vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelserDto(
                behandlingEtterVurderTilbakekreving.data!!.behandlingId,
            )

        val vedtaksperiode = vedtaksperioderMedBegrunnelser.sortedBy { it.fom }.first()
        familieBaSakKlient().oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = vedtaksperiode.id,
            putVedtaksperiodeMedStandardbegrunnelserDto =
                PutVedtaksperiodeMedStandardbegrunnelserDto(
                    standardbegrunnelser =
                        listOf(
                            Standardbegrunnelse.INNVILGET_BOR_HOS_SØKER.enumnavnTilString(),
                        ),
                ),
        )

        val vedtak =
            vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(vedtaksperiodeId = vedtaksperiode.id).vedtak

        val vedtaksbrev = brevService.hentVedtaksbrevData(vedtak)

        val hjemmeltekst =
            (vedtaksbrev as Førstegangsvedtak)
                .data.delmalData.hjemmeltekst.hjemler!!
                .first()

        Assertions.assertTrue(hjemmeltekst.contains("17"))
        Assertions.assertTrue(hjemmeltekst.contains("18"))
    }
}
