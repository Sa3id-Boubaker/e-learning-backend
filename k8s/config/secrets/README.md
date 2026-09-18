# k8s/config/secrets/ — examples only

Every file in this folder ending in `.secret.example.yaml` is a **template**, not a
real Secret. None of them contain real credentials — every value is the literal
placeholder `CHANGE_ME`.

**Never commit a copy of these files with real values filled in.** If you need a
local working copy while testing, keep it outside git (for example, copy it to a
file matching an entry in `.gitignore`, or create the Secret directly with
`kubectl` / `kubectl create secret generic ... --from-literal=...` without ever
writing the real value to a file in this repository).

## Secrets required by OMARISE

| Example file | Secret name | Real value comes from |
|---|---|---|
| `mongo.secret.example.yaml` | `omarise-mongo` | Your MongoDB Atlas database user |
| `jwt.secret.example.yaml` | `omarise-jwt` | The JWT signing secret already used by Jenkins (`jwt-secret` credential) |
| `mail.secret.example.yaml` | `omarise-mail` | Your SMTP account credentials |
| `cloudinary.secret.example.yaml` | `omarise-cloudinary` | Your Cloudinary account (cloud name + API key/secret) |
| `rabbitmq.secret.example.yaml` | `omarise-rabbitmq` | A RabbitMQ username/password you choose for the in-cluster broker |

## GHCR image pull secret

There is deliberately **no example file** for the GHCR pull secret — see
`k8s/README.md` for the exact `kubectl create secret docker-registry` command to run
manually, using your own GitHub Personal Access Token. It must never be written to a
file in this repository.
