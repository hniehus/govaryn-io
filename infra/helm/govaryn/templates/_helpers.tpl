{{- define "govaryn.name" -}}govaryn{{- end -}}
{{- define "govaryn.fullname" -}}
{{- printf "%s-%s" .Release.Name (include "govaryn.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}
