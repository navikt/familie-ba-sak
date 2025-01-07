import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.Utils.storForbokstav
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.GrunnlagForBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.hentBarnasFødselsdatoerForBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.hentSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityPeriodeResultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.EØSBegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.EØSBegrunnelseDataMedKompetanse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.EØSBegrunnelseDataUtenKompetanse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.IBegrunnelseGrunnlagForPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.begrunnelseGjelderOpphørFraForrigeBehandling
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.hentGyldigeBegrunnelserPerPerson

fun EØSStandardbegrunnelse.lagBrevBegrunnelse(
    vedtaksperiode: VedtaksperiodeMedBegrunnelser,
    grunnlag: GrunnlagForBegrunnelse,
    begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>,
    landkoder: Map<String, String>,
): List<EØSBegrunnelseData> {
    val sanityBegrunnelse = hentSanityBegrunnelse(grunnlag)
    val personerGjeldendeForBegrunnelse =
        vedtaksperiode
            .hentGyldigeBegrunnelserPerPerson(
                grunnlag,
            ).mapNotNull { (person, begrunnelserPåPerson) -> person.takeIf { this in begrunnelserPåPerson } }
    val periodegrunnlagForPersonerIBegrunnelse =
        begrunnelsesGrunnlagPerPerson.filter { (person, _) -> person in personerGjeldendeForBegrunnelse }

    val gjelderSøker = personerGjeldendeForBegrunnelse.any { it.type == PersonType.SØKER }
    val begrunnelseGjelderSøkerOgOpphørFraForrigeBehandling = (sanityBegrunnelse.begrunnelseGjelderOpphørFraForrigeBehandling()) && gjelderSøker
    val kompetanser =
        when (sanityBegrunnelse.periodeResultat) {
            SanityPeriodeResultat.INNVILGET_ELLER_ØKNING,
            SanityPeriodeResultat.INGEN_ENDRING,
            -> periodegrunnlagForPersonerIBegrunnelse.values.mapNotNull { it.dennePerioden.kompetanse }

            SanityPeriodeResultat.IKKE_INNVILGET,
            SanityPeriodeResultat.REDUKSJON,
            -> {
                if (begrunnelseGjelderSøkerOgOpphørFraForrigeBehandling) {
                    begrunnelsesGrunnlagPerPerson.values.mapNotNull { it.sammePeriodeForrigeBehandling?.kompetanse }
                } else {
                    periodegrunnlagForPersonerIBegrunnelse.values.mapNotNull { it.forrigePeriode?.kompetanse }
                }
            }

            else -> error("Feltet 'periodeResultat' er ikke satt for begrunnelse fra sanity '${sanityBegrunnelse.apiNavn}'.")
        }

    return if (kompetanser.isEmpty() && sanityBegrunnelse.periodeResultat == SanityPeriodeResultat.IKKE_INNVILGET) {
        val barnasFødselsdatoer =
            sanityBegrunnelse.hentBarnasFødselsdatoerForBegrunnelse(
                grunnlag = grunnlag,
                gjelderSøker = gjelderSøker,
                personerIBegrunnelse = personerGjeldendeForBegrunnelse,
                begrunnelsesGrunnlagPerPerson = begrunnelsesGrunnlagPerPerson,
            )

        listOf(
            EØSBegrunnelseDataUtenKompetanse(
                vedtakBegrunnelseType = this.vedtakBegrunnelseType,
                apiNavn = sanityBegrunnelse.apiNavn,
                barnasFodselsdatoer = Utils.slåSammen(barnasFødselsdatoer.sorted().map { it.tilKortString() }),
                antallBarn = barnasFødselsdatoer.size,
                maalform =
                    grunnlag.behandlingsGrunnlagForVedtaksperioder.persongrunnlag.søker.målform
                        .tilSanityFormat(),
                gjelderSoker = gjelderSøker,
            ),
        )
    } else {
        kompetanser.mapNotNull { kompetanse ->
            val barnIBegrunnelseOgIKompetanse =
                kompetanse.barnAktører.mapNotNull { barnAktør ->
                    if (begrunnelseGjelderSøkerOgOpphørFraForrigeBehandling) {
                        begrunnelsesGrunnlagPerPerson.keys.find { it.aktør == barnAktør }
                    } else {
                        personerGjeldendeForBegrunnelse.find { it.aktør == barnAktør }
                    }
                }

            if (barnIBegrunnelseOgIKompetanse.isNotEmpty()) {
                EØSBegrunnelseDataMedKompetanse(
                    vedtakBegrunnelseType = vedtakBegrunnelseType,
                    apiNavn = sanityBegrunnelse.apiNavn,
                    annenForeldersAktivitet = kompetanse.annenForeldersAktivitet,
                    annenForeldersAktivitetsland = kompetanse.annenForeldersAktivitetsland?.tilLandNavn(landkoder)?.navn,
                    barnetsBostedsland = kompetanse.barnetsBostedsland.tilLandNavn(landkoder).navn,
                    barnasFodselsdatoer = Utils.slåSammen(barnIBegrunnelseOgIKompetanse.map { it.fødselsdato.tilKortString() }),
                    antallBarn = barnIBegrunnelseOgIKompetanse.size,
                    maalform =
                        grunnlag.behandlingsGrunnlagForVedtaksperioder.persongrunnlag.søker.målform
                            .tilSanityFormat(),
                    sokersAktivitet = kompetanse.søkersAktivitet,
                    sokersAktivitetsland = kompetanse.søkersAktivitetsland.tilLandNavn(landkoder).navn,
                    gjelderSoker = gjelderSøker,
                )
            } else {
                null
            }
        }
    }
}

data class Landkode(
    val kode: String,
    val navn: String,
) {
    init {
        if (this.kode.length != 2) {
            throw Feil("Forventer landkode på 'ISO 3166-1 alpha-2'-format")
        }
    }
}

fun String.tilLandNavn(landkoderISO2: Map<String, String>): Landkode {
    val kode = landkoderISO2.entries.find { it.key == this } ?: throw Feil("Fant ikke navn for landkode $this.")

    return Landkode(kode.key, kode.value.storForbokstav())
}
