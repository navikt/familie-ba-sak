package no.nav.familie.ba.sak.integrasjoner.økonomi

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.økonomi.ØkonomiService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime

class ØkonomiIntegrasjonTest : AbstractSpringIntegrationTest() {

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var vilkårsvurderingService: VilkårsvurderingService

    @Autowired
    lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    lateinit var økonomiService: ØkonomiService

    @Autowired
    lateinit var beregningService: BeregningService

    @Autowired
    lateinit var stegService: StegService

    @Autowired
    private lateinit var vedtakService: VedtakService

    @Test
    @Tag("integration")
    fun `Iverksett vedtak på aktiv behandling`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val stønadFom = LocalDate.now()
        val stønadTom = stønadFom.plusYears(17)

        val responseBody = Ressurs.Companion.success("ok")
        stubFor(
            get(urlEqualTo("/api/aktoer/v1"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(Ressurs.Companion.success(mapOf("aktørId" to 1L))))
                )
        )
        stubFor(
            post(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(responseBody))
                )
        )

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val vilkårsvurdering = lagBehandlingResultat(behandling, fnr, barnFnr, stønadFom, stønadTom)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)
        Assertions.assertNotNull(behandling.fagsak.id)

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling = behandling)

        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
        Assertions.assertNotNull(vedtak)
        vedtak!!.vedtaksdato = LocalDateTime.now()
        vedtakService.oppdater(vedtak)

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)

        val utbetalingsoppdrag =
            økonomiService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(vedtak, "ansvarligSaksbehandler")
        assertDoesNotThrow {
            økonomiService.iverksettOppdrag(utbetalingsoppdrag)
        }
    }

    @Test
    @Tag("integration")
    fun `Hent behandlinger for løpende fagsaker til konsistensavstemming mot økonomi`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val stønadFom = LocalDate.now()
        val stønadTom = stønadFom.plusYears(17)

        stubFor(
            post(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(Ressurs.Companion.success("ok")))
                )
        )

        // Lag fagsak med behandling og personopplysningsgrunnlag og Iverksett.
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val vedtak = Vedtak(
            behandling = behandling,
            vedtaksdato = LocalDateTime.of(2020, 1, 1, 4, 35)
        )

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)
        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling)

        val vilkårsvurdering = lagBehandlingResultat(behandling, fnr, barnFnr, stønadFom, stønadTom)
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)

        val utbetalingsoppdrag =
            økonomiService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(vedtak, "ansvarligSaksbehandler")
        økonomiService.iverksettOppdrag(utbetalingsoppdrag)
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.AVSLUTTET)

        fagsak.status = FagsakStatus.LØPENDE
        fagsakService.lagre(fagsak)

        val behandlingerMedAndelerTilAvstemming = behandlingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker()

        Assertions.assertTrue(behandlingerMedAndelerTilAvstemming.contains(behandling.id))
    }

    private fun lagBehandlingResultat(
        behandling: Behandling,
        søkerFnr: String,
        barnFnr: String,
        stønadFom: LocalDate,
        stønadTom: LocalDate
    ): Vilkårsvurdering {
        val vilkårsvurdering =
            Vilkårsvurdering(behandling = behandling)
        vilkårsvurdering.personResultater = setOf(
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                fnr = søkerFnr,
                resultat = Resultat.OPPFYLT,
                periodeFom = stønadFom,
                periodeTom = stønadTom,
                lagFullstendigVilkårResultat = true,
                personType = PersonType.SØKER
            ),
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                fnr = barnFnr,
                resultat = Resultat.OPPFYLT,
                periodeFom = stønadFom,
                periodeTom = stønadTom,
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN
            )
        )
        return vilkårsvurdering
    }
}
