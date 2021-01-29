package no.nav.familie.ba.sak.totrinnskontroll.domene

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import javax.persistence.*

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Totrinnskontroll")
@Table(name = "TOTRINNSKONTROLL")
data class Totrinnskontroll(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "totrinnskontroll_seq_generator")
        @SequenceGenerator(name = "totrinnskontroll_seq_generator", sequenceName = "totrinnskontroll_seq", allocationSize = 50)
        val id: Long = 0,

        @ManyToOne(optional = false) @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
        val behandling: Behandling,

        @Column(name = "aktiv", nullable = false)
        var aktiv: Boolean = true,

        @Column(name = "saksbehandler", nullable = false)
        val saksbehandler: String,

        @Column(name = "beslutter")
        var beslutter: String? = null,

        @Column(name = "godkjent")
        var godkjent: Boolean = false
) : BaseEntitet() {

    fun erBesluttet(): Boolean {
        return beslutter != null
    }

    fun erUgyldig(): Boolean {
        return saksbehandler == beslutter &&
               !(saksbehandler == SikkerhetContext.SYSTEM_NAVN && beslutter == SikkerhetContext.SYSTEM_NAVN)
    }
}