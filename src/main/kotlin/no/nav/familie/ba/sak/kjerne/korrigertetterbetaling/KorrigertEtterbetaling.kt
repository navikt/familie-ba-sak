package no.nav.familie.ba.sak.kjerne.korrigertetterbetaling

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "KorrigertEtterbetaling")
@Table(name = "KORRIGERT_ETTERBETALING")
class KorrigertEtterbetaling(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "korrigert_etterbetaling_seq_generator")
    @SequenceGenerator(
        name = "korrigert_etterbetaling_seq_generator",
        sequenceName = "korrigert_etterbetaling_seq",
        allocationSize = 50
    )
    val id: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "aarsak")
    val årsak: KorrigertEtterbetalingÅrsak,

    @Column(name = "begrunnelse")
    val begrunnelse: String?,

    @Column(name = "belop")
    val beløp: Int,

    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_behandling_id")
    val behandling: Behandling,

    @Column(name = "aktiv")
    var aktiv: Boolean
) : BaseEntitet()

data class KorrigertEtterbetalingRequest(
    val årsak: KorrigertEtterbetalingÅrsak,
    val begrunnelse: String?,
    val beløp: Int
)

fun KorrigertEtterbetalingRequest.tilKorrigertEtterbetaling(behandling: Behandling) =
    KorrigertEtterbetaling(
        årsak = årsak,
        begrunnelse = begrunnelse,
        behandling = behandling,
        beløp = beløp,
        aktiv = true
    )

enum class KorrigertEtterbetalingÅrsak(val visningsnavn: String) {
    FEIL_TIDLIGERE_UTBETALT_BELØP("Feil i tidligere utbetalt beløp"),
    REFUSJON_FRA_UDI("Refusjon fra UDI"),
    REFUSJON_FRA_ANDRE_MYNDIGHETER("Refusjon fra andre myndigheter"),
    MOTREGNING("Motregning")
}
