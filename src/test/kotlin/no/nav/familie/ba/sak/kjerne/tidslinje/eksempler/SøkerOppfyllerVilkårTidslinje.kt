package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.KalkulerendeTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.hentUtsnitt
import no.nav.familie.ba.sak.kjerne.tidslinje.VilkårRegelverkResultat

class SøkerOppfyllerVilkårTidslinje(
    private val søkersVilkårResultatTemaer: Collection<Tidslinje<VilkårRegelverkResultat>>
) : KalkulerendeTidslinje<Boolean>(søkersVilkårResultatTemaer) {

    override fun kalkulerInnhold(tidspunkt: Tidspunkt): Boolean {
        val erAltOppfylt = søkersVilkårResultatTemaer
            .map { it.hentUtsnitt(tidspunkt) }
            .all { it?.resultat == Resultat.OPPFYLT }
        return erAltOppfylt
    }
}
