package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagVegadresse
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.SystemOnlyIntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.ArbeidsgiverType
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.arbeidsforhold.GrArbeidsforhold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.opphold.GrOpphold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.StatsborgerskapService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PreutfyllLovligOppholdServiceTest {
    @Nested
    inner class GenererLovligOppholdVilkårResultatTest {
        private val pdlRestKlient: SystemOnlyPdlRestKlient = mockk(relaxed = true)
        private val statsborgerskapService = mockk<StatsborgerskapService>(relaxed = true)
        private val systemOnlyIntegrasjonKlient: SystemOnlyIntegrasjonKlient = mockk(relaxed = true)
        private val persongrunnlagService: PersongrunnlagService = mockk(relaxed = true)
        private val featureToggleService: FeatureToggleService = mockk(relaxed = true)
        private val preutfyllLovligOppholdMedLagringIPersongrunnlagService =
            PreutfyllLovligOppholdMedLagringIPersongrunnlagService(
                persongrunnlagService,
            )
        private val preutfyllLovligOppholdService: PreutfyllLovligOppholdService =
            PreutfyllLovligOppholdService(
                pdlRestKlient,
                statsborgerskapService,
                systemOnlyIntegrasjonKlient,
                persongrunnlagService,
                preutfyllLovligOppholdMedLagringIPersongrunnlagService,
                featureToggleService,
            )

        @BeforeEach
        fun setup() {
            every { featureToggleService.isEnabled(FeatureToggle.PREUTFYLLING_PERSONOPPLYSNIGSGRUNNLAG) } returns true
        }

        @Test
        fun `skal preutfylle oppfylt lovlig opphold vilkår basert på norsk eller nordisk statsborgerskap`() {
            // Arrange

            val behandling = lagBehandling()
            val aktør = behandling.fagsak.aktør
            val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

            val persongrunnlag =
                lagPersonopplysningGrunnlag(
                    behandlingId = behandling.id,
                ) { grunnlag ->
                    setOf(
                        lagPerson(aktør = aktør, personopplysningGrunnlag = grunnlag).apply {
                            bostedsadresser =
                                mutableListOf(
                                    GrBostedsadresse.fraBostedsadresse(
                                        Bostedsadresse(
                                            gyldigFraOgMed = LocalDate.now().minusYears(10),
                                            gyldigTilOgMed = null,
                                            vegadresse = lagVegadresse(12345L),
                                        ),
                                        person = this,
                                    ),
                                )
                        },
                    )
                }

            every { persongrunnlagService.hentAktivThrows(vilkårsvurdering.behandling.id) } returns persongrunnlag

            every { pdlRestKlient.hentStatsborgerskap(any(), historikk = true) } returns
                listOf(
                    Statsborgerskap("SWE", LocalDate.now().minusYears(10), null, null),
                )

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering)

            // Assert
            val lovligOppholdVilkår =
                vilkårsvurdering.personResultater
                    .first { it.aktør == aktør }
                    .vilkårResultater
                    .single { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }
            assertThat(lovligOppholdVilkår.resultat).isEqualTo(Resultat.OPPFYLT)
        }

        @Test
        fun `skal preutfylle lovlig opphold med ikke-oppfylte perioder når statsborgerskap ikke er norsk eller nordisk, og ingen arbeidsforhold`() {
            // Arrange
            val behandling = lagBehandling()
            val aktør = behandling.fagsak.aktør
            val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

            val persongrunnlag =
                lagPersonopplysningGrunnlag(
                    behandlingId = behandling.id,
                ) { grunnlag ->
                    setOf(
                        lagPerson(aktør = aktør, personopplysningGrunnlag = grunnlag).apply {
                            bostedsadresser =
                                mutableListOf(
                                    GrBostedsadresse.fraBostedsadresse(
                                        Bostedsadresse(
                                            gyldigFraOgMed = LocalDate.now().minusYears(10),
                                            gyldigTilOgMed = null,
                                            vegadresse = lagVegadresse(12345L),
                                        ),
                                        person = this,
                                    ),
                                )
                            statsborgerskap =
                                mutableListOf(
                                    GrStatsborgerskap(landkode = "ES", gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(5), tom = LocalDate.now().minusYears(2)), medlemskap = Medlemskap.EØS, person = this),
                                    GrStatsborgerskap(landkode = "NOR", gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(2).plusDays(1), tom = null), medlemskap = Medlemskap.NORDEN, person = this),
                                )
                        },
                    )
                }

            every { persongrunnlagService.hentAktivThrows(vilkårsvurdering.behandling.id) } returns persongrunnlag

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == aktør }
                    .vilkårResultater
                    .filter { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }
            assertThat(lovligOppholdResultater).hasSize(2)
            assertThat(lovligOppholdResultater.first { it.resultat == Resultat.IKKE_OPPFYLT }).isNotNull
            assertThat(lovligOppholdResultater.last { it.resultat == Resultat.OPPFYLT }).isNotNull
        }

        @Test
        fun `skal gi riktig fom og tom på lovlig opphold vilkår på nordisk statsborger`() {
            // Arrange
            val behandling = lagBehandling()
            val aktør = behandling.fagsak.aktør
            val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

            val persongrunnlag =
                lagPersonopplysningGrunnlag(
                    behandlingId = behandling.id,
                ) { grunnlag ->
                    setOf(
                        lagPerson(aktør = aktør, personopplysningGrunnlag = grunnlag).apply {
                            bostedsadresser =
                                mutableListOf(
                                    GrBostedsadresse.fraBostedsadresse(
                                        Bostedsadresse(
                                            gyldigFraOgMed = LocalDate.now().minusYears(10),
                                            gyldigTilOgMed = null,
                                            vegadresse = lagVegadresse(12345L),
                                        ),
                                        person = this,
                                    ),
                                )
                            statsborgerskap =
                                mutableListOf(
                                    GrStatsborgerskap(landkode = "ES", gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(10), tom = LocalDate.now().minusYears(5)), medlemskap = Medlemskap.EØS, person = this),
                                    GrStatsborgerskap(landkode = "NOR", gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(5).plusDays(1), tom = null), medlemskap = Medlemskap.NORDEN, person = this),
                                )
                        },
                    )
                }

            every { persongrunnlagService.hentAktivThrows(vilkårsvurdering.behandling.id) } returns persongrunnlag

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == aktør }
                    .vilkårResultater
                    .filter { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }

            assertThat(lovligOppholdResultater).hasSize(2)
            assertThat(lovligOppholdResultater.find { it.resultat == Resultat.IKKE_OPPFYLT }).isNotNull
            assertThat(lovligOppholdResultater.find { it.resultat == Resultat.OPPFYLT }).isNotNull
            assertThat(lovligOppholdResultater.first().periodeFom).isEqualTo(LocalDate.now().minusYears(10))
            assertThat(lovligOppholdResultater.first().periodeTom).isEqualTo(LocalDate.now().minusYears(5))
            assertThat(lovligOppholdResultater.last().periodeFom).isEqualTo(LocalDate.now().minusYears(5).plusDays(1))
            assertThat(lovligOppholdResultater.last().periodeTom).isNull()
        }

        @Test
        fun `skal sette fom på lovlig opphold vilkår lik første bostedsadresse i Norge, om fom ikke finnes på statsborgerskap`() {
            // Arrange
            val behandling = lagBehandling()
            val aktør = behandling.fagsak.aktør
            val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

            val persongrunnlag =
                lagPersonopplysningGrunnlag(
                    behandlingId = behandling.id,
                ) { grunnlag ->
                    setOf(
                        lagPerson(aktør = aktør, personopplysningGrunnlag = grunnlag).apply {
                            bostedsadresser =
                                mutableListOf(
                                    GrBostedsadresse.fraBostedsadresse(
                                        Bostedsadresse(
                                            gyldigFraOgMed = LocalDate.now().minusYears(10),
                                            gyldigTilOgMed = null,
                                            vegadresse = lagVegadresse(12345L),
                                        ),
                                        person = this,
                                    ),
                                )
                            statsborgerskap =
                                mutableListOf(
                                    GrStatsborgerskap(landkode = "SWE", gyldigPeriode = DatoIntervallEntitet(fom = null, tom = null), medlemskap = Medlemskap.NORDEN, person = this),
                                )
                        },
                    )
                }

            every { persongrunnlagService.hentAktivThrows(vilkårsvurdering.behandling.id) } returns persongrunnlag

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == aktør }
                    .vilkårResultater
                    .single { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }

            assertThat(lovligOppholdResultater.periodeFom).isEqualTo(LocalDate.now().minusYears(10))
            assertThat(lovligOppholdResultater.resultat).isEqualTo(Resultat.OPPFYLT)
        }

        @Test
        fun `skal gi riktig begrunnelse for oppfylt lovlig opphold vilkår hvis nordisk statsborger`() {
            // Arrange
            val behandling = lagBehandling()
            val aktør = behandling.fagsak.aktør
            val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

            val persongrunnlag =
                lagPersonopplysningGrunnlag(
                    behandlingId = behandling.id,
                ) { grunnlag ->
                    setOf(
                        lagPerson(aktør = aktør, personopplysningGrunnlag = grunnlag).apply {
                            bostedsadresser =
                                mutableListOf(
                                    GrBostedsadresse.fraBostedsadresse(
                                        Bostedsadresse(
                                            gyldigFraOgMed = LocalDate.now().minusYears(10),
                                            gyldigTilOgMed = null,
                                            vegadresse = lagVegadresse(12345L),
                                        ),
                                        person = this,
                                    ),
                                )
                            statsborgerskap =
                                mutableListOf(
                                    GrStatsborgerskap(landkode = "NOR", gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(10), tom = null), medlemskap = Medlemskap.NORDEN, person = this),
                                )
                        },
                    )
                }

            every { persongrunnlagService.hentAktivThrows(vilkårsvurdering.behandling.id) } returns persongrunnlag

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == aktør }
                    .vilkårResultater
                    .single { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }

            assertThat(lovligOppholdResultater.begrunnelse)
                .isEqualTo("$PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT- Norsk/nordisk statsborgerskap.")
        }

        @Test
        fun `skal preutfylle oppfylt lovlig opphold vilkår hvis EØS borger og har arbeidsforhold`() {
            // Arrange
            val behandling = lagBehandling()
            val aktør = behandling.fagsak.aktør
            val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

            val persongrunnlag =
                lagPersonopplysningGrunnlag(
                    behandlingId = behandling.id,
                ) { grunnlag ->
                    setOf(
                        lagPerson(aktør = aktør, personopplysningGrunnlag = grunnlag).apply {
                            bostedsadresser =
                                mutableListOf(
                                    GrBostedsadresse.fraBostedsadresse(
                                        Bostedsadresse(
                                            gyldigFraOgMed = LocalDate.now().minusYears(10),
                                            gyldigTilOgMed = null,
                                            vegadresse = lagVegadresse(12345L),
                                        ),
                                        person = this,
                                    ),
                                )
                            statsborgerskap =
                                mutableListOf(
                                    GrStatsborgerskap(landkode = "BE", gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(20), tom = null), medlemskap = Medlemskap.EØS, person = this),
                                )
                            arbeidsforhold = mutableListOf(GrArbeidsforhold(arbeidsgiverId = null, periode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(10), tom = null), person = this, arbeidsgiverType = ArbeidsgiverType.Person.name))
                        },
                    )
                }

            every { persongrunnlagService.hentAktivThrows(vilkårsvurdering.behandling.id) } returns persongrunnlag

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == aktør }
                    .vilkårResultater
                    .find { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }
            assertThat(lovligOppholdResultater?.periodeFom).isEqualTo(LocalDate.now().minusYears(10))
            assertThat(lovligOppholdResultater?.periodeTom).isNull()
            assertThat(lovligOppholdResultater?.resultat).isEqualTo(Resultat.OPPFYLT)
        }

        @Test
        fun `skal gi riktig begrunnelse for oppfylt lovlig opphold vilkår hvis EØS borger og har arbeidsforhold`() {
            // Arrange
            val behandling = lagBehandling()
            val aktør = behandling.fagsak.aktør
            val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

            val persongrunnlag =
                lagPersonopplysningGrunnlag(
                    behandlingId = behandling.id,
                ) { grunnlag ->
                    setOf(
                        lagPerson(aktør = aktør, personopplysningGrunnlag = grunnlag).apply {
                            bostedsadresser =
                                mutableListOf(
                                    GrBostedsadresse.fraBostedsadresse(
                                        Bostedsadresse(
                                            gyldigFraOgMed = LocalDate.now().minusYears(10),
                                            gyldigTilOgMed = null,
                                            vegadresse = lagVegadresse(12345L),
                                        ),
                                        person = this,
                                    ),
                                )
                            statsborgerskap =
                                mutableListOf(
                                    GrStatsborgerskap(landkode = "BE", gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(20), tom = null), medlemskap = Medlemskap.EØS, person = this),
                                )
                            arbeidsforhold = mutableListOf(GrArbeidsforhold(arbeidsgiverId = null, periode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(10), tom = null), person = this, arbeidsgiverType = ArbeidsgiverType.Person.name))
                        },
                    )
                }

            every { persongrunnlagService.hentAktivThrows(vilkårsvurdering.behandling.id) } returns persongrunnlag

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == aktør }
                    .vilkårResultater
                    .single { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }

            assertThat(lovligOppholdResultater.resultat).isEqualTo(Resultat.OPPFYLT)
            assertThat(lovligOppholdResultater.begrunnelse)
                .isEqualTo("$PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT- EØS-borger og har arbeidsforhold i Norge.")
        }

        @Test
        fun `skal preutfylle lovlig opphold vilkår hvis oppholdstillatelse`() {
            // Arrange
            val behandling = lagBehandling()
            val aktør = behandling.fagsak.aktør
            val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

            val persongrunnlag =
                lagPersonopplysningGrunnlag(
                    behandlingId = behandling.id,
                ) { grunnlag ->
                    setOf(
                        lagPerson(aktør = aktør, personopplysningGrunnlag = grunnlag).apply {
                            bostedsadresser =
                                mutableListOf(
                                    GrBostedsadresse.fraBostedsadresse(
                                        Bostedsadresse(
                                            gyldigFraOgMed = LocalDate.now().minusYears(10),
                                            gyldigTilOgMed = null,
                                            vegadresse = lagVegadresse(12345L),
                                        ),
                                        person = this,
                                    ),
                                )
                            opphold =
                                mutableListOf(
                                    GrOpphold(type = OPPHOLDSTILLATELSE.PERMANENT, gyldigPeriode = DatoIntervallEntitet(fom = null, tom = null), person = this),
                                )
                        },
                    )
                }

            every { persongrunnlagService.hentAktivThrows(vilkårsvurdering.behandling.id) } returns persongrunnlag

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == aktør }
                    .vilkårResultater
                    .find { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }

            assertThat(lovligOppholdResultater?.resultat).isEqualTo(Resultat.OPPFYLT)
            assertThat(lovligOppholdResultater?.periodeFom).isEqualTo(LocalDate.now().minusYears(10))
        }

        @Test
        fun `siste periode skal være løpende om tom er satt på oppholdstillatelse`() {
            // Arrange
            val behandling = lagBehandling()
            val aktør = behandling.fagsak.aktør
            val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

            val persongrunnlag =
                lagPersonopplysningGrunnlag(
                    behandlingId = behandling.id,
                ) { grunnlag ->
                    setOf(
                        lagPerson(aktør = aktør, personopplysningGrunnlag = grunnlag).apply {
                            bostedsadresser =
                                mutableListOf(
                                    GrBostedsadresse.fraBostedsadresse(
                                        Bostedsadresse(
                                            gyldigFraOgMed = LocalDate.now().minusYears(5),
                                            gyldigTilOgMed = null,
                                            vegadresse = lagVegadresse(12345L),
                                        ),
                                        person = this,
                                    ),
                                )
                            opphold =
                                mutableListOf(
                                    GrOpphold(type = OPPHOLDSTILLATELSE.MIDLERTIDIG, gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(5), tom = LocalDate.now().plusYears(5)), person = this),
                                )
                        },
                    )
                }

            every { persongrunnlagService.hentAktivThrows(vilkårsvurdering.behandling.id) } returns persongrunnlag

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == aktør }
                    .vilkårResultater
                    .find { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }

            assertThat(lovligOppholdResultater?.resultat).isEqualTo(Resultat.OPPFYLT)
            assertThat(lovligOppholdResultater?.periodeFom).isEqualTo(LocalDate.now().minusYears(5))
            assertThat(lovligOppholdResultater?.periodeTom).isNull()
        }
    }
}
