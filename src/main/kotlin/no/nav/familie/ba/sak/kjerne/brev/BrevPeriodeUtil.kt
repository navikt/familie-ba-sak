package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.brev.domene.RestBehandlingsgrunnlagForBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertPersonResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertRestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertRestPerson
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilMinimertPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.slf4j.LoggerFactory

private val secureLogger = LoggerFactory.getLogger("secureLogger")

fun List<MinimertRestPerson>.tilBarnasFødselsdatoer(): String =
    Utils.slåSammen(
        this
            .filter { it.type == PersonType.BARN }
            .sortedBy { person ->
                person.fødselsdato
            }
            .map { person ->
                person.fødselsdato.tilKortString()
            }
    )

fun hentRestBehandlingsgrunnlagForBrev(
    persongrunnlag: PersonopplysningGrunnlag,
    vilkårsvurdering: Vilkårsvurdering,
    endredeUtbetalingAndeler: List<EndretUtbetalingAndel>
): RestBehandlingsgrunnlagForBrev {

    return RestBehandlingsgrunnlagForBrev(
        personerPåBehandling = persongrunnlag.søkerOgBarn.map { it.tilMinimertPerson() },
        minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
        minimerteEndredeUtbetalingAndeler = endredeUtbetalingAndeler.map { it.tilMinimertRestEndretUtbetalingAndel() },
    )
}
