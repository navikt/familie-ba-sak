package no.nav.familie.ba.sak.kjerne.dokument

import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.hentSanityBegrunnelser
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagMinimertUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.common.lagRestVedtaksbegrunnelse
import no.nav.familie.ba.sak.common.lagSanityBegrunnelse
import no.nav.familie.ba.sak.common.lagUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.common.lagUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.tilfeldigSøker
import no.nav.familie.ba.sak.dataGenerator.lagBrevBegrunnelseGrunnlagMedPersoner
import no.nav.familie.ba.sak.dataGenerator.lagBrevPeriodeGrunnlagMedPersoner
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPerson
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.EndretUtbetalingBrevPeriodeType
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.brevperioder.EndretUtbetalingBarnetrygdType
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.flettefelt
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilBegrunnelsePerson
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilMinimertPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.tilBrevPeriodeGrunnlag
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

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

        assertEquals("Mock Saksbehandler", saksbehandler)
        assertEquals("System", beslutter)
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

        assertEquals("System", saksbehandler)
        assertEquals("Beslutter", beslutter)
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

        assertEquals("Mock Saksbehandler", saksbehandler)
        assertEquals("Mock Beslutter", beslutter)
    }

    private val støttedeBehandlingsersultaterFørstegangsbehandling = listOf(
        BehandlingResultat.INNVILGET,
        BehandlingResultat.INNVILGET_OG_OPPHØRT,
        BehandlingResultat.DELVIS_INNVILGET,
        BehandlingResultat.DELVIS_INNVILGET_OG_OPPHØRT,
        BehandlingResultat.AVSLÅTT,
    )

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for innvilget førstegangsbehandling`() {

        støttedeBehandlingsersultaterFørstegangsbehandling.filterNot { it == BehandlingResultat.AVSLÅTT }.forEach {
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
                BehandlingResultat.AVSLÅTT
            ),
        )
    }

    @Test
    fun `skal kaste exception for ikke-støttede behandlingsresultater ved førstegangsbehandling`() {
        val ikkeStøttedeBehandlingsersultater =
            BehandlingResultat.values().subtract(støttedeBehandlingsersultaterFørstegangsbehandling)

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
        BehandlingResultat.INNVILGET,
        BehandlingResultat.INNVILGET_OG_ENDRET,
        BehandlingResultat.DELVIS_INNVILGET,
        BehandlingResultat.DELVIS_INNVILGET_OG_ENDRET,
        BehandlingResultat.AVSLÅTT_OG_ENDRET,
        BehandlingResultat.ENDRET
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

    private val behandlingsersultaterForOpphørt = listOf(BehandlingResultat.OPPHØRT)

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
        BehandlingResultat.INNVILGET_OG_OPPHØRT,
        BehandlingResultat.INNVILGET_ENDRET_OG_OPPHØRT,
        BehandlingResultat.DELVIS_INNVILGET_OG_OPPHØRT,
        BehandlingResultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT,
        BehandlingResultat.AVSLÅTT_OG_OPPHØRT,
        BehandlingResultat.AVSLÅTT_ENDRET_OG_OPPHØRT,
        BehandlingResultat.ENDRET_OG_OPPHØRT,
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

    private val behandlingsersultaterForFortsattInnvilget = listOf(BehandlingResultat.FORTSATT_INNVILGET)

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

    private val behandlingsersultaterForAvslag = listOf(BehandlingResultat.AVSLÅTT)

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
            BehandlingResultat.values()
                .subtract(behandlingsersultaterForVedtakEndring)
                .subtract(behandlingsersultaterForOpphørt)
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
            lagBehandling(fagsak = fagsak, automatiskOpprettelse = true, årsak = BehandlingÅrsak.FØDSELSHENDELSE).copy(
                resultat = BehandlingResultat.INNVILGET
            )
        Assertions.assertEquals(
            Brevmal.AUTOVEDTAK_NYFØDT_BARN_FRA_FØR,
            hentBrevtype(behandling)
        )
    }

    @Test
    fun `test hentAutomatiskVedtaksbrevtype gir riktig vedtaksbrevtype for førstegangsbehandling nyfødt barn`() {
        val fagsak = defaultFagsak()
        val behandling =
            lagBehandling(fagsak = fagsak, automatiskOpprettelse = true, årsak = BehandlingÅrsak.FØDSELSHENDELSE).copy(
                resultat = BehandlingResultat.INNVILGET
            )
        Assertions.assertEquals(
            Brevmal.AUTOVEDTAK_NYFØDT_FØRSTE_BARN,
            hentBrevtype(behandling)
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
            hentOverstyrtDokumenttittel(revurdering.copy(resultat = BehandlingResultat.INNVILGET_OG_ENDRET))
        )
        Assertions.assertEquals(
            "Vedtak om fortsatt barnetrygd",
            hentOverstyrtDokumenttittel(revurdering.copy(resultat = BehandlingResultat.FORTSATT_INNVILGET))
        )
        assertNull(hentOverstyrtDokumenttittel(revurdering.copy(resultat = BehandlingResultat.OPPHØRT)))
    }

    @Test
    fun `hentHjemmeltekst skal returnere sorterte hjemler`() {
        val utvidetVedtaksperioderMedBegrunnelser = listOf(
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagRestVedtaksbegrunnelse(
                        vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            ),
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagRestVedtaksbegrunnelse(
                        vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.INNVILGET_SATSENDRING
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            )
        )

        Assertions.assertEquals(
            "§§ 2, 4, 10 og 11",
            hentHjemmeltekst(
                utvidetVedtaksperioderMedBegrunnelser.map { it.tilBrevPeriodeGrunnlag(hentSanityBegrunnelser()) },
                listOf(
                    lagSanityBegrunnelse(
                        apiNavn = VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                        hjemler = listOf("11", "4", "2", "10"),
                    ),
                    lagSanityBegrunnelse(
                        apiNavn = VedtakBegrunnelseSpesifikasjon.INNVILGET_SATSENDRING.sanityApiNavn,
                        hjemler = listOf("10"),
                    )
                )
            )
        )
    }

    @Test
    fun `hentEndretUtbetalingBrevPeriode skal gi riktig periodetype`() {
        val utvidetVedtaksperiodMedBegrunnelserFullUtbetaling =
            lagBrevPeriodeGrunnlagMedPersoner(
                minimertUtbetalingsperiodeDetalj = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        prosent = BigDecimal.valueOf(100)
                    ),
                    lagMinimertUtbetalingsperiodeDetalj(
                        prosent = BigDecimal.valueOf(100)
                    )
                )
            )
        val utvidetVedtaksperiodMedBegrunnelserForskjelligUtbetaling =
            lagBrevPeriodeGrunnlagMedPersoner(
                minimertUtbetalingsperiodeDetalj = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        prosent = BigDecimal.valueOf(100)
                    ),
                    lagMinimertUtbetalingsperiodeDetalj(
                        prosent = BigDecimal.ZERO
                    )
                )

            )
        val utvidetVedtaksperiodMedBegrunnelserIngenUtbetaling =
            lagBrevPeriodeGrunnlagMedPersoner(
                minimertUtbetalingsperiodeDetalj = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        prosent = BigDecimal.ZERO
                    ),
                    lagMinimertUtbetalingsperiodeDetalj(
                        prosent = BigDecimal.ZERO
                    )
                )
            )

        Assertions.assertEquals(
            flettefelt(EndretUtbetalingBrevPeriodeType.ENDRET_UTBETALINGSPERIODE.apiNavn),
            utvidetVedtaksperiodMedBegrunnelserFullUtbetaling
                .hentEndretUtbetalingBrevPeriode("", emptyList(), UtvidetScenario.IKKE_UTVIDET_YTELSE).type
        )

        Assertions.assertEquals(
            flettefelt(EndretUtbetalingBrevPeriodeType.ENDRET_UTBETALINGSPERIODE.apiNavn),
            utvidetVedtaksperiodMedBegrunnelserForskjelligUtbetaling
                .hentEndretUtbetalingBrevPeriode("", emptyList(), UtvidetScenario.IKKE_UTVIDET_YTELSE).type
        )

        Assertions.assertEquals(
            flettefelt(EndretUtbetalingBrevPeriodeType.ENDRET_UTBETALINGSPERIODE_INGEN_UTBETALING.apiNavn),
            utvidetVedtaksperiodMedBegrunnelserIngenUtbetaling
                .hentEndretUtbetalingBrevPeriode("", emptyList(), UtvidetScenario.IKKE_UTVIDET_YTELSE).type
        )
    }

    @Test
    fun `skal gi riktig brevperiode for endret utbetaling for forskjellige utvidet scenarioer`() {

        val utvidetVedtaksperiodeMedBegrunnelserIngenUtbetaling =
            lagBrevPeriodeGrunnlagMedPersoner(
                type = Vedtaksperiodetype.ENDRET_UTBETALING,
                minimertUtbetalingsperiodeDetalj = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(prosent = BigDecimal.ZERO)
                )
            )
        val utvidetVedtaksperiodeMedBegrunnelserFullUtbetaling =
            lagBrevPeriodeGrunnlagMedPersoner(
                type = Vedtaksperiodetype.ENDRET_UTBETALING,
                minimertUtbetalingsperiodeDetalj = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(prosent = BigDecimal.valueOf(100))
                )
            )

        val begrunnelser = listOf(
            UtvidetScenario.IKKE_UTVIDET_YTELSE,
            UtvidetScenario.UTVIDET_YTELSE_ENDRET,
            UtvidetScenario.UTVIDET_YTELSE_IKKE_ENDRET
        ).map {
            utvidetVedtaksperiodeMedBegrunnelserIngenUtbetaling.hentEndretUtbetalingBrevPeriode(
                "",
                emptyList(),
                utvidetScenario = it
            )
        }

        Assertions.assertEquals(
            EndretUtbetalingBrevPeriodeType.ENDRET_UTBETALINGSPERIODE_INGEN_UTBETALING.apiNavn,
            begrunnelser[0].type?.single()
        )
        Assertions.assertEquals(
            EndretUtbetalingBarnetrygdType.DELT.navn + " ",
            begrunnelser[0].typeBarnetrygd?.single()
        )

        Assertions.assertEquals(
            EndretUtbetalingBrevPeriodeType.ENDRET_UTBETALINGSPERIODE_INGEN_UTBETALING.apiNavn,
            begrunnelser[1].type?.single()
        )
        Assertions.assertEquals(
            EndretUtbetalingBarnetrygdType.DELT_UTVIDET_NB.navn + " ",
            begrunnelser[1].typeBarnetrygd?.single()
        )

        Assertions.assertEquals(
            EndretUtbetalingBrevPeriodeType.ENDRET_UTBETALINGSPERIODE_DELVIS_UTBETALING.apiNavn,
            begrunnelser[2].type?.single()
        )
        Assertions.assertEquals(
            EndretUtbetalingBarnetrygdType.DELT_UTVIDET_NB.navn + " ",
            begrunnelser[2].typeBarnetrygd?.single()
        )

        val deltBostedEndringFullUtbetalingTilkjentYtelseUtenEndring =
            utvidetVedtaksperiodeMedBegrunnelserFullUtbetaling.hentEndretUtbetalingBrevPeriode(
                "",
                emptyList(),
                utvidetScenario = UtvidetScenario.UTVIDET_YTELSE_IKKE_ENDRET
            )

        Assertions.assertEquals(
            EndretUtbetalingBrevPeriodeType.ENDRET_UTBETALINGSPERIODE.apiNavn,
            deltBostedEndringFullUtbetalingTilkjentYtelseUtenEndring.type?.single()
        )
        Assertions.assertEquals(
            EndretUtbetalingBarnetrygdType.DELT_UTVIDET_NB.navn + " ",
            deltBostedEndringFullUtbetalingTilkjentYtelseUtenEndring.typeBarnetrygd?.single()
        )
    }

    @Test
    fun `skal legge til barn med utbetalinger og fra alle utbetalingsbegrunnelsene i brev-utbetalings-periodene`() {
        val søker = tilfeldigSøker()
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)
        val barn2 = tilfeldigPerson(personType = PersonType.BARN)
        val barn3 = tilfeldigPerson(personType = PersonType.BARN)

        val brevPeriodeGrunnlagMedPersoner = lagBrevPeriodeGrunnlagMedPersoner(
            type = Vedtaksperiodetype.UTBETALING,
            minimertUtbetalingsperiodeDetalj = listOf(
                lagMinimertUtbetalingsperiodeDetalj(barn1.tilRestPerson().tilMinimertPerson()),
                lagMinimertUtbetalingsperiodeDetalj(søker.tilRestPerson().tilMinimertPerson()),
            ),
            begrunnelser = listOf(
                lagBrevBegrunnelseGrunnlagMedPersoner(
                    vedtakBegrunnelseType = VedtakBegrunnelseType.REDUKSJON,
                    personIdenter = listOf(
                        barn3.aktør.aktivFødselsnummer()
                    ),
                ),
                lagBrevBegrunnelseGrunnlagMedPersoner(
                    vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET,
                    personIdenter = listOf(
                        barn2.aktør.aktivFødselsnummer()
                    ),
                ),
            )
        )

        val personerIPersongrunnlag = listOf(barn1, barn2, barn3, søker).map { it.tilBegrunnelsePerson() }

        val barnIPeriode = brevPeriodeGrunnlagMedPersoner.finnBarnIInnvilgelsePeriode(personerIPersongrunnlag)

        Assertions.assertEquals(2, barnIPeriode.size)
        Assertions.assertTrue(
            barnIPeriode.any { it.personIdent == barn1.personIdent.ident } &&
                barnIPeriode.any { it.personIdent == barn2.personIdent.ident }
        )
    }
}
