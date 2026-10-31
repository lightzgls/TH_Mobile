package com.example.studentmanager.utils;

import com.example.studentmanager.models.ActionResult;
import com.example.studentmanager.models.BookingDraft;

/** TODO Thu: validation; kiem tra trung trong transaction tai BookingRepository. */
public final class BookingValidator {
    private BookingValidator() {}
    public static ActionResult validate(BookingDraft draft) {
        return ActionResult.notImplemented();
    }
}
