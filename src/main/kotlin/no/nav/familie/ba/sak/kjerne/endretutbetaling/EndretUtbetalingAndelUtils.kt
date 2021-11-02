package no.nav.familie.ba.sak.kjerne.endretutbetaling.domene

import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPersonerMedAndeler
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import java.time.YearMonth

fun erStartPåUtvidetSammeMåned(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    andelTilkjentYtelser: List<AndelTilkjentYtelse>,
    fom: YearMonth?,
) = personopplysningGrunnlag.tilRestPersonerMedAndeler(andelTilkjentYtelser)
    .find { it.personIdent == personopplysningGrunnlag.søker.personIdent.ident }
    ?.ytelsePerioder
    ?.find { it.stønadFom == fom }?.ytelseType == YtelseType.UTVIDET_BARNETRYGD
