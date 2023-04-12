package no.nav.familie.ba.sak.kjerne.korrigertvedtak

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDate
import javax.persistence.Column
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
@Entity(name = "KorrigertVedtak")
@Table(name = "KORRIGERT_VEDTAK")
class KorrigertVedtak(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "korrigert_vedtak_seq_generator")
    @SequenceGenerator(
        name = "korrigert_vedtak_seq_generator",
        sequenceName = "korrigert_vedtak_seq",
        allocationSize = 50
    )
    val id: Long = 0,

    @Column(name = "vedtaksdato", columnDefinition = "DATE")
    val vedtaksdato: LocalDate,

    @Column(name = "begrunnelse")
    val begrunnelse: String?,

    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_behandling_id")
    val behandling: Behandling,

    @Column(name = "aktiv")
    var aktiv: Boolean
) : BaseEntitet()

data class KorrigerVedtakRequest(
    val vedtaksdato: LocalDate,
    val begrunnelse: String?
)

fun KorrigerVedtakRequest.tilKorrigerVedtak(behandling: Behandling) =
    KorrigertVedtak(vedtaksdato = vedtaksdato, begrunnelse = begrunnelse, behandling = behandling, aktiv = true)
