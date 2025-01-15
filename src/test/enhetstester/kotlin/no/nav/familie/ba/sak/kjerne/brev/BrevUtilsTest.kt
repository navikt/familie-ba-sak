package no.nav.familie.ba.sak.kjerne.brev

import io.mockk.mockk
import lagAndelTilkjentYtelse
import lagBehandling
import lagEndretUtbetalingAndel
import lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagSanityBegrunnelse
import no.nav.familie.ba.sak.common.lagSanityEøsBegrunnelse
import no.nav.familie.ba.sak.common.rangeTo
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.common.tilMånedÅrMedium
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.vedtak.lagVedtaksbegrunnelse
import no.nav.familie.ba.sak.integrasjoner.økonomi.sats
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.lagKompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.lagValutakurs
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilMånedTidspunkt
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.EØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksbegrunnelseFritekst
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import randomAktør
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

internal class BrevUtilsTest {
    @Test
    fun `hent dokumenttittel dersom denne skal overstyres for behandlingen`() {
        assertNull(hentOverstyrtDokumenttittel(lagBehandling().copy(type = BehandlingType.FØRSTEGANGSBEHANDLING)))
        val revurdering = lagBehandling().copy(type = BehandlingType.REVURDERING)
        assertNull(hentOverstyrtDokumenttittel(revurdering))

        Assertions.assertEquals(
            "Vedtak om endret barnetrygd - barn 18 år",
            hentOverstyrtDokumenttittel(revurdering.copy(opprettetÅrsak = BehandlingÅrsak.OMREGNING_18ÅR)),
        )
        Assertions.assertEquals(
            "Vedtak om endret barnetrygd",
            hentOverstyrtDokumenttittel(revurdering.copy(resultat = Behandlingsresultat.INNVILGET_OG_ENDRET)),
        )
        Assertions.assertEquals(
            "Vedtak om fortsatt barnetrygd",
            hentOverstyrtDokumenttittel(revurdering.copy(resultat = Behandlingsresultat.FORTSATT_INNVILGET)),
        )
        assertNull(hentOverstyrtDokumenttittel(revurdering.copy(resultat = Behandlingsresultat.OPPHØRT)))
    }

    @Test
    fun `hentHjemmeltekst skal returnere sorterte hjemler`() {
        val vedtaksperioderMedBegrunnelser =
            listOf(
                lagVedtaksperiodeMedBegrunnelser(
                    begrunnelser =
                        mutableSetOf(
                            lagVedtaksbegrunnelse(
                                standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET,
                                vedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(),
                            ),
                        ),
                ),
                lagVedtaksperiodeMedBegrunnelser(
                    begrunnelser =
                        mutableSetOf(
                            lagVedtaksbegrunnelse(
                                standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING,
                                vedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(),
                            ),
                        ),
                ),
            )

        Assertions.assertEquals(
            "barnetrygdloven §§ 2, 4, 10 og 11",
            hentHjemmeltekst(
                vedtaksperioder = vedtaksperioderMedBegrunnelser,
                standardbegrunnelseTilSanityBegrunnelse =
                    mapOf(
                        Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET to
                            lagSanityBegrunnelse(
                                apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                                hjemler = listOf("11", "4", "2", "10"),
                            ),
                        Standardbegrunnelse.INNVILGET_SATSENDRING to
                            lagSanityBegrunnelse(
                                apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                                hjemler = listOf("10"),
                            ),
                    ),
                eøsStandardbegrunnelseTilSanityBegrunnelse = emptyMap(),
                målform = Målform.NB,
                refusjonEøsHjemmelSkalMedIBrev = false,
                erFritekstIBrev = false,
            ),
        )
    }

    @Test
    fun `hentHjemmeltekst skal ikke inkludere hjemmel 17 og 18 hvis opplysningsplikt er oppfylt`() {
        val vedtaksperioderMedBegrunnelser =
            listOf(
                lagVedtaksperiodeMedBegrunnelser(
                    begrunnelser =
                        mutableSetOf(
                            lagVedtaksbegrunnelse(
                                standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET,
                            ),
                        ),
                ),
                lagVedtaksperiodeMedBegrunnelser(
                    begrunnelser =
                        mutableSetOf(
                            lagVedtaksbegrunnelse(
                                standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING,
                            ),
                        ),
                ),
            )

        Assertions.assertEquals(
            "barnetrygdloven §§ 2, 4, 10 og 11",
            hentHjemmeltekst(
                vedtaksperioder = vedtaksperioderMedBegrunnelser,
                standardbegrunnelseTilSanityBegrunnelse =
                    mapOf(
                        Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET to
                            lagSanityBegrunnelse(
                                apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                                hjemler = listOf("11", "4", "2", "10"),
                            ),
                        Standardbegrunnelse.INNVILGET_SATSENDRING to
                            lagSanityBegrunnelse(
                                apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                                hjemler = listOf("10"),
                            ),
                    ),
                eøsStandardbegrunnelseTilSanityBegrunnelse = emptyMap(),
                opplysningspliktHjemlerSkalMedIBrev = false,
                målform = Målform.NB,
                refusjonEøsHjemmelSkalMedIBrev = false,
                erFritekstIBrev = false,
            ),
        )
    }

    @Test
    fun `hentHjemmeltekst skal inkludere hjemmel for fritekst`() {
        val vedtaksperiodeMedBegrunnelser =
            lagVedtaksperiodeMedBegrunnelser(
                begrunnelser =
                    mutableSetOf(
                        lagVedtaksbegrunnelse(
                            standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING,
                        ),
                    ),
            )

        vedtaksperiodeMedBegrunnelser.fritekster.add(
            VedtaksbegrunnelseFritekst(
                fritekst = "Dette er en fritekst",
                vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
            ),
        )

        Assertions.assertEquals(
            "barnetrygdloven §§ 2, 4, 10 og 11",
            hentHjemmeltekst(
                vedtaksperioder = listOf(vedtaksperiodeMedBegrunnelser),
                standardbegrunnelseTilSanityBegrunnelse =
                    mapOf(
                        Standardbegrunnelse.INNVILGET_SATSENDRING to
                            lagSanityBegrunnelse(
                                apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                                hjemler = listOf("10"),
                            ),
                    ),
                eøsStandardbegrunnelseTilSanityBegrunnelse = emptyMap(),
                opplysningspliktHjemlerSkalMedIBrev = false,
                målform = Målform.NB,
                refusjonEøsHjemmelSkalMedIBrev = false,
                erFritekstIBrev = vedtaksperiodeMedBegrunnelser.fritekster.isNotEmpty(),
            ),
        )
    }

