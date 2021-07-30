package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import io.mockk.slot
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.RestVedtak
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.VergeResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.DødsfallData
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.VergeData
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.GrBostedsadresseperiode
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Utbetalingsperiode
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate

fun byggE2EPersonopplysningerServiceMock(mockPersonopplysningerService: PersonopplysningerService,
                                         scenario: Scenario): PersonopplysningerService {
    val personMap = mutableMapOf(scenario.søker.personIdent to scenario.søker)
    scenario.barna.forEach { personMap[it.personIdent] = it }

    val idSlotForHentAktivAktørId = slot<Ident>()
    every {
        mockPersonopplysningerService.hentAktivAktørId(capture(idSlotForHentAktivAktørId))
    } answers {
        personMap[idSlotForHentAktivAktørId.captured.ident]?.toAktørId() ?: randomAktørId()
    }

    val idSlotForHentAktivPersonIdent = slot<Ident>()
    every {
        mockPersonopplysningerService.hentAktivPersonIdent(capture(idSlotForHentAktivPersonIdent))
    } answers {
        PersonIdent(idSlotForHentAktivPersonIdent.captured.ident)
    }

    val idSlotForHentIdenter = slot<Ident>()
    every {
        mockPersonopplysningerService.hentIdenter(capture(idSlotForHentIdenter))
    } answers {
        listOf(IdentInformasjon(idSlotForHentIdenter.captured.ident, false, "FOLKEREGISTERIDENT"))
    }

    every {
        mockPersonopplysningerService.hentDødsfall(any())
    } returns DødsfallData(false, null)

    every {
        mockPersonopplysningerService.hentVergeData(any())
    } returns VergeData(false)

    every {
        mockPersonopplysningerService.harVerge(any())
    } returns VergeResponse(false)

    every {
        mockPersonopplysningerService.hentLandkodeUtenlandskBostedsadresse(any())
    } returns "NO"

    val idSlotForHentStatsborgerskap = slot<Ident>()
    every {
        mockPersonopplysningerService.hentStatsborgerskap(capture(idSlotForHentStatsborgerskap))
    } answers {
        personMap[idSlotForHentStatsborgerskap.captured.ident]?.statsborgerskap ?: emptyList()
    }

    every {
        mockPersonopplysningerService.hentOpphold(any())
    } answers {
        listOf(Opphold(type = OPPHOLDSTILLATELSE.PERMANENT,
                       oppholdFra = LocalDate.of(1990, 1, 25),
                       oppholdTil = LocalDate.of(2499, 1, 1)))
    }

    every {
        mockPersonopplysningerService.hentBostedsadresseperioder(any())
    } answers {
        listOf(GrBostedsadresseperiode(
                periode = DatoIntervallEntitet(
                        fom = LocalDate.of(2002, 1, 4),
                        tom = LocalDate.of(2002, 1, 5)
                )))
    }

    every {
        mockPersonopplysningerService.hentAdressebeskyttelseSomSystembruker(any())
    } answers {
        ADRESSEBESKYTTELSEGRADERING.UGRADERT
    }

    val idSlotForHentPersoninfo = slot<String>()
    every {
        mockPersonopplysningerService.hentPersoninfo(capture(idSlotForHentPersoninfo))
    } answers {
        personMap[idSlotForHentPersoninfo.captured]?.tilPersonInfo() ?: throw Feil("Finner ikke person")
    }

    val idSlotForHentHistoriskPersoninfoManuell = slot<String>()
    every {
        mockPersonopplysningerService.hentHistoriskPersoninfoManuell(capture(idSlotForHentHistoriskPersoninfoManuell))
    } answers {
        personMap[idSlotForHentHistoriskPersoninfoManuell.captured]?.tilPersonInfo() ?: throw Feil("Finner ikke person")
    }

    val idSlotForHentPersoninfoMedRelasjoner = slot<String>()
    every {
        mockPersonopplysningerService.hentPersoninfoMedRelasjoner(capture(idSlotForHentPersoninfoMedRelasjoner))
    } answers {
        personMap[idSlotForHentPersoninfoMedRelasjoner.captured]?.tilPersonInfo() ?: throw Feil("Finner ikke person")
    }

    return mockPersonopplysningerService
}

fun generellAssertFagsak(restFagsak: Ressurs<RestFagsak>,
                         fagsakStatus: FagsakStatus,
                         behandlingStegType: StegType? = null,
                         behandlingResultat: BehandlingResultat? = null) {
    if (restFagsak.status != Ressurs.Status.SUKSESS) throw IllegalStateException("generellAssertFagsak feilet. status: ${restFagsak.status.name},  melding: ${restFagsak.melding}")
    assertEquals(fagsakStatus, restFagsak.data?.status)
    if (behandlingStegType != null) {
        assertEquals(behandlingStegType, hentAktivBehandling(restFagsak = restFagsak.data!!)?.steg)
    }
    if (behandlingResultat != null) {
        assertEquals(behandlingResultat, hentAktivBehandling(restFagsak = restFagsak.data!!)?.resultat)
    }
}

fun assertUtbetalingsperiode(utbetalingsperiode: Utbetalingsperiode, antallBarn: Int, utbetaltPerMnd: Int) {
    assertEquals(antallBarn, utbetalingsperiode.utbetalingsperiodeDetaljer.size)
    assertEquals(utbetaltPerMnd, utbetalingsperiode.utbetaltPerMnd)
}

fun hentNåværendeEllerNesteMånedsUtbetaling(behandling: RestUtvidetBehandling): Int {
    val utbetalingsperioder =
            behandling.utbetalingsperioder.sortedBy { it.periodeFom }
    val nåværendeUtbetalingsperiode = utbetalingsperioder
            .firstOrNull { it.periodeFom.isBefore(LocalDate.now()) && it.periodeTom.isAfter(LocalDate.now()) }

    val nesteUtbetalingsperiode = utbetalingsperioder.firstOrNull { it.periodeFom.isAfter(LocalDate.now()) }

    return nåværendeUtbetalingsperiode?.utbetaltPerMnd ?: nesteUtbetalingsperiode?.utbetaltPerMnd ?: 0
}

fun hentAktivBehandling(restFagsak: RestFagsak): RestUtvidetBehandling? {
    return restFagsak.behandlinger.firstOrNull { it.aktiv }
}

fun hentAktivtVedtak(restFagsak: RestFagsak): RestVedtak? {
    return hentAktivBehandling(restFagsak)?.vedtakForBehandling?.firstOrNull { it.aktiv }
}
