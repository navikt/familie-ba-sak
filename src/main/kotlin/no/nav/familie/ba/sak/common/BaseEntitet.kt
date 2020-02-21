package no.nav.familie.ba.sak.common

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
    var opprettetAv: String? = null

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    var opprettetTidspunkt: LocalDateTime? = null

    @Column(name = "endret_av")
    var endretAv: String? = null

    @Column(name = "endret_tid")
    var endretTidspunkt: LocalDateTime? = null

    @Version
    @Column(name = "versjon", nullable = false)
    private val versjon: Long = 0

    @PrePersist
    protected fun onCreate() {
        opprettetAv = finnBrukernavn()
        opprettetTidspunkt = LocalDateTime.now()
    }

    @PreUpdate
    protected fun onUpdate() {
        endretAv = finnBrukernavn()
        endretTidspunkt = LocalDateTime.now()
    }

    companion object {
        private const val BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES = "VL"
        private fun finnBrukernavn(): String {
            val brukerident: String? = SikkerhetContext.hentSaksbehandler()
            return brukerident ?: BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES
        }
    }
}
