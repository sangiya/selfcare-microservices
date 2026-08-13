{{- define "microservice-chart.fullname" -}}
{{ .Values.serviceName }}
{{- end -}}

{{- define "microservice-chart.namespace" -}}
{{- if eq .Release.Namespace "default" -}}
{{ .Values.namespace }}
{{- else -}}
{{ .Release.Namespace }}
{{- end -}}
{{- end -}}

{{- define "microservice-chart.labels" -}}
app.kubernetes.io/name: {{ .Values.serviceName }}
app.kubernetes.io/part-of: selfcare-platform
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- with .Values.labels }}
{{ toYaml . }}
{{- end }}
{{- end -}}
