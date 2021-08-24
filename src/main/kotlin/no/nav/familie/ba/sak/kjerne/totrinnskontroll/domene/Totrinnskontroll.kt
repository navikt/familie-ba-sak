package no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.StringListConverter
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

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

        @Column(name = "saksbehandler_id", nullable = false)
        val saksbehandlerId: String,

        @Column(name = "beslutter")
        var beslutter: String? = null,

        @Column(name = "beslutter_id")
        var beslutterId: String? = null,

        @Column(name = "godkjent")
        var godkjent: Boolean = false,

        @Column(name = "kontrollerte_sider")
        @Convert(converter = StringListConverter::class)
        var kontrollerteSider: List<String> = emptyList(),
) : BaseEntitet() {

    fun erBesluttet(): Boolean {
        return beslutter != null
    }

    fun erUgyldig(): Boolean {
        return godkjent && saksbehandler == beslutter &&
               !(saksbehandler == SikkerhetContext.SYSTEM_NAVN && beslutter == SikkerhetContext.SYSTEM_NAVN)
    }
}