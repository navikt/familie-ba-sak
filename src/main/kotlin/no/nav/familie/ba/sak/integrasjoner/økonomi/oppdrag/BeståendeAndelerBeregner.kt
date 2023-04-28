package no.nav.familie.ba.sak.integrasjoner.økonomi.oppdrag

import java.time.YearMonth

private sealed interface BeståendeAndelResultat
private object NyAndelSkriverOver : BeståendeAndelResultat
private class Opphørsdato(val opphør: YearMonth) : BeståendeAndelResultat
private class AvkortAndel(val andel: AndelData, val opphør: YearMonth? = null) : BeståendeAndelResultat

data class BeståendeAndeler(
    val andeler: List<AndelData>,
    val opphørFra: YearMonth? = null
)
object BeståendeAndelerBeregner {

    // TODO på en eller annen måte så skal de som beholdes ha riktig ID, men få oppdatert offset/forrigeOffset/kildeBehandlingId fra forrige beandling
    fun finnBeståendeAndeler(
        forrige: List<AndelData>,
        nye: List<AndelData>
    ): BeståendeAndeler {
        val index = finnIndexPåFørsteDiff(forrige, nye)

        val beståendeAndeler = index?.let {
            val opphørsdato = finnBeståendeAndelOgOpphør(it, forrige, nye)
            when (opphørsdato) {
                is Opphørsdato -> {
                    BeståendeAndeler(forrige.subList(0, index), opphørsdato.opphør)
                }
                is AvkortAndel -> {
                    val avkortetAndeler = forrige.subList(0, maxOf(0, index - 1))
                    BeståendeAndeler(avkortetAndeler + opphørsdato.andel, opphørsdato.opphør)
                }
                is NyAndelSkriverOver -> {
                    BeståendeAndeler(forrige.subList(0, maxOf(0, index)))
                }
            }
        } ?: BeståendeAndeler(forrige, null)

        return beståendeAndeler
    }

    private fun finnBeståendeAndelOgOpphør(
        index: Int,
        forrige: List<AndelData>,
        nye: List<AndelData>
    ): BeståendeAndelResultat {
        val forrige = forrige[index]
        val ny = if (nye.size > index) nye[index] else null
        val nyNeste = if (nye.size > index + 1) nye[index + 1] else null

        if (ny == null || forrige.fom < ny.fom) {
            return Opphørsdato(forrige.fom)
        }
        if (forrige.fom > ny.fom || forrige.beløp != ny.beløp) {
            return NyAndelSkriverOver
        }
        if (forrige.tom > ny.tom) {
            val opphørsdato = if (nyNeste == null || nyNeste.fom != ny.tom.plusMonths(1)) {
                ny.tom.plusMonths(1)
            } else {
                null
            }
            return AvkortAndel(forrige.copy(tom = ny.tom), opphørsdato)
        }
        return NyAndelSkriverOver
    }

    private fun finnIndexPåFørsteDiff(
        forrige: List<AndelData>,
        nye: List<AndelData>
    ): Int? {
        forrige.forEachIndexed { index, andelData ->
            if (nye.size > index) {
                val nyAndelForIndex = nye[index]
                if (!andelData.erLik(nyAndelForIndex)) {
                    return index
                }
            } else {
                return index
            }
        }
        return null
    }

    fun AndelData.erLik(other: AndelData): Boolean =
        this.fom == other.tom &&
            this.tom == other.tom &&
            this.beløp == other.beløp
}
