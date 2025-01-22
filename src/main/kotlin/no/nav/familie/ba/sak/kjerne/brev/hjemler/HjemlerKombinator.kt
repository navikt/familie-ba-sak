package no.nav.familie.ba.sak.kjerne.brev.hjemler

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.kjerne.brev.slåSammen
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform

fun kombinerHjemler(
    målform: Målform,
    separasjonsavtaleStorbritanniaHjemler: List<String>,
    ordinæreHjemler: List<String>,
    folketrygdlovenHjemler: List<String>,
    eøsForordningen883Hjemler: List<String>,
    eøsForordningen987Hjemler: List<String>,
    forvaltningslovenHjemler: List<String>,
): List<String> {
    val alleHjemlerForBegrunnelser = mutableListOf<String>()

    // Rekkefølgen her er viktig
    if (separasjonsavtaleStorbritanniaHjemler.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add(
            "${
                when (målform) {
                    Målform.NB -> "Separasjonsavtalen mellom Storbritannia og Norge artikkel"
                    Målform.NN -> "Separasjonsavtalen mellom Storbritannia og Noreg artikkel"
                }
            } ${
                separasjonsavtaleStorbritanniaHjemler.slåSammen()
            }",
        )
    }

    if (ordinæreHjemler.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add(
            "${
                when (målform) {
                    Målform.NB -> "barnetrygdloven"
                    Målform.NN -> "barnetrygdlova"
                }
            } ${
                hjemlerTilHjemmeltekst(
                    hjemler = ordinæreHjemler,
                    lovForHjemmel = "barnetrygdloven",
                )
            }",
        )
    }

    if (folketrygdlovenHjemler.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add(
            "${
                when (målform) {
                    Målform.NB -> "folketrygdloven"
                    Målform.NN -> "folketrygdlova"
                }
            } ${
                hjemlerTilHjemmeltekst(
                    hjemler = folketrygdlovenHjemler,
                    lovForHjemmel = "folketrygdloven",
                )
            }",
        )
    }

    if (eøsForordningen883Hjemler.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add("EØS-forordning 883/2004 artikkel ${eøsForordningen883Hjemler.slåSammen()}")
    }

    if (eøsForordningen987Hjemler.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add("EØS-forordning 987/2009 artikkel ${eøsForordningen987Hjemler.slåSammen()}")
    }

    if (forvaltningslovenHjemler.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add(
            "${
                when (målform) {
                    Målform.NB -> "forvaltningsloven"
                    Målform.NN -> "forvaltningslova"
                }
            } ${
                hjemlerTilHjemmeltekst(
                    hjemler = forvaltningslovenHjemler,
                    lovForHjemmel = "forvaltningsloven",
                )
            }",
        )
    }

    return alleHjemlerForBegrunnelser
}

private fun hjemlerTilHjemmeltekst(
    hjemler: List<String>,
    lovForHjemmel: String,
): String =
    when (hjemler.size) {
        0 -> throw Feil("Kan ikke lage hjemmeltekst for $lovForHjemmel når ingen begrunnelser har hjemler fra $lovForHjemmel knyttet til seg.")
        1 -> "§ ${hjemler[0]}"
        else -> "§§ ${hjemler.slåSammen()}"
    }
