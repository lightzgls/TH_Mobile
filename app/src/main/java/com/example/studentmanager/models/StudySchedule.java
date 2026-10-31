package com.example.studentmanager.models;

/** Khung chung: kieu du lieu da chot, khong tinh dong gop chuc nang. */
public class StudySchedule {
    public long id;
    public long studentId;
    public long subjectId;
    public String studyDate;
    public String startTime;
    public String endTime;
    public String location;
    public String status;
}
