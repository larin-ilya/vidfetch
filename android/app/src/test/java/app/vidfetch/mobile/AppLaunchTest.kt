package app.vidfetch.mobile

import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Launches the real MainActivity (onCreate -> Compose setContent) and checks it starts without crashing. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppLaunchTest {

    @Test
    fun activityStartsWithoutCrash() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        assertNotNull(controller.get())
        controller.pause().stop().destroy()
    }
}
