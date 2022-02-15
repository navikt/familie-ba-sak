package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestNyttVilkår
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestSlettVilkår
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPersonResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType.MIGRERING_FRA_INFOTRYGD
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingMigreringUtils.kopiManglendePerioderFraForrigeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingMigreringUtils.utledPeriodeFom
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingMigreringUtils.utledPeriodeTom
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingUtils.flyttResultaterTilInitielt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingUtils.lagFjernAdvarsel
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingUtils.muterPersonResultatDelete
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingUtils.muterPersonResultatPost
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingUtils.muterPersonVilkårResultaterPut
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat.Companion.VilkårResultatComparator
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class VilkårService(
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val vilkårsvurderingMetrics: VilkårsvurderingMetrics,
    private val behandlingService: BehandlingService,
    private val personidentService: PersonidentService,
    private val featureToggleService: FeatureToggleService,
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
) {

    fun hentVilkårsvurdering(behandlingId: Long): Vilkårsvurdering? = vilkårsvurderingService.hentAktivForBehandling(
        behandlingId = behandlingId
    )

    fun hentVilkårsvurderingThrows(behandlingId: Long): Vilkårsvurdering =
        hentVilkårsvurdering(behandlingId = behandlingId)
            ?: throw IllegalStateException("Fant ikke aktiv vilkårsvurdering for behandling $behandlingId")

    companion object {
        const val fantIkkeAktivVilkårsvurderingFeilmelding = "Fant ikke aktiv vilkårsvurdering"
        const val fantIkkeVilkårsvurderingForPersonFeilmelding = "Fant ikke vilkårsvurdering for person"
    }

    @Transactional
    fun endreVilkår(
        behandlingId: Long,
        vilkårId: Long,
        restPersonResultat: RestPersonResultat
    ): List<RestPersonResultat> {

        if (!featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS)) {
            if (restPersonResultat.vilkårResultater.any { it.vurderesEtter == Regelverk.EØS_FORORDNINGEN })
                throw Feil(
                    message = "EØS er ikke togglet på",
                    frontendFeilmelding = "Funksjonalitet for EØS er ikke lansert."
                )
        }

        val vilkårsvurdering = hentVilkårsvurdering(behandlingId = behandlingId)
            ?: throw Feil(
                message = "Fant ikke aktiv vilkårsvurdering ved endring på vilkår",
                frontendFeilmelding = fantIkkeAktivVilkårsvurderingFeilmelding
            )

        val restVilkårResultat = restPersonResultat.vilkårResultater.singleOrNull { it.id == vilkårId }
            ?: throw Feil("Fant ikke vilkårResultat med id $vilkårId ved opppdatering av vikår")
        val personResultat =
            vilkårsvurdering.personResultater.singleOrNull { it.aktør.aktivFødselsnummer() == restPersonResultat.personIdent }
                ?: throw Feil(
                    message = fantIkkeVilkårsvurderingForPersonFeilmelding,
                    frontendFeilmelding = "Fant ikke vilkårsvurdering for person med ident '${restPersonResultat.personIdent}"
                )

        muterPersonVilkårResultaterPut(personResultat, restVilkårResultat)

        val vilkårResultat = personResultat.vilkårResultater.singleOrNull { it.id == vilkårId }
            ?: error("Finner ikke vilkår med vilkårId $vilkårId på personResultat ${personResultat.id}")

        vilkårResultat.also {
            it.vedtakBegrunnelseSpesifikasjoner = restVilkårResultat.avslagBegrunnelser ?: emptyList()
        }

        validerVilkårStarterIkkeFørMigreringsdatoForMigreringsbehandling(vilkårsvurdering, vilkårResultat)

        return vilkårsvurderingService.oppdater(vilkårsvurdering).personResultater.map { it.tilRestPersonResultat() }
    }

    private fun validerVilkårStarterIkkeFørMigreringsdatoForMigreringsbehandling(
        vilkårsvurdering: Vilkårsvurdering,
        vilkårResultat: VilkårResultat,
    ) {
        val behandling = vilkårsvurdering.behandling
        if (behandling.erMigrering()) {
            val migreringsdato = behandlingService.hentMigreringsdatoIBehandling(behandling.id)
            if (migreringsdato != null &&
                vilkårResultat.vilkårType !in listOf(Vilkår.UNDER_18_ÅR, Vilkår.GIFT_PARTNERSKAP) &&
                vilkårResultat.periodeFom?.isBefore(migreringsdato) == true
            ) {
                throw FunksjonellFeil(
                    melding = "${vilkårResultat.vilkårType} kan ikke endres før $migreringsdato " +
                        "for behandling=${behandling.id}",
                    frontendFeilmelding = "F.o.m. kan ikke settes tidligere " +
                        "enn migreringsdato ${migreringsdato.tilKortString()} " +
                        "Ved behov for vurdering før dette, må behandlingen henlegges, " +
                        "og migreringstidspunktet endres ved å opprette en ny migreringsbehandling."
                )
            }
        }
    }

    @Transactional
    fun deleteVilkårsperiode(behandlingId: Long, vilkårId: Long, aktør: Aktør): List<RestPersonResultat> {
        val vilkårsvurdering = hentVilkårsvurdering(behandlingId = behandlingId)
            ?: throw Feil(
                message = "Fant ikke aktiv vilkårsvurdering ved sletting av vilkår",
                frontendFeilmelding = fantIkkeAktivVilkårsvurderingFeilmelding
            )

        val personResultat = vilkårsvurdering.personResultater.find { it.aktør == aktør }
            ?: throw Feil(
                message = fantIkkeVilkårsvurderingForPersonFeilmelding,
                frontendFeilmelding = "Fant ikke vilkårsvurdering for person med ident '${aktør.aktivFødselsnummer()}"
            )

        muterPersonResultatDelete(personResultat, vilkårId)

        return vilkårsvurderingService.oppdater(vilkårsvurdering).personResultater.map { it.tilRestPersonResultat() }
    }

    @Transactional
    fun deleteVilkår(behandlingId: Long, restSlettVilkår: RestSlettVilkår): List<RestPersonResultat> {
        val vilkårsvurdering = hentVilkårsvurdering(behandlingId = behandlingId)
            ?: throw Feil(
                message = "Fant ikke aktiv vilkårsvurdering ved sletting av vilkår",
                frontendFeilmelding = fantIkkeAktivVilkårsvurderingFeilmelding
            )
        val aktørÅSlette = personidentService.hentOgLagreAktør(restSlettVilkår.personIdent)
        val personResultat = vilkårsvurdering.personResultater.find { it.aktør == aktørÅSlette }
            ?: throw Feil(
                message = fantIkkeVilkårsvurderingForPersonFeilmelding,
                frontendFeilmelding = "Fant ikke vilkårsvurdering for person med ident '${restSlettVilkår.personIdent}"
            )
        val behandling = behandlingService.hent(behandlingId)
        if (!behandling.erManuellMigrering() ||
            Vilkår.UTVIDET_BARNETRYGD != restSlettVilkår.vilkårType ||
            finnesUtvidetBarnetrydIForrigeBehandling(behandling, restSlettVilkår.personIdent)
        ) {
            throw FunksjonellFeil(
                melding = "Vilkår ${restSlettVilkår.vilkårType.beskrivelse} kan ikke slettes " +
                    "for behandling $behandlingId",
                frontendFeilmelding = "Vilkår ${restSlettVilkår.vilkårType.beskrivelse} kan ikke slettes " +
                    "for behandling $behandlingId",
            )
        }

        if (restSlettVilkår.vilkårType == Vilkår.UTVIDET_BARNETRYGD) {
            behandlingService.oppdaterBehandlingstema(
                behandling = behandling,
                nyKategori = behandling.kategori,
                nyUnderkategori = BehandlingUnderkategori.ORDINÆR,
            )
        }

        personResultat.vilkårResultater.filter { it.vilkårType == restSlettVilkår.vilkårType }
            .forEach { personResultat.removeVilkårResultat(it.id) }

        return vilkårsvurderingService.oppdater(vilkårsvurdering).personResultater.map { it.tilRestPersonResultat() }
    }

    @Transactional
    fun postVilkår(behandlingId: Long, restNyttVilkår: RestNyttVilkår): List<RestPersonResultat> {
        val vilkårsvurdering = hentVilkårsvurdering(behandlingId = behandlingId)
            ?: throw Feil(
                message = "Fant ikke aktiv vilkårsvurdering ved opprettelse av vilkårsperiode",
                frontendFeilmelding = fantIkkeAktivVilkårsvurderingFeilmelding
            )
        val behandling = vilkårsvurdering.behandling

        if (restNyttVilkår.vilkårType == Vilkår.UTVIDET_BARNETRYGD) {
            validerFørLeggeTilUtvidetBarnetrygd(behandling, restNyttVilkår, vilkårsvurdering)

            behandlingService.oppdaterBehandlingstema(
                behandling = behandling,
                nyKategori = behandling.kategori,
                nyUnderkategori = BehandlingUnderkategori.UTVIDET,
            )
        }

        val personResultat =
            vilkårsvurdering.personResultater.find { it.aktør.aktivFødselsnummer() == restNyttVilkår.personIdent }
                ?: throw Feil(
                    message = fantIkkeVilkårsvurderingForPersonFeilmelding,
                    frontendFeilmelding =
                    "Fant ikke vilkårsvurdering for person med ident '${restNyttVilkår.personIdent}"
                )

        muterPersonResultatPost(personResultat, restNyttVilkår.vilkårType)

        return vilkårsvurderingService.oppdater(vilkårsvurdering).personResultater.map { it.tilRestPersonResultat() }
    }

    private fun validerFørLeggeTilUtvidetBarnetrygd(
        behandling: Behandling,
        restNyttVilkår: RestNyttVilkår,
        vilkårsvurdering: Vilkårsvurdering
    ) {
        if (!behandling.erManuellMigrering() && !harUtvidetVilkår(vilkårsvurdering)) {
            throw FunksjonellFeil(
                melding = "${restNyttVilkår.vilkårType.beskrivelse} kan ikke legges til for behandling ${behandling.id} " +
                    "med behandlingType ${behandling.type.visningsnavn}",
                frontendFeilmelding = "${restNyttVilkår.vilkårType.beskrivelse} kan ikke legges til " +
                    "for behandling ${behandling.id} med behandlingType ${behandling.type.visningsnavn}",
            )
        }

        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
            ?: throw IllegalStateException("Fant ikke personopplysninggrunnlag for behandling ${behandling.id}")
        if (personopplysningGrunnlag.søkerOgBarn
            .single { it.aktør == personidentService.hentOgLagreAktør(restNyttVilkår.personIdent) }.type != PersonType.SØKER
        ) {
            throw FunksjonellFeil(
                melding = "${Vilkår.UTVIDET_BARNETRYGD.beskrivelse} kan ikke legges til for BARN",
                frontendFeilmelding = "${Vilkår.UTVIDET_BARNETRYGD.beskrivelse} kan ikke legges til for BARN",
            )
        }

        validerUtvidetVilkårIkkeFørMigreringsdato(behandling, vilkårsvurdering)
    }

    private fun validerUtvidetVilkårIkkeFørMigreringsdato(
        behandling: Behandling,
        vilkårsvurdering: Vilkårsvurdering
    ) {
        val migreringsdato = behandlingService.hentMigreringsdatoIBehandling(behandling.id)
        if (migreringsdato != null &&
            vilkårsvurdering.personResultater.any {
                it.vilkårResultater.any { vilkårResultat ->
                    vilkårResultat.vilkårType == Vilkår.UTVIDET_BARNETRYGD &&
                        vilkårResultat.periodeFom?.isBefore(migreringsdato) == true
                }
            }
        ) {
            throw FunksjonellFeil(
                melding = "${Vilkår.UTVIDET_BARNETRYGD} kan ikke endres før $migreringsdato " +
                    "for behandling=${behandling.id}",
                frontendFeilmelding = "F.o.m. kan ikke settes tidligere " +
                    "enn migreringsdato ${migreringsdato.tilKortString()} " +
                    "Ved behov for vurdering før dette, må behandlingen henlegges, " +
                    "og migreringstidspunktet endres ved å opprette en ny migreringsbehandling."
            )
        }
    }

    private fun harUtvidetVilkår(vilkårsvurdering: Vilkårsvurdering): Boolean =
        vilkårsvurdering.personResultater.find { it.erSøkersResultater() }?.vilkårResultater?.any { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD } == true

    fun initierVilkårsvurderingForBehandling(
        behandling: Behandling,
        bekreftEndringerViaFrontend: Boolean,
        forrigeBehandlingSomErVedtatt: Behandling? = null
    ): Vilkårsvurdering {
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
            ?: throw IllegalStateException("Fant ikke personopplysninggrunnlag for behandling ${behandling.id}")

        if (behandling.skalBehandlesAutomatisk) {
            if (personopplysningGrunnlag.barna.isEmpty()) {
                throw IllegalStateException("PersonopplysningGrunnlag for fødselshendelse skal inneholde minst ett barn")
            }
        }

        val aktivVilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandling.id)
        val barnaSomAlleredeErVurdert = aktivVilkårsvurdering?.personResultater?.mapNotNull {
            personopplysningGrunnlag.barna.firstOrNull { barn -> barn.aktør == it.aktør }
        }?.filter { it.type == PersonType.BARN }?.map { it.aktør.aktørId } ?: emptyList()

        val initiellVilkårsvurdering =
            genererInitiellVilkårsvurdering(
                behandling = behandling,
                barnaSomAlleredeErVurdert = barnaSomAlleredeErVurdert
            )

        return if (forrigeBehandlingSomErVedtatt != null && aktivVilkårsvurdering == null) {
            val vilkårsvurdering =
                genererInitiellVilkårsvurderingFraAnnenBehandling(
                    initiellVilkårsvurdering = initiellVilkårsvurdering,
                    annenBehandling = forrigeBehandlingSomErVedtatt
                )

            endretUtbetalingAndelService.kopierEndretUtbetalingAndelFraForrigeBehandling(
                behandling,
                forrigeBehandlingSomErVedtatt
            )

            return vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)
        } else {
            if (aktivVilkårsvurdering != null) {
                val (initieltSomErOppdatert, aktivtSomErRedusert) = flyttResultaterTilInitielt(
                    initiellVilkårsvurdering = initiellVilkårsvurdering,
                    aktivVilkårsvurdering = aktivVilkårsvurdering,
                    løpendeUnderkategori = behandlingService.hentLøpendeUnderkategori(initiellVilkårsvurdering.behandling.fagsak.id),
                    forrigeBehandlingVilkårsvurdering = if (forrigeBehandlingSomErVedtatt != null) hentVilkårsvurdering(
                        forrigeBehandlingSomErVedtatt.id
                    ) else null
                )

                if (aktivtSomErRedusert.personResultater.isNotEmpty() && !bekreftEndringerViaFrontend) {
                    throw FunksjonellFeil(
                        melding = "Saksbehandler forsøker å fjerne vilkår fra vilkårsvurdering",
                        frontendFeilmelding = lagFjernAdvarsel(aktivtSomErRedusert.personResultater)
                    )
                }
                return vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = initieltSomErOppdatert)
            } else {
                vilkårsvurderingService.lagreInitielt(initiellVilkårsvurdering)
            }
        }
    }

    fun genererInitiellVilkårsvurderingFraAnnenBehandling(
        annenBehandling: Behandling,
        initiellVilkårsvurdering: Vilkårsvurdering
    ): Vilkårsvurdering {

        val annenVilkårsvurdering = hentVilkårsvurdering(behandlingId = annenBehandling.id)
            ?: throw Feil(message = "Finner ikke vilkårsvurdering fra annen behandling.")

        val annenBehandlingErHenlagt = behandlingService.hent(annenBehandling.id).erHenlagt()

        if (annenBehandlingErHenlagt)
            throw Feil(message = "vilkårsvurdering skal ikke kopieres fra henlagt behandling.")
        val (oppdatert) = flyttResultaterTilInitielt(
            aktivVilkårsvurdering = annenVilkårsvurdering,
            initiellVilkårsvurdering = initiellVilkårsvurdering,
            løpendeUnderkategori = behandlingService.hentLøpendeUnderkategori(initiellVilkårsvurdering.behandling.fagsak.id),
            forrigeBehandlingVilkårsvurdering = annenVilkårsvurdering
        )
        return oppdatert
    }

    fun genererInitiellVilkårsvurdering(
        behandling: Behandling,
        barnaSomAlleredeErVurdert: List<String>
    ): Vilkårsvurdering {
        return Vilkårsvurdering(behandling = behandling).apply {
            when {
                behandling.type == MIGRERING_FRA_INFOTRYGD &&
                    behandling.opprettetÅrsak == BehandlingÅrsak.MIGRERING -> {
                    personResultater = lagVilkårsvurderingForMigreringsbehandling(this)
                }
                behandling.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE -> {
                    personResultater = lagVilkårsvurderingForFødselshendelse(this, barnaSomAlleredeErVurdert)

                    if (førstegangskjøringAvVilkårsvurdering(this)) {
                        vilkårsvurderingMetrics.tellMetrikker(this)
                    }
                }
                !behandling.skalBehandlesAutomatisk -> {
                    personResultater = lagManuellVilkårsvurdering(this)
                }
                else -> personResultater = lagTomVilkårsvurdering(this)
            }
        }
    }

    fun genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
        behandling: Behandling,
        forrigeBehandlingSomErVedtatt: Behandling? = null,
        nyMigreringsdato: LocalDate
    ): Vilkårsvurdering {
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling).apply {
            personResultater = lagVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
                this,
                forrigeBehandlingSomErVedtatt!!,
                nyMigreringsdato
            )
        }
        return vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)
    }

    fun genererVilkårsvurderingForHelmanuellMigrering(
        behandling: Behandling,
        nyMigreringsdato: LocalDate
    ): Vilkårsvurdering {
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling).apply {
            personResultater = lagVilkårsvurderingForHelmanuellMigrering(this, nyMigreringsdato)
        }
        return vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)
    }

    private fun lagTomVilkårsvurdering(vilkårsvurdering: Vilkårsvurdering): Set<PersonResultat> {
        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(vilkårsvurdering.behandling.id)
                ?: throw Feil(message = "Fant ikke personopplysninggrunnlag for behandling ${vilkårsvurdering.behandling.id}")

        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            val personResultat = PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = person.aktør
            )

            val vilkårForPerson = Vilkår.hentVilkårFor(person.type)

            val vilkårResultater = vilkårForPerson.map { vilkår ->
                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = true,
                    resultat = Resultat.IKKE_VURDERT,
                    vilkårType = vilkår,
                    begrunnelse = "",
                    behandlingId = personResultat.vilkårsvurdering.behandling.id,
                )
            }.toSortedSet(VilkårResultatComparator)

            personResultat.setSortedVilkårResultater(vilkårResultater)

            personResultat
        }.toSet()
    }

    private fun lagManuellVilkårsvurdering(vilkårsvurdering: Vilkårsvurdering): Set<PersonResultat> {
        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(vilkårsvurdering.behandling.id)
                ?: throw Feil(message = "Fant ikke personopplysninggrunnlag for behandling ${vilkårsvurdering.behandling.id}")

        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            genererPersonResultatForPerson(vilkårsvurdering, person)
        }.toSet()
    }

    private fun lagVilkårsvurderingForFødselshendelse(
        vilkårsvurdering: Vilkårsvurdering,
        barnaSomAlleredeErVurdert: List<String>
    ): Set<PersonResultat> {
        val barnaAktørSomAlleredeErVurdert = personidentService.hentOgLagreAktørIder(barnaSomAlleredeErVurdert)

        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(vilkårsvurdering.behandling.id)
                ?: throw Feil(message = "Fant ikke personopplysninggrunnlag for behandling ${vilkårsvurdering.behandling.id}")

        val annenForelder = personopplysningGrunnlag.annenForelder
        val eldsteBarnSomVurderesSinFødselsdato =
            personopplysningGrunnlag.barna.filter { !barnaAktørSomAlleredeErVurdert.contains(it.aktør) }
                .maxByOrNull { it.fødselsdato }?.fødselsdato
                ?: throw Feil("Finner ingen barn på persongrunnlag")

        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            val personResultat = PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = person.aktør
            )

            val vilkårForPerson = Vilkår.hentVilkårFor(person.type)

            val vilkårResultater = vilkårForPerson.map { vilkår ->
                genererVilkårResultatForEtVilkårPåEnPerson(
                    person = person,
                    annenForelder = annenForelder,
                    eldsteBarnSinFødselsdato = eldsteBarnSomVurderesSinFødselsdato,
                    personResultat = personResultat,
                    vilkår = vilkår
                )
            }

            personResultat.setSortedVilkårResultater(vilkårResultater.toSet())

            personResultat
        }.toSet()
    }

    private fun genererVilkårResultatForEtVilkårPåEnPerson(
        person: Person,
        eldsteBarnSinFødselsdato: LocalDate,
        personResultat: PersonResultat,
        vilkår: Vilkår,
        annenForelder: Person? = null,
    ): VilkårResultat {
        val automatiskVurderingResultat = vilkår.vurderVilkår(
            person = person,
            annenForelder = annenForelder,
            vurderFra = eldsteBarnSinFødselsdato
        )

        val fom = if (eldsteBarnSinFødselsdato >= person.fødselsdato) eldsteBarnSinFødselsdato else person.fødselsdato

        val tom: LocalDate? =
            if (vilkår == Vilkår.UNDER_18_ÅR) {
                person.fødselsdato.plusYears(18).minusDays(1)
            } else null

        return VilkårResultat(
            regelInput = automatiskVurderingResultat.regelInput,
            personResultat = personResultat,
            erAutomatiskVurdert = true,
            resultat = automatiskVurderingResultat.resultat,
            vilkårType = vilkår,
            periodeFom = fom,
            periodeTom = tom,
            begrunnelse = "Vurdert og satt automatisk: ${automatiskVurderingResultat.evaluering.begrunnelse}",
            behandlingId = personResultat.vilkårsvurdering.behandling.id,
            evalueringÅrsaker = automatiskVurderingResultat.evaluering.evalueringÅrsaker.map { it.toString() }
        )
    }

    private fun lagVilkårsvurderingForMigreringsbehandling(vilkårsvurdering: Vilkårsvurdering): Set<PersonResultat> {
        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(vilkårsvurdering.behandling.id)
                ?: throw Feil(message = "Fant ikke personopplysninggrunnlag for behandling ${vilkårsvurdering.behandling.id}")

        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            val personResultat = PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = person.aktør
            )
            // NB Dette må gjøres om når vi skal begynne å migrere EØS-saker
            val ytelseType = if (person.type == PersonType.SØKER) when (vilkårsvurdering.behandling.underkategori) {
                BehandlingUnderkategori.UTVIDET -> YtelseType.UTVIDET_BARNETRYGD
                BehandlingUnderkategori.ORDINÆR -> YtelseType.ORDINÆR_BARNETRYGD
            } else YtelseType.ORDINÆR_BARNETRYGD

            val vilkårTyperForPerson = Vilkår.hentVilkårFor(person.type, ytelseType = ytelseType)

            val vilkårResultater = vilkårTyperForPerson.map { vilkår ->
                val fom = if (vilkår.gjelderAlltidFraBarnetsFødselsdato()) person.fødselsdato else null

                val tom: LocalDate? =
                    if (vilkår == Vilkår.UNDER_18_ÅR) person.fødselsdato.plusYears(18).minusDays(1) else null

                val begrunnelse = "Migrering"

                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = false,
                    resultat = Resultat.OPPFYLT,
                    vilkårType = vilkår,
                    periodeFom = fom,
                    periodeTom = tom,
                    begrunnelse = begrunnelse,
                    behandlingId = personResultat.vilkårsvurdering.behandling.id
                )
            }.toSortedSet(VilkårResultatComparator)

            personResultat.setSortedVilkårResultater(vilkårResultater)

            personResultat
        }.toSet()
    }

    private fun lagVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
        vilkårsvurdering: Vilkårsvurdering,
        forrigeBehandlingSomErVedtatt: Behandling,
        nyMigreringsdato: LocalDate
    ): Set<PersonResultat> {
        val forrigeBehandlingsvilkårsvurdering = vilkårsvurderingService
            .hentAktivForBehandling(forrigeBehandlingSomErVedtatt.id)
            ?: throw Feil(
                "Kan ikke kopiere vilkårsvurdering fra forrige behandling ${forrigeBehandlingSomErVedtatt.id}" +
                    "til behandling ${vilkårsvurdering.behandling.id}"
            )
        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(vilkårsvurdering.behandling.id)
                ?: throw Feil(
                    message = "Fant ikke personopplysninggrunnlag " +
                        "for behandling ${vilkårsvurdering.behandling.id}"
                )

        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            val personResultat = PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = person.aktør
            )

            val vilkårTyperForPerson = forrigeBehandlingsvilkårsvurdering.personResultater
                .single { it.aktør == person.aktør }.vilkårResultater.map { it.vilkårType }

            val vilkårResultater = vilkårTyperForPerson.map { vilkår ->
                val fom = utledPeriodeFom(forrigeBehandlingsvilkårsvurdering, vilkår, person, nyMigreringsdato)

                val tom: LocalDate? =
                    utledPeriodeTom(forrigeBehandlingsvilkårsvurdering, vilkår, person, fom)

                val begrunnelse = "Migrering"

                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = false,
                    resultat = Resultat.OPPFYLT,
                    vilkårType = vilkår,
                    periodeFom = fom,
                    periodeTom = tom,
                    begrunnelse = begrunnelse,
                    behandlingId = personResultat.vilkårsvurdering.behandling.id
                )
            }.toSortedSet(VilkårResultatComparator)

            val manglendePerioder = kopiManglendePerioderFraForrigeVilkårsvurdering(
                vilkårResultater,
                forrigeBehandlingsvilkårsvurdering, person
            )
            vilkårResultater.addAll(manglendePerioder.map { it.kopierMedParent(personResultat) }.toSet())
            personResultat.setSortedVilkårResultater(vilkårResultater)

            personResultat
        }.toSet()
    }

    private fun lagVilkårsvurderingForHelmanuellMigrering(
        vilkårsvurdering: Vilkårsvurdering,
        nyMigreringsdato: LocalDate
    ): Set<PersonResultat> {
        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(vilkårsvurdering.behandling.id)
                ?: throw Feil(
                    message = "Fant ikke personopplysninggrunnlag " +
                        "for behandling ${vilkårsvurdering.behandling.id}"
                )
        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            val personResultat = PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = person.aktør
            )

            val vilkårTyperForPerson = Vilkår.hentVilkårFor(person.type)
            val vilkårResultater = vilkårTyperForPerson.map { vilkår ->
                val fom = when {
                    vilkår.gjelderAlltidFraBarnetsFødselsdato() -> person.fødselsdato
                    nyMigreringsdato.isBefore(person.fødselsdato) -> person.fødselsdato
                    else -> nyMigreringsdato
                }

                val tom: LocalDate? = when (vilkår) {
                    Vilkår.UNDER_18_ÅR -> person.fødselsdato.plusYears(18)
                        .minusDays(1)
                    else -> null
                }

                val begrunnelse = "Migrering"

                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = false,
                    resultat = Resultat.OPPFYLT,
                    vilkårType = vilkår,
                    periodeFom = fom,
                    periodeTom = tom,
                    begrunnelse = begrunnelse,
                    behandlingId = personResultat.vilkårsvurdering.behandling.id
                )
            }.toSortedSet(VilkårResultatComparator)

            personResultat.setSortedVilkårResultater(vilkårResultater)

            personResultat
        }.toSet()
    }

    private fun førstegangskjøringAvVilkårsvurdering(vilkårsvurdering: Vilkårsvurdering): Boolean {
        return vilkårsvurderingService
            .hentAktivForBehandling(behandlingId = vilkårsvurdering.behandling.id) == null
    }

    private fun finnesUtvidetBarnetrydIForrigeBehandling(behandling: Behandling, personIdent: String): Boolean {
        val forrigeBehandlingSomErVedtatt = behandlingService.hentForrigeBehandlingSomErVedtatt(behandling)
        if (forrigeBehandlingSomErVedtatt != null) {
            val forrigeBehandlingsvilkårsvurdering =
                vilkårsvurderingService.hentAktivForBehandling(forrigeBehandlingSomErVedtatt.id) ?: throw Feil(
                    message = "Forrige behandling $${forrigeBehandlingSomErVedtatt.id} " +
                        "har ikke en aktiv vilkårsvurdering"
                )
            val aktør = personidentService.hentOgLagreAktør(personIdent)
            return forrigeBehandlingsvilkårsvurdering.personResultater.single { it.aktør == aktør }
                .vilkårResultater.any { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD }
        }
        return false
    }
}

fun Vilkår.gjelderAlltidFraBarnetsFødselsdato() = this == Vilkår.GIFT_PARTNERSKAP || this == Vilkår.UNDER_18_ÅR

fun SIVILSTAND.somForventetHosBarn() = this == SIVILSTAND.UOPPGITT || this == SIVILSTAND.UGIFT
