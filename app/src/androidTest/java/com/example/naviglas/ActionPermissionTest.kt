package com.example.naviglas
import android.Manifest
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry
import com.example.naviglas.engine.*
import org.junit.Assert.*
import org.junit.Test

class ActionPermissionTest {
    private val denied = object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
        override fun checkSelfPermission(permission: String) = PackageManager.PERMISSION_DENIED
    }
    @Test fun deniedCallPermissionReportsFailureWithoutLaunchingDialer() {
        var message = ""
        ActionExecutor.execute(denied, ParsedCommandResult(CommandAction.CALL, phoneNumber = "5556", spokenFeedback = ""), emptyList()) { message = it }
        assertTrue(message.contains("dozvola za pozive"))
    }
    @Test fun deniedSmsPermissionDoesNotSubmitMessage() {
        var message = ""
        SmsSender.send(denied, "5556", "Only a local permission test", { message = it })
        assertTrue(message.contains("dozvola za slanje SMS"))
    }
}

