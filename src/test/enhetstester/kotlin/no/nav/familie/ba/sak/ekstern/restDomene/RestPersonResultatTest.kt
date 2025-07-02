package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class RestPersonResultatTest {
    @Test
    fun `Skal nullstille automatisk begrunnelse hvis vilkår er endret av saksbehandler`() {
        // Arrange
        val personident = randomFnr()
        val restVilkårResultat =
            RestVilkårResultat(
                id = 1L,
                vilkårType = BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = LocalDate.now(),
                periodeTom = LocalDate.now().plusDays(1),
                begrunnelse = "Fylt ut automatisk fra registerdata i PDL",
                endretAv = "Test",
                endretTidspunkt = LocalDateTime.now(),
                behandlingId = 1L,
                erVurdert = true,
                erAutomatiskVurdert = true,
                begrunnelseForManuellKontroll = "Fylt ut automatisk fra søknad",
            )
        val restPersonResultat = RestPersonResultat(personIdent = personident, vilkårResultater = listOf(restVilkårResultat))

        // Act
        val nyttRestPersonResultat = restPersonResultat.fjernAutomatiskBegrunnelse()

        // Assert
        nyttRestPersonResultat.vilkårResultater.forEach {
            assertThat(it.begrunnelse).doesNotContain("Fylt ut automatisk fra registerdata i PDL")
            assertThat(it.erAutomatiskVurdert).isFalse()
            assertThat(it.begrunnelseForManuellKontroll).isNull()
        }
    }
}
