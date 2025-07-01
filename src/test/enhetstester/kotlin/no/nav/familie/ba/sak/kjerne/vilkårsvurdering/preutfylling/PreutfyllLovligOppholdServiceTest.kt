package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagVegadresse
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PreutfyllLovligOppholdServiceTest {
    private val pdlRestClient: PdlRestClient = mockk(relaxed = true)
    private val preutfyllLovligOppholdService: PreutfyllLovligOppholdService = PreutfyllLovligOppholdService(pdlRestClient)

    @Test
    fun `skal preutfylle oppfylt lovlig opphold vilkår basert på norsk eller nordisk statsborgerskap`() {
        // Arrange
        val vilkårsvurdering = lagVilkårsvurdering()
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)

        every { pdlRestClient.hentStatsborgerskap(personResultat.aktør, historikk = true) } returns
            listOf(
                Statsborgerskap("SWE", LocalDate.now().minusYears(10), null, null),
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
                Statsborgerskap("NOR", LocalDate.now().minusYears(5), null, null),
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
    fun `skal gi riktig fom og tom på lovlig opphold vilkår`() {
        // Arrange
        val vilkårsvurdering = lagVilkårsvurdering()
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)

        every { pdlRestClient.hentStatsborgerskap(personResultat.aktør, historikk = true) } returns
            listOf(
                Statsborgerskap("ES", LocalDate.now().minusYears(10), LocalDate.now().minusYears(5), null),
                Statsborgerskap("NOR", LocalDate.now().minusYears(5), null, null),
            )

        // Act
        val vilkårResultat = preutfyllLovligOppholdService.genererLovligOppholdVilkårResultat(personResultat = personResultat)

        // Assert
        assertThat(vilkårResultat).hasSize(2)
        assertThat(vilkårResultat.find { it.resultat == Resultat.IKKE_OPPFYLT }).isNotNull
        assertThat(vilkårResultat.find { it.resultat == Resultat.OPPFYLT }).isNotNull
        assertThat(vilkårResultat.first().periodeFom).isEqualTo(LocalDate.now().minusYears(10))
        assertThat(vilkårResultat.first().periodeTom).isEqualTo(LocalDate.now().minusYears(5).minusDays(1))
        assertThat(vilkårResultat.last().periodeFom).isEqualTo(LocalDate.now().minusYears(5))
        assertThat(vilkårResultat.last().periodeTom).isNull()
    }

    @Test
    fun `skal gi riktig begrunnelse for oppfylt lovlig opphold vilkår`() {
        // Arrange
        val vilkårsvurdering = lagVilkårsvurdering()
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)

        every { pdlRestClient.hentStatsborgerskap(personResultat.aktør, historikk = true) } returns
            listOf(
                Statsborgerskap("NOR", LocalDate.now().minusYears(10), null, null),
            )

        // Act
        val vilkårResultat = preutfyllLovligOppholdService.genererLovligOppholdVilkårResultat(personResultat = personResultat)

        // Assert
        assertThat(vilkårResultat).hasSize(1)
        assertThat(vilkårResultat.find { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }?.begrunnelse)
            .isEqualTo("Fylt ut automatisk fra registerdata i PDL \n- Norsk/nordisk statsborgerskap")
    }

    @Test
    fun `skal preutfylle bosatt i riket vilkår i EØS saker`() {
        // Arrange
        val behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS)
        val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)

        every { pdlRestClient.hentStatsborgerskap(personResultat.aktør, historikk = true) } returns
            listOf(
                Statsborgerskap("SWE", LocalDate.now().minusYears(10), null, null),
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
        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.vilkårType).isEqualTo(Vilkår.LOVLIG_OPPHOLD)
            assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
        }
    }
}
