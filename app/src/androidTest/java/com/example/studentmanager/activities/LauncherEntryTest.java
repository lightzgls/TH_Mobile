package com.example.studentmanager.activities;

import static org.junit.Assert.*;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.widget.EditText;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.example.studentmanager.MainActivity;
import com.example.studentmanager.R;
import com.example.studentmanager.dal.AuthRepository;
import com.example.studentmanager.utils.DbExecutor;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Kiem tra dung loi mo nham demo khi Run app bang default activity. */
@RunWith(AndroidJUnit4.class)
public class LauncherEntryTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private Context context;

    @Before public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        DbExecutor.execute(() -> new AuthRepository(context).logout());
        awaitTasks();
    }

    @After public void tearDown() throws Exception {
        DbExecutor.execute(() -> new AuthRepository(context).logout());
        awaitTasks();
        instrumentation.runOnMainSync(() -> {
            for (Activity activity : new ArrayList<>(ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED))) activity.finish();
        });
    }

    private void awaitTasks() throws Exception {
        for (int i = 0; i < 3; i++) {
            CountDownLatch done = new CountDownLatch(1);
            DbExecutor.execute(done::countDown);
            assertTrue(done.await(30, TimeUnit.SECONDS));
            instrumentation.waitForIdleSync();
        }
    }

    private Intent launcher() {
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(context.getPackageName());
        assertEquals("Debug app must have exactly one launcher", 1,
                context.getPackageManager().queryIntentActivities(intent, 0).size());
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        assertNotNull(launch);
        assertNotNull(launch.getComponent());
        assertEquals(MainActivity.class.getName(), launch.getComponent().getClassName());
        return launch;
    }

    private <T extends Activity> T current(Class<T> type) {
        AtomicReference<T> found = new AtomicReference<>();
        instrumentation.runOnMainSync(() -> {
            for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)) {
                if (type.isInstance(activity)) found.set(type.cast(activity));
            }
        });
        assertNotNull("Expected " + type.getSimpleName(), found.get());
        return found.get();
    }

    @Test public void defaultLauncherShowsLoginThenAdminAndLogoutReturnsToLogin() throws Exception {
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(launcher())) {
            awaitTasks();
            LoginActivity login = current(LoginActivity.class);
            instrumentation.runOnMainSync(() -> {
                assertTrue(login.findViewById(R.id.auth_submit).isShown());
                ((EditText) login.findViewById(R.id.auth_username)).setText("admin");
                ((EditText) login.findViewById(R.id.auth_password)).setText("Admin@123");
                login.findViewById(R.id.auth_submit).performClick();
            });
            awaitTasks();
            AdminHomeActivity home = current(AdminHomeActivity.class);
            instrumentation.runOnMainSync(() -> home.findViewById(R.id.admin_home_back).performClick());
            awaitTasks();
            LoginActivity returned = current(LoginActivity.class);
            instrumentation.runOnMainSync(() -> assertTrue(returned.findViewById(R.id.auth_submit).isShown()));
        }
    }

    @Test public void savedSessionOpensCorrectHomeInsteadOfDemo() throws Exception {
        AtomicReference<Boolean> loggedIn = new AtomicReference<>(false);
        DbExecutor.execute(() -> loggedIn.set(new AuthRepository(context).login("admin", "Admin@123").ok));
        awaitTasks();
        assertTrue(loggedIn.get());
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(launcher())) {
            awaitTasks();
            AdminHomeActivity home = current(AdminHomeActivity.class);
            instrumentation.runOnMainSync(() -> assertTrue(home.findViewById(R.id.admin_home_back).isShown()));
        }
    }
}
