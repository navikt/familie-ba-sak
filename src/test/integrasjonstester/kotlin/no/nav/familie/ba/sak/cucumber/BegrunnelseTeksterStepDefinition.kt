package no.nav.familie.ba.sak.cucumber

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Og
import io.cucumber.java.no.Så
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.cucumber.domeneparser.BrevBegrunnelseParser.mapStandardBegrunnelser
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.brev.domene.RestSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.brevBegrunnelseProdusent.hentGyldigeBegrunnelserForPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.tilUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.GrunnlagForVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.genererVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate

class BegrunnelseTeksterStepDefinition {

    private var behandlinger = mutableMapOf<Long, Behandling>()
    private var behandlingTilForrigeBehandling = mutableMapOf<Long, Long?>()
    private var vedtaksliste = mutableListOf<Vedtak>()
    private var persongrunnlag = mutableMapOf<Long, PersonopplysningGrunnlag>()
    private var personResultater = mutableMapOf<Long, Set<PersonResultat>>()
    private var vedtaksperioderMedBegrunnelser = listOf<VedtaksperiodeMedBegrunnelser>()
    private var kompetanser = mutableMapOf<Long, List<Kompetanse>>()
    private var endredeUtbetalinger = mutableMapOf<Long, List<EndretUtbetalingAndel>>()
    private var andelerTilkjentYtelse = mutableMapOf<Long, List<AndelTilkjentYtelse>>()

    private var gjeldendeBehandlingId: Long? = null

    private var utvidetVedtaksperiodeMedBegrunnelser = mutableListOf<UtvidetVedtaksperiodeMedBegrunnelser>()

    @Gitt("følgende behandling")
    fun `følgende behandling`(dataTable: DataTable) {
        lagVedtak(dataTable, behandlinger, behandlingTilForrigeBehandling, vedtaksliste)
    }

    @Og("følgende persongrunnlag for begrunnelse")
    fun `følgende persongrunnlag for begrunnelse`(dataTable: DataTable) {
        persongrunnlag.putAll(lagPersonGrunnlag(dataTable))
    }

    @Og("lag personresultater for begrunnelse for behandling {}")
    fun `lag personresultater for begrunnelse`(behandlingId: Long) {
        val persongrunnlagForBehandling = persongrunnlag.finnPersonGrunnlagForBehandling(behandlingId)
        val behandling = behandlinger.finnBehandling(behandlingId)
        personResultater[behandlingId] = lagPersonresultater(persongrunnlagForBehandling, behandling)
    }

    @Og("legg til nye vilkårresultater for begrunnelse for behandling {}")
    fun `legg til nye vilkårresultater for behandling`(behandlingId: Long, dataTable: DataTable) {
        val vilkårPerPerson = dataTable.asMaps().groupBy { VedtaksperiodeMedBegrunnelserParser.parseAktørId(it) }
        val personResultatForBehandling =
            personResultater[behandlingId] ?: error("Finner ikke personresultater for behandling med id $behandlingId")
        personResultater[behandlingId] = personResultatForBehandling.map { personResultat ->
            val vilkårPerPerson = vilkårPerPerson[personResultat.aktør.aktørId]

            personResultat.vilkårResultater.forEach { vilkårResultat ->
                oppdaterVilkårResultat(
                    vilkårResultat,
                    vilkårPerPerson,
                )
            }
            personResultat
        }.toSet()
    }

    @Og("med kompetanser for begrunnelse")
    fun `med kompetanser for begrunnelse`(dataTable: DataTable) {
        val nyeKompetanserPerBarn = dataTable.asMaps()
        kompetanser = lagKompetanser(nyeKompetanserPerBarn, persongrunnlag)
    }

    @Og("med endrede utbetalinger for begrunnelse")
    fun `med endrede utbetalinger for begrunnelse`(dataTable: DataTable) {
        val nyeEndredeUtbetalingAndeler = dataTable.asMaps()
        endredeUtbetalinger = lagEndredeUtbetalinger(nyeEndredeUtbetalingAndeler, persongrunnlag)
    }

    @Og("med andeler tilkjent ytelse for begrunnelse")
    fun `med andeler tilkjent ytelse for begrunnelse`(dataTable: DataTable) {
        andelerTilkjentYtelse = lagAndelerTilkjentYtelse(dataTable, behandlinger, persongrunnlag)
    }

