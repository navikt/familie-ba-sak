package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.KalkulerendeTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.PeriodeInnhold
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidslinje
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.idListe
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.inneholder
import no.nav.familie.ba.sak.kjerne.tidslinje.VilkårRegelverkResultat

class BarnetsYtelseTidslinje(
    private val søkerOppfyllerVilkårTema: Tidslinje<Boolean>,
    private val barnetsVilkårsresultater: List<Tidslinje<VilkårRegelverkResultat>>
) : KalkulerendeTidslinje<YtelseType>(barnetsVilkårsresultater + søkerOppfyllerVilkårTema) {
    override fun kalkulerInnhold(tidspunkt: Tidspunkt): PeriodeInnhold<YtelseType> {
        val søkersFragment = søkerOppfyllerVilkårTema.hentUtsnitt(tidspunkt)
        val barnasFragmenter = barnetsVilkårsresultater.map { it.hentUtsnitt(tidspunkt) }
        val erBarnetsVilkårOppfylt = barnasFragmenter.all { it.innhold?.resultat == Resultat.OPPFYLT }

        return if (søkersFragment.inneholder(true) && erBarnetsVilkårOppfylt)
            PeriodeInnhold(YtelseType.ORDINÆR_BARNETRYGD, (barnasFragmenter + søkersFragment).idListe())
        else
            PeriodeInnhold(avhengerAv = (barnasFragmenter + søkersFragment).idListe())
    }
}
