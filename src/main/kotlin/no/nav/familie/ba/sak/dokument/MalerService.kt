package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.SøknadDTO
import no.nav.familie.ba.sak.behandling.restDomene.TypeSøker
import no.nav.familie.ba.sak.behandling.restDomene.TypeSøker.TREDJELANDSBORGER
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.vilkår.VilkårService
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.beregnUtbetalingsperioderUtenKlassifisering
import no.nav.familie.ba.sak.client.Norg2RestClient
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.dokument.domene.MalMedData
import no.nav.familie.ba.sak.dokument.domene.maler.Innvilget
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class MalerService(
        private val vilkårService: VilkårService,
        private val totrinnskontrollService: TotrinnskontrollService,
        private val beregningService: BeregningService,
        private val persongrunnlagService: PersongrunnlagService,
        private val norg2RestClient: Norg2RestClient
) {

    fun mapTilBrevfelter(vedtak: Vedtak,
                         søknad: SøknadDTO?,
                         behandlingResultatType: BehandlingResultatType): MalMedData {

        return MalMedData(
                mal = malNavnForTypeSøkerOgResultatType(søknad?.typeSøker, behandlingResultatType),
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
        val barna = persongrunnlagService.hentBarna(behandling)

        val andelTilkjentYtelse = beregningService.hentAndelerTilkjentYtelseForBehandling(behandling.id)
        val utbetalingsperioder = beregnUtbetalingsperioderUtenKlassifisering(andelTilkjentYtelse.toSet())
        val beløp = utbetalingsperioder.toSegments()
                            .find { segment -> segment.fom <= LocalDate.now() && segment.tom >= LocalDate.now() }?.value
                    ?: utbetalingsperioder.toSegments().first().value
                    ?: error("Finner ikke beløp for vedtaksbrev")

        val vilkårsdato = vilkårService.hentVilkårsdato(behandling = behandling)
                          ?: error("Finner ikke vilkårsdato for vedtaksbrev")

        val barnasFødselsdatoer = Utils.slåSammen(barna.sortedBy { it.fødselsdato }.map { it.fødselsdato.tilKortString() })

        val innvilget = Innvilget(
                enhet = if (vedtak.ansvarligEnhet != null) norg2RestClient.hentEnhet(vedtak.ansvarligEnhet).navn
                else throw Feil(message = "Ansvarlig enhet er ikke satt ved generering av brev",
                                frontendFeilmelding = "Ansvarlig enhet er ikke satt ved generering av brev"),
                //saksbehandler = totrinnskontroll.saksbehandler,
                beslutter = totrinnskontroll.beslutter
                            ?: totrinnskontroll.saksbehandler,
                barnasFodselsdatoer = barnasFødselsdatoer,
                virkningsdato = utbetalingsperioder.minLocalDate.førsteDagIInneværendeMåned().tilDagMånedÅr(),
                vilkårsdato = vilkårsdato.tilDagMånedÅr(),
                vedtaksdato = vedtak.vedtaksdato.tilKortString(),
                belop = Utils.formaterBeløp(beløp),
                antallBarn = barna.size,
                flereBarn = barna.size > 1,
                hjemmel = Utils.slåSammen(listOf("§§ 2", "4", "11"))
        )

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
        fun malNavnForTypeSøkerOgResultatType(typeSøker: TypeSøker?,
                                              resultatType: BehandlingResultatType): String {
            return when (typeSøker) {
                TREDJELANDSBORGER -> "${resultatType.brevMal}-Tredjelandsborger"
                else -> resultatType.brevMal
            }
        }
    }
}
