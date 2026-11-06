package com.rentease.renteasesystem.reports.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Validated admin form for changing only internal review-management fields. */
public class ReviewManagementUpdate {

    @NotNull(message = "Choose a management status.")
    private Review.ManagementStatus managementStatus;

    @Size(max = 2000, message = "The internal note cannot exceed 2,000 characters.")
    private String managementNote;

    public Review.ManagementStatus getManagementStatus() { return managementStatus; }
    public void setManagementStatus(Review.ManagementStatus managementStatus) { this.managementStatus = managementStatus; }
    public String getManagementNote() { return managementNote; }
    public void setManagementNote(String managementNote) { this.managementNote = managementNote; }
}
