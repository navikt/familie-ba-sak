package no.nav.familie.ba.sak.kjerne.eøs.util

import no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering.tilSisteVirkedag
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Vurderingsform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import java.time.YearMonth

class ValutakursBuilder(
    startMåned: YearMonth = jan(2020),
    behandlingId: BehandlingId = BehandlingId(1),
    private val automatiskSettValutakursdato: Boolean = false,
) : SkjemaBuilder<Valutakurs, ValutakursBuilder>(startMåned, behandlingId) {
    fun medKurs(
        k: String,
        valutakode: String?,
        vararg barn: Person,
    ) = medSkjema(k, barn.toList()) {
        when {
            it == '-' -> {
                Valutakurs.NULL
            }

            it == '$' -> {
                Valutakurs.NULL.copy(valutakode = valutakode)
            }

            it?.isDigit() ?: false -> {
                Valutakurs.NULL.copy(
                    kurs = it?.digitToInt()?.toBigDecimal(),
                    valutakode = valutakode,
                    valutakursdato = null,
                )
            }

            else -> {
                null
            }
        }
    }.apply {
        if (automatiskSettValutakursdato) {
            medTransformasjon { valutakurs ->
                if (valutakurs.fom != null && valutakurs.valutakursdato == null) {
                    valutakurs.copy(
                        valutakursdato = valutakurs.fom.minusMonths(1).tilSisteVirkedag()
                    )
                } else {
                    valutakurs
                }
            }
        }
    }

    fun medVurderingsform(vurderingsform: Vurderingsform) = medTransformasjon { utenlandskPeriodebeløp -> utenlandskPeriodebeløp.copy(vurderingsform = vurderingsform) }
}
