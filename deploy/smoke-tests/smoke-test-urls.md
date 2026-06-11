# TAMIAS — Smoke Test URLs

## Testing / Portfolio

Frontend:

```text
https://tamias.juantzun.dev
```

Backend health:

```text
https://tamias-api-testing.onrender.com/actuator/health
```

Backend API base:

```text
https://tamias-api-testing.onrender.com/api/v1
```

Minimum checks:

```text
[ ] Frontend loads login page
[ ] Browser console has no environment/API URL errors
[ ] Backend health returns UP
[ ] Login works
[ ] Dashboard analytics loads
[ ] Properties list loads
[ ] Documents list loads
[ ] Upload/download file works
[ ] AI assistant responds using testing documents only
```

---

## Production real

Frontend:

```text
https://tamias-prod.juantzun.dev
```

Backend health:

```text
https://tamias-api-prod.onrender.com/actuator/health
```

Backend API base:

```text
https://tamias-api-prod.onrender.com/api/v1
```

Minimum checks:

```text
[ ] Frontend loads login page
[ ] Browser console has no environment/API URL errors
[ ] Backend health returns UP
[ ] Login works
[ ] Dashboard analytics loads
[ ] Properties list loads
[ ] Documents list loads
[ ] Upload/download file works
[ ] AI assistant responds using production documents only
```

---

## Cross-environment safety checks

```text
[ ] Testing frontend does not call production backend
[ ] Production frontend does not call testing backend
[ ] Testing backend CORS does not allow production frontend unless intentionally configured
[ ] Production backend CORS does not allow testing frontend unless intentionally configured
[ ] Testing uploads land in tamias-testing-files
[ ] Production uploads land in tamias-prod-files
[ ] Testing document chunks use tamias_testing_documents
[ ] Production document chunks use tamias_prod_documents
```
