package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertRestPerson

data class RestBehandlingsgrunnlagForBrev(
    val personerPåBehandling: List<MinimertRestPerson>,
    val minimertePersonResultater: List<MinimertRestPersonResultat>,
    val minimerteEndredeUtbetalingAndeler: List<MinimertRestEndretAndel>,
) {
    fun finnSøker(): MinimertRestPerson? {
        return personerPåBehandling.find { it.type == PersonType.SØKER }
    }
}
