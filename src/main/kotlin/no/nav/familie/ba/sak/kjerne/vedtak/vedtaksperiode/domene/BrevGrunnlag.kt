package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene

import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertPerson

data class BrevGrunnlag(
    val personerPåBehandling: List<MinimertPerson>,
    val minimertePersonResultater: List<MinimertPersonResultat>,
    val minimerteEndredeUtbetalingAndeler: List<MinimertEndretUtbetalingAndel>,
)



