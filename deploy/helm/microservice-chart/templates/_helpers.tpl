{{- define "microservice-chart.fullname" -}}
{{ .Values.serviceName }}
{{- end -}}

{{- define "microservice-chart.labels" -}}
app.kubernetes.io/name: {{ .Values.serviceName }}
app.kubernetes.io/part-of: selfcare-platform
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- with .Values.labels }}
{{ toYaml . }}
{{- end }}
{{- end -}}
