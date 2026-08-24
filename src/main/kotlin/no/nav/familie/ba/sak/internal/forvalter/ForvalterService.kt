package no.nav.familie.ba.sak.internal.forvalter

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.UNDER_18_ÅR
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class ForvalterService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val fagsakRepository: FagsakRepository,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val persongrunnlagService: PersongrunnlagService,
    private val aktørIdRepository: AktørIdRepository,
) {
    private val logger = LoggerFactory.getLogger(ForvalterService::class.java)

    fun settFomPåVilkårTilPersonsFødselsdato(behandlingId: Long): Vilkårsvurdering {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val personerPåBehandling = persongrunnlagService.hentSøkerOgBarnPåBehandling(behandlingId)

        if (!behandling.aktiv || !behandling.erVedtatt()) {
            throw Feil("Behandlingen er ikke aktiv eller ikke vedtatt, så den burde ikke patches.")
        }

        val vilkårsvurdering =
            vilkårsvurderingService.hentAktivForBehandling(behandlingId)
                ?: throw Feil("Det er ingen vilkårsvurdering for behandling: $behandlingId")

        vilkårsvurdering.personResultater.forEach { personResultat ->
            personResultat.vilkårResultater.forEach vilkårresultatLoop@{ vilkårResultat ->
                if (vilkårResultat.vilkårType == UNDER_18_ÅR) return@vilkårresultatLoop

                val person =
                    personerPåBehandling?.singleOrNull { it.aktør == personResultat.aktør }
                        ?: throw Feil("Finner ikke person på behandling med aktørId ${personResultat.aktør.aktørId}.")

                validerKunEttVilkårResultatFørFødselsdato(personResultat, vilkårResultat.vilkårType, person.fødselsdato)

                val vilkårFomErFørFødselsdato =
                    vilkårResultat.periodeFom?.isBefore(person.fødselsdato)
                        ?: throw Feil("Vilkår ${vilkårResultat.id} har ingen fom-dato og kan ikke patches.")

                val periodeFomOgFødselsdatoErISammeMåned = vilkårResultat.periodeFom!!.toYearMonth() == person.fødselsdato.toYearMonth()
                if (vilkårFomErFørFødselsdato && periodeFomOgFødselsdatoErISammeMåned) {
                    secureLogger.info(
                        "Vilkårresultat ${vilkårResultat.vilkårType} med id=${vilkårResultat.id} på behandling $behandlingId har periodeFom ${vilkårResultat.periodeFom} som er før personens fødselsdato ${person.fødselsdato}. " +
                            "Setter den til personens fødselsdato.",
                    )
                    vilkårResultat.periodeFom = person.fødselsdato
                } else if (vilkårFomErFørFødselsdato) {
                    throw Feil("Vilkår ${vilkårResultat.id} har fom-dato før ${person.fødselsdato}, men de er ikke i samme måned.")
                }
            }
        }

        vilkårsvurdering.personResultater.forEach { personResultat ->
            val person =
                personerPåBehandling?.singleOrNull { it.aktør == personResultat.aktør }
                    ?: throw Feil("Finner ikke person på behandling.")

            if (personResultat.vilkårResultater.any { it.periodeFom?.isBefore(person.fødselsdato) == true && it.vilkårType != UNDER_18_ÅR }) {
                throw Feil("Er fortsatt vilkår som starter før fødselsdato på barn.")
            }
        }
        return vilkårsvurderingService.oppdater(vilkårsvurdering)
    }

    private fun validerKunEttVilkårResultatFørFødselsdato(
        personResultat: PersonResultat,
        vilkårType: Vilkår,
        fødselsdato: LocalDate,
    ) {
        val vilkårResultatAvSammeTypeFørFødselsdatoForPerson =
            personResultat.vilkårResultater
                .filter {
                    it.vilkårType == vilkårType &&
                        it.periodeFom?.isBefore(fødselsdato) ?: true
                }
        if (vilkårResultatAvSammeTypeFørFødselsdatoForPerson.size > 1) {
            throw Feil("Det finnes flere vilkårresultater som begynner før fødselsdato til person: $this")
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun slettAktørId(aktørId: String) {
        val aktør =
            aktørIdRepository.findByAktørIdOrNull(aktørId)
                ?: throw Feil("Fant ikke aktør med aktørId $aktørId som skal slettes")

        val fødselsnummer = if (aktør.personidenter.isNotEmpty()) aktør.aktivFødselsnummer() else null
        aktørIdRepository.delete(aktør)
        secureLogger.info("Slettet aktør med aktørId=$aktørId og fnr=$fødselsnummer")
    }

    @Transactional
    fun endreFagsakStatusFraLøpendeTilOpprettet(fagsakId: Long) {
        val fagsak =
            fagsakRepository.finnFagsak(fagsakId)
                ?: throw Feil("Finner ikke fagsak med id $fagsakId")

        if (fagsak.status != FagsakStatus.LØPENDE) {
            throw Feil("Fagsak $fagsakId har status ${fagsak.status}. Kan bare endre fra LØPENDE til OPPRETTET.")
        }

        val behandlinger = behandlingHentOgPersisterService.hentBehandlinger(fagsak.id)
        if (behandlinger.any { it.status != BehandlingStatus.AVSLUTTET || !it.erHenlagt() }) {
            throw Feil("Fagsak $fagsakId har behandlinger som ikke er henlagt og status kan ikke endres.")
        }

        fagsak.status = FagsakStatus.OPPRETTET
        fagsakRepository.save(fagsak)

        logger.info("Endret status fra LØPENDE til OPPRETTET for fagsak $fagsakId.")
    }
}

interface FagsakMedFlereMigreringer {
    val fagsakId: Long
    val fødselsnummer: String
}
