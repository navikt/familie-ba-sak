package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.NavnOgIdent
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.kjerne.beregning.SatsService.tilleggOrdinærSatsTilTester
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollInterval
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.TimeUnit


class JournalførOgBehandleFørstegangssøknadNasjonalTest : AbstractVerdikjedetest() {

    @Test
    fun `Skal journalføre og behandle ordinær nasjonal sak`() {
        val scenario = mockServerKlient().lagScenario(RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1996-01-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                        RestScenarioPerson(fødselsdato = LocalDate.now().minusMonths(6).toString(),
                                           fornavn = "Barn",
                                           etternavn = "Barnesen",
                                           bostedsadresser = emptyList()
                        )
                )
        ))

        val fagsakId: Ressurs<String> = familieBaSakKlient().journalfør(
                journalpostId = "1234",
                oppgaveId = "5678",
                journalførendeEnhet = "4833",
                restJournalføring = lagMockRestJournalføring(bruker = NavnOgIdent(
                        navn = scenario.søker.navn,
                        id = scenario.søker.ident!!
                ))
        )

        assertEquals(Ressurs.Status.SUKSESS, fagsakId.status)

        val restFagsakEtterJournalføring = familieBaSakKlient().hentFagsak(fagsakId = fagsakId.data?.toLong()!!)
        generellAssertFagsak(restFagsak = restFagsakEtterJournalføring,
                             fagsakStatus = FagsakStatus.OPPRETTET,
                             behandlingStegType = StegType.REGISTRERE_SØKNAD)

        val aktivBehandling = hentAktivBehandling(restFagsak = restFagsakEtterJournalføring.data!!)
        val restRegistrerSøknad =
                RestRegistrerSøknad(søknad = lagSøknadDTO(søkerIdent = scenario.søker.ident,
                                                          barnasIdenter = scenario.barna.map { it.ident!! }),
                                    bekreftEndringerViaFrontend = false)
        val restFagsakEtterRegistrertSøknad: Ressurs<RestFagsak> =
                familieBaSakKlient().registrererSøknad(
                        behandlingId = aktivBehandling!!.behandlingId,
                        restRegistrerSøknad = restRegistrerSøknad
                )
        generellAssertFagsak(restFagsak = restFagsakEtterRegistrertSøknad,
                             fagsakStatus = FagsakStatus.OPPRETTET,
                             behandlingStegType = StegType.VILKÅRSVURDERING)


        // Godkjenner alle vilkår på førstegangsbehandling.
        val aktivBehandlingEtterRegistrertSøknad = hentAktivBehandling(restFagsakEtterRegistrertSøknad.data!!)!!
        aktivBehandlingEtterRegistrertSøknad.personResultater.forEach { restPersonResultat ->
            restPersonResultat.vilkårResultater.filter { it.resultat == Resultat.IKKE_VURDERT }.forEach {

                familieBaSakKlient().putVilkår(
                        behandlingId = aktivBehandlingEtterRegistrertSøknad.behandlingId,
                        vilkårId = it.id,
                        restPersonResultat =
                        RestPersonResultat(personIdent = restPersonResultat.personIdent,
                                           vilkårResultater = listOf(it.copy(
                                                   resultat = Resultat.OPPFYLT,
                                                   periodeFom = LocalDate.now().minusMonths(2)
                                           ))))
            }
        }

        val restFagsakEtterVilkårsvurdering =
                familieBaSakKlient().validerVilkårsvurdering(
                        behandlingId = aktivBehandlingEtterRegistrertSøknad.behandlingId
                )
        val behandlingEtterVilkårsvurdering = hentAktivBehandling(restFagsak = restFagsakEtterVilkårsvurdering.data!!)!!

        assertEquals(tilleggOrdinærSatsTilTester.beløp,
                     hentNåværendeEllerNesteMånedsUtbetaling(
                             behandling = behandlingEtterVilkårsvurdering
                     )
        )

        generellAssertFagsak(restFagsak = restFagsakEtterVilkårsvurdering,
                             fagsakStatus = FagsakStatus.OPPRETTET,
                             behandlingStegType = StegType.VURDER_TILBAKEKREVING)

        val restFagsakEtterVurderTilbakekreving = familieBaSakKlient().lagreTilbakekrevingOgGåVidereTilNesteSteg(
                behandlingEtterVilkårsvurdering.behandlingId,
                RestTilbakekreving(Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING, begrunnelse = "begrunnelse"))
        generellAssertFagsak(restFagsak = restFagsakEtterVurderTilbakekreving,
                             fagsakStatus = FagsakStatus.OPPRETTET,
                             behandlingStegType = StegType.SEND_TIL_BESLUTTER)

        val vedtaksperiodeId =
                hentAktivtVedtak(restFagsakEtterVurderTilbakekreving.data!!)!!.vedtaksperioderMedBegrunnelser.first()
        familieBaSakKlient().oppdaterVedtaksperiodeMedStandardbegrunnelser(vedtaksperiodeId = vedtaksperiodeId.id,
                                                                           restPutVedtaksperiodeMedStandardbegrunnelser = RestPutVedtaksperiodeMedStandardbegrunnelser(
                                                                                   standardbegrunnelser = listOf(
                                                                                           VedtakBegrunnelseSpesifikasjon.INNVILGET_BOR_HOS_SØKER)
                                                                           ))

        val restFagsakEtterSendTilBeslutter =
                familieBaSakKlient().sendTilBeslutter(fagsakId = restFagsakEtterVurderTilbakekreving.data!!.id)

        generellAssertFagsak(restFagsak = restFagsakEtterSendTilBeslutter,
                             fagsakStatus = FagsakStatus.OPPRETTET,
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
                             fagsakStatus = FagsakStatus.OPPRETTET,
                             behandlingStegType = StegType.IVERKSETT_MOT_OPPDRAG)

        await.atMost(80, TimeUnit.SECONDS).withPollInterval(Duration.ofSeconds(1)).until {

            val fagsak = familieBaSakKlient().hentFagsak(fagsakId = restFagsakEtterIverksetting.data!!.id).data
            println("FAGSAK ved manuell journalføring: ${fagsak?.convertDataClassToJson()}")
            fagsak?.status == FagsakStatus.LØPENDE
        }

        val restFagsakEtterBehandlingAvsluttet =
                familieBaSakKlient().hentFagsak(fagsakId = restFagsakEtterIverksetting.data!!.id)
        generellAssertFagsak(restFagsak = restFagsakEtterBehandlingAvsluttet,
                             fagsakStatus = FagsakStatus.LØPENDE,
                             behandlingStegType = StegType.BEHANDLING_AVSLUTTET)
    }
}
