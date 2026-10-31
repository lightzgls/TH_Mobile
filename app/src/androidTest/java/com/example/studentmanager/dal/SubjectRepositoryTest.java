package com.example.studentmanager.dal;

import static org.junit.Assert.*;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.studentmanager.models.ActionResult;
import com.example.studentmanager.models.Session;
import com.example.studentmanager.models.Subject;
import com.example.studentmanager.models.SubjectDraft;
import com.example.studentmanager.utils.AuthGuard;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SubjectRepositoryTest {
    private DBHelper helper;
    private SubjectRepository repository;
    private TestGuard guard;

    private static class TestGuard extends AuthGuard {
        boolean admin = true;
        boolean student;
        TestGuard(Context context) { super(context); }
        @Override public boolean requireAdmin() { return admin; }
        @Override public Session requireStudent() { return student ? new Session() : null; }
    }

    @Before public void openDatabase() {
        Context context = ApplicationProvider.getApplicationContext();
        helper = new DBHelper(context, null);
        guard = new TestGuard(context);
        repository = new SubjectRepository(context, helper, guard);
    }
    @After public void closeDatabase() { helper.close(); }

    private SubjectDraft draft(String code) {
        SubjectDraft draft = new SubjectDraft();
        draft.subjectCode = code;
        draft.subjectName = "  Kiểm thử  ";
        draft.credits = 2;
        return draft;
    }

    @Test public void crudNormalizesDataAndFindsByCodeOrName() {
        ActionResult created = repository.create(draft("  test01  "));
        assertTrue(created.ok);
        Subject subject = repository.get(created.id);
        assertEquals("TEST01", subject.subjectCode);
        assertEquals("Kiểm thử", subject.subjectName);
        assertEquals("", subject.description);
        assertEquals(created.id, repository.list("test01").get(0).id);
        assertEquals(1, repository.list("Kiểm thử").size());
        SubjectDraft edited = draft("test02");
        edited.description = "  Mô tả  ";
        edited.credits = 4;
        assertTrue(repository.update(created.id, edited).ok);
        assertEquals("Mô tả", repository.get(created.id).description);
        assertEquals(4, repository.get(created.id).credits);
        assertTrue(repository.delete(created.id).ok);
        assertNull(repository.get(created.id));
    }

    @Test public void validatesRequiredFieldsAndPositiveCredits() {
        assertEquals(ActionResult.VALIDATION, repository.create(null).code);
        assertEquals(ActionResult.VALIDATION, repository.create(draft("  ")).code);
        SubjectDraft invalid = draft("VALID");
        invalid.subjectName = null;
        assertEquals(ActionResult.VALIDATION, repository.create(invalid).code);
        invalid.subjectName = "Test";
        invalid.credits = 0;
        assertEquals(ActionResult.VALIDATION, repository.create(invalid).code);
        invalid.credits = -2;
        assertEquals(ActionResult.VALIDATION, repository.create(invalid).code);
    }

    @Test public void rejectsDuplicateCodeOnCreateAndUpdateWithoutLosingData() {
        assertEquals(ActionResult.DUPLICATE, repository.create(draft("mob101")).code);
        long id = repository.create(draft("UNIQUE")).id;
        assertEquals(ActionResult.DUPLICATE, repository.update(id, draft("db101")).code);
        assertEquals("UNIQUE", repository.get(id).subjectCode);
    }

    @Test public void restrictsDeletionEvenForCancelledSchedules() {
        helper.getWritableDatabase().execSQL("UPDATE StudySchedule SET status='CANCELLED' WHERE subjectId=1");
        assertEquals(ActionResult.IN_USE, repository.delete(1).code);
        assertNotNull(repository.get(1));
        assertEquals(ActionResult.NOT_FOUND, repository.delete(9999).code);
        assertEquals(ActionResult.NOT_FOUND, repository.update(9999, draft("MISSING")).code);
        assertEquals(ActionResult.NOT_FOUND, repository.update(-1, draft("MISSING")).code);
    }

    @Test public void treatsSearchWildcardsAndQuotesAsLiteralText() {
        long id = repository.create(draft("100%_\\'")).id;
        assertTrue(id > 0);
        assertEquals(id, repository.list("%_\\'").get(0).id);
        assertEquals(1, repository.list("%").size());
        assertTrue(repository.list("' OR 1=1 --").isEmpty());
    }

    @Test public void studentReadsButCannotWriteAndLoggedOutUserReadsNothing() {
        guard.admin = false;
        guard.student = true;
        assertEquals(3, repository.list(null).size());
        assertNotNull(repository.get(1));
        assertEquals(ActionResult.FORBIDDEN, repository.create(draft("DENIED")).code);
        assertEquals(ActionResult.FORBIDDEN, repository.update(1, draft("DENIED")).code);
        assertEquals(ActionResult.FORBIDDEN, repository.delete(1).code);
        guard.student = false;
        assertTrue(repository.list("").isEmpty());
        assertNull(repository.get(1));
    }
}
