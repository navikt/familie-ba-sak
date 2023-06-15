package no.nav.familie.ba.sak.cucumber

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Og
import io.cucumber.java.no.Så
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.cucumber.domeneparser.Domenebegrep
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.mapForventetVedtaksperioderMedBegrunnelser
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.parseAktørId
import no.nav.familie.ba.sak.cucumber.domeneparser.parseDato
import no.nav.familie.ba.sak.cucumber.domeneparser.parseLong
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import org.assertj.core.api.Assertions
import java.time.LocalDate

class VedtaksperiodeMedBegrunnelserStepDefinition {

    private var behandlinger = mutableMapOf<Long, Behandling>()
    private var behandlingTilForrigeBehandling = mutableMapOf<Long, Long?>()
    private var vedtaksliste = mutableListOf<Vedtak>()
    private var persongrunnlag = mutableMapOf<Long, PersonopplysningGrunnlag>()
    private var personResultater = mutableMapOf<Long, Set<PersonResultat>>()
    private var vedtaksperioderMedBegrunnelser = listOf<VedtaksperiodeMedBegrunnelser>()
    private var kompetanser = mutableMapOf<Long, List<Kompetanse>>()
    private var endredeUtbetalinger = mutableMapOf<Long, List<EndretUtbetalingAndel>>()
    private var andelerTilkjentYtelse = mutableMapOf<Long, List<AndelTilkjentYtelse>>()
    private var overstyrtEndringstidspunkt = mapOf<Long, LocalDate?>()

    private var gjeldendeBehandlingId: Long? = null

    @Gitt("følgende vedtak")
    fun `følgende vedtak`(dataTable: DataTable) {
        lagVedtak(dataTable, behandlinger, behandlingTilForrigeBehandling, vedtaksliste)
    }

    @Og("følgende persongrunnlag")
    fun `følgende persongrunnlag`(dataTable: DataTable) {
        persongrunnlag.putAll(lagPersonGrunnlag(dataTable))
    }

    @Og("lag personresultater for behandling {}")
    fun `lag personresultater`(behandlingId: Long) {
        val persongrunnlagForBehandling = persongrunnlag.finnPersonGrunnlagForBehandling(behandlingId)
        val behandling = behandlinger.finnBehandling(behandlingId)
        personResultater[behandlingId] = lagPersonresultater(persongrunnlagForBehandling, behandling)
    }

    @Og("legg til nye vilkårresultater for behandling {}")
    fun `legg til nye vilkårresultater for behandling`(behandlingId: Long, dataTable: DataTable) {
        val nyeVilkårPerPerson = dataTable.asMaps().groupBy { parseAktørId(it) }
        val personResultatForBehandling =
            personResultater[behandlingId] ?: error("Finner ikke personresultater for behandling med id $behandlingId")
        personResultater[behandlingId] = personResultatForBehandling.map { personResultat ->
            val nyeVilkårForPerson = nyeVilkårPerPerson[personResultat.aktør.aktørId]

            val nyeVilkårResultater = nyeVilkårForPerson.tilVilkårResultater(behandlingId, personResultat)
            personResultat.vilkårResultater.addAll(nyeVilkårResultater)
            personResultat
        }.toSet()
    }

    @Og("med overstyring av vilkår for behandling {}")
    fun overstyrPersonResultater(behandlingId: Long, dataTable: DataTable) {
        val overstyringerPerPerson = dataTable.asMaps().groupBy { parseAktørId(it) }
        val personResultatForBehandling =
            personResultater[behandlingId] ?: error("Finner ikke personresultater for behandling med id $behandlingId")
        personResultater[behandlingId] = personResultatForBehandling.map { personResultat ->
            val overstyringerForPerson = overstyringerPerPerson[personResultat.aktør.aktørId]

            personResultat.vilkårResultater.forEach { vilkårResultat ->
                oppdaterVilkårResultat(
                    vilkårResultat,
                    overstyringerForPerson,
                )
            }
            personResultat
        }.toSet()
    }

    @Og("med overstyrt endringstidspunkt")
    fun settEndringstidspunkt(dataTable: DataTable) {
        overstyrtEndringstidspunkt = dataTable.asMaps().associate { rad ->
            parseLong(Domenebegrep.BEHANDLING_ID, rad) to
                parseDato(DomenebegrepVedtaksperiodeMedBegrunnelser.ENDRINGSTIDSPUNKT, rad)
        }
    }

    @Og("med kompetanser")
    fun `med kompetanser`(dataTable: DataTable) {
        val nyeKompetanserPerBarn = dataTable.asMaps()
        kompetanser = lagKompetanser(nyeKompetanserPerBarn, persongrunnlag)
    }

    @Og("med endrede utbetalinger")
    fun `med endrede utbetalinger`(dataTable: DataTable) {
        val nyeEndredeUtbetalingAndeler = dataTable.asMaps()
        endredeUtbetalinger = lagEndredeUtbetalinger(nyeEndredeUtbetalingAndeler, persongrunnlag)
    }

    @Og("med andeler tilkjent ytelse")
    fun `med andeler tilkjent ytelse`(dataTable: DataTable) {
        andelerTilkjentYtelse = lagAndelerTilkjentYtelse(dataTable, behandlinger, persongrunnlag)
    }

    @Når("vedtaksperioder med begrunnelser genereres for behandling {}")
    fun `generer vedtaksperiode med begrunnelse`(behandlingId: Long) {
        gjeldendeBehandlingId = behandlingId

        vedtaksperioderMedBegrunnelser = lagVedtaksPerioder(
            behandlingId = behandlingId,
            vedtaksListe = vedtaksliste,
            behandlingTilForrigeBehandling = behandlingTilForrigeBehandling,
            personGrunnlag = persongrunnlag,
            personResultater = personResultater,
            kompetanser = kompetanser,
            endredeUtbetalinger = endredeUtbetalinger,
            andelerTilkjentYtelse = andelerTilkjentYtelse,
            endringstidspunkt = overstyrtEndringstidspunkt,
        )
    }

    @Så("forvent følgende vedtaksperioder med begrunnelser")
    fun `forvent følgende vedtaksperioder med begrunnelser`(dataTable: DataTable) {
        val forventedeVedtaksperioder = mapForventetVedtaksperioderMedBegrunnelser(
            dataTable = dataTable,
            vedtak = vedtaksliste.find { it.behandling.id == gjeldendeBehandlingId }
                ?: throw Feil("Fant ingen vedtak for behandling $gjeldendeBehandlingId"),
        )

        val vedtaksperioderComparator = compareBy<VedtaksperiodeMedBegrunnelser>({ it.type }, { it.fom }, { it.tom })
        Assertions.assertThat(vedtaksperioderMedBegrunnelser.sortedWith(vedtaksperioderComparator))
            .isEqualTo(forventedeVedtaksperioder.sortedWith(vedtaksperioderComparator))
    }
}