    @Test
    fun `hentHjemmeltekst skal inkludere hjemmel 17 og 18 hvis opplysningsplikt ikke er oppfylt`() {
        val vedtaksperioderMedBegrunnelser =
            listOf(
                lagVedtaksperiodeMedBegrunnelser(
                    begrunnelser =
                        mutableSetOf(
                            lagVedtaksbegrunnelse(
                                standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET,
                            ),
                        ),
                ),
                lagVedtaksperiodeMedBegrunnelser(
                    begrunnelser =
                        mutableSetOf(
                            lagVedtaksbegrunnelse(
                                standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING,
                            ),
                        ),
                ),
            )

        Assertions.assertEquals(
            "barnetrygdloven §§ 2, 4, 10, 11, 17 og 18",
            hentHjemmeltekst(
                vedtaksperioder = vedtaksperioderMedBegrunnelser,
                standardbegrunnelseTilSanityBegrunnelse =
                    mapOf(
                        Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET to
                            lagSanityBegrunnelse(
                                apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                                hjemler = listOf("11", "4", "2", "10"),
                            ),
                        Standardbegrunnelse.INNVILGET_SATSENDRING to
                            lagSanityBegrunnelse(
                                apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                                hjemler = listOf("10"),
                            ),
                    ),
                eøsStandardbegrunnelseTilSanityBegrunnelse = emptyMap(),
                opplysningspliktHjemlerSkalMedIBrev = true,
                målform = Målform.NB,
                refusjonEøsHjemmelSkalMedIBrev = false,
                erFritekstIBrev = false,
            ),
        )
    }

    @Test
    fun `hentHjemmeltekst skal inkludere EØS-forordning 987 artikkel 60 hvis det eksisterer eøs refusjon på behandlingen`() {
        Assertions.assertEquals(
            "EØS-forordning 987/2009 artikkel 60",
            hentHjemmeltekst(
                vedtaksperioder = emptyList(),
                standardbegrunnelseTilSanityBegrunnelse = emptyMap(),
                eøsStandardbegrunnelseTilSanityBegrunnelse = emptyMap(),
                opplysningspliktHjemlerSkalMedIBrev = false,
                målform = Målform.NB,
                refusjonEøsHjemmelSkalMedIBrev = true,
                erFritekstIBrev = false,
            ),
        )
    }

