package no.nav.familie.ba.sak.kjerne.brev

import io.mockk.mockk
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagSanityBegrunnelse
import no.nav.familie.ba.sak.common.lagSanityEøsBegrunnelse
import no.nav.familie.ba.sak.common.lagUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.common.lagUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.dataGenerator.vedtak.lagVedtaksbegrunnelse
import no.nav.familie.ba.sak.integrasjoner.sanity.hentBegrunnelser
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.EØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Opphørsperiode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class BrevUtilsTest {

    private val støttedeBehandlingsersultaterFørstegangsbehandling = listOf(
        Behandlingsresultat.INNVILGET,
        Behandlingsresultat.INNVILGET_OG_OPPHØRT,
        Behandlingsresultat.DELVIS_INNVILGET,
        Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT,
        Behandlingsresultat.AVSLÅTT
    )

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for innvilget førstegangsbehandling`() {
        støttedeBehandlingsersultaterFørstegangsbehandling.filterNot { it == Behandlingsresultat.AVSLÅTT }.forEach {
            Assertions.assertEquals(
                Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
                hentManuellVedtaksbrevtypeGammel(
                    BehandlingType.FØRSTEGANGSBEHANDLING,
                    it
                )
            )
        }
    }

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for innvilget førstegangsbehandling og institusjon`() {
        støttedeBehandlingsersultaterFørstegangsbehandling.filterNot { it == Behandlingsresultat.AVSLÅTT }.forEach {
            Assertions.assertEquals(
                Brevmal.VEDTAK_FØRSTEGANGSVEDTAK_INSTITUSJON,
                hentManuellVedtaksbrevtypeGammel(
                    BehandlingType.FØRSTEGANGSBEHANDLING,
                    it,
                    true
                )
            )
        }
    }

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for avslått førstegangsbehandling`() {
        Assertions.assertEquals(
            Brevmal.VEDTAK_AVSLAG,
            hentManuellVedtaksbrevtypeGammel(
                BehandlingType.FØRSTEGANGSBEHANDLING,
                Behandlingsresultat.AVSLÅTT
            )
        )
    }

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for avslått førstegangsbehandling og institusjon`() {
        Assertions.assertEquals(
            Brevmal.VEDTAK_AVSLAG_INSTITUSJON,
            hentManuellVedtaksbrevtypeGammel(
                BehandlingType.FØRSTEGANGSBEHANDLING,
                Behandlingsresultat.AVSLÅTT,
                true
            )
        )
    }

    @Test
    fun `skal kaste exception for ikke-støttede behandlingsresultater ved førstegangsbehandling`() {
        val ikkeStøttedeBehandlingsersultater =
            Behandlingsresultat.values().subtract(støttedeBehandlingsersultaterFørstegangsbehandling)

        ikkeStøttedeBehandlingsersultater.forEach {
            assertThrows<Exception> {
                hentManuellVedtaksbrevtypeGammel(
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
                hentManuellVedtaksbrevtypeGammel(
                    BehandlingType.REVURDERING,
                    it
                )
            )
        }
    }

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for 'Vedtak endring' og institusjon`() {
        behandlingsersultaterForVedtakEndring.forEach {
            Assertions.assertEquals(
                Brevmal.VEDTAK_ENDRING_INSTITUSJON,
                hentManuellVedtaksbrevtypeGammel(
                    BehandlingType.REVURDERING,
                    it,
                    true
                )
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
                hentManuellVedtaksbrevtypeGammel(
                    BehandlingType.REVURDERING,
                    it
                )
            )
        }
    }

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for 'Opphørt' og institusjon`() {
        behandlingsersultaterForOpphørt.forEach {
            Assertions.assertEquals(
                Brevmal.VEDTAK_OPPHØRT_INSTITUSJON,
                hentManuellVedtaksbrevtypeGammel(
                    BehandlingType.REVURDERING,
                    it,
                    true
                )
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
        Behandlingsresultat.ENDRET_OG_OPPHØRT
    )

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for 'Opphør med endring'`() {
        behandlingsersultaterForOpphørMedEndring.forEach {
            Assertions.assertEquals(
                Brevmal.VEDTAK_OPPHØR_MED_ENDRING,
                hentManuellVedtaksbrevtypeGammel(
                    BehandlingType.REVURDERING,
                    it
                )
            )
        }
    }

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for 'Opphør med endring' og institusjon`() {
        behandlingsersultaterForOpphørMedEndring.forEach {
            Assertions.assertEquals(
                Brevmal.VEDTAK_OPPHØR_MED_ENDRING_INSTITUSJON,
                hentManuellVedtaksbrevtypeGammel(
                    BehandlingType.REVURDERING,
                    it,
                    true
                )
            )
        }
    }

    private val behandlingsersultaterForFortsattInnvilget = listOf(Behandlingsresultat.FORTSATT_INNVILGET, Behandlingsresultat.ENDRET_OG_FORTSATT_INNVILGET)

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for 'Fortsatt innvilget'`() {
        behandlingsersultaterForFortsattInnvilget.forEach {
            Assertions.assertEquals(
                Brevmal.VEDTAK_FORTSATT_INNVILGET,
                hentManuellVedtaksbrevtypeGammel(
                    BehandlingType.REVURDERING,
                    it
                )
            )
        }
    }

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for 'Fortsatt innvilget' og institusjon`() {
        behandlingsersultaterForFortsattInnvilget.forEach {
            Assertions.assertEquals(
                Brevmal.VEDTAK_FORTSATT_INNVILGET_INSTITUSJON,
                hentManuellVedtaksbrevtypeGammel(
                    BehandlingType.REVURDERING,
                    it,
                    true
                )
            )
        }
    }

    private val behandlingsersultaterForAvslag = listOf(Behandlingsresultat.AVSLÅTT)

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for 'Avslag'`() {
        behandlingsersultaterForAvslag.forEach {
            Assertions.assertEquals(
                Brevmal.VEDTAK_AVSLAG,
                hentManuellVedtaksbrevtypeGammel(
                    BehandlingType.REVURDERING,
                    it
                )
            )
        }
    }

    @Test
    fun `test hentManuellVedtaksbrevtype gir riktig vedtaksbrevtype for 'Avslag' og institusjon`() {
        behandlingsersultaterForAvslag.forEach {
            Assertions.assertEquals(
                Brevmal.VEDTAK_AVSLAG_INSTITUSJON,
                hentManuellVedtaksbrevtypeGammel(
                    BehandlingType.REVURDERING,
                    it,
                    true
                )
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
                hentManuellVedtaksbrevtypeGammel(
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
            hentBrevmalGammel(behandling)
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
            hentBrevmalGammel(behandling)
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
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET,
                        vedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser()
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            ),
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING,
                        vedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser()
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            )
        )

        Assertions.assertEquals(
            "barnetrygdloven §§ 2, 4, 10 og 11",
            hentHjemmeltekst(
                målform = Målform.NB,
                minimerteVedtaksperioder = utvidetVedtaksperioderMedBegrunnelser.map {
                    it.tilMinimertVedtaksperiode(
                        hentBegrunnelser(),
                        emptyList()
                    )
                },
                sanityBegrunnelser = listOf(
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                        hjemler = listOf("11", "4", "2", "10")
                    ),
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                        hjemler = listOf("10")
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
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            ),
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            )
        )

        Assertions.assertEquals(
            "barnetrygdloven §§ 2, 4, 10 og 11",
            hentHjemmeltekst(
                målform = Målform.NB,
                minimerteVedtaksperioder = utvidetVedtaksperioderMedBegrunnelser.map {
                    it.tilMinimertVedtaksperiode(
                        sanityBegrunnelser = hentBegrunnelser(),
                        sanityEØSBegrunnelser = emptyList()
                    )
                },
                sanityBegrunnelser = listOf(
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                        hjemler = listOf("11", "4", "2", "10")
                    ),
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                        hjemler = listOf("10")
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
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            ),
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            )
        )

        Assertions.assertEquals(
            "barnetrygdloven §§ 2, 4, 10, 11, 17 og 18",
            hentHjemmeltekst(
                målform = Målform.NB,
                minimerteVedtaksperioder = utvidetVedtaksperioderMedBegrunnelser.map {
                    it.tilMinimertVedtaksperiode(
                        sanityBegrunnelser = hentBegrunnelser(),
                        sanityEØSBegrunnelser = emptyList()
                    )
                },
                sanityBegrunnelser = listOf(
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                        hjemler = listOf("11", "4", "2", "10")
                    ),
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                        hjemler = listOf("10")
                    )
                ),
                opplysningspliktHjemlerSkalMedIBrev = true
            )
        )
    }

    @Test
    fun `Skal gi riktig hjemmeltekst ved hjemler både fra barnetrygdloven og folketrygdloven`() {
        val utvidetVedtaksperioderMedBegrunnelser = listOf(
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_SØKER_OG_BARN_FRIVILLIG_MEDLEM
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            ),
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            )
        )

        val sanityBegrunnelser = listOf(
            lagSanityBegrunnelse(
                apiNavn = Standardbegrunnelse.INNVILGET_SØKER_OG_BARN_FRIVILLIG_MEDLEM.sanityApiNavn,
                hjemler = listOf("11", "4"),
                hjemlerFolketrygdloven = listOf("2-5", "2-8")
            ),
            lagSanityBegrunnelse(
                apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                hjemler = listOf("10")
            )
        )

        Assertions.assertEquals(
            "barnetrygdloven §§ 4, 10 og 11 og folketrygdloven §§ 2-5 og 2-8",
            hentHjemmeltekst(
                målform = Målform.NB,
                minimerteVedtaksperioder = utvidetVedtaksperioderMedBegrunnelser.map {
                    it.tilMinimertVedtaksperiode(
                        sanityBegrunnelser = sanityBegrunnelser,
                        sanityEØSBegrunnelser = emptyList()
                    )
                },
                sanityBegrunnelser = sanityBegrunnelser,
                opplysningspliktHjemlerSkalMedIBrev = false
            )
        )
    }

    @Test
    fun `Skal gi riktig formattering ved hjemler fra barnetrygdloven og 2 EØS-forordninger`() {
        val utvidetVedtaksperioderMedBegrunnelser = listOf(
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET
                    )
                ),
                eøsBegrunnelser = listOf(
                    EØSBegrunnelse(
                        vedtaksperiodeMedBegrunnelser = mockk(),
                        begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            ),
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING
                    )
                ),
                eøsBegrunnelser = listOf(
                    EØSBegrunnelse(
                        vedtaksperiodeMedBegrunnelser = mockk(),
                        begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            )
        )

        val sanityBegrunnelser = listOf(
            lagSanityBegrunnelse(
                apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                hjemler = listOf("11", "4")
            ),
            lagSanityBegrunnelse(
                apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                hjemler = listOf("10")
            )
        )

        val sanityEøsBegrunnelser = listOf(
            lagSanityEøsBegrunnelse(
                apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR.sanityApiNavn,
                hjemler = listOf("4"),
                hjemlerEØSForordningen883 = listOf("11-16")
            ),
            lagSanityEøsBegrunnelse(
                apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE.sanityApiNavn,
                hjemler = listOf("11"),
                hjemlerEØSForordningen987 = listOf("58", "60")
            )
        )

        Assertions.assertEquals(
            "barnetrygdloven §§ 4, 10 og 11, EØS-forordning 883/2004 artikkel 11-16 og EØS-forordning 987/2009 artikkel 58 og 60",
            hentHjemmeltekst(
                målform = Målform.NB,
                minimerteVedtaksperioder = utvidetVedtaksperioderMedBegrunnelser.map {
                    it.tilMinimertVedtaksperiode(
                        sanityBegrunnelser = sanityBegrunnelser,
                        sanityEØSBegrunnelser = sanityEøsBegrunnelser
                    )
                },
                sanityBegrunnelser = sanityBegrunnelser,
                opplysningspliktHjemlerSkalMedIBrev = false
            )
        )
    }

    @Test
    fun `Skal gi riktig formattering ved hjemler fra Separasjonsavtale og to EØS-forordninger`() {
        val utvidetVedtaksperioderMedBegrunnelser = listOf(
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET
                    )
                ),
                eøsBegrunnelser = listOf(
                    EØSBegrunnelse(
                        vedtaksperiodeMedBegrunnelser = mockk(),
                        begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            ),
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING
                    )
                ),
                eøsBegrunnelser = listOf(
                    EØSBegrunnelse(
                        vedtaksperiodeMedBegrunnelser = mockk(),
                        begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            )
        )

        val sanityBegrunnelser = listOf(
            lagSanityBegrunnelse(
                apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                hjemler = listOf("11", "4")
            ),
            lagSanityBegrunnelse(
                apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                hjemler = listOf("10")
            )
        )

        val sanityEøsBegrunnelser = listOf(
            lagSanityEøsBegrunnelse(
                apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR.sanityApiNavn,
                hjemler = listOf("4"),
                hjemlerEØSForordningen883 = listOf("11-16"),
                hjemlerSeperasjonsavtalenStorbritannina = listOf("29")
            ),
            lagSanityEøsBegrunnelse(
                apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE.sanityApiNavn,
                hjemler = listOf("11"),
                hjemlerEØSForordningen987 = listOf("58", "60")
            )
        )

        Assertions.assertEquals(
            "Separasjonsavtalen mellom Storbritannia og Norge artikkel 29, barnetrygdloven §§ 4, 10 og 11, EØS-forordning 883/2004 artikkel 11-16 og EØS-forordning 987/2009 artikkel 58 og 60",
            hentHjemmeltekst(
                målform = Målform.NB,
                minimerteVedtaksperioder = utvidetVedtaksperioderMedBegrunnelser.map {
                    it.tilMinimertVedtaksperiode(
                        sanityBegrunnelser = sanityBegrunnelser,
                        sanityEØSBegrunnelser = sanityEøsBegrunnelser
                    )
                },
                sanityBegrunnelser = sanityBegrunnelser,
                opplysningspliktHjemlerSkalMedIBrev = false
            )
        )
    }

    @Test
    fun `Skal gi riktig formattering ved nynorsk og hjemler fra Separasjonsavtale og to EØS-forordninger`() {
        val utvidetVedtaksperioderMedBegrunnelser = listOf(
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET
                    )
                ),
                eøsBegrunnelser = listOf(
                    EØSBegrunnelse(
                        vedtaksperiodeMedBegrunnelser = mockk(),
                        begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            ),
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING
                    )
                ),
                eøsBegrunnelser = listOf(
                    EØSBegrunnelse(
                        vedtaksperiodeMedBegrunnelser = mockk(),
                        begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            )
        )

        val sanityBegrunnelser = listOf(
            lagSanityBegrunnelse(
                apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                hjemler = listOf("11", "4")
            ),
            lagSanityBegrunnelse(
                apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                hjemler = listOf("10")
            )
        )

        val sanityEøsBegrunnelser = listOf(
            lagSanityEøsBegrunnelse(
                apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR.sanityApiNavn,
                hjemler = listOf("4"),
                hjemlerEØSForordningen883 = listOf("11-16"),
                hjemlerSeperasjonsavtalenStorbritannina = listOf("29")
            ),
            lagSanityEøsBegrunnelse(
                apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE.sanityApiNavn,
                hjemler = listOf("11"),
                hjemlerEØSForordningen987 = listOf("58", "60")
            )
        )

        Assertions.assertEquals(
            "Separasjonsavtalen mellom Storbritannia og Noreg artikkel 29, barnetrygdlova §§ 4, 10 og 11, EØS-forordning 883/2004 artikkel 11-16 og EØS-forordning 987/2009 artikkel 58 og 60",
            hentHjemmeltekst(
                målform = Målform.NN,
                minimerteVedtaksperioder = utvidetVedtaksperioderMedBegrunnelser.map {
                    it.tilMinimertVedtaksperiode(
                        sanityBegrunnelser = sanityBegrunnelser,
                        sanityEØSBegrunnelser = sanityEøsBegrunnelser
                    )
                },
                sanityBegrunnelser = sanityBegrunnelser,
                opplysningspliktHjemlerSkalMedIBrev = false
            )
        )
    }

    @Test
    fun `Skal slå sammen hjemlene riktig når det er 3 eller flere hjemler på "siste" hjemmeltype`() {
        val utvidetVedtaksperioderMedBegrunnelser = listOf(
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET
                    )
                ),
                eøsBegrunnelser = listOf(
                    EØSBegrunnelse(
                        vedtaksperiodeMedBegrunnelser = mockk(),
                        begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            ),
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING
                    )
                ),
                eøsBegrunnelser = listOf(
                    EØSBegrunnelse(
                        vedtaksperiodeMedBegrunnelser = mockk(),
                        begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            )
        )

        val sanityBegrunnelser = listOf(
            lagSanityBegrunnelse(
                apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                hjemler = listOf("11", "4")
            ),
            lagSanityBegrunnelse(
                apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                hjemler = listOf("10")
            )
        )

        val sanityEøsBegrunnelser = listOf(
            lagSanityEøsBegrunnelse(
                apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR.sanityApiNavn,
                hjemler = listOf("4"),
                hjemlerEØSForordningen883 = listOf("2", "11-16", "67", "68"),
                hjemlerSeperasjonsavtalenStorbritannina = listOf("29")
            ),
            lagSanityEøsBegrunnelse(
                apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE.sanityApiNavn,
                hjemler = listOf("11")
            )
        )

        Assertions.assertEquals(
            "Separasjonsavtalen mellom Storbritannia og Noreg artikkel 29, barnetrygdlova §§ 4, 10 og 11 og EØS-forordning 883/2004 artikkel 2, 11-16, 67 og 68",
            hentHjemmeltekst(
                målform = Målform.NN,
                minimerteVedtaksperioder = utvidetVedtaksperioderMedBegrunnelser.map {
                    it.tilMinimertVedtaksperiode(
                        sanityBegrunnelser = sanityBegrunnelser,
                        sanityEØSBegrunnelser = sanityEøsBegrunnelser
                    )
                },
                sanityBegrunnelser = sanityBegrunnelser,
                opplysningspliktHjemlerSkalMedIBrev = false
            )
        )
    }

    @Test
    fun `Skal kun ta med en hjemmel 1 gang hvis flere begrunnelser er knyttet til samme hjemmel`() {
        val utvidetVedtaksperioderMedBegrunnelser = listOf(
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET
                    )
                ),
                eøsBegrunnelser = listOf(
                    EØSBegrunnelse(
                        vedtaksperiodeMedBegrunnelser = mockk(),
                        begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            ),
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING
                    )
                ),
                eøsBegrunnelser = listOf(
                    EØSBegrunnelse(
                        vedtaksperiodeMedBegrunnelser = mockk(),
                        begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE
                    )
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
            )
        )

        val sanityBegrunnelser = listOf(
            lagSanityBegrunnelse(
                apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                hjemler = listOf("11", "4")
            ),
            lagSanityBegrunnelse(
                apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                hjemler = listOf("10")
            )
        )

        val sanityEøsBegrunnelser = listOf(
            lagSanityEøsBegrunnelse(
                apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR.sanityApiNavn,
                hjemler = listOf("4"),
                hjemlerEØSForordningen883 = listOf("2", "11-16", "67", "68"),
                hjemlerSeperasjonsavtalenStorbritannina = listOf("29"),
                hjemlerEØSForordningen987 = listOf("58")
            ),
            lagSanityEøsBegrunnelse(
                apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE.sanityApiNavn,
                hjemler = listOf("11"),
                hjemlerEØSForordningen883 = listOf("2", "67", "68"),
                hjemlerSeperasjonsavtalenStorbritannina = listOf("29"),
                hjemlerEØSForordningen987 = listOf("58")

            )
        )

        Assertions.assertEquals(
            "Separasjonsavtalen mellom Storbritannia og Noreg artikkel 29, barnetrygdlova §§ 4, 10 og 11, EØS-forordning 883/2004 artikkel 2, 11-16, 67 og 68 og EØS-forordning 987/2009 artikkel 58",
            hentHjemmeltekst(
                målform = Målform.NN,
                minimerteVedtaksperioder = utvidetVedtaksperioderMedBegrunnelser.map {
                    it.tilMinimertVedtaksperiode(
                        sanityBegrunnelser = sanityBegrunnelser,
                        sanityEØSBegrunnelser = sanityEøsBegrunnelser
                    )
                },
                sanityBegrunnelser = sanityBegrunnelser,
                opplysningspliktHjemlerSkalMedIBrev = false
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
            )
        )

        Assertions.assertEquals(sisteFom.tilMånedÅr(), hentVirkningstidspunkt(opphørsperioder, 0L))
    }
}
