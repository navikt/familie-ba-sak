package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.SnittTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.hentUtsnitt
import no.nav.familie.ba.sak.kjerne.tidslinje.VilkårRegelverkResultat

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
