export type UiTelemetryEvent = {
  name: string
  context?: Record<string, string | number | boolean | null | undefined>
}

type WindowWithDataLayer = Window & {
  dataLayer?: Array<Record<string, unknown>>
}

export function trackUiEvent(event: UiTelemetryEvent) {
  const payload = {
    event: 'ui_interaction',
    eventName: event.name,
    timestamp: new Date().toISOString(),
    context: event.context ?? {},
  }

  window.dispatchEvent(new CustomEvent('app:ui-telemetry', { detail: payload }))

  const windowWithDataLayer = window as WindowWithDataLayer
  if (Array.isArray(windowWithDataLayer.dataLayer)) {
    windowWithDataLayer.dataLayer.push(payload)
  }

  if (import.meta.env.DEV) {
    console.debug('[ui-telemetry]', payload)
  }
}
