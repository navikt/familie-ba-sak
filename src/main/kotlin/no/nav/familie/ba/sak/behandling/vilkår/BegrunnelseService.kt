package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform

object BegrunnelseService {

    private val begrunnelser = listOf(
            Begrunnelse(BegrunnelseKategori.INNVILGET,
                        Vilkår.BOSATT_I_RIKET,
                        "Norsk, nordisk, tredjelandsborger med lovlig opphold samtidig som bosatt i Norge"
            ) { gjelderSøker, barnasFødselsdatoer, vilkårsdato, målform ->
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi${if (gjelderSøker && barnasFødselsdatoer.isNotBlank()) " du og " else if (gjelderSøker) " du " else " "}${if (barnasFødselsdatoer.isNotBlank()) "barn født $barnasFødselsdatoer " else ""}er bosatt i Norge fra $vilkårsdato."
                    Målform.NN -> ""
                }
            },

            Begrunnelse(BegrunnelseKategori.INNVILGET,
                        Vilkår.LOVLIG_OPPHOLD,
                        "Tredjelandsborger bosatt før lovlig opphold i Norge"
            ) { gjelderSøker, barnasFødselsdatoer, vilkårsdato, målform ->
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi${if (gjelderSøker && barnasFødselsdatoer.isNotBlank()) " du og " else if (gjelderSøker) " du " else " "}${if (barnasFødselsdatoer.isNotBlank()) "barn født $barnasFødselsdatoer " else ""}har oppholdstillatelse fra $vilkårsdato."
                    Målform.NN -> ""
                }
            },
            Begrunnelse(BegrunnelseKategori.INNVILGET, Vilkår.LOVLIG_OPPHOLD, "EØS-borger: Søker har oppholdsrett"
            ) { _, _, vilkårsdato, målform ->
                when (målform) {
                    Målform.NB -> "Du får barnetrygd fordi du har oppholdsrett som EØS-borger fra $vilkårsdato."
                    Målform.NN -> ""
                }
            },
            Begrunnelse(BegrunnelseKategori.REDUKSJON,
                        Vilkår.BOSATT_I_RIKET,
                        ""
            ) { gjelderSøker, barnasFødselsdatoer, vilkårsdato, målform ->
                when (målform) {
                    Målform.NB -> "Barnetrygden reduseres fordi barn født $barnasFødselsdatoer har flyttet fra deg $vilkårsdato" // TODO: Husk å håndtere reduksjoner med tom-datoer i stedet for fom
                    Målform.NN -> ""
                }
            },
    )

    fun hentBegrunnelserFor(vilkår: Vilkår, begrunnelsekategori: BegrunnelseKategori): List<Begrunnelse> = begrunnelser.filter { it.relevantFor == vilkår && it.begrunnelsekategori == begrunnelsekategori }
    fun hentBegrunnelserFor(vilkår: Vilkår): List<Begrunnelse> = begrunnelser.filter { it.relevantFor == vilkår}


    data class Begrunnelse(val begrunnelsekategori: BegrunnelseKategori,
                           val relevantFor: Vilkår, // TODO: Relevant for flere? Gjøre om til liste?
                           val tittel: String,
                           val beskrivelseGenerator:
                           (gjelderSøker: Boolean,
                            barnasFødselsdatoer: String,
                            vilkårsdato: String,
                            målform: Målform)
                           -> String
    )

    enum class BegrunnelseKategori {
        INNVILGET,
        REDUKSJON
    }
}