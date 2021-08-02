package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.ekstern.restDomene.RestHentFagsakForPerson
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollInterval
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class ManuellBehandlingAvSoknadOgTekniskOpphorTest : AbstractVerdikjedetest() {

    @Test
    fun `Skal teknisk opphøre behandling`() {
        val scenario = mockServerKlient().lagScenario(RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1996-01-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                        RestScenarioPerson(fødselsdato = LocalDate.now().minusDays(2).toString(),
                                           fornavn = "Barn",
                                           etternavn = "Barnesen")
                )
        ))
        familieBaSakKlient().triggFødselshendelse(
                NyBehandlingHendelse(
                        morsIdent = scenario.søker.ident!!,
                        barnasIdenter = listOf(scenario.barna.first().ident!!)
                )
        )

        await.atMost(80, TimeUnit.SECONDS).withPollInterval(Duration.ofSeconds(1)).until {
            val fagsak =
                    familieBaSakKlient().hentFagsak(restHentFagsakForPerson = RestHentFagsakForPerson(personIdent = scenario.søker.ident)).data
            println("FAGSAK ved fødselshendelse: $fagsak")
            fagsak?.status == FagsakStatus.LØPENDE && hentAktivBehandling(fagsak)?.steg == StegType.BEHANDLING_AVSLUTTET
        }

        val restFagsakMedBehandling = familieBaSakKlient().opprettBehandling(søkersIdent = scenario.søker.ident,
                                                                             behandlingType = BehandlingType.TEKNISK_OPPHØR,
                                                                             behandlingÅrsak = BehandlingÅrsak.TEKNISK_OPPHØR)
        generellAssertFagsak(restFagsak = restFagsakMedBehandling,
                             fagsakStatus = FagsakStatus.LØPENDE,
                             behandlingStegType = StegType.VILKÅRSVURDERING)
        assertEquals(2, restFagsakMedBehandling.data?.behandlinger?.size)

        val aktivBehandling = hentAktivBehandling(restFagsak = restFagsakMedBehandling.data!!)!!

        // Setter alle vilkår til ikke-oppfylt på løpende førstegangsbehandling
        aktivBehandling.personResultater.forEach { restPersonResultat ->
            restPersonResultat.vilkårResultater.forEach {
                familieBaSakKlient().putVilkår(
                        behandlingId = aktivBehandling.behandlingId,
                        vilkårId = it.id,
                        restPersonResultat =
                        RestPersonResultat(personIdent = restPersonResultat.personIdent,
                                           vilkårResultater = listOf(it.copy(
                                                   resultat = Resultat.IKKE_OPPFYLT
                                           ))))
            }
        }

        val restFagsakEtterVilkårsvurdering =
                familieBaSakKlient().validerVilkårsvurdering(
                        behandlingId = aktivBehandling.behandlingId
                )

        //Midlertidig løsning for å håndtere med og uten simuleringssteg.
        // TODO: Fjern når toggelen bruk_simulering er fjernet fra familie-ba-sak.
        val behandlingEtterVilkårsvurdering = hentAktivBehandling(restFagsak = restFagsakEtterVilkårsvurdering.data!!)!!
        var restFagsakEtterVurderTilbakekreving = restFagsakEtterVilkårsvurdering

        if (behandlingEtterVilkårsvurdering.steg == StegType.VURDER_TILBAKEKREVING) {
            generellAssertFagsak(restFagsak = restFagsakEtterVilkårsvurdering,
                                 fagsakStatus = FagsakStatus.OPPRETTET,
                                 behandlingStegType = StegType.VURDER_TILBAKEKREVING,
                                 behandlingResultat = BehandlingResultat.OPPHØRT)

            restFagsakEtterVurderTilbakekreving = familieBaSakKlient().lagreTilbakekrevingOgGåVidereTilNesteSteg(
                    behandlingEtterVilkårsvurdering.behandlingId,
                    RestTilbakekreving(Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING, begrunnelse = "begrunnelse"))
        }

        generellAssertFagsak(restFagsak = restFagsakEtterVurderTilbakekreving,
                             fagsakStatus = FagsakStatus.LØPENDE,
                             behandlingStegType = StegType.SEND_TIL_BESLUTTER,
                             behandlingResultat = BehandlingResultat.OPPHØRT)

        val restFagsakEtterSendTilBeslutter =
                familieBaSakKlient().sendTilBeslutter(fagsakId = restFagsakEtterVurderTilbakekreving.data!!.id)
        generellAssertFagsak(restFagsak = restFagsakEtterSendTilBeslutter,
                             fagsakStatus = FagsakStatus.LØPENDE,
                             behandlingStegType = StegType.BESLUTTE_VEDTAK)

        val restFagsakEtterIverksetting =
                familieBaSakKlient().iverksettVedtak(fagsakId = restFagsakEtterVurderTilbakekreving.data!!.id,
                                                     restBeslutningPåVedtak = RestBeslutningPåVedtak(
                                                             Beslutning.GODKJENT),
                                                     beslutterHeaders = HttpHeaders().apply {
                                                         setBearerAuth(token(
                                                                 mapOf("groups" to listOf("SAKSBEHANDLER", "BESLUTTER"),
                                                                       "azp" to "e2e-test",
                                                                       "name" to "Mock McMockface Beslutter",
                                                                       "preferred_username" to "mock.mcmockface.beslutter@nav.no")
                                                         ))
                                                     })

        generellAssertFagsak(restFagsak = restFagsakEtterIverksetting,
                             fagsakStatus = FagsakStatus.LØPENDE,
                             behandlingStegType = StegType.IVERKSETT_MOT_OPPDRAG)

        await.atMost(80, TimeUnit.SECONDS).withPollInterval(Duration.ofSeconds(1)).until {

            val fagsak = familieBaSakKlient().hentFagsak(fagsakId = restFagsakEtterIverksetting.data!!.id).data
            println("TEKNISK OPPHØR PÅ FAGSAK: $fagsak")
            fagsak?.status == FagsakStatus.AVSLUTTET
        }

        val restFagsakEtterBehandlingAvsluttet =
                familieBaSakKlient().hentFagsak(fagsakId = restFagsakEtterIverksetting.data!!.id)
        generellAssertFagsak(restFagsak = restFagsakEtterBehandlingAvsluttet,
                             fagsakStatus = FagsakStatus.AVSLUTTET,
                             behandlingStegType = StegType.BEHANDLING_AVSLUTTET)
    }
}
