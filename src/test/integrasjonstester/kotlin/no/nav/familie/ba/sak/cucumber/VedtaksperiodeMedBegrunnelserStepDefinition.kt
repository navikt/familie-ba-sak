package no.nav.familie.ba.sak.cucumber

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Og
import io.cucumber.java.no.Så
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.cucumber.domeneparser.DomeneparserUtil.groupByBehandlingId
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepPersongrunnlag
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.mapForventetVedtaksperioderMedBegrunnelser
import no.nav.familie.ba.sak.cucumber.domeneparser.parseEnum
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.utledVedtaksPerioderMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import org.assertj.core.api.Assertions
import java.time.LocalDate

class VedtaksperiodeMedBegrunnelserStepDefinition {

    private var behandlinger = mapOf<Long, Behandling>()
    private var vedtaksliste = listOf<Vedtak>()
    private var persongrunnlag = mapOf<Long, PersonopplysningGrunnlag>()
    private var personResultater = setOf<PersonResultat>()
    private var vedtaksperioderMedBegrunnelser = listOf<VedtaksperiodeMedBegrunnelser>()

    @Gitt("følgende vedtak")
    fun `følgende vedtak`(dataTable: DataTable) {
        genererVedtak(dataTable)
    }

    @Og("følgende persongrunnlag")
    fun `følgende persongrunnlag`(dataTable: DataTable) {
        lagPersonGrunnlag(dataTable)
    }

    @Og("med personresultater for behandling {}")
    fun `med personresultater`(behandlingId: Long) {
        val periodeFom = LocalDate.of(2022, 1, 1)
        personResultater = persongrunnlag[behandlingId]!!.personer.map { person ->
            lagPersonResultat(
                vilkårsvurdering = lagVilkårsvurdering(person.aktør, behandlinger[behandlingId]!!, Resultat.OPPFYLT),
                person = person,
                resultat = Resultat.OPPFYLT,
                personType = person.type,
                lagFullstendigVilkårResultat = true,
                periodeFom = periodeFom,
                periodeTom = if (person.type == PersonType.BARN) periodeFom.plusYears(18) else null
            )
        }.toSet()
    }

    @Når("vedtaksperioder med begrunnelser genereres for behandling {}")
    fun `generer vedtaksperiode med begrunnelse`(behandlingId: Long) {
        vedtaksperioderMedBegrunnelser = utledVedtaksPerioderMedBegrunnelser(
            persongrunnlag[behandlingId]!!,
            personResultater,
            vedtaksliste.last()
        )
    }

    @Så("forvent følgende vedtaksperioder med begrunnelser")
    fun `forvent følgende vedtaksperioder med begrunnelser`(dataTable: DataTable) {
        val forventedeVedtaksperioder = mapForventetVedtaksperioderMedBegrunnelser(
            dataTable,
            vedtak = vedtaksliste.last()
        )

        Assertions.assertThat(forventedeVedtaksperioder).isEqualTo(vedtaksperioderMedBegrunnelser)
    }

    private fun genererVedtak(dataTable: DataTable) {
        val fagsak = defaultFagsak()
        behandlinger = dataTable.groupByBehandlingId()
            .map { lagBehandling(fagsak = fagsak).copy(id = it.key) }
            .associateBy { it.id }
        vedtaksliste = dataTable.groupByBehandlingId()
            .map { lagVedtak(behandlinger[it.key]!!) }
    }

    private fun lagPersonGrunnlag(dataTable: DataTable) {
        persongrunnlag = dataTable.groupByBehandlingId()
            .map { behandlingMedPersongrunnlag ->
                PersonopplysningGrunnlag(
                    behandlingId = behandlingMedPersongrunnlag.key,
                    personer = behandlingMedPersongrunnlag.value.map {
                        tilfeldigPerson(
                            personType = parseEnum(
                                DomenebegrepPersongrunnlag.PERSON_TYPE,
                                it
                            )
                        )
                    }.toMutableSet()
                )
            }.associateBy { it.behandlingId }
    }
}
