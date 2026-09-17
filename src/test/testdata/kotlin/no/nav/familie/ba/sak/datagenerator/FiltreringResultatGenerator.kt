package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.Filtreringsregel
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.domene.FiltreringResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat

fun lagFiltreringResultat(
    id: Long = 0,
    behandlingId: Long = 1,
    filtreringsregel: Filtreringsregel.Identifikator = Filtreringsregel.Identifikator.MOR_LEVER,
    resultat: Resultat = Resultat.OPPFYLT,
    begrunnelse: String = "Mor lever",
    evalueringsårsaker: List<String> = emptyList(),
    regelInput: String? = null,
): FiltreringResultat =
    FiltreringResultat(
        id = id,
        behandlingId = behandlingId,
        filtreringsregel = filtreringsregel,
        resultat = resultat,
        begrunnelse = begrunnelse,
        evalueringsårsaker = evalueringsårsaker,
        regelInput = regelInput,
    )
