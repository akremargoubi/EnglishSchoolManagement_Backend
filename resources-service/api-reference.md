# Resources Service — API Reference

**Base URL:** `http://localhost:8080/api/resources`  
**Auth:** `Authorization: Bearer <jwt>` required on all endpoints  
**Roles:** `ADMIN` · `TUTOR` · `STUDENT`

---

## Endpoints

### Upload a resource
```
POST /api/resources/upload
Content-Type: multipart/form-data
```

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `file` | File | yes | Max 20 MB |
| `title` | string | yes | Display name |
| `type` | string | yes | e.g. `PDF`, `VIDEO`, `IMAGE`, `DOCUMENT` |
| `published` | boolean | yes | `true` = visible to students |
| `assessmentId` | number | yes | Links resource to an assessment |

**Access:** ADMIN or TUTOR (tutor must own the assessment's class)

**Response `200`:**
```json
{
  "id": 1,
  "title": "Lecture Notes",
  "type": "PDF",
  "published": true,
  "assessmentId": 42,
  "fileUrl": "https://esms3.dominnovate.com/englishschoolstorage/resources/1714000000000_uuid.pdf",
  "uploadedBy": "uuid",
  "uploadedAt": "2026-04-29T10:00:00Z"
}
```

> `fileUrl` is always a direct URL — render or link to it as-is.

---

### Get resources for an assessment
```
GET /api/resources/assessment/{assessmentId}
```

**Access:**
- ADMIN → all resources
- TUTOR → all resources (must own the class)
- STUDENT → only `published: true` resources (must be enrolled in the class)

**Response `200`:** array of resource objects (same shape as upload response)

---

### Delete a resource
```
DELETE /api/resources/{id}
```

**Access:** ADMIN (any) · TUTOR (own uploads only)

**Response `204 No Content`**

---

## Error responses

| Status | Meaning |
|--------|---------|
| `400` | File is empty |
| `401` | Missing or invalid JWT |
| `403` | Wrong role or not your class/resource |
| `404` | Assessment not found |
| `500` | Storage failure |

---

## Frontend integration notes

- Send as `multipart/form-data` — **not** JSON
- `fileUrl` returned by the API is the final file URL (S3 or local fallback) — no extra path prefix needed
- To show resources for an assessment page: `GET /api/resources/assessment/{assessmentId}`
- Students automatically get filtered to published-only; no frontend filtering needed
