package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.KalkulerendeTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.PeriodeInnhold
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.idListe
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat

class SøkerOppfyllerVilkårTidslinje(
    private val søkersVilkårResultatTemaer: Collection<Tidslinje<VilkårResultat>>
) : KalkulerendeTidslinje<Boolean>(søkersVilkårResultatTemaer) {

    override fun kalkulerInnhold(tidspunkt: Tidspunkt): PeriodeInnhold<Boolean> {
        val erAltOppfylt = søkersVilkårResultatTemaer
            .map { it.hentUtsnitt(tidspunkt) }
            .all { it.innhold?.resultat == Resultat.OPPFYLT }
        return PeriodeInnhold(erAltOppfylt, søkersVilkårResultatTemaer.map { it.hentUtsnitt(tidspunkt) }.idListe())
    }
}
