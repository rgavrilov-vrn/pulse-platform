# Removing incident materials

DELETE /api/v1/incidents/{incidentId}/materials/{materialId} requires incident:write and returns 204. Only DRAFT incidents may be modified; other statuses return 409. Missing incident or link returns 404. Only the UUID-key LOCAL storage introduced by this application is managed.

The transaction locks the incident and media asset, deletes the link and removes the media metadata only when no incident links remain. Disk removal happens after commit; rollback never removes the physical file. A file shared with another incident is preserved.

Limitations: physical deletion after commit is not atomic with PostgreSQL. A crash or disk error can leave an orphan file. Disk failures are logged with the material UUID and path; 204 means the attachment link was removed, not a guarantee of physical cleanup. Automatic orphan recovery is not implemented. Logs are not a centralized audit event. Do not deploy without compilation and transaction/permission tests.
