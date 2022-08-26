package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class VilkårsresultatDagTidslinjeKtTest {

    @Test
    fun `kan ha to overlappende perioder hvis det er bor med søker-vilkåret`() {
        val personAktørId = randomAktør()
        val behandling = lagBehandling()
        val vilkårsvurdering = lagVilkårsvurdering(personAktørId, behandling, Resultat.OPPFYLT)

        val personResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = personAktørId
        )

        val vilkårResultat = setOf(
            VilkårResultat(
                personResultat = personResultat,
                resultat = Resultat.OPPFYLT,
                vilkårType = Vilkår.BOR_MED_SØKER,
                periodeFom = LocalDate.of(2020, 3, 31),
                periodeTom = null,
                begrunnelse = "",
                behandlingId = personResultat.vilkårsvurdering.behandling.id
            ),
            VilkårResultat(
                personResultat = personResultat,
                resultat = Resultat.IKKE_OPPFYLT,
                vilkårType = Vilkår.BOR_MED_SØKER,
                periodeFom = null,
                periodeTom = null,
                begrunnelse = "",
                behandlingId = personResultat.vilkårsvurdering.behandling.id
            )
        )

        val tidslinje = vilkårResultat.tilVilkårRegelverkResultatTidslinje()
        tidslinje.perioder()
    }
}
