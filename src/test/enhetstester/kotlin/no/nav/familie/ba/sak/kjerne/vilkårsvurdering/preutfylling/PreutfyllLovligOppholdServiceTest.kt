package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.ArbeidsgiverType
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.arbeidsforhold.GrArbeidsforhold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.opphold.GrOpphold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PreutfyllLovligOppholdServiceTest {
    @Nested
    inner class GenererLovligOppholdVilkårResultatTest {
        private val persongrunnlagService: PersongrunnlagService = mockk(relaxed = true)
        private val preutfyllLovligOppholdService = PreutfyllLovligOppholdService(persongrunnlagService)

        private val behandling = lagBehandling()
        private val søkerAktør = behandling.fagsak.aktør
        private val barn = lagPerson(aktør = randomAktør(), type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(8))
        private val sisteTiÅr = DatoIntervallEntitet(fom = LocalDate.now().minusYears(10), tom = null)
        private val periodeUtenFomOgTom = DatoIntervallEntitet(fom = null, tom = null)

        @Test
        fun `skal preutfylle oppfylt lovlig opphold vilkår basert på norsk eller nordisk statsborgerskap`() {
            // Arrange
            val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

            every { persongrunnlagService.hentAktivThrows(behandling.id) } returns
                lagPersonopplysningGrunnlagMedSøkerOgBarn {
                    it.apply {
                        statsborgerskap =
                            mutableListOf(
                                GrStatsborgerskap(
                                    landkode = "SWE",
                                    gyldigPeriode = sisteTiÅr,
                                    medlemskap = Medlemskap.NORDEN,
                                    person = this,
                                ),
                            )
                    }
                }

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdVilkår =
                vilkårsvurdering.personResultater
                    .first { it.aktør == søkerAktør }
                    .vilkårResultater
                    .single { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }
            assertThat(lovligOppholdVilkår.resultat).isEqualTo(Resultat.OPPFYLT)
        }

        @Test
        fun `skal preutfylle lovlig opphold med ikke-oppfylte perioder når statsborgerskap ikke er norsk eller nordisk, og ingen arbeidsforhold`() {
            // Arrange
            val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

            every { persongrunnlagService.hentAktivThrows(behandling.id) } returns
                lagPersonopplysningGrunnlagMedSøkerOgBarn { søker ->
                    søker.apply {
                        statsborgerskap =
                            mutableListOf(
                                GrStatsborgerskap(
                                    landkode = "ES",
                                    gyldigPeriode = sisteTiÅr,
                                    medlemskap = Medlemskap.EØS,
                                    person = this,
                                ),
                                GrStatsborgerskap(
                                    landkode = "NOR",
                                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(2).plusDays(1), tom = null),
                                    medlemskap = Medlemskap.NORDEN,
                                    person = this,
                                ),
                            )
                    }
                }

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == søkerAktør }
                    .vilkårResultater
                    .filter { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }
            assertThat(lovligOppholdResultater).hasSize(2)
            assertThat(lovligOppholdResultater.first { it.resultat == Resultat.IKKE_OPPFYLT }).isNotNull
            assertThat(lovligOppholdResultater.last { it.resultat == Resultat.OPPFYLT }).isNotNull
        }

        @Test
        fun `skal gi riktig fom og tom på lovlig opphold vilkår på nordisk statsborger`() {
            // Arrange
            val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

            every { persongrunnlagService.hentAktivThrows(behandling.id) } returns
                lagPersonopplysningGrunnlagMedSøkerOgBarn { søker ->
                    søker.apply {
                        statsborgerskap =
                            mutableListOf(
                                GrStatsborgerskap(
                                    landkode = "ES",
                                    gyldigPeriode = sisteTiÅr,
                                    medlemskap = Medlemskap.EØS,
                                    person = this,
                                ),
                                GrStatsborgerskap(
                                    landkode = "NOR",
                                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(5).plusDays(1), tom = null),
                                    medlemskap = Medlemskap.NORDEN,
                                    person = this,
                                ),
                            )
                    }
                }

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == søkerAktør }
                    .vilkårResultater
                    .filter { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }

            assertThat(lovligOppholdResultater).hasSize(2)
            assertThat(lovligOppholdResultater.find { it.resultat == Resultat.IKKE_OPPFYLT }).isNotNull
            assertThat(lovligOppholdResultater.find { it.resultat == Resultat.OPPFYLT }).isNotNull
            assertThat(lovligOppholdResultater.first().periodeFom).isEqualTo(barn.fødselsdato)
            assertThat(lovligOppholdResultater.first().periodeTom).isEqualTo(LocalDate.now().minusYears(5))
            assertThat(lovligOppholdResultater.last().periodeFom).isEqualTo(LocalDate.now().minusYears(5).plusDays(1))
            assertThat(lovligOppholdResultater.last().periodeTom).isNull()
        }

        @Test
        fun `skal sette fom på lovlig opphold vilkår lik første bostedsadresse i Norge, om fom ikke finnes på statsborgerskap`() {
            // Arrange
            val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

            every { persongrunnlagService.hentAktivThrows(behandling.id) } returns
                lagPersonopplysningGrunnlagMedSøkerOgBarn { søker ->
                    søker.apply {
                        statsborgerskap =
                            mutableListOf(
                                GrStatsborgerskap(
                                    landkode = "SWE",
                                    gyldigPeriode = periodeUtenFomOgTom,
                                    medlemskap = Medlemskap.NORDEN,
                                    person = this,
                                ),
                            )
                    }
                }

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == søkerAktør }
                    .vilkårResultater
                    .single { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }

            assertThat(lovligOppholdResultater.periodeFom).isEqualTo(barn.fødselsdato)
            assertThat(lovligOppholdResultater.resultat).isEqualTo(Resultat.OPPFYLT)
        }

        @Test
        fun `skal gi riktig begrunnelse for oppfylt lovlig opphold vilkår hvis nordisk statsborger`() {
            // Arrange
            val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

            every { persongrunnlagService.hentAktivThrows(behandling.id) } returns
                lagPersonopplysningGrunnlagMedSøkerOgBarn { søker ->
                    søker.apply {
                        statsborgerskap =
                            mutableListOf(
                                GrStatsborgerskap(
                                    landkode = "NOR",
                                    gyldigPeriode = sisteTiÅr,
                                    medlemskap = Medlemskap.NORDEN,
                                    person = this,
                                ),
                            )
                    }
                }

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == søkerAktør }
                    .vilkårResultater
                    .single { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }

            assertThat(lovligOppholdResultater.begrunnelse)
                .isEqualTo("$PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT- Norsk/nordisk statsborgerskap.")
        }

        @Test
        fun `skal preutfylle oppfylt lovlig opphold vilkår hvis EØS borger og har arbeidsforhold`() {
            // Arrange
            val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

            every { persongrunnlagService.hentAktivThrows(behandling.id) } returns
                lagPersonopplysningGrunnlagMedSøkerOgBarn { søker ->
                    søker.apply {
                        statsborgerskap =
                            mutableListOf(
                                GrStatsborgerskap(
                                    landkode = "BE",
                                    gyldigPeriode = sisteTiÅr,
                                    medlemskap = Medlemskap.EØS,
                                    person = this,
                                ),
                            )
                        arbeidsforhold =
                            mutableListOf(
                                GrArbeidsforhold(
                                    arbeidsgiverId = null,
                                    periode = sisteTiÅr,
                                    person = this,
                                    arbeidsgiverType = ArbeidsgiverType.Person.name,
                                ),
                            )
                    }
                }

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == søkerAktør }
                    .vilkårResultater
                    .find { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }
            assertThat(lovligOppholdResultater?.periodeFom).isEqualTo(barn.fødselsdato)
            assertThat(lovligOppholdResultater?.periodeTom).isNull()
            assertThat(lovligOppholdResultater?.resultat).isEqualTo(Resultat.OPPFYLT)
        }

        @Test
        fun `skal håndtere flere av samme statsborgerskap ved EØS-sjekk`() {
            // Arrange
            val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

            every { persongrunnlagService.hentAktivThrows(behandling.id) } returns
                lagPersonopplysningGrunnlagMedSøkerOgBarn { søker ->
                    søker.apply {
                        statsborgerskap =
                            mutableListOf(
                                GrStatsborgerskap(
                                    landkode = "BE",
                                    gyldigPeriode = sisteTiÅr,
                                    medlemskap = Medlemskap.EØS,
                                    person = this,
                                ),
                                GrStatsborgerskap(
                                    landkode = "BE",
                                    gyldigPeriode = sisteTiÅr,
                                    medlemskap = Medlemskap.EØS,
                                    person = this,
                                ),
                                GrStatsborgerskap(
                                    landkode = "GUF",
                                    gyldigPeriode = periodeUtenFomOgTom,
                                    medlemskap = Medlemskap.TREDJELANDSBORGER,
                                    person = this,
                                ),
                                GrStatsborgerskap(
                                    landkode = "GUF",
                                    gyldigPeriode = periodeUtenFomOgTom,
                                    medlemskap = Medlemskap.TREDJELANDSBORGER,
                                    person = this,
                                ),
                            )
                        arbeidsforhold =
                            mutableListOf(
                                GrArbeidsforhold(
                                    arbeidsgiverId = null,
                                    periode = sisteTiÅr,
                                    person = this,
                                    arbeidsgiverType = ArbeidsgiverType.Person.name,
                                ),
                            )
                    }
                }

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == søkerAktør }
                    .vilkårResultater
                    .find { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }
            assertThat(lovligOppholdResultater?.periodeFom).isEqualTo(barn.fødselsdato)
            assertThat(lovligOppholdResultater?.periodeTom).isNull()
            assertThat(lovligOppholdResultater?.resultat).isEqualTo(Resultat.OPPFYLT)
        }

        @Test
        fun `skal gi riktig begrunnelse for oppfylt lovlig opphold vilkår hvis EØS borger og har arbeidsforhold`() {
            // Arrange
            val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

            every { persongrunnlagService.hentAktivThrows(behandling.id) } returns
                lagPersonopplysningGrunnlagMedSøkerOgBarn { søker ->
                    søker.apply {
                        statsborgerskap =
                            mutableListOf(
                                GrStatsborgerskap(
                                    landkode = "BE",
                                    gyldigPeriode = sisteTiÅr,
                                    medlemskap = Medlemskap.EØS,
                                    person = this,
                                ),
                            )
                        arbeidsforhold =
                            mutableListOf(
                                GrArbeidsforhold(
                                    arbeidsgiverId = null,
                                    periode = sisteTiÅr,
                                    person = this,
                                    arbeidsgiverType = ArbeidsgiverType.Person.name,
                                ),
                            )
                    }
                }

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == søkerAktør }
                    .vilkårResultater
                    .single { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }

            assertThat(lovligOppholdResultater.resultat).isEqualTo(Resultat.OPPFYLT)
            assertThat(lovligOppholdResultater.begrunnelse)
                .isEqualTo("$PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT- EØS-borger og har arbeidsforhold i Norge.")
        }

        @Test
        fun `skal preutfylle lovlig opphold vilkår hvis oppholdstillatelse`() {
            // Arrange
            val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

            every { persongrunnlagService.hentAktivThrows(behandling.id) } returns
                lagPersonopplysningGrunnlagMedSøkerOgBarn { søker ->
                    søker.apply {
                        opphold =
                            mutableListOf(
                                GrOpphold(
                                    type = OPPHOLDSTILLATELSE.PERMANENT,
                                    gyldigPeriode = periodeUtenFomOgTom,
                                    person = this,
                                ),
                            )
                    }
                }

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == søkerAktør }
                    .vilkårResultater
                    .find { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }

            assertThat(lovligOppholdResultater?.resultat).isEqualTo(Resultat.OPPFYLT)
            assertThat(lovligOppholdResultater?.periodeFom).isEqualTo(barn.fødselsdato)
        }

        @Test
        fun `skal ikke preutfylle lovlig opphold om personen har ukrainsk statsborgerskap`() {
            // Arrange
            val aktør = randomAktør()
            val persongrunnlag =
                lagPersonopplysningGrunnlag {
                    setOf(
                        lagPerson(
                            personIdent = PersonIdent(aktør.aktivFødselsnummer()),
                            aktør = aktør,
                        ).also { person ->
                            person.opphold =
                                mutableListOf(
                                    GrOpphold(
                                        type = OPPHOLDSTILLATELSE.MIDLERTIDIG,
                                        gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(1), tom = LocalDate.now().plusYears(1)),
                                        person = person,
                                    ),
                                )
                            person.statsborgerskap =
                                mutableListOf(
                                    GrStatsborgerskap(
                                        landkode = "UKR",
                                        gyldigPeriode =
                                            DatoIntervallEntitet(
                                                fom = LocalDate.now().minusYears(10),
                                                tom = null,
                                            ),
                                        person = person,
                                    ),
                                )
                        },
                    )
                }

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    lagPersonResultater = {
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                aktør = aktør,
                                lagVilkårResultater = { emptySet() },
                                lagAnnenVurderinger = { emptySet() },
                            ),
                        )
                    },
                )

            every { persongrunnlagService.hentAktivThrows(vilkårsvurdering.behandling.id) } returns persongrunnlag

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdVilkårResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == aktør }
                    .vilkårResultater

            assertThat(lovligOppholdVilkårResultater).isEmpty()
        }

        @Test
        fun `skal sette lovlig opphold til ikke oppfylt fra dato oppholdstillatelsen her utløpt`() {
            // Arrange
            val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)
            val personopplysningsgrunnlag =
                lagPersonopplysningGrunnlagMedSøkerOgBarn { søker ->
                    søker.apply {
                        opphold =
                            mutableListOf(
                                GrOpphold(
                                    type = OPPHOLDSTILLATELSE.MIDLERTIDIG,
                                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(10), tom = LocalDate.now().minusYears(1)),
                                    person = this,
                                ),
                            )
                        statsborgerskap =
                            mutableListOf(
                                GrStatsborgerskap(
                                    landkode = "AFG",
                                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(10), tom = null),
                                    person = this,
                                ),
                            )
                    }
                }

            every { persongrunnlagService.hentAktivThrows(behandling.id) } returns personopplysningsgrunnlag

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == søkerAktør }
                    .vilkårResultater
                    .filter { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }

            assertThat(lovligOppholdResultater).hasSize(2)

            val oppfyltPeriode = lovligOppholdResultater.single { it.resultat == Resultat.OPPFYLT }
            val ikkeOppfyltPeriode = lovligOppholdResultater.single { it.resultat == Resultat.IKKE_OPPFYLT }

            assertThat(oppfyltPeriode.periodeFom).isEqualTo(barn.fødselsdato)
            assertThat(oppfyltPeriode.periodeTom).isEqualTo(LocalDate.now().minusYears(1))

            assertThat(ikkeOppfyltPeriode.periodeFom).isEqualTo(LocalDate.now().minusYears(1).plusDays(1))
            assertThat(ikkeOppfyltPeriode.periodeTom).isNull()
        }

        private fun lagPersonopplysningGrunnlagMedSøkerOgBarn(
            søker: (Person) -> Person = { it },
        ): PersonopplysningGrunnlag =
            lagPersonopplysningGrunnlag(behandlingId = behandling.id) { grunnlag ->
                setOf(
                    barn,
                    søker(lagPerson(aktør = søkerAktør, personopplysningGrunnlag = grunnlag)),
                )
            }
    }
}
