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
        val personResultat = PersonResultat(
            vilkårsvurdering = lagVilkårsvurdering(personAktørId, lagBehandling(), Resultat.OPPFYLT),
            aktør = personAktørId
        )

        val vilkårResultat = setOf(
            lagVilkårResultat(personResultat, Resultat.OPPFYLT, fom = LocalDate.of(2020, 3, 31)),
            lagVilkårResultat(personResultat, Resultat.IKKE_OPPFYLT, fom = null)
        )

        val tidslinje = vilkårResultat.tilVilkårRegelverkResultatTidslinje()
        tidslinje.perioder()
    }

    private fun lagVilkårResultat(personResultat: PersonResultat, resultat: Resultat, fom: LocalDate?) =
        VilkårResultat(
            personResultat = personResultat,
            resultat = resultat,
            vilkårType = Vilkår.BOR_MED_SØKER,
            periodeFom = fom,
            periodeTom = null,
            begrunnelse = "",
            behandlingId = personResultat.vilkårsvurdering.behandling.id
        )
}
