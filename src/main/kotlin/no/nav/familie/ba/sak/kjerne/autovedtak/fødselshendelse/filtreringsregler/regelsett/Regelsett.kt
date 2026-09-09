package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.regelsett

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.Filtreringsregel
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.FiltreringsreglerFakta

class Regelsett<T : FiltreringsreglerFakta>(
    val regel: Filtreringsregel,
    val evaluer: (T) -> Evaluering,
)



