package no.nav.familie.ba.sak.dataGenerator.brev

import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.MinimertPerson
import java.time.LocalDate

fun lagMinimertPerson(
    type: PersonType = PersonType.BARN,
    fødselsdato: LocalDate = LocalDate.now().minusYears(if (type == PersonType.BARN) 2 else 30),
    aktivPersonIdent: String = randomFnr(),
    aktørId: String = randomAktørId(aktivPersonIdent).aktørId,
) = MinimertPerson(
    type = type,
    fødselsdato = fødselsdato,
    aktivPersonIdent = aktivPersonIdent,
    aktørId = aktørId
)
