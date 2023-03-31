package no.nav.familie.ba.sak.kjerne.eøs.felles

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingId
import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class PeriodeOgBarnSkjemaEntitet<T : PeriodeOgBarnSkjema<T>> :
    BaseEntitet(),
    PeriodeOgBarnSkjema<T> {

    abstract var id: Long
    abstract var behandlingId: BehandlingId
}
