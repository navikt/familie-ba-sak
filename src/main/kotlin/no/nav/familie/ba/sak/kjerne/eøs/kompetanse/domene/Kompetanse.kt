package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.YearMonthConverter
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEntitet
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import java.time.YearMonth

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Kompetanse")
@Table(name = "KOMPETANSE")
data class Kompetanse(
    @Column(name = "fom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    override val fom: YearMonth?,
    @Column(name = "tom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    override val tom: YearMonth?,
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "AKTOER_TIL_KOMPETANSE",
        joinColumns = [JoinColumn(name = "fk_kompetanse_id")],
        inverseJoinColumns = [JoinColumn(name = "fk_aktoer_id")],
    )
    // kan ikke være tom
    override val barnAktører: Set<Aktør> = emptySet(),
    @Enumerated(EnumType.STRING)
    @Column(name = "soekers_aktivitet")
    val søkersAktivitet: KompetanseAktivitet? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "annen_forelderes_aktivitet")
    val annenForeldersAktivitet: KompetanseAktivitet? = null,
    @Column(name = "annen_forelderes_aktivitetsland")
    val annenForeldersAktivitetsland: String? = null,
    @Column(name = "sokers_aktivitetsland")
    val søkersAktivitetsland: String? = null,
    @Column(name = "barnets_bostedsland")
    val barnetsBostedsland: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "resultat")
    val resultat: KompetanseResultat? = null,
    @Column(name = "er_annen_forelder_omfattet_av_norsk_lovgivning")
    val erAnnenForelderOmfattetAvNorskLovgivning: Boolean? = false,
) : PeriodeOgBarnSkjemaEntitet<Kompetanse>() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kompetanse_seq_generator")
    @SequenceGenerator(
        name = "kompetanse_seq_generator",
        sequenceName = "kompetanse_seq",
        allocationSize = 50,
    )
    override var id: Long = 0

    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    override var behandlingId: Long = 0

    override fun utenInnhold() =
        this.copy(
            søkersAktivitet = null,
            søkersAktivitetsland = null,
            annenForeldersAktivitet = null,
            annenForeldersAktivitetsland = null,
            barnetsBostedsland = null,
            resultat = null,
        )

    override fun kopier(
        fom: YearMonth?,
        tom: YearMonth?,
        barnAktører: Set<Aktør>,
    ) = copy(
        fom = fom,
        tom = tom,
        barnAktører = barnAktører.toSet(),
    )

    fun erObligatoriskeFelterSatt() =
        fom != null &&
            erObligatoriskeFelterUtenomTidsperioderSatt()

    fun erObligatoriskeFelterUtenomTidsperioderSatt() =
        this.søkersAktivitet != null &&
            this.annenForeldersAktivitet != null &&
            this.søkersAktivitetsland != null &&
            this.barnetsBostedsland != null &&
            this.resultat != null &&
            this.barnAktører.isNotEmpty()

    companion object {
        val NULL = Kompetanse(null, null, emptySet())
    }
}

enum class KompetanseAktivitet(
    val gyldigForSøker: Boolean,
    val gyldigForAnnenForelder: Boolean,
) {
    ARBEIDER(true, false),
    SELVSTENDIG_NÆRINGSDRIVENDE(true, false),
    UTSENDT_ARBEIDSTAKER_FRA_NORGE(true, false),
    MOTTAR_UFØRETRYGD(true, false),
    ARBEIDER_PÅ_NORSKREGISTRERT_SKIP(true, false),
    ARBEIDER_PÅ_NORSK_SOKKEL(true, false),
    ARBEIDER_FOR_ET_NORSK_FLYSELSKAP(true, false),
    ARBEIDER_VED_UTENLANDSK_UTENRIKSSTASJON(true, false),
    MOTTAR_UTBETALING_FRA_NAV_UNDER_OPPHOLD_I_UTLANDET(true, false),
    MOTTAR_UFØRETRYGD_FRA_NAV_UNDER_OPPHOLD_I_UTLANDET(true, false),
    MOTTAR_PENSJON_FRA_NAV_UNDER_OPPHOLD_I_UTLANDET(true, false),

    MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN(true, true),
    MOTTAR_PENSJON(true, true),
    INAKTIV(true, true),

    I_ARBEID(false, true),
    FORSIKRET_I_BOSTEDSLAND(false, true),
    IKKE_AKTUELT(false, true),
    UTSENDT_ARBEIDSTAKER(false, true),
}

enum class KompetanseResultat {
    NORGE_ER_PRIMÆRLAND,
    NORGE_ER_SEKUNDÆRLAND,
    TO_PRIMÆRLAND,
}

sealed interface IKompetanse {
    val id: Long
    val behandlingId: Long
}

data class TomKompetanse(
    override val id: Long,
    override val behandlingId: Long,
) : IKompetanse

interface IUtfyltKompetanse : IKompetanse {
    override val id: Long
    override val behandlingId: Long
    val barnAktører: Set<Aktør>
    val søkersAktivitet: KompetanseAktivitet
    val annenForeldersAktivitet: KompetanseAktivitet
    val annenForeldersAktivitetsland: String?
    val søkersAktivitetsland: String
    val barnetsBostedsland: String
    val resultat: KompetanseResultat
    val erAnnenForelderOmfattetAvNorskLovgivning: Boolean
}

