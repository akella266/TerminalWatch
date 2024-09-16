package by.akella.terminalwatch.watchface

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.view.SurfaceHolder
import androidx.core.content.res.ResourcesCompat
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.WatchFaceLayer
import by.akella.terminalwatch.R
import by.akella.terminalwatch.data.BatteryRepository
import by.akella.terminalwatch.data.CalendarRepository
import by.akella.terminalwatch.data.EventModel
import by.akella.terminalwatch.data.PassiveDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// Default for how long each frame is displayed at expected frame rate.
private const val FRAME_PERIOD_MS_DEFAULT: Long = 16L

class TerminalWatchfaceRenderer(
    private val context: Context,
    private val passiveDataRepository: PassiveDataRepository,
    private val batteryRepository: BatteryRepository,
    private val calendarRepository: CalendarRepository,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int
    ) : Renderer.CanvasRenderer2<TerminalWatchfaceRenderer.SharedAssets>(
        surfaceHolder,
        currentUserStyleRepository,
        watchState,
        canvasType,
    FRAME_PERIOD_MS_DEFAULT,
        false
    ) {

        class SharedAssets : Renderer.SharedAssets {
            override fun onDestroy() {}
        }

    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val steps: StateFlow<Long> = passiveDataRepository.latestSteps
        .stateIn(scope, SharingStarted.Eagerly, 0)
    private val calories: StateFlow<Double> = passiveDataRepository.latestCalories
        .stateIn(scope, SharingStarted.Eagerly, 0.0)
    private val distance: StateFlow<Double> = passiveDataRepository.latestDistance
        .stateIn(scope, SharingStarted.Eagerly, 0.0)
    private val battery: StateFlow<Int> = batteryRepository.batteryLevel
        .stateIn(scope, SharingStarted.Eagerly, 0)

    private val events: StateFlow<EventModel?> = flow {
        emit(calendarRepository.getNearestEvent())
    }.onEach { delay(30_000L) }
        .stateIn(scope, SharingStarted.WhileSubscribed(), null)

    private val fireTypeface = ResourcesCompat.getFont(context, R.font.firecode)
    private val paint: Paint = Paint().apply {
        color = context.resources.getColor(R.color.text_color, context.theme)
        textSize = context.resources.getDimension(R.dimen.number_line)
        typeface = Typeface.create(fireTypeface, Typeface.BOLD)
    }
    private val ambientPaint: Paint = Paint().apply {
        color = context.resources.getColor(R.color.text_color, context.theme)
        textSize = context.resources.getDimension(R.dimen.ambient_number_line)
        typeface = Typeface.create(fireTypeface, Typeface.BOLD)
    }
    private val lineNUmberTextSizePx = context.resources.getDimensionPixelSize(R.dimen.number_line)
    private val ambientLineNUmberTextSizePx = context.resources.getDimensionPixelSize(R.dimen.ambient_number_line)
    private val lineSpacer = context.resources.getDimension(R.dimen.line_spacer)

    override suspend fun createSharedAssets(): SharedAssets = SharedAssets()

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: SharedAssets
    ) {
        canvas.drawColor(renderParameters.highlightLayer!!.backgroundTint)
        //render complications
    }

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: SharedAssets
    ) {
        val backgroundColor = Color.BLACK

        canvas.drawColor(backgroundColor)

        val x = bounds.exactCenterX() / 3f
        val y = bounds.exactCenterY() / 2f

        val ambientX = (bounds.exactCenterX() / 6f)
        val ambientY = (bounds.exactCenterY() / 1.15f)

        if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.BASE)) {
            val time = zonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            val ambientTime = zonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            val date = zonedDateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))

            val lastEvent = events.value
            var lineCounter = 0

            if (renderParameters.drawMode == DrawMode.AMBIENT) {
                canvas.drawText("TIME: $ambientTime", ambientX, ambientY, ambientPaint)
                canvas.drawText("DATE: $date", ambientX, ambientY + ambientLineNUmberTextSizePx + lineSpacer, ambientPaint)
                canvas.drawText("BATT: ${battery.value}/100", ambientX, ambientY + 2 * ambientLineNUmberTextSizePx + lineSpacer, ambientPaint)
            } else {
                canvas.drawText("user@watchface: now", x, y, paint)
                canvas.drawText("--------------", x, y + 1 * lineNUmberTextSizePx + lineSpacer, paint)
                canvas.drawText("TIME: $time", x, y + 2 * lineNUmberTextSizePx + lineSpacer, paint)
                canvas.drawText("DATE: $date", x, y + 3 * lineNUmberTextSizePx + lineSpacer, paint)
                canvas.drawText("STEP: ${steps.value}", x, y + 4 * lineNUmberTextSizePx + lineSpacer, paint)
                canvas.drawText("DIST: ${distance.value.toInt()}", x, y + 5 * lineNUmberTextSizePx + lineSpacer, paint)
                canvas.drawText("CALS: ${calories.value.toInt()}", x, y + 6 * lineNUmberTextSizePx + lineSpacer, paint)
                canvas.drawText("BATT: ${battery.value}/100", x, y + 7 * lineNUmberTextSizePx + lineSpacer, paint)
                if (lastEvent != null) {
                    val eventTime = Instant.ofEpochMilli(lastEvent.startTime)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("HH:mm"))

                    canvas.drawText("CLND: $eventTime ${lastEvent.title}", x, y + 8 * lineNUmberTextSizePx + lineSpacer, paint)
                    lineCounter++
                }
                canvas.drawText("--------------", x, y + ((8 + lineCounter)) * lineNUmberTextSizePx + lineSpacer, paint)
                canvas.drawText("user@watchface", x, y + ((9 + lineCounter)) * lineNUmberTextSizePx + lineSpacer, paint)
            }
        }
    }
}

//adb shell am broadcast -a com.google.android.wearable.app.DEBUG_SURFACE --es operation set-watchface --es watchFaceId by.akella.terminalwatch