package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.VilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.SnittTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.hentUtsnitt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt

class BarnetsYtelseTidslinje(
    private val søkerOppfyllerVilkårTema: Tidslinje<Boolean>,
    private val barnetsVilkårsresultater: Collection<Tidslinje<VilkårRegelverkResultat>>
) : SnittTidslinje<YtelseType>(barnetsVilkårsresultater + søkerOppfyllerVilkårTema) {
    override fun beregnSnitt(tidspunkt: Tidspunkt): YtelseType? {
        val søkerOppfyllerVilkår = søkerOppfyllerVilkårTema.hentUtsnitt(tidspunkt)
        val barnetsVilkår = barnetsVilkårsresultater.map { it.hentUtsnitt(tidspunkt) }
        val erBarnetsVilkårOppfylt = barnetsVilkår.all { it?.resultat == Resultat.OPPFYLT }

        return if (søkerOppfyllerVilkår == true && erBarnetsVilkårOppfylt)
            YtelseType.ORDINÆR_BARNETRYGD
        else
            null
    }
}
