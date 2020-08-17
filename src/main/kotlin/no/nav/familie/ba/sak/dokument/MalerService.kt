package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.vilkår.finnNåværendeMedlemskap
import no.nav.familie.ba.sak.behandling.vilkår.finnSterkesteMedlemskap
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.TilkjentYtelseUtils
import no.nav.familie.ba.sak.client.Norg2RestClient
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.dokument.domene.MalMedData
import no.nav.familie.ba.sak.dokument.domene.maler.DuFårSeksjon
import no.nav.familie.ba.sak.dokument.domene.maler.Innvilget
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.stereotype.Service

@Service
class MalerService(
        private val totrinnskontrollService: TotrinnskontrollService,
        private val beregningService: BeregningService,
        private val persongrunnlagService: PersongrunnlagService,
        private val norg2RestClient: Norg2RestClient
) {

    fun mapTilBrevfelter(vedtak: Vedtak,
                         behandlingResultatType: BehandlingResultatType): MalMedData {

        val statsborgerskap = persongrunnlagService.hentSøker(vedtak.behandling)?.statsborgerskap?: error("Finner ikke søker på behandling")
        val medlemskap = finnNåværendeMedlemskap(statsborgerskap)
        val sterkesteMedlemskap = finnSterkesteMedlemskap(medlemskap)

        return MalMedData(
                mal = malNavnForMedlemskapOgResultatType(sterkesteMedlemskap, behandlingResultatType),
                fletteFelter = when (behandlingResultatType) {
                    BehandlingResultatType.INNVILGET -> mapTilInnvilgetBrevFelter(vedtak)
                    BehandlingResultatType.AVSLÅTT -> mapTilAvslagBrevFelter(vedtak)
                    BehandlingResultatType.OPPHØRT -> mapTilOpphørtBrevFelter(vedtak)
                    else -> error("Invalid/unsupported behandling resultat type")
                }
        )
    }

    private fun mapTilOpphørtBrevFelter(vedtak: Vedtak): String {
        val behandling = vedtak.behandling
        return "{\"fodselsnummer\": \"${behandling.fagsak.hentAktivIdent().ident}\",\n" +
               "\"navn\": \"No Name\",\n" +
               "\"tdato\": \"01.01.01\",\n" +
               "\"hjemmel\": \"\",\n" +
               "\"fritekst\": \"${""}\"}" //TODO: Begrunnelse her
    }

    private fun mapTilInnvilgetBrevFelter(vedtak: Vedtak): String {
        val behandling = vedtak.behandling
        val totrinnskontroll = totrinnskontrollService.opprettEllerHentTotrinnskontroll(behandling)

        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandling.id)
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandling.id)
                                       ?: throw Feil("Finner ikke persongrunnlag ved mapping til brevfelter")

        val beregningOversikt = TilkjentYtelseUtils.hentBeregningOversikt(tilkjentYtelseForBehandling = tilkjentYtelse,
                                                                          personopplysningGrunnlag = personopplysningGrunnlag)

        val innvilget = Innvilget(
                enhet = if (vedtak.ansvarligEnhet != null) norg2RestClient.hentEnhet(vedtak.ansvarligEnhet).navn
                else throw Feil(message = "Ansvarlig enhet er ikke satt ved generering av brev",
                                frontendFeilmelding = "Ansvarlig enhet er ikke satt ved generering av brev"),
                saksbehandler = totrinnskontroll.saksbehandler,
                beslutter = totrinnskontroll.beslutter
                            ?: totrinnskontroll.saksbehandler,
                hjemmel = Utils.slåSammen(listOf("§§ 2", "4", "11"))
        )

        innvilget.duFaar = beregningOversikt.map {
            val barnasFødselsdatoer =
                    Utils.slåSammen(it.beregningDetaljer
                                            .filter { restBeregningDetalj -> restBeregningDetalj.person.type == PersonType.BARN }
                                            .sortedBy { restBeregningDetalj -> restBeregningDetalj.person.fødselsdato }
                                            .map { restBeregningDetalj ->
                                                restBeregningDetalj.person.fødselsdato?.tilKortString() ?: ""
                                            })

            val begrunnelse: String = vedtak.stønadBrevBegrunnelser[Periode(it.periodeFom!!, it.periodeTom!!).hash]
                                      ?: "Ikke satt"

            DuFårSeksjon(
                    fom = it.periodeFom.tilDagMånedÅr(),
                    tom = it.periodeTom.tilDagMånedÅr(),
                    belop = Utils.formaterBeløp(it.utbetaltPerMnd),
                    antallBarn = it.antallBarn,
                    barnasFodselsdatoer = barnasFødselsdatoer,
                    begrunnelser = listOf(begrunnelse)
            )
        }

        return objectMapper.writeValueAsString(innvilget)
    }

    private fun mapTilAvslagBrevFelter(vedtak: Vedtak): String {
        val behandling = vedtak.behandling

        //TODO: sett navn, hjemmel og fritekst
        return "{\"fodselsnummer\": \"${behandling.fagsak.hentAktivIdent().ident}\",\n" +
               "\"navn\": \"No Name\",\n" +
               "\"hjemmel\": \"\",\n" +
               "\"fritekst\": \"${""}\"}" //TODO: Begrunnelse her
    }

    companion object {
        fun malNavnForMedlemskapOgResultatType(medlemskap: Medlemskap?,
                                               resultatType: BehandlingResultatType): String {
            return when (medlemskap) {
                Medlemskap.TREDJELANDSBORGER -> "${resultatType.brevMal}-Tredjelandsborger"
                else -> resultatType.brevMal
            }
        }
    }
}
