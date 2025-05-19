package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlBostedsVegadresse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlBostedsadresse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlBostedsadressePerson
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlMatrikkeladresse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

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

        every { pdlRestClient.hentBostedsadresserForPerson(any()) } returns PdlBostedsadressePerson(listOf(PdlBostedsadresse(
            gyldigFraOgMed = LocalDateTime.now().minusYears(4),
            gyldigTilOgMed = LocalDateTime.now().minusYears(3),
            vegadresse = PdlBostedsVegadresse(12345.toBigInteger()),
            matrikkeladresse = null,
            ukjentBosted = null
        ), PdlBostedsadresse(
            gyldigFraOgMed = LocalDateTime.now().minusYears(3).plusDays(1),
            gyldigTilOgMed = LocalDateTime.now().minusYears(2),
            vegadresse = null,
            matrikkeladresse = PdlMatrikkeladresse(54321.toBigInteger()),
            ukjentBosted = null
        ), PdlBostedsadresse(
            gyldigFraOgMed = LocalDateTime.now().minusYears(1),
            gyldigTilOgMed = null,
            vegadresse = PdlBostedsVegadresse(98765.toBigInteger()),
            matrikkeladresse = null,
            ukjentBosted = null
        )))

        // Act
        val vilkårsresutat = preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(personResultat)

        // Assert
        assertThat(vilkårsresutat).hasSize(2)
        assertThat(vilkårsresutat.first().periodeFom).isEqualTo(LocalDate.now().minusYears(4))
        assertThat(vilkårsresutat.first().periodeTom).isEqualTo(LocalDate.now().minusYears(2))
        assertThat(vilkårsresutat.last().periodeFom).isEqualTo(LocalDate.now().minusYears(1))
        assertThat(vilkårsresutat.first().vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
    }
}
