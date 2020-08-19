package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.kontrakter.felles.objectMapper
import java.time.LocalDate
import java.time.Period

data class Fakta(val personForVurdering: Person,
                 val periode: Periode = Periode(fom = TIDENES_MORGEN, tom = TIDENES_ENDE),
                 val behandlingOpprinnelse: BehandlingOpprinnelse = BehandlingOpprinnelse.MANUELL) {

    val alder = Period.between(personForVurdering.fødselsdato, LocalDate.now()).years
    fun toJson(): String =
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)
}