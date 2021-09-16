package no.nav.familie.ba.sak.integrasjoner.infotrygd

import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime
import java.time.YearMonth

class InfotrygdLøpendeBarnetrygdResponse(val harLøpendeBarnetrygd: Boolean)

class InfotrygdÅpenSakResponse(val harÅpenSak: Boolean)

class InfotrygdSkatteetatenPerioderUtvidetRequest(val personIdent: String,
                                                  val år: Int)

class InfotrygdSkatteetatenPerioderUtvidetResponse(val perioder: List<InfotrygdUtvidetBarnetrygdPeriodeSkatteetaten>)

class InfotrygdUtvidetBarnetrygdPeriodeSkatteetaten(@ApiModelProperty(dataType = "java.lang.String", example = "2020-05")
                                            val fomMåned: YearMonth,
                                            @ApiModelProperty(dataType = "java.lang.String", example = "2020-12")
                                            val tomMåned: YearMonth?,
                                            val maxDelingsprosent: String,
                                            val sisteVedtakPaaIdent: LocalDateTime
)