    @Når("begrunnelsetekster genereres for behandling {}")
    fun `generer begrunnelsetekst for `(behandlingId: Long) {
        gjeldendeBehandlingId = behandlingId
        val behandling = behandlinger.finnBehandling(behandlingId)

        val vedtak = vedtaksliste.find { it.behandling.id == behandlingId && it.aktiv } ?: error("Finner ikke vedtak")

        val grunnlagForVedtaksperiode = GrunnlagForVedtaksperioder(
            persongrunnlag = persongrunnlag.finnPersonGrunnlagForBehandling(behandlingId),
            personResultater = personResultater[behandlingId] ?: error("Finner ikke personresultater"),
            fagsakType = vedtak.behandling.fagsak.type,
            kompetanser = kompetanser[behandlingId] ?: emptyList(),
            endredeUtbetalinger = endredeUtbetalinger[behandlingId] ?: emptyList(),
            andelerTilkjentYtelse = andelerTilkjentYtelse[behandlingId] ?: emptyList(),
            perioderOvergangsstønad = emptyList(),
        )
        val forrigeBehandlingId = behandlingTilForrigeBehandling[behandlingId]

        val grunnlagForVedtaksperiodeForrigeBehandling = forrigeBehandlingId?.let {
            val forrigeVedtak =
                vedtaksliste.find { it.behandling.id == forrigeBehandlingId && it.aktiv } ?: error("Finner ikke vedtak")
            GrunnlagForVedtaksperioder(
                persongrunnlag = persongrunnlag.finnPersonGrunnlagForBehandling(forrigeBehandlingId),
                personResultater = personResultater[forrigeBehandlingId] ?: error("Finner ikke personresultater"),
                fagsakType = forrigeVedtak.behandling.fagsak.type,
                kompetanser = kompetanser[forrigeBehandlingId] ?: emptyList(),
                endredeUtbetalinger = endredeUtbetalinger[forrigeBehandlingId] ?: emptyList(),
                andelerTilkjentYtelse = andelerTilkjentYtelse[forrigeBehandlingId] ?: emptyList(),
                perioderOvergangsstønad = emptyList(),
            )
        }

        vedtaksperioderMedBegrunnelser = genererVedtaksperioder(
            vedtak = vedtak,
            grunnlagForVedtakPerioder = grunnlagForVedtaksperiode,
            grunnlagForVedtakPerioderForrigeBehandling = grunnlagForVedtaksperiodeForrigeBehandling,
        )

        val utvidedeVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser.map {
            it.tilUtvidetVedtaksperiodeMedBegrunnelser(
                personopplysningGrunnlag = persongrunnlag.finnPersonGrunnlagForBehandling(behandlingId),
                andelerTilkjentYtelse = andelerTilkjentYtelse[behandlingId]!!.map {
                    AndelTilkjentYtelseMedEndreteUtbetalinger(
                        it,
                        endredeUtbetalinger[behandlingId] ?: emptySet(),
                    )
                },
                skalBrukeNyVedtaksperiodeLøsning = true,
            )
        }

        utvidetVedtaksperiodeMedBegrunnelser = utvidedeVedtaksperioderMedBegrunnelser.map {
            it.copy(
                gyldigeBegrunnelser = it.hentGyldigeBegrunnelserForPeriode(
                    grunnlagForVedtaksperiode,
                    grunnlagForVedtaksperiodeForrigeBehandling,
                    mockHentSanityBegrunnelser(),
                    behandling.underkategori,
                    behandling.fagsak.type,
                ).toList(),
            )
        }.toMutableList()
    }

    @Så("forvent følgende standardBegrunnelser")
    fun `forvent følgende standardBegrunnelser`(dataTable: DataTable) {
        val forventedeStandardBegrunnelser = mapStandardBegrunnelser(dataTable).toSet()

        forventedeStandardBegrunnelser.forEach { forventet ->
            val faktisk =
                utvidetVedtaksperiodeMedBegrunnelser.find { it.fom == forventet.fom && it.tom == forventet.tom }
                    ?: throw Feil(
                        "Fant ingen vedtaksperiode med \n" +
                            "   fom: ${forventet.fom} og tom: ${forventet.tom}. \n" +
                            "Vedtaksperiodene var \n${
                                utvidetVedtaksperiodeMedBegrunnelser.joinToString("\n") {
                                    "   Fom: ${it.fom}, Tom: ${it.tom}"
                                }
                            }",
                    )
            assertThat(faktisk.gyldigeBegrunnelser)
                .`as`("For periode: ${forventet.fom} til ${forventet.tom}")
                .containsAll(forventet.inkluderteStandardBegrunnelser)

            if (faktisk.gyldigeBegrunnelser.isNotEmpty() && forventet.ekskluderteStandardBegrunnelser.isNotEmpty()) {
                assertThat(faktisk.gyldigeBegrunnelser).doesNotContainAnyElementsOf(forventet.ekskluderteStandardBegrunnelser)
            }
        }
    }

    private fun mockHentSanityBegrunnelser(): Map<Standardbegrunnelse, SanityBegrunnelse> {
        val restSanityBegrunnelserJson =
            this::class.java.getResource("/no/nav/familie/ba/sak/cucumber/begrunnelsetekster/restSanityBegrunnelser.json")!!

        val restSanityBegrunnelser =
            objectMapper.readValue(restSanityBegrunnelserJson.readText(), Array<RestSanityBegrunnelse>::class.java)
                .toList()

        val enumPåApiNavn = Standardbegrunnelse.values().associateBy { it.sanityApiNavn }
        val sanityBegrunnelser = restSanityBegrunnelser.mapNotNull { it.tilSanityBegrunnelse() }

        return sanityBegrunnelser
            .mapNotNull {
                val begrunnelseEnum = enumPåApiNavn[it.apiNavn]
                if (begrunnelseEnum == null) {
                    null
                } else {
                    begrunnelseEnum to it
                }
            }.toMap()
    }
}

data class SammenlignbarBegrunnelse(
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: Vedtaksperiodetype,
    val inkluderteStandardBegrunnelser: Set<IVedtakBegrunnelse>,
    val ekskluderteStandardBegrunnelser: Set<IVedtakBegrunnelse> = emptySet<IVedtakBegrunnelse>(),
)
