package com.prime.toolz2.common.compose

import android.app.Activity
import android.content.res.Resources
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.installStatus
import com.google.android.play.core.review.ReviewManager
import com.primex.core.Text
import com.primex.core.resolve
import com.primex.core.runCatching
import com.primex.core.spannedResource

/**
 * A Utility extension function for managing status bar UI.
 *
 * @param color: The background color of the statusBar. if [Color.Unspecified] the status bar will
 * be painted by primaryVariant.
 * @param darkIcons: same as name suggests works in collaboration with color. if it is unspecified; uses
 * light icons as we will use primaryVariant as background.
 */
fun Modifier.statusBarsPadding2(
    color: Color = Color.Unspecified,
    darkIcons: Boolean = false,
) = composed {
    val controller = LocalSystemUiController.current

    // invoke but control only icons not color.
    SideEffect {
        controller.setStatusBarColor(
            //INFO we are not going to change the background of the statusBar here.
            // Reasons are.
            //  * It adds a delay and the change becomes ugly.
            //  * animation to color can't be added.
            Color.Transparent,

            // dark icons only when requested by user and color is unSpecified.
            // because we are going to paint status bar with primaryVariant if unspecified.
            darkIcons && !color.isUnspecified
        )
    }

    val paint = color.takeOrElse { MaterialTheme.colors.primaryVariant }
    // add padding

    val height = with(LocalDensity.current) {
        WindowInsets.statusBars.getTop(this).toFloat()
    }

    // add background
    Modifier
        .drawWithContent {
            drawContent()
            drawRect(paint, size = size.copy(height = height))
        }
        .then(this@composed)
        .statusBarsPadding()
}

val LocalSystemUiController = staticCompositionLocalOf<SystemUiController> {
    error("No ui controller defined!!")
}

// Nav Host Controller
val LocalNavController = staticCompositionLocalOf<NavHostController> {
    error("no local nav host controller found")
}

/**
 * The content padding for the screen under current [NavGraph]
 */
val LocalWindowPadding = compositionLocalOf {
    PaddingValues(0.dp)
}

val NavHostController.current
    @Composable
    get() = currentBackStackEntryAsState().value?.destination?.route

// Setup animation related default things

typealias Anim = AnimationConstants

private const val LONG_DURATION_TIME = 500

/**
 * 500 Mills
 */
val Anim.LongDurationMills get() = LONG_DURATION_TIME

private const val MEDIUM_DURATION_TIME = 400

/**
 * 400 Mills
 */
val Anim.MediumDurationMills get() = MEDIUM_DURATION_TIME

private const val SHORT_DURATION_TIME = 200

/**
 * 200 Mills
 */
val Anim.ShortDurationMills get() = SHORT_DURATION_TIME

private const val ACTIVITY_SHORT_DURATION = 150

/**
 * 150 Mills
 */
val Anim.ActivityShortDurationMills get() = ACTIVITY_SHORT_DURATION

private const val ACTIVITY_LONG_DURATION = 220

/**
 * 220 Mills
 */
val Anim.ActivityLongDurationMills get() = ACTIVITY_LONG_DURATION

object ContentPadding {
    /**
     * A small 4 [Dp] Padding
     */
    val small: Dp = 4.dp

    /**
     * A Medium 8 [Dp] Padding
     */
    val medium: Dp = 8.dp

    /**
     * Normal 16 [Dp] Padding
     */
    val normal: Dp = 16.dp

    /**
     * Large 32 [Dp] Padding
     */
    val large: Dp = 32.dp
}

/**
 * The Standard Elevation Values.
 */
object ContentElevation {
    /**
     * Zero Elevation.
     */
    val none = 0.dp

    /**
     * Elevation of 6 [Dp]
     */
    val low = 6.dp

    /**
     * Elevation of 12 [Dp]
     */
    val medium = 12.dp

    /**
     * Elevation of 20 [Dp]
     */
    val high = 20.dp

    /**
     * Elevation of 30 [Dp]
     */
    val xHigh = 30.dp
}

/**
 * The recommended divider Alpha
 */
val ContentAlpha.Divider get() = com.prime.toolz2.common.compose.Divider
private const val Divider = 0.12f


/**
 * The recommended LocalIndication Alpha
 */
val ContentAlpha.Indication get() = com.prime.toolz2.common.compose.Indication
private const val Indication = 0.1f

@Composable
@ReadOnlyComposable
@NonRestartableComposable
inline fun stringResource(res: Text) =
    spannedResource(value = res)


inline fun Resources.stringResource(res: Text) = resolve(res)

@JvmName("stringResource1")
inline fun Resources.stringResource(res: Text?) = resolve(res)


val LocalAppUpdateManager =
    compositionLocalOf<AppUpdateManager> {
        error("No  Local App Update Manager set.")
    }

val LocalAppReviewManager =
    compositionLocalOf<ReviewManager> {
        error("No Local Review Manager set. ")
    }

private const val TAG = "Util"

fun AppUpdateManager.check(
    activity: Activity,
    resultCode: Int,
    report: Boolean = false,
    emit: (snack: Snack) -> Unit
){
    // obtain the task for each check
    val task = appUpdateInfo
    task.addOnSuccessListener { info ->
        val availability = info.updateAvailability()
        val status = info.installStatus
        emit(Snack("$status"))
        when(availability){
            UpdateAvailability.UNKNOWN ->  emit(Snack("Unknown error occurred!. $availability"))

            UpdateAvailability.UPDATE_NOT_AVAILABLE ->
                emit(Snack("App is already updated to latest version."))

            // UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
            // resume or start new
            else -> {
                val downloaded = info.bytesDownloaded()

                // in case it is 0 to avoid exception.
                val total = info.totalBytesToDownload() + 1
                val progress =  downloaded / total  * 100
                emit(Snack("Downloaded: $progress"))
                // update is available
                //FixMe - Add way to manually adjust flexibility.
                //val isFlexible = (info.clientVersionStalenessDays ?: -1) <= FLEXIBLE_UPDATE_STALENESS_DAYS
                val isFlexible = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                val options =  if (isFlexible) AppUpdateType.FLEXIBLE else AppUpdateType.IMMEDIATE
                emit(Snack("$options"))
                runCatching(TAG){
                    startUpdateFlowForResult(info, options, activity,  resultCode)
                }
            }

        }
    }
    task.addOnFailureListener {
        emit(Snack("Unknown error occured: ${it.message}"))
    }
}