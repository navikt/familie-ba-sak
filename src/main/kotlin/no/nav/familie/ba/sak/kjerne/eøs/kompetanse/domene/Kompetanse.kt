package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.YearMonthConverter
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.YearMonth
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.SequenceGenerator
import javax.persistence.Table
import javax.persistence.Transient

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Kompetanse")
@Table(name = "KOMPETANSE")
data class Kompetanse(
    @Column(name = "fom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    val fom: YearMonth?,

    @Column(name = "tom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    val tom: YearMonth?,

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "AKTOER_TIL_KOMPETANSE",
        joinColumns = [JoinColumn(name = "fk_kompetanse_id")],
        inverseJoinColumns = [JoinColumn(name = "fk_aktoer_id")]
    )
    val barnAktører: Set<Aktør> = emptySet(),

    @Enumerated(EnumType.STRING)
    @Column(name = "soekers_aktivitet")
    val søkersAktivitet: SøkersAktivitet? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "annen_forelderes_aktivitet")
    val annenForeldersAktivitet: AnnenForeldersAktivitet? = null,

    @Column(name = "annen_forelderes_aktivitetsland")
    val annenForeldersAktivitetsland: String? = null,

    @Column(name = "barnets_bostedsland")
    val barnetsBostedsland: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "resultat")
    val resultat: KompetanseResultat? = null
) : BaseEntitet() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kompetanse_seq_generator")
    @SequenceGenerator(
        name = "kompetanse_seq_generator",
        sequenceName = "kompetanse_seq",
        allocationSize = 50
    )
    var id: Long = 0

    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    var behandlingId: Long = 0

    @Transient
    var status: KompetanseStatus? = KompetanseStatus.IKKE_UTFYLT
}

enum class KompetanseStatus {
    IKKE_UTFYLT,
    UFULLSTENDIG,
    OK
}

fun Kompetanse.blankUt() = this.copy(
    søkersAktivitet = null,
    annenForeldersAktivitet = null,
    annenForeldersAktivitetsland = null,
    barnetsBostedsland = null,
    resultat = null
)

fun Kompetanse.inneholder(kompetanse: Kompetanse): Boolean {
    return this.bareSkjema() == kompetanse.bareSkjema() &&
        (this.fom == null || this.fom <= kompetanse.fom) &&
        (this.tom == null || this.tom >= kompetanse.tom) &&
        this.barnAktører.containsAll(kompetanse.barnAktører)
}

enum class SøkersAktivitet {
    ARBEIDER_I_NORGE,
    SELVSTENDIG_NÆRINGSDRIVENDE,
    MOTTAR_UTBETALING_FRA_NAV_SOM_ERSTATTER_LØNN,
    UTSENDT_ARBEIDSTAKER_FRA_NORGE,
    MOTTAR_UFØRETRYGD_FRA_NORGE,
    MOTTAR_PENSJON_FRA_NORGE,
    INAKTIV
}

enum class AnnenForeldersAktivitet {
    I_ARBEID,
    MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN,
    FORSIKRET_I_BOSTEDSLAND,
    MOTTAR_PENSJON,
    INAKTIV,
    IKKE_AKTUELT
}

enum class KompetanseResultat {
    NORGE_ER_PRIMÆRLAND,
    NORGE_ER_SEKUNDÆRLAND
}

fun Kompetanse.bareSkjema() =
    this.copy(fom = null, tom = null, barnAktører = emptySet())

fun Kompetanse.utenBarn() =
    this.copy(barnAktører = emptySet())

fun Kompetanse.utenPeriode() =
    this.copy(fom = null, tom = null)
