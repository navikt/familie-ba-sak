package no.nav.familie.ba.sak.kjerne.brev.hjemler

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagEØSBegrunnelse
import no.nav.familie.ba.sak.common.lagSanityBegrunnelse
import no.nav.familie.ba.sak.common.lagSanityEøsBegrunnelse
import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.datagenerator.vedtak.lagVedtaksbegrunnelse
import no.nav.familie.ba.sak.integrasjoner.sanity.SanityService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.EØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksbegrunnelseFritekst
import no.nav.familie.ba.sak.kjerne.vedtak.refusjonEøs.RefusjonEøsService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HjemlerServiceTest {
    private val vilkårsvurderingService = mockk<VilkårsvurderingService>()
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val refusjonEøsService = mockk<RefusjonEøsService>()
    private val sanityService = mockk<SanityService>()
    private val hjemlerService: HjemlerService =
        HjemlerService(
            vilkårsvurderingService = vilkårsvurderingService,
            sanityService = sanityService,
            persongrunnlagService = persongrunnlagService,
            refusjonEøsService = refusjonEøsService,
        )

    @Test
    fun `skal returnere sorterte hjemler`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

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

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns false
        every { persongrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NB

        every {
            vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every {
            sanityService.hentSanityBegrunnelser()
        } returns
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
            )

        every {
            sanityService.hentSanityEØSBegrunnelser()
        } returns emptyMap()

        // Act
        val hentHjemmeltekst =
            hjemlerService.hentHjemmeltekst(
                behandlingId = behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = false,
                sorterteVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
            )

        // Assert
        assertThat("barnetrygdloven §§ 2, 4, 10 og 11").isEqualTo(hentHjemmeltekst)
    }

    @Test
    fun `skal ikke inkludere hjemmel 17 og 18 hvis opplysningsplikt er oppfylt`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

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

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns false
        every { persongrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NB

        every {
            vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every {
            sanityService.hentSanityBegrunnelser()
        } returns
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
            )

        every {
            sanityService.hentSanityEØSBegrunnelser()
        } returns emptyMap()

        // Act
        val hjemler =
            hjemlerService.hentHjemmeltekst(
                behandlingId = behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = false,
                sorterteVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
            )

        // Assert
        assertThat(hjemler).isEqualTo("barnetrygdloven §§ 2, 4, 10 og 11")
    }

    @Test
    fun `skal inkludere hjemmel for fritekst`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

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

        val vedtaksperioderMedBegrunnelser = listOf(vedtaksperiodeMedBegrunnelser)

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns false
        every { persongrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NB

        every {
            vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every {
            sanityService.hentSanityBegrunnelser()
        } returns
            mapOf(
                Standardbegrunnelse.INNVILGET_SATSENDRING to
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                        hjemler = listOf("10"),
                    ),
            )

        every {
            sanityService.hentSanityEØSBegrunnelser()
        } returns emptyMap()

        // Act
        val hjemler =
            hjemlerService.hentHjemmeltekst(
                behandlingId = behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = false,
                sorterteVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
            )

        // Assert
        assertThat(hjemler).isEqualTo("barnetrygdloven §§ 2, 4, 10 og 11")
    }

    @Test
    fun `skal inkludere hjemmel 17 og 18 hvis opplysningsplikt ikke er oppfylt`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

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

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns false
        every { persongrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NB

        every {
            vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.IKKE_OPPFYLT,
            )

        every {
            sanityService.hentSanityBegrunnelser()
        } returns
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
            )

        every {
            sanityService.hentSanityEØSBegrunnelser()
        } returns emptyMap()

        // Act
        val hjemler =
            hjemlerService.hentHjemmeltekst(
                behandlingId = behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = false,
                sorterteVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
            )

        // Assert
        assertThat(hjemler).isEqualTo("barnetrygdloven §§ 2, 4, 10, 11, 17 og 18")
    }

    @Test
    fun `skal inkludere EØS-forordning 987 artikkel 60 hvis det eksisterer eøs refusjon på behandlingen`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns true
        every { persongrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NB

        every {
            vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every {
            sanityService.hentSanityBegrunnelser()
        } returns emptyMap()

        every {
            sanityService.hentSanityEØSBegrunnelser()
        } returns emptyMap()

        // Act
        val hjemler =
            hjemlerService.hentHjemmeltekst(
                behandlingId = behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = false,
                sorterteVedtaksperioderMedBegrunnelser = emptyList(),
            )

        // Assert
        assertThat(hjemler).isEqualTo("EØS-forordning 987/2009 artikkel 60")
    }

    @Test
    fun `skal gi riktig hjemmeltekst ved hjemler både fra barnetrygdloven og folketrygdloven`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

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

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns false
        every { persongrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NB

        every {
            vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every {
            sanityService.hentSanityBegrunnelser()
        } returns
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

        every {
            sanityService.hentSanityEØSBegrunnelser()
        } returns emptyMap()

        // Act
        val hjemler =
            hjemlerService.hentHjemmeltekst(
                behandlingId = behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = false,
                sorterteVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
            )

        // Assert
        assertThat(hjemler).isEqualTo("barnetrygdloven §§ 4, 10 og 11 og folketrygdloven §§ 2-5 og 2-8")
    }

    @Test
    fun `skal gi riktig formattering ved hjemler fra barnetrygdloven og 2 EØS-forordninger`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

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

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns false
        every { persongrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NB

        every {
            vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every {
            sanityService.hentSanityBegrunnelser()
        } returns
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

        every {
            sanityService.hentSanityEØSBegrunnelser()
        } returns
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

        // Act
        val hjemler =
            hjemlerService.hentHjemmeltekst(
                behandlingId = behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = false,
                sorterteVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
            )

        // Assert
        assertThat(hjemler).isEqualTo("barnetrygdloven §§ 4, 10 og 11, EØS-forordning 883/2004 artikkel 11-16 og EØS-forordning 987/2009 artikkel 58 og 60")
    }

    @Test
    fun `skal gi riktig formattering ved hjemler fra Separasjonsavtale og to EØS-forordninger`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

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

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns false
        every { persongrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NB

        every {
            vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every {
            sanityService.hentSanityBegrunnelser()
        } returns
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

        every {
            sanityService.hentSanityEØSBegrunnelser()
        } returns
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

        // Act
        val hjemler =
            hjemlerService.hentHjemmeltekst(
                behandlingId = behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = false,
                sorterteVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
            )

        // Arrange
        assertThat(hjemler).isEqualTo("Separasjonsavtalen mellom Storbritannia og Norge artikkel 29, barnetrygdloven §§ 4, 10 og 11, EØS-forordning 883/2004 artikkel 11-16 og EØS-forordning 987/2009 artikkel 58 og 60")
    }

    @Test
    fun `skal gi riktig formattering ved nynorsk og hjemler fra Separasjonsavtale og to EØS-forordninger`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

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

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns false
        every { persongrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NN

        every {
            vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every {
            sanityService.hentSanityBegrunnelser()
        } returns
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

        every {
            sanityService.hentSanityEØSBegrunnelser()
        } returns
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

        // Act
        val hjemler =
            hjemlerService.hentHjemmeltekst(
                behandlingId = behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = false,
                sorterteVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
            )

        // Assert
        assertThat(hjemler).isEqualTo("Separasjonsavtalen mellom Storbritannia og Noreg artikkel 29, barnetrygdlova §§ 4, 10 og 11, EØS-forordning 883/2004 artikkel 11-16 og EØS-forordning 987/2009 artikkel 58 og 60")
    }

    @Test
    fun `skal slå sammen hjemlene riktig når det er 3 eller flere hjemler på 'siste' hjemmeltype`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

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

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns false
        every { persongrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NN

        every {
            vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every {
            sanityService.hentSanityBegrunnelser()
        } returns
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

        every {
            sanityService.hentSanityEØSBegrunnelser()
        } returns
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

        // Act
        val hjemler =
            hjemlerService.hentHjemmeltekst(
                behandlingId = behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = false,
                sorterteVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
            )

        // Assert
        assertThat(hjemler).isEqualTo("Separasjonsavtalen mellom Storbritannia og Noreg artikkel 29, barnetrygdlova §§ 4, 10 og 11 og EØS-forordning 883/2004 artikkel 2, 11-16, 67 og 68")
    }

    @Test
    fun `skal kun ta med en hjemmel 1 gang hvis flere begrunnelser er knyttet til samme hjemmel`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

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

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns false
        every { persongrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NN

        every {
            vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every {
            sanityService.hentSanityBegrunnelser()
        } returns
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

        every {
            sanityService.hentSanityEØSBegrunnelser()
        } returns
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

        // Act
        val hjemler =
            hjemlerService.hentHjemmeltekst(
                behandlingId = behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = false,
                sorterteVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
            )

        // Assert
        assertThat(hjemler).isEqualTo("Separasjonsavtalen mellom Storbritannia og Noreg artikkel 29, barnetrygdlova §§ 4, 10 og 11, EØS-forordning 883/2004 artikkel 2, 11-16, 67 og 68 og EØS-forordning 987/2009 artikkel 58")
    }

    @Test
    fun `skal utlede hjemmeltekst for alle hjemler på bokmål`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

        val vedtaksperioderMedBegrunnelser =
            listOf(
                lagVedtaksperiodeMedBegrunnelser(
                    begrunnelser =
                        mutableSetOf(
                            lagVedtaksbegrunnelse(
                                standardbegrunnelse = Standardbegrunnelse.INNVILGET_MEDLEM_I_FOLKETRYGDEN,
                            ),
                        ),
                ),
                lagVedtaksperiodeMedBegrunnelser(
                    eøsBegrunnelser =
                        mutableSetOf(
                            lagEØSBegrunnelse(
                                begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_STANDARD,
                            ),
                        ),
                ),
            )

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns true
        every { persongrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NB

        every {
            vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every {
            sanityService.hentSanityBegrunnelser()
        } returns
            mapOf(
                Standardbegrunnelse.INNVILGET_MEDLEM_I_FOLKETRYGDEN to
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                        hjemler = listOf("11", "4", "2", "10"),
                        hjemlerFolketrygdloven = listOf("644", "322"),
                    ),
            )

        every {
            sanityService.hentSanityEØSBegrunnelser()
        } returns
            mapOf(
                EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_STANDARD to
                    lagSanityEøsBegrunnelse(
                        apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR.sanityApiNavn,
                        hjemlerSeperasjonsavtalenStorbritannina = listOf("29"),
                        hjemler = listOf("1"),
                        hjemlerFolketrygdloven = listOf("1337", "3154"),
                        hjemlerEØSForordningen883 = listOf("2", "11-16", "67", "68"),
                        hjemlerEØSForordningen987 = listOf("58"),
                    ),
            )

        // Act
        val hjemler =
            hjemlerService.hentHjemmeltekst(
                behandlingId = behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = true,
                sorterteVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
            )

        // Assert
        assertThat(hjemler).isEqualTo(
            "Separasjonsavtalen mellom Storbritannia og Norge artikkel 29, " +
                "barnetrygdloven §§ 1, 2, 4, 10 og 11, " +
                "folketrygdloven §§ 644, 322, 1337 og 3154, " +
                "EØS-forordning 883/2004 artikkel 2, 11-16, 67 og 68, " +
                "EØS-forordning 987/2009 artikkel 58 og 60 og forvaltningsloven § 35",
        )
    }

    @Test
    fun `skal utlede hjemmeltekst for alle hjemler på nynorsk`() {
        // Arrange
        val søker = randomAktør()

        val behandling = lagBehandling()

        val vedtaksperioderMedBegrunnelser =
            listOf(
                lagVedtaksperiodeMedBegrunnelser(
                    begrunnelser =
                        mutableSetOf(
                            lagVedtaksbegrunnelse(
                                standardbegrunnelse = Standardbegrunnelse.INNVILGET_MEDLEM_I_FOLKETRYGDEN,
                            ),
                        ),
                ),
                lagVedtaksperiodeMedBegrunnelser(
                    eøsBegrunnelser =
                        mutableSetOf(
                            lagEØSBegrunnelse(
                                begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_STANDARD,
                            ),
                        ),
                ),
            )

        every { refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId = behandling.id) } returns true
        every { persongrunnlagService.hentSøkersMålform(behandlingId = behandling.id) } returns Målform.NN

        every {
            vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)
        } returns
            lagVilkårsvurdering(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )

        every {
            sanityService.hentSanityBegrunnelser()
        } returns
            mapOf(
                Standardbegrunnelse.INNVILGET_MEDLEM_I_FOLKETRYGDEN to
                    lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                        hjemler = listOf("11", "4", "2", "10"),
                        hjemlerFolketrygdloven = listOf("644", "322"),
                    ),
            )

        every {
            sanityService.hentSanityEØSBegrunnelser()
        } returns
            mapOf(
                EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_STANDARD to
                    lagSanityEøsBegrunnelse(
                        apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR.sanityApiNavn,
                        hjemlerSeperasjonsavtalenStorbritannina = listOf("29"),
                        hjemler = listOf("1"),
                        hjemlerFolketrygdloven = listOf("1337", "3154"),
                        hjemlerEØSForordningen883 = listOf("2", "11-16", "67", "68"),
                        hjemlerEØSForordningen987 = listOf("58"),
                    ),
            )

        // Act
        val hjemler =
            hjemlerService.hentHjemmeltekst(
                behandlingId = behandling.id,
                vedtakKorrigertHjemmelSkalMedIBrev = true,
                sorterteVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
            )

        // Assert
        assertThat(hjemler).isEqualTo(
            "Separasjonsavtalen mellom Storbritannia og Noreg artikkel 29, " +
                "barnetrygdlova §§ 1, 2, 4, 10 og 11, " +
                "folketrygdlova §§ 644, 322, 1337 og 3154, " +
                "EØS-forordning 883/2004 artikkel 2, 11-16, 67 og 68, " +
                "EØS-forordning 987/2009 artikkel 58 og 60 og forvaltningslova § 35",
        )
    }
}
