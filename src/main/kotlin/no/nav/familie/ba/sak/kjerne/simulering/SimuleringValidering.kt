import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Vurderingsform
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.filtrerErUtfylt
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottaker

fun validerIngenAutomatiskeValutakurserFørEtterSisteManuellePostering(
    valutakurser: Collection<Valutakurs>,
    økonomiSimuleringMottaker: List<ØkonomiSimuleringMottaker>,
) {
    val førsteAutomatiskeValutakurs = valutakurser.filtrerErUtfylt().firstOrNull { it.vurderingsform == Vurderingsform.AUTOMATISK }
    val sisteManuellePostering = økonomiSimuleringMottaker.flatMap { it.økonomiSimuleringPostering }.filter { it.erManuellPostering }.maxByOrNull { it.tom }

    if (førsteAutomatiskeValutakurs != null &&
        sisteManuellePostering != null &&
        førsteAutomatiskeValutakurs.fom.isSameOrBefore(sisteManuellePostering.tom.toYearMonth())
    ) {
        throw Feil(
            "Det finnes en automatisk valutakurs som er før siste manuelle postering. " +
                "Vi kan ikke ha valutakurser som er vurdert automatisk før måneden etter til og med datoen til siste manuelle postering",
        )
    }
}
