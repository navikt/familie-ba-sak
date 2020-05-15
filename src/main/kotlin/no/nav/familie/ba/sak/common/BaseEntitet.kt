package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import java.io.Serializable
import java.time.LocalDateTime
import javax.persistence.*

/**
 * En basis [Entity] klasse som håndtere felles standarder for utformign av tabeller (eks. sporing av hvem som har
 * opprettet eller oppdatert en rad, og når).
 */
@MappedSuperclass
abstract class BaseEntitet : Serializable {

    @Column(name = "opprettet_av", nullable = false, updatable = false)
    val opprettetAv: String = SikkerhetContext.hentSaksbehandler()

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now()

    @Column(name = "endret_av")
    var endretAv: String = SikkerhetContext.hentSaksbehandler()

    @Column(name = "endret_tid")
    var endretTidspunkt: LocalDateTime = LocalDateTime.now()

    @Version
    @Column(name = "versjon", nullable = false)
    private val versjon: Long = 0

    @PreUpdate
    protected fun onUpdate() {
        endretAv = SikkerhetContext.hentSaksbehandler()
        endretTidspunkt = LocalDateTime.now()
    }
}
