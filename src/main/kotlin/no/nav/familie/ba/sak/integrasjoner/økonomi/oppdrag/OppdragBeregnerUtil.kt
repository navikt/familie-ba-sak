package no.nav.familie.ba.sak.integrasjoner.økonomi.oppdrag

object OppdragBeregnerUtil {

    fun validerAndeler(
        forrige: List<AndelData>?,
        nye: List<AndelData>,
    ) {
        val forrigeEllerEmpty = (forrige ?: emptyList()).filter { it.beløp != 0 }
        val id = forrigeEllerEmpty.map { it.id } + nye.map { it.id }
        if (id.size != id.toSet().size) {
            error("Inneholder duplikat av id'er")
        }
        if (forrigeEllerEmpty.any { it.offset == null }) {
            error("Tidligere andeler må inneholde offset ")
        }
        if (nye.any { it.offset != null || it.forrigeOffset != null }) {
            error("Nye andeler skal ikke ha offset")
        }
    }
}
