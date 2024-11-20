package no.nav.familie.ba.sak.kjerne.brev.hjemler

import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagSanityBegrunnelse
import no.nav.familie.ba.sak.common.lagSanityEøsBegrunnelse
import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.datagenerator.vedtak.lagVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.hentVirkningstidspunktForDødsfallbrev
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.EØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksbegrunnelseFritekst
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class HjemlerServiceTest {
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
}