    @Test
    fun `Skal gi riktig hjemmeltekst ved hjemler både fra barnetrygdloven og folketrygdloven`() {
        val vedtaksperioderMedBegrunnelser =
            listOf(
                lagVedtaksperiodeMedBegrunnelser(
                    begrunnelser =
                        mutableSetOf(
                            lagVedtaksbegrunnelse(
                                standardbegrunnelse = Standardbegrunnelse.INNVILGET_SØKER_OG_BARN_FRIVILLIG_MEDLEM,
                            ),
                        ),
                ),
                lagVedtaksperiodeMedBegrunnelser(
                    begrunnelser =
                        mutableSetOf(
                            lagVedtaksbegrunnelse(
                                standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING,
                            ),
                        ),
                ),
            )

        val sanityBegrunnelser =
            mapOf(
                Standardbegrunnelse.INNVILGET_SØKER_OG_BARN_FRIVILLIG_MEDLEM to
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_SØKER_OG_BARN_FRIVILLIG_MEDLEM.sanityApiNavn,
                        hjemler = listOf("11", "4"),
                        hjemlerFolketrygdloven = listOf("2-5", "2-8"),
                    ),
                Standardbegrunnelse.INNVILGET_SATSENDRING to
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                        hjemler = listOf("10"),
                    ),
            )

        Assertions.assertEquals(
            "barnetrygdloven §§ 4, 10 og 11 og folketrygdloven §§ 2-5 og 2-8",
            hentHjemmeltekst(
                vedtaksperioder = vedtaksperioderMedBegrunnelser,
                standardbegrunnelseTilSanityBegrunnelse = sanityBegrunnelser,
                eøsStandardbegrunnelseTilSanityBegrunnelse = emptyMap(),
                opplysningspliktHjemlerSkalMedIBrev = false,
                målform = Målform.NB,
                refusjonEøsHjemmelSkalMedIBrev = false,
                erFritekstIBrev = false,
            ),
        )
    }

    @Test
    fun `Skal gi riktig formattering ved hjemler fra barnetrygdloven og 2 EØS-forordninger`() {
        val vedtaksperioderMedBegrunnelser =
            listOf(
                lagVedtaksperiodeMedBegrunnelser(
                    begrunnelser =
                        mutableSetOf(
                            lagVedtaksbegrunnelse(
                                standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET,
                            ),
                        ),
                    eøsBegrunnelser =
                        mutableSetOf(
                            EØSBegrunnelse(
                                vedtaksperiodeMedBegrunnelser = mockk(),
                                begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR,
                            ),
                        ),
                ),
                lagVedtaksperiodeMedBegrunnelser(
                    begrunnelser =
                        mutableSetOf(
                            lagVedtaksbegrunnelse(
                                standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING,
                            ),
                        ),
                    eøsBegrunnelser =
                        mutableSetOf(
                            EØSBegrunnelse(
                                vedtaksperiodeMedBegrunnelser = mockk(),
                                begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE,
                            ),
                        ),
                ),
            )

        val sanityBegrunnelser =
            mapOf(
                Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET to
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                        hjemler = listOf("11", "4"),
                    ),
                Standardbegrunnelse.INNVILGET_SATSENDRING to
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                        hjemler = listOf("10"),
                    ),
            )

        val sanityEøsBegrunnelser =
            mapOf(
                EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR to
                    lagSanityEøsBegrunnelse(
                        apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR.sanityApiNavn,
                        hjemler = listOf("4"),
                        hjemlerEØSForordningen883 = listOf("11-16"),
                    ),
                EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE to
                    lagSanityEøsBegrunnelse(
                        apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE.sanityApiNavn,
                        hjemler = listOf("11"),
                        hjemlerEØSForordningen987 = listOf("58", "60"),
                    ),
            )

        Assertions.assertEquals(
            "barnetrygdloven §§ 4, 10 og 11, EØS-forordning 883/2004 artikkel 11-16 og EØS-forordning 987/2009 artikkel 58 og 60",
            hentHjemmeltekst(
                vedtaksperioder = vedtaksperioderMedBegrunnelser,
                standardbegrunnelseTilSanityBegrunnelse = sanityBegrunnelser,
                eøsStandardbegrunnelseTilSanityBegrunnelse = sanityEøsBegrunnelser,
                opplysningspliktHjemlerSkalMedIBrev = false,
                målform = Målform.NB,
                refusjonEøsHjemmelSkalMedIBrev = false,
                erFritekstIBrev = false,
            ),
        )
    }

    @Test
    fun `Skal gi riktig formattering ved hjemler fra Separasjonsavtale og to EØS-forordninger`() {
        val vedtaksperioderMedBegrunnelser =
            listOf(
                lagVedtaksperiodeMedBegrunnelser(
                    begrunnelser =
                        mutableSetOf(
                            lagVedtaksbegrunnelse(
                                standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET,
                            ),
                        ),
                    eøsBegrunnelser =
                        mutableSetOf(
                            EØSBegrunnelse(
                                vedtaksperiodeMedBegrunnelser = mockk(),
                                begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR,
                            ),
                        ),
                ),
                lagVedtaksperiodeMedBegrunnelser(
                    begrunnelser =
                        mutableSetOf(
                            lagVedtaksbegrunnelse(
                                standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING,
                            ),
                        ),
                    eøsBegrunnelser =
                        mutableSetOf(
                            EØSBegrunnelse(
                                vedtaksperiodeMedBegrunnelser = mockk(),
                                begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE,
                            ),
                        ),
                ),
            )

        val sanityBegrunnelser =
            mapOf(
                Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET to
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                        hjemler = listOf("11", "4"),
                    ),
                Standardbegrunnelse.INNVILGET_SATSENDRING to
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                        hjemler = listOf("10"),
                    ),
            )

        val sanityEøsBegrunnelser =
            mapOf(
                EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR to
                    lagSanityEøsBegrunnelse(
                        apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR.sanityApiNavn,
                        hjemler = listOf("4"),
                        hjemlerEØSForordningen883 = listOf("11-16"),
                        hjemlerSeperasjonsavtalenStorbritannina = listOf("29"),
                    ),
                EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE to
                    lagSanityEøsBegrunnelse(
                        apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE.sanityApiNavn,
                        hjemler = listOf("11"),
                        hjemlerEØSForordningen987 = listOf("58", "60"),
                    ),
            )

        Assertions.assertEquals(
            "Separasjonsavtalen mellom Storbritannia og Norge artikkel 29, barnetrygdloven §§ 4, 10 og 11, EØS-forordning 883/2004 artikkel 11-16 og EØS-forordning 987/2009 artikkel 58 og 60",
            hentHjemmeltekst(
                vedtaksperioder = vedtaksperioderMedBegrunnelser,
                standardbegrunnelseTilSanityBegrunnelse = sanityBegrunnelser,
                eøsStandardbegrunnelseTilSanityBegrunnelse = sanityEøsBegrunnelser,
                opplysningspliktHjemlerSkalMedIBrev = false,
                målform = Målform.NB,
                refusjonEøsHjemmelSkalMedIBrev = false,
                erFritekstIBrev = false,
            ),
        )
    }

    @Test
    fun `Skal gi riktig formattering ved nynorsk og hjemler fra Separasjonsavtale og to EØS-forordninger`() {
        val vedtaksperioderMedBegrunnelser =
            listOf(
                lagVedtaksperiodeMedBegrunnelser(
                    begrunnelser =
                        mutableSetOf(
                            lagVedtaksbegrunnelse(
                                standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET,
                            ),
                        ),
                    eøsBegrunnelser =
                        mutableSetOf(
                            EØSBegrunnelse(
                                vedtaksperiodeMedBegrunnelser = mockk(),
                                begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR,
                            ),
                        ),
                ),
                lagVedtaksperiodeMedBegrunnelser(
                    begrunnelser =
                        mutableSetOf(
                            lagVedtaksbegrunnelse(
                                standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING,
                            ),
                        ),
                    eøsBegrunnelser =
                        mutableSetOf(
                            EØSBegrunnelse(
                                vedtaksperiodeMedBegrunnelser = mockk(),
                                begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE,
                            ),
                        ),
                ),
            )

        val sanityBegrunnelser =
            mapOf(
                Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET to
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                        hjemler = listOf("11", "4"),
                    ),
                Standardbegrunnelse.INNVILGET_SATSENDRING to
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                        hjemler = listOf("10"),
                    ),
            )

        val sanityEøsBegrunnelser =
            mapOf(
                EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR to
                    lagSanityEøsBegrunnelse(
                        apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR.sanityApiNavn,
                        hjemler = listOf("4"),
                        hjemlerEØSForordningen883 = listOf("11-16"),
                        hjemlerSeperasjonsavtalenStorbritannina = listOf("29"),
                    ),
                EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE to
                    lagSanityEøsBegrunnelse(
                        apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE.sanityApiNavn,
                        hjemler = listOf("11"),
                        hjemlerEØSForordningen987 = listOf("58", "60"),
                    ),
            )

        Assertions.assertEquals(
            "Separasjonsavtalen mellom Storbritannia og Noreg artikkel 29, barnetrygdlova §§ 4, 10 og 11, EØS-forordning 883/2004 artikkel 11-16 og EØS-forordning 987/2009 artikkel 58 og 60",
            hentHjemmeltekst(
                vedtaksperioder = vedtaksperioderMedBegrunnelser,
                standardbegrunnelseTilSanityBegrunnelse = sanityBegrunnelser,
                eøsStandardbegrunnelseTilSanityBegrunnelse = sanityEøsBegrunnelser,
                opplysningspliktHjemlerSkalMedIBrev = false,
                målform = Målform.NN,
                refusjonEøsHjemmelSkalMedIBrev = false,
                erFritekstIBrev = false,
            ),
        )
    }

    @Test
    fun `Skal slå sammen hjemlene riktig når det er 3 eller flere hjemler på 'siste' hjemmeltype`() {
        val vedtaksperioderMedBegrunnelser =
            listOf(
                lagVedtaksperiodeMedBegrunnelser(
                    begrunnelser =
                        mutableSetOf(
                            lagVedtaksbegrunnelse(
                                standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET,
                            ),
                        ),
                    eøsBegrunnelser =
                        mutableSetOf(
                            EØSBegrunnelse(
                                vedtaksperiodeMedBegrunnelser = mockk(),
                                begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR,
                            ),
                        ),
                ),
                lagVedtaksperiodeMedBegrunnelser(
                    begrunnelser =
                        mutableSetOf(
                            lagVedtaksbegrunnelse(
                                standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING,
                            ),
                        ),
                    eøsBegrunnelser =
                        mutableSetOf(
                            EØSBegrunnelse(
                                vedtaksperiodeMedBegrunnelser = mockk(),
                                begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE,
                            ),
                        ),
                ),
            )

        val sanityBegrunnelser =
            mapOf(
                Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET to
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                        hjemler = listOf("11", "4"),
                    ),
                Standardbegrunnelse.INNVILGET_SATSENDRING to
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                        hjemler = listOf("10"),
                    ),
            )

        val sanityEøsBegrunnelser =
            mapOf(
                EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR to
                    lagSanityEøsBegrunnelse(
                        apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR.sanityApiNavn,
                        hjemler = listOf("4"),
                        hjemlerEØSForordningen883 = listOf("2", "11-16", "67", "68"),
                        hjemlerSeperasjonsavtalenStorbritannina = listOf("29"),
                    ),
                EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE to
                    lagSanityEøsBegrunnelse(
                        apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE.sanityApiNavn,
                        hjemler = listOf("11"),
                    ),
            )

        Assertions.assertEquals(
            "Separasjonsavtalen mellom Storbritannia og Noreg artikkel 29, barnetrygdlova §§ 4, 10 og 11 og EØS-forordning 883/2004 artikkel 2, 11-16, 67 og 68",
            hentHjemmeltekst(
                vedtaksperioder = vedtaksperioderMedBegrunnelser,
                standardbegrunnelseTilSanityBegrunnelse = sanityBegrunnelser,
                eøsStandardbegrunnelseTilSanityBegrunnelse = sanityEøsBegrunnelser,
                opplysningspliktHjemlerSkalMedIBrev = false,
                målform = Målform.NN,
                refusjonEøsHjemmelSkalMedIBrev = false,
                erFritekstIBrev = false,
            ),
        )
    }

    @Test
    fun `Skal kun ta med en hjemmel 1 gang hvis flere begrunnelser er knyttet til samme hjemmel`() {
        val vedtaksperioderMedBegrunnelser =
            listOf(
                lagVedtaksperiodeMedBegrunnelser(
                    begrunnelser =
                        mutableSetOf(
                            lagVedtaksbegrunnelse(
                                standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET,
                            ),
                        ),
                    eøsBegrunnelser =
                        mutableSetOf(
                            EØSBegrunnelse(
                                vedtaksperiodeMedBegrunnelser = mockk(),
                                begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR,
                            ),
                        ),
                ),
                lagVedtaksperiodeMedBegrunnelser(
                    begrunnelser =
                        mutableSetOf(
                            lagVedtaksbegrunnelse(
                                standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING,
                            ),
                        ),
                    eøsBegrunnelser =
                        mutableSetOf(
                            EØSBegrunnelse(
                                vedtaksperiodeMedBegrunnelser = mockk(),
                                begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE,
                            ),
                        ),
                ),
            )

        val sanityBegrunnelser =
            mapOf(
                Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET to
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                        hjemler = listOf("11", "4"),
                    ),
                Standardbegrunnelse.INNVILGET_SATSENDRING to
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                        hjemler = listOf("10"),
                    ),
            )

        val sanityEøsBegrunnelser =
            mapOf(
                EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR to
                    lagSanityEøsBegrunnelse(
                        apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR.sanityApiNavn,
                        hjemler = listOf("4"),
                        hjemlerEØSForordningen883 = listOf("2", "11-16", "67", "68"),
                        hjemlerSeperasjonsavtalenStorbritannina = listOf("29"),
                        hjemlerEØSForordningen987 = listOf("58"),
                    ),
                EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE to
                    lagSanityEøsBegrunnelse(
                        apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE.sanityApiNavn,
                        hjemler = listOf("11"),
                        hjemlerEØSForordningen883 = listOf("2", "67", "68"),
                        hjemlerSeperasjonsavtalenStorbritannina = listOf("29"),
                        hjemlerEØSForordningen987 = listOf("58"),
                    ),
            )

        Assertions.assertEquals(
            "Separasjonsavtalen mellom Storbritannia og Noreg artikkel 29, barnetrygdlova §§ 4, 10 og 11, EØS-forordning 883/2004 artikkel 2, 11-16, 67 og 68 og EØS-forordning 987/2009 artikkel 58",
            hentHjemmeltekst(
                vedtaksperioder = vedtaksperioderMedBegrunnelser,
                standardbegrunnelseTilSanityBegrunnelse = sanityBegrunnelser,
                eøsStandardbegrunnelseTilSanityBegrunnelse = sanityEøsBegrunnelser,
                opplysningspliktHjemlerSkalMedIBrev = false,
                målform = Målform.NN,
                refusjonEøsHjemmelSkalMedIBrev = false,
                erFritekstIBrev = false,
            ),
        )
    }

    @Test
    fun `Skal gi riktig dato for opphørstester`() {
        val sisteFom = LocalDate.now().minusMonths(2)

        val opphørsperioder =
            listOf(
                lagVedtaksperiodeMedBegrunnelser(
                    fom = LocalDate.now().minusYears(1),
                    tom = LocalDate.now().minusYears(1).plusMonths(2),
                    type = Vedtaksperiodetype.OPPHØR,
                ),
                lagVedtaksperiodeMedBegrunnelser(
                    fom = LocalDate.now().minusMonths(5),
                    tom = LocalDate.now().minusMonths(4),
                    type = Vedtaksperiodetype.OPPHØR,
                ),
                lagVedtaksperiodeMedBegrunnelser(
                    fom = sisteFom,
                    tom = LocalDate.now(),
                    type = Vedtaksperiodetype.OPPHØR,
                ),
            )

        Assertions.assertEquals(sisteFom.tilMånedÅr(), hentVirkningstidspunktForDødsfallbrev(opphørsperioder, 0L))
    }

    @Test
    fun `hentUtbetalingerPerMndEøs - Skal gi utbetalingsinfo for alle måneder etter endringstidspunktet for ett barn hvor søker har utvidet og småbarnstillegg`() {
        val søker = randomAktør()
        val barn = randomAktør()

        val endringstidspunkt = LocalDate.now().minusMonths(7)

        val andelerTilkjentYtelse =
            listOf(
                // Søker har utvidet barnetrygd og småbarnstillegg de siste 12 månedene.
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(12).toYearMonth(),
                    tom = LocalDate.now().toYearMonth(),
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                    aktør = søker,
                    kalkulertUtbetalingsbeløp = sats(YtelseType.UTVIDET_BARNETRYGD),
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(12).toYearMonth(),
                    tom = LocalDate.now().toYearMonth(),
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                    aktør = søker,
                    kalkulertUtbetalingsbeløp = sats(YtelseType.SMÅBARNSTILLEGG),
                ),
                // Barn har barnetrygd de siste 12 månedene, og fra og med 7 måneder siden har vi kjørt månedlig valutajustering.
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(12).toYearMonth(),
                    tom = LocalDate.now().minusMonths(8).toYearMonth(),
                    aktør = barn,
                    kalkulertUtbetalingsbeløp = 1000,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(7).toYearMonth(),
                    tom = LocalDate.now().minusMonths(7).toYearMonth(),
                    aktør = barn,
                    kalkulertUtbetalingsbeløp = 900,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(6).toYearMonth(),
                    tom = LocalDate.now().minusMonths(6).toYearMonth(),
                    aktør = barn,
                    kalkulertUtbetalingsbeløp = 800,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(5).toYearMonth(),
                    tom = LocalDate.now().minusMonths(5).toYearMonth(),
                    aktør = barn,
                    kalkulertUtbetalingsbeløp = 700,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(4).toYearMonth(),
                    tom = LocalDate.now().minusMonths(4).toYearMonth(),
                    aktør = barn,
                    kalkulertUtbetalingsbeløp = 600,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(3).toYearMonth(),
                    tom = LocalDate.now().minusMonths(3).toYearMonth(),
                    aktør = barn,
                    kalkulertUtbetalingsbeløp = 500,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(2).toYearMonth(),
                    tom = LocalDate.now().minusMonths(2).toYearMonth(),
                    aktør = barn,
                    kalkulertUtbetalingsbeløp = 400,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(1).toYearMonth(),
                    tom = LocalDate.now().minusMonths(1).toYearMonth(),
                    aktør = barn,
                    kalkulertUtbetalingsbeløp = 300,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().toYearMonth(),
                    tom = LocalDate.now().toYearMonth(),
                    aktør = barn,
                    kalkulertUtbetalingsbeløp = 200,
                ),
            )
        val utenlandskePeriodebeløp =
            listOf(
                // Barnet mottar det samme beløpet fra det andre landet i hele perioden.
                UtenlandskPeriodebeløp(fom = LocalDate.now().minusMonths(12).toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(barn), beløp = BigDecimal.valueOf(500), valutakode = "SEK", intervall = Intervall.MÅNEDLIG, kalkulertMånedligBeløp = BigDecimal.valueOf(500)),
            )

        val valutakurser =
            listOf(
                // Barnets valutakurser de siste månedene. Fra og med 7 måneder siden ble kursen endret hver måned.
                lagValutakurs(fom = LocalDate.now().minusMonths(12).toYearMonth(), tom = LocalDate.now().minusMonths(8).toYearMonth(), barnAktører = setOf(barn), valutakursdato = LocalDate.now().minusMonths(7), valutakode = "SEK", kurs = BigDecimal.valueOf(1)),
                lagValutakurs(fom = LocalDate.now().minusMonths(7).toYearMonth(), tom = LocalDate.now().minusMonths(7).toYearMonth(), barnAktører = setOf(barn), valutakursdato = LocalDate.now().minusMonths(7), valutakode = "SEK", kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().minusMonths(6).toYearMonth(), tom = LocalDate.now().minusMonths(6).toYearMonth(), barnAktører = setOf(barn), valutakursdato = LocalDate.now().minusMonths(6), valutakode = "SEK", kurs = BigDecimal.valueOf(1.3)),
                lagValutakurs(fom = LocalDate.now().minusMonths(5).toYearMonth(), tom = LocalDate.now().minusMonths(5).toYearMonth(), barnAktører = setOf(barn), valutakursdato = LocalDate.now().minusMonths(5), valutakode = "SEK", kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().minusMonths(4).toYearMonth(), tom = LocalDate.now().minusMonths(4).toYearMonth(), barnAktører = setOf(barn), valutakursdato = LocalDate.now().minusMonths(4), valutakode = "SEK", kurs = BigDecimal.valueOf(1.3)),
                lagValutakurs(fom = LocalDate.now().minusMonths(3).toYearMonth(), tom = LocalDate.now().minusMonths(3).toYearMonth(), barnAktører = setOf(barn), valutakursdato = LocalDate.now().minusMonths(3), valutakode = "SEK", kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().minusMonths(2).toYearMonth(), tom = LocalDate.now().minusMonths(2).toYearMonth(), barnAktører = setOf(barn), valutakursdato = LocalDate.now().minusMonths(2), valutakode = "SEK", kurs = BigDecimal.valueOf(1.3)),
                lagValutakurs(fom = LocalDate.now().minusMonths(1).toYearMonth(), tom = LocalDate.now().minusMonths(1).toYearMonth(), barnAktører = setOf(barn), valutakursdato = LocalDate.now().minusMonths(1), valutakode = "SEK", kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(barn), valutakursdato = LocalDate.now(), valutakode = "SEK", kurs = BigDecimal.valueOf(1)),
            )

        val utbetalingerPerMndEøs =
            hentUtbetalingerPerMndEøs(
                endringstidspunkt = endringstidspunkt,
                andelTilkjentYtelserForBehandling = andelerTilkjentYtelse,
                utenlandskePeriodebeløp = utenlandskePeriodebeløp,
                valutakurser = valutakurser,
                endretutbetalingAndeler = emptyList(),
            )

        // Skal inneholde de siste 8 månedene.
        assertThat(utbetalingerPerMndEøs.size).isEqualTo(8)
        assertThat(utbetalingerPerMndEøs.keys).isEqualTo(setAvMånedÅrMediumForPeriode(7, 0))

        // Hver mnd skal inneholde 1 utbetaling for barn og 2 utbetalinger for søker per måned.
        assertThat(utbetalingerPerMndEøs.all { it.value.utbetalinger.size == 3 }).isTrue
    }

    @Test
    fun `hentUtbetalingerPerMndEøs - Skal gi bare utbetalingsinfo for relevante andeler som ikke er satt til 0 pga endret utbetaling`() {
        val barn = lagPerson()

        val endringstidspunkt = LocalDate.now().minusMonths(7)

        val andelerTilkjentYtelse =
            listOf(
                // Barn har barnetrygd de siste 12 månedene, og fra og med 7 måneder siden har vi kjørt månedlig valutajustering. Endretutbetaling andel som reduserer utbetaling til 0 finnes på de 4 siste månedene.
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(12).toYearMonth(),
                    tom = LocalDate.now().minusMonths(8).toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 1000,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(7).toYearMonth(),
                    tom = LocalDate.now().minusMonths(7).toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 900,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(6).toYearMonth(),
                    tom = LocalDate.now().minusMonths(6).toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 800,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(5).toYearMonth(),
                    tom = LocalDate.now().minusMonths(5).toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 700,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(4).toYearMonth(),
                    tom = LocalDate.now().minusMonths(4).toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 600,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(3).toYearMonth(),
                    tom = LocalDate.now().minusMonths(3).toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 500,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(2).toYearMonth(),
                    tom = LocalDate.now().minusMonths(2).toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 400,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(1).toYearMonth(),
                    tom = LocalDate.now().minusMonths(1).toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 300,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().toYearMonth(),
                    tom = LocalDate.now().toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 200,
                ),
            )

        val endretUtbetalingAndeler =
            listOf(
                lagEndretUtbetalingAndel(person = barn, fom = LocalDate.now().minusMonths(4).toYearMonth(), tom = LocalDate.now().toYearMonth(), årsak = Årsak.ENDRE_MOTTAKER, prosent = BigDecimal(0)),
            )

        val utenlandskePeriodebeløp =
            listOf(
                // Barnet mottar det samme beløpet fra det andre landet i hele perioden.
                UtenlandskPeriodebeløp(fom = LocalDate.now().minusMonths(12).toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(barn.aktør), beløp = BigDecimal.valueOf(500), valutakode = "SEK", intervall = Intervall.MÅNEDLIG, kalkulertMånedligBeløp = BigDecimal.valueOf(500)),
            )

        val valutakurser =
            listOf(
                // Barnets valutakurser de siste månedene. Fra og med 7 måneder siden ble kursen endret hver måned.
                lagValutakurs(fom = LocalDate.now().minusMonths(12).toYearMonth(), tom = LocalDate.now().minusMonths(8).toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now().minusMonths(7), valutakode = "SEK", kurs = BigDecimal.valueOf(1)),
                lagValutakurs(fom = LocalDate.now().minusMonths(7).toYearMonth(), tom = LocalDate.now().minusMonths(7).toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now().minusMonths(7), valutakode = "SEK", kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().minusMonths(6).toYearMonth(), tom = LocalDate.now().minusMonths(6).toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now().minusMonths(6), valutakode = "SEK", kurs = BigDecimal.valueOf(1.3)),
                lagValutakurs(fom = LocalDate.now().minusMonths(5).toYearMonth(), tom = LocalDate.now().minusMonths(5).toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now().minusMonths(5), valutakode = "SEK", kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().minusMonths(4).toYearMonth(), tom = LocalDate.now().minusMonths(4).toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now().minusMonths(4), valutakode = "SEK", kurs = BigDecimal.valueOf(1.3)),
                lagValutakurs(fom = LocalDate.now().minusMonths(3).toYearMonth(), tom = LocalDate.now().minusMonths(3).toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now().minusMonths(3), valutakode = "SEK", kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().minusMonths(2).toYearMonth(), tom = LocalDate.now().minusMonths(2).toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now().minusMonths(2), valutakode = "SEK", kurs = BigDecimal.valueOf(1.3)),
                lagValutakurs(fom = LocalDate.now().minusMonths(1).toYearMonth(), tom = LocalDate.now().minusMonths(1).toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now().minusMonths(1), valutakode = "SEK", kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now(), valutakode = "SEK", kurs = BigDecimal.valueOf(1)),
            )

        val utbetalingerPerMndEøs =
            hentUtbetalingerPerMndEøs(
                endringstidspunkt = endringstidspunkt,
                andelTilkjentYtelserForBehandling = andelerTilkjentYtelse,
                utenlandskePeriodebeløp = utenlandskePeriodebeløp,
                valutakurser = valutakurser,
                endretutbetalingAndeler = endretUtbetalingAndeler,
            )

        // Skal inneholde de de 3 første månedene.
        assertThat(utbetalingerPerMndEøs.size).isEqualTo(3)
        assertThat(utbetalingerPerMndEøs.keys).isEqualTo(setAvMånedÅrMediumForPeriode(7, 5))

        // Hver mnd skal inneholde 1 utbetaling for barn per måned.
        assertThat(utbetalingerPerMndEøs.all { it.value.utbetalinger.size == 1 }).isTrue
    }

    @Test
    fun `hentUtbetalingerPerMndEøs - Skal gi utbetalingsinfo for alle måneder etter endringstidspunktet for ett primærlandsbarn og ett sekundærlandsbarn`() {
        val sekundærlandsbarn = randomAktør()
        val primærlandsbarn = randomAktør()

        val endringstidspunkt = LocalDate.now().minusMonths(4)

        val andelerTilkjentYtelse =
            listOf(
                // Sekundærlandsbarn har barnetrygd de siste 12 månedene, og fra og med 5 måneder siden har vi kjørt månedlig valutajustering.
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(12).toYearMonth(),
                    tom = LocalDate.now().minusMonths(5).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 1000,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(4).toYearMonth(),
                    tom = LocalDate.now().minusMonths(4).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 600,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(3).toYearMonth(),
                    tom = LocalDate.now().minusMonths(3).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 500,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(2).toYearMonth(),
                    tom = LocalDate.now().minusMonths(2).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 400,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(1).toYearMonth(),
                    tom = LocalDate.now().minusMonths(1).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 300,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().toYearMonth(),
                    tom = LocalDate.now().toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 200,
                ),
                // Primærlandsbarn har barntrygd de siste 12 månedene
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(12).toYearMonth(),
                    tom = LocalDate.now().toYearMonth(),
                    aktør = primærlandsbarn,
                    kalkulertUtbetalingsbeløp = 1054,
                ),
            )
        val utenlandskePeriodebeløp =
            listOf(
                // Sekundærlandsbarnet mottar to forskjellige beløp fra det andre landet i perioden.
                UtenlandskPeriodebeløp(fom = LocalDate.now().minusMonths(12).toYearMonth(), tom = LocalDate.now().minusMonths(3).toYearMonth(), barnAktører = setOf(sekundærlandsbarn), beløp = BigDecimal.valueOf(500), valutakode = "SEK", intervall = Intervall.MÅNEDLIG, kalkulertMånedligBeløp = BigDecimal.valueOf(500)),
                UtenlandskPeriodebeløp(fom = LocalDate.now().minusMonths(2).toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(sekundærlandsbarn), beløp = BigDecimal.valueOf(550), valutakode = "SEK", intervall = Intervall.MÅNEDLIG, kalkulertMånedligBeløp = BigDecimal.valueOf(550)),
            )

        val valutakurser =
            listOf(
                // Barnets valutakurser de siste månedene. Fra og med 5 måneder siden ble kursen endret hver måned.
                lagValutakurs(fom = LocalDate.now().minusMonths(4).toYearMonth(), tom = LocalDate.now().minusMonths(4).toYearMonth(), barnAktører = setOf(sekundærlandsbarn), valutakursdato = LocalDate.now().minusMonths(4), valutakode = "SEK", kurs = BigDecimal.valueOf(1.3)),
                lagValutakurs(fom = LocalDate.now().minusMonths(3).toYearMonth(), tom = LocalDate.now().minusMonths(3).toYearMonth(), barnAktører = setOf(sekundærlandsbarn), valutakursdato = LocalDate.now().minusMonths(3), valutakode = "SEK", kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().minusMonths(2).toYearMonth(), tom = LocalDate.now().minusMonths(2).toYearMonth(), barnAktører = setOf(sekundærlandsbarn), valutakursdato = LocalDate.now().minusMonths(2), valutakode = "SEK", kurs = BigDecimal.valueOf(1.3)),
                lagValutakurs(fom = LocalDate.now().minusMonths(1).toYearMonth(), tom = LocalDate.now().minusMonths(1).toYearMonth(), barnAktører = setOf(sekundærlandsbarn), valutakursdato = LocalDate.now().minusMonths(1), valutakode = "SEK", kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(sekundærlandsbarn), valutakursdato = LocalDate.now(), valutakode = "SEK", kurs = BigDecimal.valueOf(1)),
            )

        val utbetalingerPerMndEøs =
            hentUtbetalingerPerMndEøs(
                endringstidspunkt = endringstidspunkt,
                andelTilkjentYtelserForBehandling = andelerTilkjentYtelse,
                utenlandskePeriodebeløp = utenlandskePeriodebeløp,
                valutakurser = valutakurser,
                endretutbetalingAndeler = emptyList(),
            )

        // Skal inneholde de siste 8 månedene.
        assertThat(utbetalingerPerMndEøs.size).isEqualTo(5)
        assertThat(utbetalingerPerMndEøs.keys).isEqualTo(setAvMånedÅrMediumForPeriode(4, 0))

        // Hver måned skal inneholde 1 utbetaling for sekundærlandsbarn og 1 utbetaling for primærlandsbarn.
        assertThat(utbetalingerPerMndEøs.all { it.value.utbetalinger.size == 2 }).isTrue
    }

    @Test
    fun `hentUtbetalingerPerMndEøs - Skal gi utbetalingsinfo for alle måneder etter endringstidspunktet for to sekundærlandsbarn hvor det ene barnet mottar barnetrygd fra midt i endringsperioden`() {
        val sekundærlandsbarn = randomAktør()
        val sekundærlandsbarn2 = randomAktør()

        val endringstidspunkt = LocalDate.now().minusMonths(4)

        val andelerTilkjentYtelse =
            listOf(
                // Sekundærlandsbarn har barnetrygd de siste 12 månedene, og fra og med 5 måneder siden har vi kjørt månedlig valutajustering.
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(12).toYearMonth(),
                    tom = LocalDate.now().minusMonths(5).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 1000,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(4).toYearMonth(),
                    tom = LocalDate.now().minusMonths(4).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 600,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(3).toYearMonth(),
                    tom = LocalDate.now().minusMonths(3).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 500,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(2).toYearMonth(),
                    tom = LocalDate.now().minusMonths(2).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 400,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(1).toYearMonth(),
                    tom = LocalDate.now().minusMonths(1).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 300,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().toYearMonth(),
                    tom = LocalDate.now().toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 200,
                ),
                // Det andre sekundærlandsbarnet har barntrygd de siste 3 månedene
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(2).toYearMonth(),
                    tom = LocalDate.now().minusMonths(2).toYearMonth(),
                    aktør = sekundærlandsbarn2,
                    kalkulertUtbetalingsbeløp = 1000,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(1).toYearMonth(),
                    tom = LocalDate.now().minusMonths(1).toYearMonth(),
                    aktør = sekundærlandsbarn2,
                    kalkulertUtbetalingsbeløp = 900,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().toYearMonth(),
                    tom = LocalDate.now().toYearMonth(),
                    aktør = sekundærlandsbarn2,
                    kalkulertUtbetalingsbeløp = 800,
                ),
            )
        val utenlandskePeriodebeløp =
            listOf(
                // Sekundærlandsbarnet mottar to forskjellige beløp fra det andre landet i perioden.
                UtenlandskPeriodebeløp(fom = LocalDate.now().minusMonths(12).toYearMonth(), tom = LocalDate.now().minusMonths(3).toYearMonth(), barnAktører = setOf(sekundærlandsbarn), beløp = BigDecimal.valueOf(500), valutakode = "SEK", intervall = Intervall.MÅNEDLIG, kalkulertMånedligBeløp = BigDecimal.valueOf(500)),
                // Sekundærlandsbarn 1 og 2 mottar det samme beløpet fra det andre landet
                UtenlandskPeriodebeløp(fom = LocalDate.now().minusMonths(2).toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(sekundærlandsbarn, sekundærlandsbarn2), beløp = BigDecimal.valueOf(550), valutakode = "SEK", intervall = Intervall.MÅNEDLIG, kalkulertMånedligBeløp = BigDecimal.valueOf(550)),
            )

        val valutakurser =
            listOf(
                // Barnets valutakurser de siste månedene. Fra og med 5 måneder siden ble kursen endret hver måned.
                lagValutakurs(fom = LocalDate.now().minusMonths(4).toYearMonth(), tom = LocalDate.now().minusMonths(4).toYearMonth(), barnAktører = setOf(sekundærlandsbarn), valutakursdato = LocalDate.now().minusMonths(4), valutakode = "SEK", kurs = BigDecimal.valueOf(1.3)),
                lagValutakurs(fom = LocalDate.now().minusMonths(3).toYearMonth(), tom = LocalDate.now().minusMonths(3).toYearMonth(), barnAktører = setOf(sekundærlandsbarn), valutakursdato = LocalDate.now().minusMonths(3), valutakode = "SEK", kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().minusMonths(2).toYearMonth(), tom = LocalDate.now().minusMonths(2).toYearMonth(), barnAktører = setOf(sekundærlandsbarn, sekundærlandsbarn2), valutakursdato = LocalDate.now().minusMonths(2), valutakode = "SEK", kurs = BigDecimal.valueOf(1.3)),
                lagValutakurs(fom = LocalDate.now().minusMonths(1).toYearMonth(), tom = LocalDate.now().minusMonths(1).toYearMonth(), barnAktører = setOf(sekundærlandsbarn, sekundærlandsbarn2), valutakursdato = LocalDate.now().minusMonths(1), valutakode = "SEK", kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(sekundærlandsbarn, sekundærlandsbarn2), valutakursdato = LocalDate.now(), valutakode = "SEK", kurs = BigDecimal.valueOf(1)),
            )

        val utbetalingerPerMndEøs =
            hentUtbetalingerPerMndEøs(
                endringstidspunkt = endringstidspunkt,
                andelTilkjentYtelserForBehandling = andelerTilkjentYtelse,
                utenlandskePeriodebeløp = utenlandskePeriodebeløp,
                valutakurser = valutakurser,
                endretutbetalingAndeler = emptyList(),
            )

        // Skal inneholde de siste 8 månedene.
        assertThat(utbetalingerPerMndEøs.size).isEqualTo(5)
        assertThat(utbetalingerPerMndEøs.keys).isEqualTo(setAvMånedÅrMediumForPeriode(fraAntallMndSiden = 4, tilAndtalMndSiden = 0))

        // De første 2 månedene skal kun inneholde utbetaling for det første sekundærlandsbarnet.
        assertThat(utbetalingerPerMndEøs.filterKeys { setAvMånedÅrMediumForPeriode(fraAntallMndSiden = 4, tilAndtalMndSiden = 3).contains(it) }.values.all { it.utbetalinger.size == 1 }).isTrue

        // De siste 3 månedene skal inneholde utbetalinger for begge sekundærlandsbarna.
        assertThat(utbetalingerPerMndEøs.filterKeys { setAvMånedÅrMediumForPeriode(fraAntallMndSiden = 2, tilAndtalMndSiden = 0).contains(it) }.values.all { it.utbetalinger.size == 2 }).isTrue
    }

    @Test
    fun `hentUtbetalingerPerMndEøs - Skal kaste feil dersom utbetalt fra annet land, valutakode eller valutakurs er null`() {
        val sekundærlandsbarn = randomAktør()

        val endringstidspunkt = LocalDate.now().minusMonths(4)

        val andelerTilkjentYtelse =
            listOf(
                // Sekundærlandsbarn har barnetrygd de siste 12 månedene, og fra og med 5 måneder siden har vi kjørt månedlig valutajustering.
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(12).toYearMonth(),
                    tom = LocalDate.now().minusMonths(5).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 1000,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(4).toYearMonth(),
                    tom = LocalDate.now().minusMonths(4).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 600,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(3).toYearMonth(),
                    tom = LocalDate.now().minusMonths(3).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 500,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(2).toYearMonth(),
                    tom = LocalDate.now().minusMonths(2).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 400,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(1).toYearMonth(),
                    tom = LocalDate.now().minusMonths(1).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 300,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().toYearMonth(),
                    tom = LocalDate.now().toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 200,
                ),
            )
        val utenlandskePeriodebeløp =
            listOf(
                // Sekundærlandsbarnet har to perioder med utenlandsk periodebeløp men disse mangler beløp, intervall og valutakode.
                UtenlandskPeriodebeløp(fom = LocalDate.now().minusMonths(12).toYearMonth(), tom = LocalDate.now().minusMonths(3).toYearMonth(), barnAktører = setOf(sekundærlandsbarn)),
                UtenlandskPeriodebeløp(fom = LocalDate.now().minusMonths(2).toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(sekundærlandsbarn)),
            )

        val valutakurser =
            listOf(
                // Barnets valutakurser de siste månedene. Mangler kurs.
                lagValutakurs(fom = LocalDate.now().minusMonths(4).toYearMonth(), tom = LocalDate.now().minusMonths(4).toYearMonth(), barnAktører = setOf(sekundærlandsbarn)),
                lagValutakurs(fom = LocalDate.now().minusMonths(3).toYearMonth(), tom = LocalDate.now().minusMonths(3).toYearMonth(), barnAktører = setOf(sekundærlandsbarn)),
                lagValutakurs(fom = LocalDate.now().minusMonths(2).toYearMonth(), tom = LocalDate.now().minusMonths(2).toYearMonth(), barnAktører = setOf(sekundærlandsbarn)),
                lagValutakurs(fom = LocalDate.now().minusMonths(1).toYearMonth(), tom = LocalDate.now().minusMonths(1).toYearMonth(), barnAktører = setOf(sekundærlandsbarn)),
                lagValutakurs(fom = LocalDate.now().toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(sekundærlandsbarn)),
            )

        assertThrows<Feil> {
            hentUtbetalingerPerMndEøs(
                endringstidspunkt = endringstidspunkt,
                andelTilkjentYtelserForBehandling = andelerTilkjentYtelse,
                utenlandskePeriodebeløp = utenlandskePeriodebeløp,
                valutakurser = valutakurser,
                endretutbetalingAndeler = emptyList(),
            )
        }
    }

    @Test
    fun `skalHenteUtbetalingerEøs - skal returne true når det finnes valutakurser etter endringstidspunktet`() {
        val endringstidspunkt = LocalDate.now().minusMonths(2)
        val barn = randomAktør()

        val valutakurser =
            listOf(
                lagValutakurs(fom = LocalDate.now().minusMonths(4).toYearMonth(), tom = LocalDate.now().minusMonths(4).toYearMonth(), barnAktører = setOf(barn), kurs = BigDecimal.valueOf(1)),
                lagValutakurs(fom = LocalDate.now().minusMonths(3).toYearMonth(), tom = LocalDate.now().minusMonths(3).toYearMonth(), barnAktører = setOf(barn), kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().minusMonths(2).toYearMonth(), tom = LocalDate.now().minusMonths(2).toYearMonth(), barnAktører = setOf(barn), kurs = BigDecimal.valueOf(1.1)),
                lagValutakurs(fom = LocalDate.now().minusMonths(1).toYearMonth(), tom = LocalDate.now().minusMonths(1).toYearMonth(), barnAktører = setOf(barn), kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(barn), kurs = BigDecimal.valueOf(1.3)),
            )

        assertThat(skalHenteUtbetalingerEøs(endringstidspunkt = endringstidspunkt, valutakurser = valutakurser)).isTrue
    }

    @Test
    fun `skalHenteUtbetalingerEøs - skal returne false når det ikke finnes valutakurser etter endringstidspunktet`() {
        val endringstidspunkt = LocalDate.now().minusMonths(2)
        val barn = randomAktør()

        val valutakurser =
            listOf(
                lagValutakurs(fom = LocalDate.now().minusMonths(4).toYearMonth(), tom = LocalDate.now().minusMonths(4).toYearMonth(), barnAktører = setOf(barn), kurs = BigDecimal.valueOf(1)),
                lagValutakurs(fom = LocalDate.now().minusMonths(3).toYearMonth(), tom = LocalDate.now().minusMonths(3).toYearMonth(), barnAktører = setOf(barn), kurs = BigDecimal.valueOf(1.2)),
            )

        assertThat(skalHenteUtbetalingerEøs(endringstidspunkt = endringstidspunkt, valutakurser = valutakurser)).isFalse
    }

    @Test
    fun `skalHenteUtbetalingerEøs - skal returnere false uavhengig av valutakurs dersom endringstidspunktet er satt til tidenes ende`() {
        val endringstidspunkt = TIDENES_ENDE
        val barn = randomAktør()

        val valutakurser =
            listOf(
                lagValutakurs(fom = LocalDate.now().minusMonths(4).toYearMonth(), tom = LocalDate.now().minusMonths(4).toYearMonth(), barnAktører = setOf(barn), kurs = BigDecimal.valueOf(1)),
                lagValutakurs(fom = LocalDate.now().minusMonths(3).toYearMonth(), tom = LocalDate.now().minusMonths(3).toYearMonth(), barnAktører = setOf(barn), kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().minusMonths(2).toYearMonth(), tom = LocalDate.now().minusMonths(2).toYearMonth(), barnAktører = setOf(barn), kurs = BigDecimal.valueOf(1.1)),
                lagValutakurs(fom = LocalDate.now().minusMonths(1).toYearMonth(), tom = LocalDate.now().minusMonths(1).toYearMonth(), barnAktører = setOf(barn), kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(barn), kurs = BigDecimal.valueOf(1.3)),
            )

        assertThat(skalHenteUtbetalingerEøs(endringstidspunkt = endringstidspunkt, valutakurser = valutakurser)).isFalse
    }

    @Test
    fun `hentLandOgStartdatoForUtbetalingstabell - skal finne alle kompetanser som gjelder etter endringstidspunktet`() {
        val endringstidspunkt = LocalDate.now().tilMånedTidspunkt()

        val kompetanser =
            listOf(
                lagKompetanse(
                    fom = YearMonth.now().minusMonths(2),
                    tom = YearMonth.now().plusMonths(2),
                    søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                    søkersAktivitetsland = "NO",
                    annenForeldersAktivitet = KompetanseAktivitet.ARBEIDER,
                    annenForeldersAktivitetsland = "SE",
                    barnetsBostedsland = "SE",
                    kompetanseResultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
                    erAnnenForelderOmfattetAvNorskLovgivning = false,
                    barnAktører =
                        setOf(
                            randomAktør(),
                        ),
                ),
                lagKompetanse(
                    fom = YearMonth.now().plusMonths(3),
                    søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                    søkersAktivitetsland = "NO",
                    annenForeldersAktivitet = KompetanseAktivitet.ARBEIDER,
                    annenForeldersAktivitetsland = "DK",
                    barnetsBostedsland = "DK",
                    kompetanseResultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
                    erAnnenForelderOmfattetAvNorskLovgivning = false,
                    barnAktører =
                        setOf(
                            randomAktør(),
                        ),
                ),
            )

        val landkoder =
            mapOf(
                "SE" to "Sverige",
                "DK" to "Danmark",
            )

        val utbetalingstabellAutomatiskValutajustering = hentLandOgStartdatoForUtbetalingstabell(endringstidspunkt = endringstidspunkt, landkoder = landkoder, kompetanser = kompetanser)
        assertThat(utbetalingstabellAutomatiskValutajustering).isNotNull
        assertThat(utbetalingstabellAutomatiskValutajustering.utbetalingerEosLand?.first()).isEqualTo("Sverige og Danmark")
    }

    @Test
    fun `hentLandOgStartdatoForUtbetalingstabell - skal finne korrekt utbetalingsland når hovedregelen gir Norge`() {
        val endringstidspunkt = LocalDate.now().tilMånedTidspunkt()

        val kompetanser =
            listOf(
                lagKompetanse(
                    fom = YearMonth.now(),
                    tom = YearMonth.now().plusMonths(2),
                    søkersAktivitet = KompetanseAktivitet.MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN,
                    søkersAktivitetsland = "SE",
                    annenForeldersAktivitet = KompetanseAktivitet.IKKE_AKTUELT,
                    annenForeldersAktivitetsland = null,
                    barnetsBostedsland = "NO",
                    kompetanseResultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
                    erAnnenForelderOmfattetAvNorskLovgivning = false,
                    barnAktører =
                        setOf(
                            randomAktør(),
                        ),
                ),
            )

        val landkoder =
            mapOf(
                "SE" to "Sverige",
            )

        val utbetalingstabellAutomatiskValutajustering = hentLandOgStartdatoForUtbetalingstabell(endringstidspunkt = endringstidspunkt, landkoder = landkoder, kompetanser = kompetanser)
        assertThat(utbetalingstabellAutomatiskValutajustering).isNotNull
        assertThat(utbetalingstabellAutomatiskValutajustering.utbetalingerEosLand?.first()).isEqualTo("Sverige")
    }
}

private fun setAvMånedÅrMediumForPeriode(
    fraAntallMndSiden: Long,
    tilAndtalMndSiden: Long,
): Set<String> =
    LocalDate
        .now()
        .minusMonths(fraAntallMndSiden)
        .toYearMonth()
        .rangeTo(LocalDate.now().minusMonths(tilAndtalMndSiden).toYearMonth())
        .map { it.tilMånedÅrMedium() }
        .toSet()