data class UtfyltKompetanse(
    val fom: YearMonth,
    val tom: YearMonth?,
    override val id: Long,
    override val behandlingId: Long,
    override val barnAktører: Set<Aktør>,
    override val søkersAktivitet: KompetanseAktivitet,
    override val annenForeldersAktivitet: KompetanseAktivitet,
    override val annenForeldersAktivitetsland: String?,
    override val søkersAktivitetsland: String,
    override val barnetsBostedsland: String,
    override val resultat: KompetanseResultat,
    override val erAnnenForelderOmfattetAvNorskLovgivning: Boolean,
) : IUtfyltKompetanse

data class UtfyltKompetanseUtenTidsperiode(
    override val id: Long,
    override val behandlingId: Long,
    override val barnAktører: Set<Aktør>,
    override val søkersAktivitet: KompetanseAktivitet,
    override val annenForeldersAktivitet: KompetanseAktivitet,
    override val annenForeldersAktivitetsland: String?,
    override val søkersAktivitetsland: String,
    override val barnetsBostedsland: String,
    override val resultat: KompetanseResultat,
    override val erAnnenForelderOmfattetAvNorskLovgivning: Boolean,
) : IUtfyltKompetanse

fun Kompetanse.tilIKompetanse(): IKompetanse =
    if (this.erObligatoriskeFelterSatt()) {
        UtfyltKompetanse(
            id = this.id,
            behandlingId = this.behandlingId,
            fom = this.fom!!,
            tom = this.tom,
            barnAktører = this.barnAktører,
            søkersAktivitet = this.søkersAktivitet!!,
            annenForeldersAktivitet = this.annenForeldersAktivitet!!,
            annenForeldersAktivitetsland = this.annenForeldersAktivitetsland,
            søkersAktivitetsland = this.søkersAktivitetsland!!,
            barnetsBostedsland = this.barnetsBostedsland!!,
            resultat = this.resultat!!,
            erAnnenForelderOmfattetAvNorskLovgivning = this.erAnnenForelderOmfattetAvNorskLovgivning!!,
        )
    } else if (this.erObligatoriskeFelterUtenomTidsperioderSatt()) {
        UtfyltKompetanseUtenTidsperiode(
            id = this.id,
            behandlingId = this.behandlingId,
            barnAktører = this.barnAktører,
            søkersAktivitet = this.søkersAktivitet!!,
            annenForeldersAktivitet = this.annenForeldersAktivitet!!,
            annenForeldersAktivitetsland = this.annenForeldersAktivitetsland,
            søkersAktivitetsland = this.søkersAktivitetsland!!,
            barnetsBostedsland = this.barnetsBostedsland!!,
            resultat = this.resultat!!,
            erAnnenForelderOmfattetAvNorskLovgivning = this.erAnnenForelderOmfattetAvNorskLovgivning!!,
        )
    } else {
        TomKompetanse(
            id = this.id,
            behandlingId = this.behandlingId,
        )
    }

fun List<UtfyltKompetanse>.tilTidslinje() =
    this
        .map {
            Periode(
                verdi = it,
                fom = it.fom.førsteDagIInneværendeMåned(),
                tom = it.tom?.sisteDagIInneværendeMåned(),
            )
        }.tilTidslinje()

fun Collection<Kompetanse>.tilUtfylteKompetanserEtterEndringstidpunktPerAktør(endringstidspunkt: YearMonth): Map<Aktør, List<UtfyltKompetanse>> {
    val alleBarnAktørIder = this.map { it.barnAktører }.reduce { akk, neste -> akk + neste }

    val utfylteKompetanser =
        this
            .map { it.tilIKompetanse() }
            .filterIsInstance<UtfyltKompetanse>()

    return alleBarnAktørIder.associateWith { aktør ->
        utfylteKompetanser
            .filter { it.barnAktører.contains(aktør) }
            .tilTidslinje()
            .beskjærFraOgMed(endringstidspunkt.førsteDagIInneværendeMåned())
            .tilPerioder()
            .mapNotNull { it.verdi }
    }
}

fun Kompetanse.utbetalingsland(): String? {
    val kompetanse = this.tilIKompetanse()
    return when (kompetanse) {
        is IUtfyltKompetanse -> kompetanse.utbetalingsland()
        is TomKompetanse -> null
    }
}

fun IUtfyltKompetanse.utbetalingsland(): String {
    if (this.resultat == KompetanseResultat.NORGE_ER_PRIMÆRLAND) return "NO"

    // Hovedregel
    val utbetalingsland =
        if (this.erAnnenForelderOmfattetAvNorskLovgivning) {
            this.søkersAktivitetsland
        } else {
            this.annenForeldersAktivitetsland ?: this.barnetsBostedsland
        }

    return when (utbetalingsland) {
        "NO" -> {
            // Unntak. Finner landet som er registrert på kompetansen som ikke er Norge.
            setOf(this.søkersAktivitetsland, this.annenForeldersAktivitetsland, this.barnetsBostedsland).filterNotNull().singleOrNull { it != "NO" } ?: utbetalingsland
        }

        else -> {
            utbetalingsland
        }
    }
}
