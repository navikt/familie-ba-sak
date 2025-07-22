package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagVegadresse
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Ansettelsesperiode
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsforhold
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Periode
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.StatsborgerskapService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PreutfyllLovligOppholdServiceTest {
    @Nested
    inner class GenererLovligOppholdVilkårResultatTest {
        private val pdlRestClient: PdlRestClient = mockk(relaxed = true)
        private val statsborgerskapService = mockk<StatsborgerskapService>(relaxed = true)
        private val integrasjonClient: IntegrasjonClient = mockk(relaxed = true)
        private val preutfyllLovligOppholdService: PreutfyllLovligOppholdService = PreutfyllLovligOppholdService(pdlRestClient, statsborgerskapService, integrasjonClient)

        @Test
        fun `skal preutfylle oppfylt lovlig opphold vilkår basert på norsk eller nordisk statsborgerskap`() {
            // Arrange
            val vilkårsvurdering = lagVilkårsvurdering()
            val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)

            every { pdlRestClient.hentStatsborgerskap(personResultat.aktør, historikk = true) } returns
                listOf(
                    Statsborgerskap("SWE", LocalDate.now().minusYears(10), null, null),
                )
            every { pdlRestClient.hentBostedsadresserForPerson(any()) } returns
                listOf(
                    Bostedsadresse(
                        gyldigFraOgMed = LocalDate.now().minusYears(6),
                        gyldigTilOgMed = null,
                        vegadresse = lagVegadresse(12345L),
                    ),
                )

            // Act
            val vilkårResultat = preutfyllLovligOppholdService.genererLovligOppholdVilkårResultat(personResultat = personResultat)

            // Assert
            assertThat(vilkårResultat).hasSize(1)
            assertThat(vilkårResultat.find { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }).isNotNull
            assertThat(vilkårResultat.find { it.resultat == Resultat.OPPFYLT }).isNotNull
        }

        @Test
        fun `skal preutfylle lovlig opphold med ikke-oppfylte perioder når statsborgerskap ikke er norsk eller nordisk`() {
            // Arrange
            val vilkårsvurdering = lagVilkårsvurdering()
            val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)

            every { pdlRestClient.hentStatsborgerskap(personResultat.aktør, historikk = true) } returns
                listOf(
                    Statsborgerskap("ES", LocalDate.now().minusYears(10), LocalDate.now().minusYears(5), null),
                    Statsborgerskap("NOR", LocalDate.now().minusYears(5).plusDays(1), null, null),
                )

            every { pdlRestClient.hentBostedsadresserForPerson(any()) } returns
                listOf(
                    Bostedsadresse(
                        gyldigFraOgMed = LocalDate.now().minusYears(10),
                        gyldigTilOgMed = null,
                        vegadresse = lagVegadresse(12345L),
                    ),
                )

            // Act
            val vilkårResultat = preutfyllLovligOppholdService.genererLovligOppholdVilkårResultat(personResultat = personResultat)

            // Assert
            assertThat(vilkårResultat).hasSize(2)
            assertThat(vilkårResultat).allSatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.LOVLIG_OPPHOLD)
            }
            assertThat(vilkårResultat.find { it.resultat == Resultat.IKKE_OPPFYLT }).isNotNull
            assertThat(vilkårResultat.find { it.resultat == Resultat.OPPFYLT }).isNotNull
        }

        @Test
        fun `skal gi riktig fom og tom på lovlig opphold vilkår på nordisk statsborger`() {
            // Arrange
            val vilkårsvurdering = lagVilkårsvurdering()
            val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)

            every { pdlRestClient.hentStatsborgerskap(personResultat.aktør, historikk = true) } returns
                listOf(
                    Statsborgerskap("ES", LocalDate.now().minusYears(10), LocalDate.now().minusYears(5), null),
                    Statsborgerskap("NOR", LocalDate.now().minusYears(5).plusDays(1), null, null),
                )

            every { pdlRestClient.hentBostedsadresserForPerson(any()) } returns
                listOf(
                    Bostedsadresse(
                        gyldigFraOgMed = LocalDate.now().minusYears(10),
                        gyldigTilOgMed = null,
                        vegadresse = lagVegadresse(12345L),
                    ),
                )

            // Act
            val vilkårResultat = preutfyllLovligOppholdService.genererLovligOppholdVilkårResultat(personResultat = personResultat)

            // Assert
            assertThat(vilkårResultat).hasSize(2)
            assertThat(
                vilkårResultat.find
                    { it.resultat == Resultat.IKKE_OPPFYLT },
            ).isNotNull
            assertThat(
                vilkårResultat.find
                    { it.resultat == Resultat.OPPFYLT },
            ).isNotNull
            assertThat(vilkårResultat.first().periodeFom).isEqualTo(LocalDate.now().minusYears(10))
            assertThat(vilkårResultat.first().periodeTom).isEqualTo(LocalDate.now().minusYears(5))
            assertThat(vilkårResultat.last().periodeFom).isEqualTo(LocalDate.now().minusYears(5).plusDays(1))
            assertThat(vilkårResultat.last().periodeTom).isNull()
        }

        @Test
        fun `skal sette fom på lovlig opphold vilkår lik første bostedsadresse i Norge, om fom ikke finnes på statsborgerskap`() {
            // Arrange
            val vilkårsvurdering = lagVilkårsvurdering()
            val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)

            every { pdlRestClient.hentStatsborgerskap(personResultat.aktør, historikk = true) } returns
                listOf(
                    Statsborgerskap("SWE", null, null, null),
                )

            every { pdlRestClient.hentBostedsadresserForPerson(any()) } returns
                listOf(
                    Bostedsadresse(
                        gyldigFraOgMed = LocalDate.now().minusYears(1),
                        vegadresse = lagVegadresse(12345L),
                    ),
                )

            // Act
            val vilkårResultat = preutfyllLovligOppholdService.genererLovligOppholdVilkårResultat(personResultat = personResultat)

            // Assert
            assertThat(vilkårResultat).hasSize(1)
            assertThat(vilkårResultat.first().periodeFom).isEqualTo(LocalDate.now().minusYears(1))
        }

        @Test
        fun `skal gi riktig begrunnelse for oppfylt lovlig opphold vilkår hvis nordisk statsborger`() {
            // Arrange
            val vilkårsvurdering = lagVilkårsvurdering()
            val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)

            every { pdlRestClient.hentStatsborgerskap(personResultat.aktør, historikk = true) } returns
                listOf(
                    Statsborgerskap("NOR", LocalDate.now().minusYears(10), null, null),
                )

            every { pdlRestClient.hentBostedsadresserForPerson(any()) } returns
                listOf(
                    Bostedsadresse(
                        gyldigFraOgMed = LocalDate.now().minusYears(10),
                        gyldigTilOgMed = null,
                        vegadresse = lagVegadresse(12345L),
                    ),
                )

            // Act
            val vilkårResultat = preutfyllLovligOppholdService.genererLovligOppholdVilkårResultat(personResultat = personResultat)

            // Assert
            assertThat(vilkårResultat).hasSize(1)
            assertThat(vilkårResultat.find { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }?.begrunnelse)
                .isEqualTo("Fylt ut automatisk fra registerdata i PDL\n- Norsk/nordisk statsborgerskap.")
        }

        @Test
        fun `skal gi riktig begrunnelse for oppfylt lovlig opphold vilkår hvis eøs borger og arbeidsforhold`() {
            // Arrange
            val vilkårsvurdering = lagVilkårsvurdering()
            val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)

            every { pdlRestClient.hentStatsborgerskap(personResultat.aktør, historikk = true) } returns
                listOf(
                    Statsborgerskap("BE", LocalDate.now().minusYears(20), null, null),
                )
            every { pdlRestClient.hentBostedsadresserForPerson(any()) } returns
                listOf(
                    Bostedsadresse(
                        gyldigFraOgMed = LocalDate.now().minusYears(10),
                        gyldigTilOgMed = null,
                        vegadresse = lagVegadresse(12345L),
                    ),
                )
            every { statsborgerskapService.hentSterkesteMedlemskap(Statsborgerskap("BE", LocalDate.now().minusYears(20), null, null)) } returns Medlemskap.EØS
            every { integrasjonClient.hentArbeidsforhold(any(), LocalDate.now().minusYears(10)) } returns
                listOf(Arbeidsforhold(arbeidsgiver = null, ansettelsesperiode = Ansettelsesperiode(Periode(LocalDate.now().minusYears(10), null))))

            // Act
            val vilkårResultat = preutfyllLovligOppholdService.genererLovligOppholdVilkårResultat(personResultat = personResultat)

            // Assert
            assertThat(vilkårResultat).hasSize(1)
            assertThat(vilkårResultat.find { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }?.begrunnelse)
                .isEqualTo("Fylt ut automatisk fra registerdata i PDL\n- EØS-borger og har arbeidsforhold i Norge.")
        }

        @Test
        fun `skal preutfylle oppfylt lovlig opphold vilkår hvis EØS borger og arbeidsforhold`() {
            // Arrange
            val vilkårsvurdering = lagVilkårsvurdering()
            val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)

            every { pdlRestClient.hentStatsborgerskap(personResultat.aktør, historikk = true) } returns
                listOf(
                    Statsborgerskap("BE", LocalDate.now().minusYears(20), null, null),
                )

            every { pdlRestClient.hentBostedsadresserForPerson(any()) } returns
                listOf(
                    Bostedsadresse(
                        gyldigFraOgMed = LocalDate.now().minusYears(5),
                        gyldigTilOgMed = null,
                        vegadresse = lagVegadresse(12345L),
                    ),
                )
            every { statsborgerskapService.hentSterkesteMedlemskap(Statsborgerskap("BE", LocalDate.now().minusYears(20), null, null)) } returns Medlemskap.EØS

            every { integrasjonClient.hentArbeidsforhold(any(), LocalDate.now().minusYears(5)) } returns
                listOf(Arbeidsforhold(arbeidsgiver = null, ansettelsesperiode = Ansettelsesperiode(Periode(LocalDate.now().minusYears(5), null))))

            // Act
            val vilkårResultat = preutfyllLovligOppholdService.genererLovligOppholdVilkårResultat(personResultat = personResultat)

            // Assert
            assertThat(vilkårResultat).hasSize(1)
            assertThat(vilkårResultat.first().periodeFom).isEqualTo(LocalDate.now().minusYears(5))
            assertThat(vilkårResultat.first().periodeTom).isNull()
            assertThat(vilkårResultat.first().resultat).isEqualTo(Resultat.OPPFYLT)
        }

        @Test
        fun `skal preutfylle oppfylt lovlig vilkår hvis EØS borger og arbeidsforhold, med ikke oppfylt periode uten norsk bostedadresse`() {
            // Arrange
            val vilkårsvurdering = lagVilkårsvurdering()
            val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)

            every { pdlRestClient.hentStatsborgerskap(personResultat.aktør, historikk = true) } returns
                listOf(
                    Statsborgerskap("BE", LocalDate.now().minusYears(20), null, null),
                )

            every { pdlRestClient.hentBostedsadresserForPerson(any()) } returns
                listOf(
                    Bostedsadresse(
                        gyldigFraOgMed = LocalDate.now().minusYears(5),
                        gyldigTilOgMed = LocalDate.now().minusYears(3),
                        vegadresse = lagVegadresse(12345L),
                    ),
                    Bostedsadresse(
                        gyldigFraOgMed = LocalDate.now().minusYears(1),
                        vegadresse = lagVegadresse(54321L),
                    ),
                )
            every { statsborgerskapService.hentSterkesteMedlemskap(Statsborgerskap("BE", LocalDate.now().minusYears(20), null, null)) } returns Medlemskap.EØS

            every { integrasjonClient.hentArbeidsforhold(any(), LocalDate.now().minusYears(5)) } returns
                listOf(Arbeidsforhold(arbeidsgiver = null, ansettelsesperiode = Ansettelsesperiode(Periode(LocalDate.now().minusYears(5), null))))

            // Act
            val vilkårResultat = preutfyllLovligOppholdService.genererLovligOppholdVilkårResultat(personResultat = personResultat)

            // Assert
            assertThat(vilkårResultat).hasSize(3)
            assertThat(vilkårResultat.first().periodeFom).isEqualTo(LocalDate.now().minusYears(5))
            assertThat(vilkårResultat.first().periodeTom).isEqualTo(LocalDate.now().minusYears(3))
            assertThat(vilkårResultat.first().resultat).isEqualTo(Resultat.OPPFYLT)

            assertThat(vilkårResultat.find { it.resultat == Resultat.IKKE_OPPFYLT }!!.periodeFom).isEqualTo(LocalDate.now().minusYears(3).plusDays(1))
            assertThat(vilkårResultat.find { it.resultat == Resultat.IKKE_OPPFYLT }!!.periodeTom).isEqualTo(LocalDate.now().minusYears(1).minusDays(1))

            assertThat(vilkårResultat.last().periodeFom).isEqualTo(LocalDate.now().minusYears(1))
            assertThat(vilkårResultat.last().periodeTom).isNull()
            assertThat(vilkårResultat.last().resultat).isEqualTo(Resultat.OPPFYLT)
        }
    }
}
