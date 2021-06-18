package no.nav.familie.ba.sak.kjerne.e2e

import io.mockk.every
import io.mockk.slot
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.RestVedtak
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.DødsfallData
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.VergeData
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.GrBostedsadresseperiode
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import org.junit.jupiter.api.Assertions
import java.time.LocalDate

fun byggE2EPersonopplysningerServiceMock(mockPersonopplysningerService: PersonopplysningerService,
                                         scenario: Scenario): PersonopplysningerService {
    val personMap = mutableMapOf(scenario.søker.personIdent to scenario.søker)
    scenario.barna.forEach { personMap[it.personIdent] = it }

    every {
        mockPersonopplysningerService.hentMaskertPersonInfoVedManglendeTilgang(any())
    } returns null

    every {
        mockPersonopplysningerService.hentAktivAktørId(any())
    } answers {
        randomAktørId()
    }

    every {
        mockPersonopplysningerService.hentAktivPersonIdent(any())
    } answers {
        PersonIdent(randomFnr())
    }

    val identSlot = slot<Ident>()
    every {
        mockPersonopplysningerService.hentIdenter(capture(identSlot))
    } answers {
        listOf(IdentInformasjon(identSlot.captured.ident, false, "FOLKEREGISTERIDENT"))
    }

    every {
        mockPersonopplysningerService.hentDødsfall(any())
    } returns DødsfallData(false, null)

    every {
        mockPersonopplysningerService.hentVergeData(any())
    } returns VergeData(false)

    every {
        mockPersonopplysningerService.hentLandkodeUtenlandskBostedsadresse(any())
    } returns "NO"

    every {
        mockPersonopplysningerService.hentStatsborgerskap(capture(identSlot))
    } answers {
        personMap[identSlot.captured.ident]?.statsborgerskap ?: emptyList()
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
        personMap[identSlot.captured.ident]?.tilPersonInfo() ?: throw Feil("Finner ikke person")
    }

    every {
        mockPersonopplysningerService.hentHistoriskPersoninfoManuell(capture(idSlotForHentPersoninfo))
    } answers {
        personMap[identSlot.captured.ident]?.tilPersonInfo() ?: throw Feil("Finner ikke person")
    }

    every {
        mockPersonopplysningerService.hentPersoninfoMedRelasjoner(capture(idSlotForHentPersoninfo))
    } answers {
        personMap[identSlot.captured.ident]?.tilPersonInfo() ?: throw Feil("Finner ikke person")
    }

    return mockPersonopplysningerService
}

fun generellAssertFagsak(restFagsak: Ressurs<RestFagsak>,
                         fagsakStatus: FagsakStatus,
                         behandlingStegType: StegType? = null,
                         behandlingResultat: BehandlingResultat? = null) {
    if (restFagsak.status != Ressurs.Status.SUKSESS) throw IllegalStateException("generellAssertFagsak feilet. status: ${restFagsak.status.name},  melding: ${restFagsak.melding}")
    Assertions.assertEquals(fagsakStatus, restFagsak.data?.status)
    if (behandlingStegType != null) {
        Assertions.assertEquals(behandlingStegType, hentAktivBehandling(restFagsak = restFagsak.data!!)?.steg)
    }
    if (behandlingResultat != null) {
        Assertions.assertEquals(behandlingResultat, hentAktivBehandling(restFagsak = restFagsak.data!!)?.resultat)
    }
}

fun hentAktivBehandling(restFagsak: RestFagsak): RestUtvidetBehandling? {
    return restFagsak.behandlinger.firstOrNull { it.aktiv }
}

fun hentAktivtVedtak(restFagsak: RestFagsak): RestVedtak? {
    return hentAktivBehandling(restFagsak)?.vedtakForBehandling?.firstOrNull { it.aktiv }
}
