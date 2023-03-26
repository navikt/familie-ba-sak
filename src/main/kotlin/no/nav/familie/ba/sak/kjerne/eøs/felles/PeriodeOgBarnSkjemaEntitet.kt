package no.nav.familie.ba.sak.kjerne.eøs.felles

import no.nav.familie.ba.sak.common.BaseEntitet
import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class PeriodeOgBarnSkjemaEntitet<T : PeriodeOgBarnSkjema<T>> :
    BaseEntitet(),
    PeriodeOgBarnSkjema<T> {

    abstract var id: Long
    abstract var behandlingId: Long // TODO må være en basic type?
}
