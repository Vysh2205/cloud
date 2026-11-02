package com.cloudbackup.platform.dto.request;

public class RestoreRequest {

    /**
     * If true, promotes the restored version to become the new latest active version of the file.
     * If false, downloads or restores the historical version without altering current version pointer.
     */
    private boolean promoteToCurrentVersion = true;

    private String restoreReason;

    public RestoreRequest() {}

    public RestoreRequest(boolean promoteToCurrentVersion, String restoreReason) {
        this.promoteToCurrentVersion = promoteToCurrentVersion;
        this.restoreReason = restoreReason;
    }

    public boolean isPromoteToCurrentVersion() {
        return promoteToCurrentVersion;
    }

    public void setPromoteToCurrentVersion(boolean promoteToCurrentVersion) {
        this.promoteToCurrentVersion = promoteToCurrentVersion;
    }

    public String getRestoreReason() {
        return restoreReason;
    }

    public void setRestoreReason(String restoreReason) {
        this.restoreReason = restoreReason;
    }
}
