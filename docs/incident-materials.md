# Incident materials: local disk

Existing tables `incident.media_assets` and `incident.incident_materials` are required (schema 007). The API explicitly writes storage_provider=LOCAL; it does not change existing S3 records.

Configuration can be added under the existing YAML sections or supplied as environment variables:

```yaml
app:
  storage:
    local:
      root-directory: ${INCIDENT_FILES_DIRECTORY:./data/incident-files}
    max-file-size: 50MB
spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 55MB
```

Set INCIDENT_FILES_DIRECTORY to an absolute durable directory in production. The backend account must own it; other users must not be able to modify its contents. Back up the directory and PostgreSQL together. In Docker mount a persistent volume; replicas require shared storage. Proxy request limits must permit multipart overhead.

POST /api/v1/incidents/{id}/materials takes one multipart `file`, requires incident:write and DRAFT, returns 201. GET on the same path lists materials; GET /{materialId}/download requires incident:read and checks the incident-material association. Downloads are attachments, never executable static content. Allowed extensions: jpg, jpeg, png, webp, pdf, txt, csv, mp4, webm, docx, xlsx. Extension allowlisting is not content verification or malware scanning.

Generated UUID storage keys never use supplied filenames. Files are streamed and hashed. A final database transaction locks the incident row, rechecks DRAFT and inserts both records. Rolled-back transactions remove the saved file. A process crash can leave orphan files; automatic orphan cleanup and malware scanning are not included in this first implementation. No deletion or preview API is provided. Attachment logging is not a centralized audit event. Run compilation, integration and permission tests before deployment; this change has not been executed against a running database.
