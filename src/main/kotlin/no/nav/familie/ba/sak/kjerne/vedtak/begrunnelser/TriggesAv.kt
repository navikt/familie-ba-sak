package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.kjerne.brev.domene.EndretUtbetalingsperiodeDeltBostedTriggere
import no.nav.familie.ba.sak.kjerne.brev.domene.Valgbarhet
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

data class TriggesAv(
    val vilkår: Set<Vilkår>,
    val personTyper: Set<PersonType>,
    val personerManglerOpplysninger: Boolean,
    val satsendring: Boolean,
    val barnMedSeksårsdag: Boolean,
    val vurderingAnnetGrunnlag: Boolean,
    val medlemskap: Boolean,
    val deltbosted: Boolean,
    val deltBostedSkalIkkeDeles: Boolean,
    val valgbar: Boolean,
    val valgbarhet: Valgbarhet?,
    val endringsaarsaker: Set<Årsak>,
    val etterEndretUtbetaling: Boolean,
    val endretUtbetalingSkalUtbetales: EndretUtbetalingsperiodeDeltBostedTriggere,
    val småbarnstillegg: Boolean,
    val gjelderFørstePeriode: Boolean,
    val gjelderFraInnvilgelsestidspunkt: Boolean,
    val barnDød: Boolean,
)
