package no.nav.familie.ba.sak.kjerne.vedtak.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.RestVedtaksbegrunnelse
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Vedtaksbegrunnelse")
@Table(name = "VEDTAKSBEGRUNNELSE")
class Vedtaksbegrunnelse(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vedtaksbegrunnelse_seq_generator")
    @SequenceGenerator(
        name = "vedtaksbegrunnelse_seq_generator",
        sequenceName = "vedtaksbegrunnelse_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "fk_vedtaksperiode_id", nullable = false, updatable = false)
    val vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
    @Enumerated(EnumType.STRING)
    @Column(name = "vedtak_begrunnelse_spesifikasjon", updatable = false)
    val standardbegrunnelse: Standardbegrunnelse,
) {
    fun kopier(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser): Vedtaksbegrunnelse =
        Vedtaksbegrunnelse(
            vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
            standardbegrunnelse = this.standardbegrunnelse,
        )

    override fun toString(): String {
        return "Vedtaksbegrunnelse(id=$id, standardbegrunnelse=$standardbegrunnelse)"
    }
}

fun Vedtaksbegrunnelse.tilRestVedtaksbegrunnelse() =
    RestVedtaksbegrunnelse(
        standardbegrunnelse = this.standardbegrunnelse.enumnavnTilString(),
        vedtakBegrunnelseType = this.standardbegrunnelse.vedtakBegrunnelseType,
        vedtakBegrunnelseSpesifikasjon = this.standardbegrunnelse.enumnavnTilString(),
    )

enum class Begrunnelsetype {
    STANDARD_BEGRUNNELSE,
    EØS_BEGRUNNELSE,
    FRITEKST,
}

sealed interface BrevBegrunnelse : Comparable<BrevBegrunnelse> {
    val type: Begrunnelsetype
    val vedtakBegrunnelseType: VedtakBegrunnelseType?

    override fun compareTo(other: BrevBegrunnelse): Int {
        return when {
            this.type == Begrunnelsetype.FRITEKST -> Int.MAX_VALUE
            other.type == Begrunnelsetype.FRITEKST -> -Int.MAX_VALUE
            this.vedtakBegrunnelseType == null -> Int.MAX_VALUE
            other.vedtakBegrunnelseType == null -> -Int.MAX_VALUE

            else -> this.vedtakBegrunnelseType!!.sorteringsrekkefølge - other.vedtakBegrunnelseType!!.sorteringsrekkefølge
        }
    }
}

interface BegrunnelseMedData : BrevBegrunnelse {
    val apiNavn: String
}

data class BegrunnelseData(
    override val vedtakBegrunnelseType: VedtakBegrunnelseType,
    override val apiNavn: String,
    val gjelderSoker: Boolean,
    val barnasFodselsdatoer: String,
    @Deprecated("Brukes ikke. Kan slettes når vi har fjernet gammel begrunnelseskode.")
    val fodselsdatoerBarnOppfyllerTriggereOgHarUtbetaling: String = "",
    @Deprecated("Brukes ikke. Kan slettes når vi har fjernet gammel begrunnelseskode.")
    val fodselsdatoerBarnOppfyllerTriggereOgHarNullutbetaling: String = "",
    val antallBarn: Int,
    @Deprecated("Brukes ikke. Kan slettes når vi har fjernet gammel begrunnelseskode.")
    val antallBarnOppfyllerTriggereOgHarUtbetaling: Int = 0,
    @Deprecated("Brukes ikke. Kan slettes når vi har fjernet gammel begrunnelseskode.")
    val antallBarnOppfyllerTriggereOgHarNullutbetaling: Int = 0,
    val maanedOgAarBegrunnelsenGjelderFor: String?,
    val maalform: String,
    val belop: String,
    val soknadstidspunkt: String,
    val avtaletidspunktDeltBosted: String,
    val sokersRettTilUtvidet: String,
) : BegrunnelseMedData {
    override val type: Begrunnelsetype = Begrunnelsetype.STANDARD_BEGRUNNELSE
}

data class FritekstBegrunnelse(
    val fritekst: String,
) : BrevBegrunnelse {
    override val vedtakBegrunnelseType: VedtakBegrunnelseType? = null
    override val type: Begrunnelsetype = Begrunnelsetype.FRITEKST
}

sealed class EØSBegrunnelseData : BegrunnelseMedData

data class EØSBegrunnelseDataMedKompetanse(
    override val vedtakBegrunnelseType: VedtakBegrunnelseType,
    override val apiNavn: String,
    val annenForeldersAktivitet: KompetanseAktivitet,
    val annenForeldersAktivitetsland: String?,
    val barnetsBostedsland: String,
    val sokersAktivitet: KompetanseAktivitet,
    val sokersAktivitetsland: String?,
    val barnasFodselsdatoer: String,
    val antallBarn: Int,
    val maalform: String,
) : EØSBegrunnelseData() {
    override val type: Begrunnelsetype = Begrunnelsetype.EØS_BEGRUNNELSE
}

data class EØSBegrunnelseDataUtenKompetanse(
    override val vedtakBegrunnelseType: VedtakBegrunnelseType,
    override val apiNavn: String,
    val barnasFodselsdatoer: String,
    val antallBarn: Int,
    val maalform: String,
    val gjelderSoker: Boolean,
) : EØSBegrunnelseData() {
    override val type: Begrunnelsetype = Begrunnelsetype.EØS_BEGRUNNELSE
}
