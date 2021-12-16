package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertRestPerson

data class BrevGrunnlag(
    val personerPåBehandling: List<MinimertRestPerson>,
    val minimertePersonResultater: List<MinimertPersonResultat>,
    val minimerteEndredeUtbetalingAndeler: List<MinimertRestEndretAndel>,
)
