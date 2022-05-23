package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagRestVedtaksbegrunnelse
import no.nav.familie.ba.sak.common.lagSanityBegrunnelse
import no.nav.familie.ba.sak.common.lagUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.common.lagUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.integrasjoner.sanity.hentSanityBegrunnelser
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Opphørsperiode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class BrevUtilsTest {

    /**
     * Siden navnet til saksbehandler blir hentet fra sikkerhetscontext er det riktig at denne er system her.
     */
    @Test
    fun `Saksbehandler blir hentet fra sikkerhetscontext og beslutter viser placeholder tekst under behandling`() {
        val behandling = lagBehandling()

        val (saksbehandler, beslutter) = hentSaksbehandlerOgBeslutter(
            behandling = behandling,
            totrinnskontroll = null
        )

        Assertions.assertEquals("System", saksbehandler)
        Assertions.assertEquals("Beslutter", beslutter)
    }

    @Test
    fun `Saksbehandler blir hentet og beslutter er hentet fra sikkerhetscontext under beslutning`() {
        val behandling = lagBehandling()
        behandling.leggTilBehandlingStegTilstand(StegType.BESLUTTE_VEDTAK)

        val (saksbehandler, beslutter) = hentSaksbehandlerOgBeslutter(
            behandling = behandling,
            totrinnskontroll = Totrinnskontroll(
                behandling = behandling,
                saksbehandler = "Mock Saksbehandler",
                saksbehandlerId = "mock.saksbehandler@nav.no"
            )
        )

        Assertions.assertEquals("Mock Saksbehandler", saksbehandler)
        Assertions.assertEquals("System", beslutter)
    }

    @Test
    fun `Saksbehandler blir hentet og beslutter viser placeholder tekst under beslutning`() {
        val behandling = lagBehandling()
        behandling.leggTilBehandlingStegTilstand(StegType.BESLUTTE_VEDTAK)

        val (saksbehandler, beslutter) = hentSaksbehandlerOgBeslutter(
            behandling = behandling,
            totrinnskontroll = Totrinnskontroll(
                behandling = behandling,
                saksbehandler = "System",
                saksbehandlerId = "systembruker"
            )
        )

        Assertions.assertEquals("System", saksbehandler)
        Assertions.assertEquals("Beslutter", beslutter)
    }

    @Test
    fun `Saksbehandler og beslutter blir hentet etter at totrinnskontroll er besluttet`() {
        val behandling = lagBehandling()
        behandling.leggTilBehandlingStegTilstand(StegType.BESLUTTE_VEDTAK)

        val (saksbehandler, beslutter) = hentSaksbehandlerOgBeslutter(
            behandling = behandling,
            totrinnskontroll = Totrinnskontroll(
                behandling = behandling,
                saksbehandler = "Mock Saksbehandler",
                saksbehandlerId = "mock.saksbehandler@nav.no",
                beslutter = "Mock Beslutter",
                beslutterId = "mock.beslutter@nav.no"
            )
        )

        Assertions.assertEquals("Mock Saksbehandler", saksbehandler)
        Assertions.assertEquals("Mock Beslutter", beslutter)
    }

    private val støttedeBehandlingsersultaterFørstegangsbehandling = listOf(
        Behandlingsresultat.INNVILGET,
        Behandlingsresultat.INNVILGET_OG_OPPHØRT,
        Behandlingsresultat.DELVIS_INNVILGET,
        Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT,
        Behandlingsresultat.AVSLÅTT,
    )

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for innvilget førstegangsbehandling`() {

        støttedeBehandlingsersultaterFørstegangsbehandling.filterNot { it == Behandlingsresultat.AVSLÅTT }.forEach {
            Assertions.assertEquals(
                Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
                hentManuellVedtaksbrevtype(
                    BehandlingType.FØRSTEGANGSBEHANDLING,
                    it
                ),
            )
        }
    }

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for avslått førstegangsbehandling`() {
        Assertions.assertEquals(
            Brevmal.VEDTAK_AVSLAG,
            hentManuellVedtaksbrevtype(
                BehandlingType.FØRSTEGANGSBEHANDLING,
                Behandlingsresultat.AVSLÅTT
            ),
        )
    }

    @Test
    fun `skal kaste exception for ikke-støttede behandlingsresultater ved førstegangsbehandling`() {
        val ikkeStøttedeBehandlingsersultater =
            Behandlingsresultat.values().subtract(støttedeBehandlingsersultaterFørstegangsbehandling)

        ikkeStøttedeBehandlingsersultater.forEach {
            assertThrows<Exception> {
                hentManuellVedtaksbrevtype(
                    BehandlingType.FØRSTEGANGSBEHANDLING,
                    it
                )
            }
        }
    }

    private val behandlingsersultaterForVedtakEndring = listOf(
        Behandlingsresultat.INNVILGET,
        Behandlingsresultat.INNVILGET_OG_ENDRET,
        Behandlingsresultat.DELVIS_INNVILGET,
        Behandlingsresultat.DELVIS_INNVILGET_OG_ENDRET,
        Behandlingsresultat.AVSLÅTT_OG_ENDRET,
        Behandlingsresultat.ENDRET_UTBETALING,
        Behandlingsresultat.ENDRET_UTEN_UTBETALING
    )

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for 'Vedtak endring'`() {
        behandlingsersultaterForVedtakEndring.forEach {
            Assertions.assertEquals(
                Brevmal.VEDTAK_ENDRING,
                hentManuellVedtaksbrevtype(
                    BehandlingType.REVURDERING,
                    it
                ),
            )
        }
    }

    private val behandlingsersultaterForOpphørt = listOf(Behandlingsresultat.OPPHØRT)
    private val behandlingsersultaterForFortsattOpphørt = listOf(Behandlingsresultat.FORTSATT_OPPHØRT)

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for 'Opphørt'`() {
        behandlingsersultaterForOpphørt.forEach {
            Assertions.assertEquals(
                Brevmal.VEDTAK_OPPHØRT,
                hentManuellVedtaksbrevtype(
                    BehandlingType.REVURDERING,
                    it
                ),
            )
        }
    }

    private val behandlingsersultaterForOpphørMedEndring = listOf(
        Behandlingsresultat.INNVILGET_OG_OPPHØRT,
        Behandlingsresultat.INNVILGET_ENDRET_OG_OPPHØRT,
        Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT,
        Behandlingsresultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT,
        Behandlingsresultat.AVSLÅTT_OG_OPPHØRT,
        Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT,
        Behandlingsresultat.ENDRET_OG_OPPHØRT,
    )

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for 'Opphør med endring'`() {
        behandlingsersultaterForOpphørMedEndring.forEach {
            Assertions.assertEquals(
                Brevmal.VEDTAK_OPPHØR_MED_ENDRING,
                hentManuellVedtaksbrevtype(
                    BehandlingType.REVURDERING,
                    it
                ),
            )
        }
    }

    private val behandlingsersultaterForFortsattInnvilget = listOf(Behandlingsresultat.FORTSATT_INNVILGET)

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for 'Fortsatt innvilget'`() {
        behandlingsersultaterForFortsattInnvilget.forEach {
            Assertions.assertEquals(
                Brevmal.VEDTAK_FORTSATT_INNVILGET,
                hentManuellVedtaksbrevtype(
                    BehandlingType.REVURDERING,
                    it
                ),
            )
        }
    }

    private val behandlingsersultaterForAvslag = listOf(Behandlingsresultat.AVSLÅTT)

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for 'Avslag'`() {
        behandlingsersultaterForAvslag.forEach {
            Assertions.assertEquals(
                Brevmal.VEDTAK_AVSLAG,
                hentManuellVedtaksbrevtype(
                    BehandlingType.REVURDERING,
                    it
                ),
            )
        }
    }

    @Test
    fun `test hentManuellVedtaksbrevtype kaster exception for ikke-støttede behandlingsresultater ved revurdering`() {
        val ikkeStøttedeBehandlingsersultater =
            Behandlingsresultat.values()
                .subtract(behandlingsersultaterForVedtakEndring)
                .subtract(behandlingsersultaterForOpphørt)
                .subtract(behandlingsersultaterForFortsattOpphørt)
                .subtract(behandlingsersultaterForOpphørMedEndring)
                .subtract(behandlingsersultaterForOpphørMedEndring)
                .subtract(behandlingsersultaterForAvslag)
                .subtract(behandlingsersultaterForFortsattInnvilget)

        ikkeStøttedeBehandlingsersultater.forEach {
            assertThrows<Exception> {
                hentManuellVedtaksbrevtype(
                    BehandlingType.REVURDERING,
                    it
                )
            }
        }
    }

    @Test
    fun `test hentAutomatiskVedtaksbrevtype gir riktig vedtaksbrevtype for revurdering barn fra før`() {

        val fagsak = defaultFagsak().copy(status = FagsakStatus.LØPENDE)
        val behandling =
            lagBehandling(
                fagsak = fagsak,
                skalBehandlesAutomatisk = true,
                årsak = BehandlingÅrsak.FØDSELSHENDELSE
            ).copy(
                resultat = Behandlingsresultat.INNVILGET
            )
        Assertions.assertEquals(
            Brevmal.AUTOVEDTAK_NYFØDT_BARN_FRA_FØR,
            hentBrevmal(behandling)
        )
    }

    @Test
    fun `test hentAutomatiskVedtaksbrevtype gir riktig vedtaksbrevtype for førstegangsbehandling nyfødt barn`() {
        val fagsak = defaultFagsak()
        val behandling =
            lagBehandling(
                fagsak = fagsak,
                skalBehandlesAutomatisk = true,
                årsak = BehandlingÅrsak.FØDSELSHENDELSE
            ).copy(
                resultat = Behandlingsresultat.INNVILGET
            )
        Assertions.assertEquals(
            Brevmal.AUTOVEDTAK_NYFØDT_FØRSTE_BARN,
            hentBrevmal(behandling)
        )
    }

    @Test
    fun `hent dokumenttittel dersom denne skal overstyres for behandlingen`() {
        assertNull(hentOverstyrtDokumenttittel(lagBehandling().copy(type = BehandlingType.FØRSTEGANGSBEHANDLING)))
        val revurdering = lagBehandling().copy(type = BehandlingType.REVURDERING)
        assertNull(hentOverstyrtDokumenttittel(revurdering))
        Assertions.assertEquals(
            "Vedtak om endret barnetrygd - barn 6 år",
            hentOverstyrtDokumenttittel(revurdering.copy(opprettetÅrsak = BehandlingÅrsak.OMREGNING_6ÅR))
        )
        Assertions.assertEquals(
            "Vedtak om endret barnetrygd - barn 18 år",
            hentOverstyrtDokumenttittel(revurdering.copy(opprettetÅrsak = BehandlingÅrsak.OMREGNING_18ÅR))
        )
        Assertions.assertEquals(
            "Vedtak om endret barnetrygd",
            hentOverstyrtDokumenttittel(revurdering.copy(resultat = Behandlingsresultat.INNVILGET_OG_ENDRET))
        )
        Assertions.assertEquals(
            "Vedtak om fortsatt barnetrygd",
            hentOverstyrtDokumenttittel(revurdering.copy(resultat = Behandlingsresultat.FORTSATT_INNVILGET))
        )
        assertNull(hentOverstyrtDokumenttittel(revurdering.copy(resultat = Behandlingsresultat.OPPHØRT)))
    }

    @Test
    fun `hentHjemmeltekst skal returnere sorterte hjemler`() {
        val utvidetVedtaksperioderMedBegrunnelser = listOf(
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagRestVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            ),
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagRestVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            )
        )

        Assertions.assertEquals(
            "§§ 2, 4, 10 og 11",
            hentHjemmeltekst(
                utvidetVedtaksperioderMedBegrunnelser.map { it.tilMinimertVedtaksperiode(hentSanityBegrunnelser()) },
                listOf(
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                        hjemler = listOf("11", "4", "2", "10"),
                    ),
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                        hjemler = listOf("10"),
                    )
                )
            )
        )
    }

    @Test
    fun `hentHjemmeltekst skal ikke inkludere hjemmel 17 og 18 hvis opplysningsplikt er oppfylt`() {
        val utvidetVedtaksperioderMedBegrunnelser = listOf(
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagRestVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            ),
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagRestVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            )
        )

        Assertions.assertEquals(
            "§§ 2, 4, 10 og 11",
            hentHjemmeltekst(
                minimerteVedtaksperioder = utvidetVedtaksperioderMedBegrunnelser.map {
                    it.tilMinimertVedtaksperiode(
                        hentSanityBegrunnelser()
                    )
                },
                sanityBegrunnelser = listOf(
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                        hjemler = listOf("11", "4", "2", "10"),
                    ),
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                        hjemler = listOf("10"),
                    )
                ),
                opplysningspliktHjemlerSkalMedIBrev = false
            )
        )
    }

    @Test
    fun `hentHjemmeltekst skal inkludere hjemmel 17 og 18 hvis opplysningsplikt ikke er oppfylt`() {
        val utvidetVedtaksperioderMedBegrunnelser = listOf(
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagRestVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            ),
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagRestVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            )
        )

        Assertions.assertEquals(
            "§§ 2, 4, 10, 11, 17 og 18",
            hentHjemmeltekst(
                minimerteVedtaksperioder = utvidetVedtaksperioderMedBegrunnelser.map {
                    it.tilMinimertVedtaksperiode(
                        hentSanityBegrunnelser()
                    )
                },
                sanityBegrunnelser = listOf(
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                        hjemler = listOf("11", "4", "2", "10"),
                    ),
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                        hjemler = listOf("10"),
                    )
                ),
                opplysningspliktHjemlerSkalMedIBrev = true
            )
        )
    }

    @Test
    fun `Skal gi riktig dato for opphørstester`() {
        val sisteFom = LocalDate.now().minusMonths(2)

        val opphørsperioder = listOf(
            Opphørsperiode(
                periodeFom = LocalDate.now().minusYears(1),
                periodeTom = LocalDate.now().minusYears(1).plusMonths(2)
            ),
            Opphørsperiode(
                periodeFom = LocalDate.now().minusMonths(5),
                periodeTom = LocalDate.now().minusMonths(4)
            ),
            Opphørsperiode(
                periodeFom = sisteFom,
                periodeTom = LocalDate.now()
            ),
        )

        Assertions.assertEquals(sisteFom.tilMånedÅr(), hentVirkningstidspunkt(opphørsperioder, 0L))
    }
}
