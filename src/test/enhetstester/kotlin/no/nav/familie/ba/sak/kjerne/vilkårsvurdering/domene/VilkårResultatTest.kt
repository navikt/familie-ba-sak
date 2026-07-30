package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårResultatTest {
    @Nested
    inner class Nullstill {
        @Test
        fun `skal nullstille vilkår resultat`() {
            // Arrange
            val dagensDato = LocalDate.now()

            val vilkårResultat =
                lagVilkårResultat(
                    periodeFom = dagensDato,
                    periodeTom = dagensDato.plusDays(1),
                    begrunnelse = "begrunnelse",
                    utdypendeVilkårsvurderinger =
                        listOf(
                            UtdypendeVilkårsvurdering.DELT_BOSTED,
                        ),
                    resultat = Resultat.OPPFYLT,
                )

            // Act
            vilkårResultat.nullstill()

            // Assert
            assertThat(vilkårResultat.periodeFom).isNull()
            assertThat(vilkårResultat.periodeTom).isNull()
            assertThat(vilkårResultat.begrunnelse).isEmpty()
            assertThat(vilkårResultat.utdypendeVilkårsvurderinger).isEmpty()
            assertThat(vilkårResultat.resultat).isEqualTo(Resultat.IKKE_VURDERT)
        }
    }
}
