package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagMatrikkeladresse
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagVegadresse
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PreutfyllBosattIRiketServiceTest {
    private var pdlRestClient: PdlRestClient = mockk(relaxed = true)
    private var preutfyllBosattIRiketService = PreutfyllBosattIRiketService(pdlRestClient)

    @Test
    fun `skal lage preutfylt vilkårresultat basert på data fra pdl`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id)
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)

        every { pdlRestClient.hentBostedsadresserForPerson(any()) } returns
            listOf(
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusYears(4),
                    gyldigTilOgMed = LocalDate.now().minusYears(3),
                    vegadresse = lagVegadresse(12345L),
                ),
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusYears(3).plusDays(1),
                    gyldigTilOgMed = LocalDate.now().minusYears(2),
                    matrikkeladresse = lagMatrikkeladresse(54321L),
                ),
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusYears(1),
                    gyldigTilOgMed = null,
                    vegadresse = lagVegadresse(98765L),
                ),
            )

        // Act
        val vilkårsresutat = preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(personResultat)

        // Assert
        assertThat(vilkårsresutat).hasSize(3)
        assertThat(vilkårsresutat.first().periodeFom).isEqualTo(LocalDate.now().minusYears(4))
        assertThat(vilkårsresutat.first().periodeTom).isEqualTo(LocalDate.now().minusYears(2))
        assertThat(vilkårsresutat.last().periodeFom).isEqualTo(LocalDate.now().minusYears(1))
        assertThat(vilkårsresutat.first().vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
        assertThat(vilkårsresutat.find { it.resultat == Resultat.IKKE_OPPFYLT }).isNotNull
    }

    @Test
    fun `skal gi vilkårsresultat IKKE_OPPFYLT hvis bosatt i riket er mindre enn 12 mnd`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id)
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)

        every { pdlRestClient.hentBostedsadresserForPerson(any()) } returns
            listOf(
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusMonths(6),
                    gyldigTilOgMed = LocalDate.now().minusMonths(4),
                    vegadresse = lagVegadresse(12345L),
                ),
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusMonths(4).plusDays(1),
                    gyldigTilOgMed = LocalDate.now().minusMonths(2),
                    matrikkeladresse = lagMatrikkeladresse(54321L),
                ),
            )

        // Act
        val vilkårsresutat = preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(personResultat)

        // Assert
        assertThat(vilkårsresutat.first().resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
        assertThat(vilkårsresutat.find { it.resultat == Resultat.IKKE_OPPFYLT }).isNotNull
    }
}
