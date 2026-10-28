package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.ekstern.restDomene.NyttVilkårDto
import no.nav.familie.ba.sak.ekstern.restDomene.PersonResultatDto
import no.nav.familie.ba.sak.ekstern.restDomene.SlettVilkårDto
import no.nav.familie.ba.sak.ekstern.restDomene.VilkårResultatDto
import no.nav.familie.ba.sak.ekstern.restDomene.tilPersonResultatDto
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.lagForskjøvetTidslinjeForOppfylteVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingUtils.muterPersonResultatDelete
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingUtils.muterPersonResultatPost
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingUtils.muterPersonVilkårResultaterPut
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.EndringIPreutfyltVilkårLogg.Companion.opprettLoggForEndringIPreutfyltVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.EndringIPreutfyltVilkårLoggRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.mapIkkeNull
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VilkårService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingstemaService: BehandlingstemaService,
    private val behandlingService: BehandlingService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val personidentService: PersonidentService,
    private val persongrunnlagService: PersongrunnlagService,
    private val endringIPreutfyltVilkårLoggRepository: EndringIPreutfyltVilkårLoggRepository,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
    private val kompetanseService: KompetanseService,
) {
    fun hentVilkårsvurdering(behandlingId: Long): Vilkårsvurdering? =
        vilkårsvurderingService.hentAktivForBehandling(
            behandlingId = behandlingId,
        )

    fun hentVilkårsvurderingThrows(behandlingId: Long): Vilkårsvurdering =
        hentVilkårsvurdering(behandlingId) ?: throw Feil(
            message = "Fant ikke aktiv vilkårsvurdering for behandling $behandlingId",
            frontendFeilmelding = FANT_IKKE_AKTIV_VILKÅRSVURDERING_FEILMELDING,
        )

    @Transactional
    fun endreVilkår(
        behandlingId: Long,
        vilkårId: Long,
        personResultatDto: PersonResultatDto,
    ): List<PersonResultatDto> {
        val vilkårsvurdering = hentVilkårsvurderingThrows(behandlingId)

        val vilkårResultatDto =
            personResultatDto.vilkårResultater.singleOrNull { it.id == vilkårId }
                ?: throw Feil("Fant ikke vilkårResultat med id $vilkårId ved oppdatering av vilkår")

        validerResultatBegrunnelse(vilkårResultatDto)

        val personResultat =
            finnPersonResultatForPersonThrows(vilkårsvurdering.personResultater, personResultatDto.personIdent)

        val eksisterendeVilkårResultat =
            personResultat.vilkårResultater.singleOrNull { it.id == vilkårId }
                ?: throw Feil("Finner ikke vilkår med vilkårId $vilkårId på personResultat ${personResultat.id}")

        val erEndringSomPåvirkerResultat = erEndringIVilkårSomPåvirkerResultat(eksisterendeVilkårResultat, vilkårResultatDto)

        if (eksisterendeVilkårResultat.erOpprinneligPreutfyltIBehandling == behandlingId) {
            val erEndringIBegrunnelse = eksisterendeVilkårResultat.begrunnelse != vilkårResultatDto.begrunnelse

            if (!erEndringIBegrunnelse && !erEndringSomPåvirkerResultat) {
                return vilkårsvurdering.personResultater.map { it.tilPersonResultatDto() }
            }

            val begrunnelseErTomEllerAutomatiskUtfylt =
                vilkårResultatDto.begrunnelse.run { isBlank() || startsWith(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT) }

            if (erEndringSomPåvirkerResultat && begrunnelseErTomEllerAutomatiskUtfylt) {
                throw FunksjonellFeil(
                    melding = "Begrunnelse må være fylt inn ved oppdatering av preutfylt vilkår",
                    frontendFeilmelding = "Du har endret vilkåret, og må derfor fjerne den automatiske begrunnelsen og fylle inn en ny begrunnelse.",
                )
            }

            val eksisterendeLogger =
                endringIPreutfyltVilkårLoggRepository.findByVilkårResultatId(eksisterendeVilkårResultat.id)

            val logg =
                if (eksisterendeLogger.size == 1) {
                    eksisterendeLogger.single().oppdaterEndringIPreutfyltVilkårLogg(vilkårResultatDto)
                } else {
                    opprettLoggForEndringIPreutfyltVilkår(
                        behandling = vilkårsvurdering.behandling,
                        forrigeVilkår = eksisterendeVilkårResultat,
                        nyttVilkår = vilkårResultatDto,
                    )
                }

            endringIPreutfyltVilkårLoggRepository.save(logg)
        }

        val erBosattIRiketVilkårIBehandlingMedKompetanse =
            eksisterendeVilkårResultat.vilkårType == Vilkår.BOSATT_I_RIKET &&
                kompetanseService.hentKompetanser(BehandlingId(behandlingId)).isNotEmpty()
        val utdypendeVilkårsvurderingerFørEndringTidslinje =
            if (erBosattIRiketVilkårIBehandlingMedKompetanse) {
                personResultat.tilUtdypendeVilkårsvurderingerForBosattIRiketTidslinje()
            } else {
                null
            }

        muterPersonVilkårResultaterPut(personResultat, vilkårResultatDto)

        val vilkårResultat =
            personResultat.vilkårResultater.singleOrNull { it.id == vilkårId }
                ?: throw Feil("Finner ikke vilkår med vilkårId $vilkårId på personResultat ${personResultat.id}")

        vilkårResultat.also {
            it.standardbegrunnelser = vilkårResultatDto.avslagBegrunnelser ?: emptyList()
        }

        val migreringsdatoPåFagsak =
            behandlingService.hentMigreringsdatoPåFagsak(fagsakId = vilkårsvurdering.behandling.fagsak.id)
        validerVilkårStarterIkkeFørMigreringsdatoForMigreringsbehandling(
            vilkårsvurdering,
            vilkårResultat,
            migreringsdatoPåFagsak,
        )

        val personResultatList = vilkårsvurderingService.oppdater(vilkårsvurdering).personResultater.map { it.tilPersonResultatDto() }
        if (erEndringSomPåvirkerResultat) {
            // Kompetanseendring tilbakestiller steget til behandlingsresultat, så nullstilling må skje før tilbakestilling til vilkårsvurdering
            if (utdypendeVilkårsvurderingerFørEndringTidslinje != null) {
                nullstillKompetanseVedEndringIUtdypendeVilkårsvurdering(BehandlingId(behandlingId), personResultat, utdypendeVilkårsvurderingerFørEndringTidslinje)
            }
            tilbakestillBehandlingService.resettStegVedEndringPåVilkår(behandlingId)
        }
        return personResultatList
    }

    private fun nullstillKompetanseVedEndringIUtdypendeVilkårsvurdering(
        behandlingId: BehandlingId,
        personResultat: PersonResultat,
        utdypendeVilkårsvurderingerFørEndringTidslinje: Tidslinje<Set<UtdypendeVilkårsvurdering>>,
    ) {
        val perioderMedNyEllerEndretUtdypendeVilkårsvurdering =
            utdypendeVilkårsvurderingerFørEndringTidslinje
                .kombinerMed(personResultat.tilUtdypendeVilkårsvurderingerForBosattIRiketTidslinje()) { før, etter -> etter != null && før != etter }
                .tilPerioderIkkeNull()
                .filter { it.verdi }

        val barnAktører =
            if (personResultat.erSøkersResultater()) {
                kompetanseService.hentKompetanser(behandlingId).flatMap { it.barnAktører }.toSet()
            } else {
                setOf(personResultat.aktør)
            }

        perioderMedNyEllerEndretUtdypendeVilkårsvurdering.forEach { periode ->
            val blankKompetanse = Kompetanse.NULL.copy(fom = periode.fom?.toYearMonth(), tom = periode.tom?.toYearMonth(), barnAktører = barnAktører)
            kompetanseService.oppdaterKompetanse(behandlingId, blankKompetanse)
        }
    }

    private fun PersonResultat.tilUtdypendeVilkårsvurderingerForBosattIRiketTidslinje(): Tidslinje<Set<UtdypendeVilkårsvurdering>> =
        vilkårResultater
            .lagForskjøvetTidslinjeForOppfylteVilkår(Vilkår.BOSATT_I_RIKET)
            .mapIkkeNull { it.utdypendeVilkårsvurderinger.toSet() - UTDYPENDE_VILKÅR_SOM_GIR_TILLEGG }

    private fun erEndringIVilkårSomPåvirkerResultat(
        vilkårResultat: VilkårResultat,
        vilkårResultatDto: VilkårResultatDto,
    ): Boolean =
        vilkårResultat.periodeFom != vilkårResultatDto.periodeFom ||
            vilkårResultat.periodeTom != vilkårResultatDto.periodeTom ||
            vilkårResultat.resultat != vilkårResultatDto.resultat ||
            vilkårResultat.resultatBegrunnelse != vilkårResultatDto.resultatBegrunnelse ||
            vilkårResultat.erEksplisittAvslagPåSøknad != vilkårResultatDto.erEksplisittAvslagPåSøknad ||
            vilkårResultat.vurderesEtter != vilkårResultatDto.vurderesEtter ||
            vilkårResultat.utdypendeVilkårsvurderinger.toSet() != vilkårResultatDto.utdypendeVilkårsvurderinger.toSet() ||
            vilkårResultat.standardbegrunnelser.toSet() != vilkårResultatDto.avslagBegrunnelser.orEmpty().toSet()

    @Transactional
    fun deleteVilkårsperiode(
        behandlingId: Long,
        vilkårId: Long,
        aktør: Aktør,
    ): List<PersonResultatDto> {
        val vilkårsvurdering = hentVilkårsvurderingThrows(behandlingId)

        val personResultat =
            finnPersonResultatForPersonThrows(vilkårsvurdering.personResultater, aktør.aktivFødselsnummer())

        muterPersonResultatDelete(personResultat, vilkårId)

        return vilkårsvurderingService.oppdater(vilkårsvurdering).personResultater.map { it.tilPersonResultatDto() }
    }

    @Transactional
    fun deleteVilkår(
        behandlingId: Long,
        slettVilkårDto: SlettVilkårDto,
    ): List<PersonResultatDto> {
        val vilkårsvurdering = hentVilkårsvurderingThrows(behandlingId)
        val personResultat =
            finnPersonResultatForPersonThrows(vilkårsvurdering.personResultater, slettVilkårDto.personIdent)
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        if (!behandling.kanLeggeTilOgFjerneUtvidetVilkår() ||
            Vilkår.UTVIDET_BARNETRYGD != slettVilkårDto.vilkårType ||
            finnesUtvidetBarnetrydIForrigeBehandling(behandling, slettVilkårDto.personIdent)
        ) {
            throw FunksjonellFeil(
                melding =
                    "Vilkår ${slettVilkårDto.vilkårType.beskrivelse} kan ikke slettes " +
                        "for behandling $behandlingId",
                frontendFeilmelding =
                    "Vilkår ${slettVilkårDto.vilkårType.beskrivelse} kan ikke slettes " +
                        "for behandling $behandlingId",
            )
        }

        personResultat.vilkårResultater
            .filter { it.vilkårType == slettVilkårDto.vilkårType }
            .forEach { personResultat.removeVilkårResultat(it.id) }

        if (slettVilkårDto.vilkårType == Vilkår.UTVIDET_BARNETRYGD) {
            behandlingstemaService.oppdaterBehandlingstemaForVilkår(
                behandling = behandling,
                overstyrtUnderkategori = BehandlingUnderkategori.ORDINÆR,
            )
        }

        return vilkårsvurderingService.oppdater(vilkårsvurdering).personResultater.map { it.tilPersonResultatDto() }
    }

    @Transactional
    fun postVilkår(
        behandlingId: Long,
        nyttVilkårDto: NyttVilkårDto,
    ): List<PersonResultatDto> {
        val vilkårsvurdering = hentVilkårsvurderingThrows(behandlingId)

        val behandling = vilkårsvurdering.behandling

        if (nyttVilkårDto.vilkårType == Vilkår.UTVIDET_BARNETRYGD) {
            validerFørLeggeTilUtvidetBarnetrygd(behandling, nyttVilkårDto, vilkårsvurdering)

            behandlingstemaService.oppdaterBehandlingstemaForVilkår(
                behandling = behandling,
                overstyrtUnderkategori = BehandlingUnderkategori.UTVIDET,
            )
        }

        val personResultat =
            finnPersonResultatForPersonThrows(vilkårsvurdering.personResultater, nyttVilkårDto.personIdent)

        muterPersonResultatPost(personResultat, nyttVilkårDto.vilkårType)

        return vilkårsvurderingService.oppdater(vilkårsvurdering).personResultater.map { it.tilPersonResultatDto() }
    }

    private fun validerFørLeggeTilUtvidetBarnetrygd(
        behandling: Behandling,
        nyttVilkårDto: NyttVilkårDto,
        vilkårsvurdering: Vilkårsvurdering,
    ) {
        if (!behandling.kanLeggeTilOgFjerneUtvidetVilkår() && !harUtvidetVilkår(vilkårsvurdering)) {
            throw FunksjonellFeil(
                melding =
                    "${nyttVilkårDto.vilkårType.beskrivelse} kan ikke legges til for behandling ${behandling.id} " +
                        "med behandlingsårsak ${behandling.opprettetÅrsak.visningsnavn}",
                frontendFeilmelding =
                    "${nyttVilkårDto.vilkårType.beskrivelse} kan ikke legges til " +
                        "for behandling ${behandling.id} med behandlingsårsak ${behandling.opprettetÅrsak.visningsnavn}",
            )
        }

        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandling.id)
        if (personopplysningGrunnlag.søkerOgBarn
                .single { it.aktør == personidentService.hentAktør(nyttVilkårDto.personIdent) }
                .type != PersonType.SØKER
        ) {
            throw FunksjonellFeil(
                melding = "${Vilkår.UTVIDET_BARNETRYGD.beskrivelse} kan ikke legges til for BARN",
                frontendFeilmelding = "${Vilkår.UTVIDET_BARNETRYGD.beskrivelse} kan ikke legges til for BARN",
            )
        }
    }

    private fun harUtvidetVilkår(vilkårsvurdering: Vilkårsvurdering): Boolean =
        vilkårsvurdering.personResultater
            .find { it.erSøkersResultater() }
            ?.vilkårResultater
            ?.any { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD } == true

    private fun finnesUtvidetBarnetrydIForrigeBehandling(
        behandling: Behandling,
        personIdent: String,
    ): Boolean {
        val forrigeBehandlingSomErVedtatt =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)
        if (forrigeBehandlingSomErVedtatt != null) {
            val forrigeBehandlingsvilkårsvurdering =
                hentVilkårsvurdering(forrigeBehandlingSomErVedtatt.id) ?: throw Feil(
                    message =
                        "Forrige behandling $${forrigeBehandlingSomErVedtatt.id} " +
                            "har ikke en aktiv vilkårsvurdering",
                )
            val aktør = personidentService.hentAktør(personIdent)
            return forrigeBehandlingsvilkårsvurdering.personResultater
                .single { it.aktør == aktør }
                .vilkårResultater
                .any { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD }
        }
        return false
    }

    private fun finnPersonResultatForPersonThrows(
        personResultater: Set<PersonResultat>,
        personIdent: String,
    ): PersonResultat {
        val aktør = personidentService.hentAktør(personIdent)
        return personResultater.find { it.aktør == aktør } ?: throw Feil(
            message = FANT_IKKE_VILKÅRSVURDERING_FOR_PERSON_FEILMELDING,
            frontendFeilmelding = "Vilkårsvurderingen finnes ikke lenger for en av personene. Oppdater siden og forsøk på nytt.",
        )
    }

    companion object {
        const val FANT_IKKE_AKTIV_VILKÅRSVURDERING_FEILMELDING = "Fant ikke aktiv vilkårsvurdering"
        const val FANT_IKKE_VILKÅRSVURDERING_FOR_PERSON_FEILMELDING = "Fant ikke vilkårsvurdering for person"
        private val UTDYPENDE_VILKÅR_SOM_GIR_TILLEGG = setOf(BOSATT_I_FINNMARK_NORD_TROMS, BOSATT_PÅ_SVALBARD)
    }
}

fun Vilkår.gjelderAlltidFraBarnetsFødselsdato() = this == Vilkår.GIFT_PARTNERSKAP || this == Vilkår.UNDER_18_ÅR

fun SIVILSTANDTYPE.somForventetHosBarn() = this == SIVILSTANDTYPE.UOPPGITT || this == SIVILSTANDTYPE.UGIFT
