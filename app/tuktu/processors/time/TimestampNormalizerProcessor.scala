package tuktu.processors.time

import tuktu.api.BaseProcessor
import play.api.libs.json.JsObject
import play.api.libs.iteratee.Enumeratee
import tuktu.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.github.nscala_time.time.Imports._
import org.joda.time.format.DateTimeFormatter

/**
 * Floors a given datetimeField, based on the timeframes. Only one timeframe is used, e.g. only years or months,
 * in which a higher timeframe has preference.
 */
class TimestampNormalizerProcessor(resultName: String) extends BaseProcessor(resultName) {

    // the joda timeformatting
    var datetimeFormat = ""
    // the field containing the datetime
    var datetimeField = ""
    // do we append or overwrite the datetimeField
    var overwrite = false

    var millis = 0
    var seconds = 0
    var minutes = 0
    var hours = 0
    var days = 0
    var months = 0
    var years = 0
    
    var dateTimeFormatter: DateTimeFormatter = _
    
    override def initialize(config: JsObject) = {
        datetimeFormat = (config \ "datetime_format").as[String]
        datetimeField = (config \ "datetime_field").as[String]
        overwrite = (config \ "overwrite").asOpt[Boolean].getOrElse(false)
        millis = (config \ "time" \ "millis").asOpt[Int].getOrElse(0)
        seconds = (config \ "time" \ "seconds").asOpt[Int].getOrElse(0)
        minutes = (config \ "time" \ "minutes").asOpt[Int].getOrElse(0)
        hours = (config \ "time" \ "hours").asOpt[Int].getOrElse(0)
        days = (config \ "time" \ "days").asOpt[Int].getOrElse(0)
        months = (config \ "time" \ "months").asOpt[Int].getOrElse(0)
        years = (config \ "time" \ "years").asOpt[Int].getOrElse(0)
        
        // make sure at least a timeframe is set
        if (seconds + minutes + hours + days + months + years == 0)
            seconds = 1

        dateTimeFormatter = DateTimeFormat.forPattern(datetimeFormat)
    }

    override def processor(): Enumeratee[DataPacket, DataPacket] = Enumeratee.mapM(data => {
        Future {
            new DataPacket(for (datum <- data.data) yield {
                val dt = dateTimeFormatter.parseDateTime(tuktu.api.utils.evaluateTuktuString(datum(datetimeField).asInstanceOf[String], datum))
                val newDate = {
                    if (years > 0) {
                        val currentYear = dt.year.roundFloorCopy
                        currentYear.minusYears(currentYear.year.get % years)
                    } else if (months > 0) {
                        val currentMonth = dt.monthOfYear.roundFloorCopy
                        currentMonth.minusMonths(currentMonth.month.get % months)
                    } else if (days > 0) {
                        val currentDay = dt.dayOfYear.roundFloorCopy
                        currentDay.minusDays(currentDay.dayOfYear.get % days)
                    } else if (hours > 0) {
                        val currentHours = dt.hourOfDay.roundFloorCopy
                        currentHours.minusHours(currentHours.hourOfDay.get % hours)
                    } else if (minutes > 0) {
                        val currentMinutes = dt.minuteOfDay.roundFloorCopy
                        currentMinutes.minusMinutes(currentMinutes.minuteOfDay.get % minutes)
                    } else if (seconds > 0) {
                        val currentSeconds = dt.secondOfDay.roundFloorCopy
                        currentSeconds.minusSeconds(currentSeconds.secondOfDay.get % seconds)
                    } else {
                        val currentMillis = dt.millisOfDay.roundFloorCopy
                        currentMillis.minusMillis(currentMillis.millisOfDay.get % millis)
                    }
                }
                
                datum + {
                    if (overwrite) datetimeField -> newDate
                    else resultName -> newDate
                }
            })
        }
    })
    
}