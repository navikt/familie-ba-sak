package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.ekstern.restDomene.VedtakBegrunnelseTilknyttetVilkårDto
import no.nav.familie.ba.sak.integrasjoner.sanity.SanityService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth

@Service
class VilkårsvurderingService(
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val sanityService: SanityService,
) {
    fun hentAktivForBehandling(behandlingId: Long): Vilkårsvurdering? = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId)

    fun hentAktivForBehandlingThrows(behandlingId: Long): Vilkårsvurdering =
        hentAktivForBehandling(behandlingId)
            ?: throw Feil("Fant ikke vilkårsvurdering knyttet til behandling=$behandlingId")

    fun oppdater(vilkårsvurdering: Vilkårsvurdering): Vilkårsvurdering {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppdaterer vilkårsvurdering $vilkårsvurdering")
        return vilkårsvurderingRepository.saveAndFlush(vilkårsvurdering)
    }

    fun lagreNyOgDeaktiverGammel(vilkårsvurdering: Vilkårsvurdering): Vilkårsvurdering {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter vilkårsvurdering $vilkårsvurdering")

        val aktivVilkårsvurdering = hentAktivForBehandling(vilkårsvurdering.behandling.id)

        if (aktivVilkårsvurdering != null) {
            vilkårsvurderingRepository.saveAndFlush(aktivVilkårsvurdering.also { it.aktiv = false })
        }

        return vilkårsvurderingRepository.save(vilkårsvurdering)
    }

    fun lagreInitielt(vilkårsvurdering: Vilkårsvurdering): Vilkårsvurdering {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter vilkårsvurdering $vilkårsvurdering")

        val aktivVilkårsvurdering = hentAktivForBehandling(vilkårsvurdering.behandling.id)
        if (aktivVilkårsvurdering != null) {
            throw Feil("Det finnes allerede et aktivt vilkårsvurdering for behandling ${vilkårsvurdering.behandling.id}")
        }

        return vilkårsvurderingRepository.save(vilkårsvurdering)
    }

    fun hentVilkårsbegrunnelser(): Map<VedtakBegrunnelseType, List<VedtakBegrunnelseTilknyttetVilkårDto>> =
        standardbegrunnelserTilNedtrekksmenytekster(sanityService.hentSanityBegrunnelser(filtrerBortBegrunnelserSomIkkeErIBruk = true)) +
            eøsStandardbegrunnelserTilNedtrekksmenytekster(sanityService.hentSanityEØSBegrunnelser(filtrerBortBegrunnelserSomIkkeErIBruk = true))

    fun hentTidligsteVilkårsvurderingKnyttetTilMigrering(behandlingId: Long): YearMonth? {
        val vilkårsvurdering =
            hentAktivForBehandling(
                behandlingId = behandlingId,
            )

        return vilkårsvurdering
            ?.personResultater
            ?.flatMap { it.vilkårResultater }
            ?.filter { it.periodeFom != null }
            ?.filter { it.vilkårType != Vilkår.UNDER_18_ÅR && it.vilkårType != Vilkår.GIFT_PARTNERSKAP }
            ?.minByOrNull { it.periodeFom!! }
            ?.periodeFom
            ?.toYearMonth()
    }

    @Transactional
    fun oppdaterVilkårVedDødsfall(
        behandlingId: BehandlingId,
        dødsfallsDato: LocalDate,
        aktør: Aktør,
    ) {
        val vilkårsvurdering = hentAktivForBehandlingThrows(behandlingId.id)

        val personResultat =
            vilkårsvurdering.personResultater.find { it.aktør == aktør }
                ?: throw Feil(message = "Fant ikke vilkårsvurdering for person under manuell registrering av dødsfall dato")

        personResultat.vilkårResultater.filter { it.periodeTom != null && it.periodeTom!! > dødsfallsDato }.forEach {
            it.periodeTom = dødsfallsDato
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(VilkårsvurderingService::class.java)
    }
}
