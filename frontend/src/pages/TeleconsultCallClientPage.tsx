import { useEffect, useMemo, useRef, useState } from 'react'
import {
  CallClient,
  LocalVideoStream,
  VideoStreamRenderer,
  type Call,
  type CallAgent,
  type DeviceManager,
  type RemoteParticipant,
  type RemoteVideoStream,
} from '@azure/communication-calling'
import { AzureCommunicationTokenCredential } from '@azure/communication-common'
import { useAuth } from '../auth/useAuth'
import { requestTeleconsultToken, type TeleconsultTokenResponse } from '../services/platformApi'

type Props = {
  joinUrl: string
  role: string
}

function isSupportedJoinUrl(value: string) {
  try {
    const parsed = new URL(value)
    return parsed.protocol === 'https:' || parsed.protocol === 'http:'
  } catch {
    return false
  }
}

function isUuid(value: string) {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value)
}

function buildTeleconsultCallClientUrl(joinUrl: string, role: string) {
  const encodedJoinUrl = encodeURIComponent(joinUrl)
  const encodedRole = encodeURIComponent(role || 'UNKNOWN')
  return `/teleconsult/call?role=${encodedRole}&joinUrl=${encodedJoinUrl}`
}

type RenderedView = {
  renderer: VideoStreamRenderer
  view: { target: HTMLElement; dispose?: () => void }
}

export function TeleconsultCallClientPage({ joinUrl, role }: Props) {
  const { session } = useAuth()
  const [showEmbedded, setShowEmbedded] = useState(true)
  const [cameraEnabled, setCameraEnabled] = useState(true)
  const [micEnabled, setMicEnabled] = useState(true)
  const [callReady, setCallReady] = useState(false)
  const [callState, setCallState] = useState<'idle' | 'connecting' | 'connected' | 'disconnected' | 'error'>('idle')
  const [tokenBootstrap, setTokenBootstrap] = useState<TeleconsultTokenResponse | null>(null)
  const [tokenMessage, setTokenMessage] = useState<string | null>(null)
  const [tokenError, setTokenError] = useState(false)

  const localVideoContainerRef = useRef<HTMLDivElement | null>(null)
  const remoteVideosContainerRef = useRef<HTMLDivElement | null>(null)

  const callClientRef = useRef<CallClient | null>(null)
  const callAgentRef = useRef<CallAgent | null>(null)
  const callRef = useRef<Call | null>(null)
  const deviceManagerRef = useRef<DeviceManager | null>(null)
  const localVideoStreamRef = useRef<LocalVideoStream | null>(null)
  const localViewRef = useRef<RenderedView | null>(null)
  const remoteViewsRef = useRef<Map<string, RenderedView>>(new Map())

  const validJoinUrl = useMemo(() => isSupportedJoinUrl(joinUrl), [joinUrl])
  const isPlaceholderJoinHost = useMemo(() => {
    if (!validJoinUrl) {
      return false
    }

    try {
      const parsed = new URL(joinUrl)
      return parsed.hostname.toLowerCase() === 'teleconsult.healthcare.local'
    } catch {
      return false
    }
  }, [joinUrl, validJoinUrl])

  const sessionId = useMemo(() => {
    try {
      const parsed = new URL(joinUrl)
      const segments = parsed.pathname.split('/').filter(Boolean)
      return segments[segments.length - 1] ?? ''
    } catch {
      return ''
    }
  }, [joinUrl])

  const canUseAcsCalling = useMemo(
    () => isUuid(sessionId) && role.toUpperCase() !== 'UNKNOWN',
    [role, sessionId],
  )

  useEffect(() => {
    if (isPlaceholderJoinHost) {
      setShowEmbedded(false)
    }
  }, [isPlaceholderJoinHost])

  useEffect(() => {
    let active = true

    async function bootstrapToken() {
      if (!session?.accessToken || !sessionId || (role !== 'DOCTOR' && role !== 'PATIENT')) {
        return
      }

      try {
        const response = await requestTeleconsultToken(
          {
            sessionId,
            role,
          },
          session.accessToken,
        )

        if (!active) {
          return
        }

        setTokenBootstrap(response)
        setTokenError(false)
        setTokenMessage(`Call bootstrap ready. Provider: ${response.tokenProvider}`)
      } catch (cause) {
        if (!active) {
          return
        }
        const message = cause instanceof Error
          ? cause.message
          : 'Unable to bootstrap teleconsult token. Direct join link will still be used.'
        setTokenError(true)
        setTokenMessage(message)
      }
    }

    void bootstrapToken()

    return () => {
      active = false
    }
  }, [role, session?.accessToken, sessionId])

  useEffect(() => {
    let active = true

    async function ensureLocalPreview() {
      if (!cameraEnabled || !localVideoContainerRef.current || !localVideoStreamRef.current) {
        return
      }

      if (localViewRef.current) {
        return
      }

      const renderer = new VideoStreamRenderer(localVideoStreamRef.current)
      const view = await renderer.createView()
      if (!active) {
        view.dispose?.()
        renderer.dispose()
        return
      }

      localVideoContainerRef.current.innerHTML = ''
      localVideoContainerRef.current.appendChild(view.target)
      localViewRef.current = { renderer, view }
    }

    async function startCallClient() {
      if (!tokenBootstrap?.accessToken || !canUseAcsCalling) {
        return
      }

      try {
        setCallState('connecting')
        setTokenError(false)

        const credential = new AzureCommunicationTokenCredential(tokenBootstrap.accessToken)
        const callClient = new CallClient()
        const deviceManager = await callClient.getDeviceManager()
        await deviceManager.askDevicePermission({ audio: true, video: true })

        const cameras = await deviceManager.getCameras()
        if (cameras.length > 0) {
          localVideoStreamRef.current = new LocalVideoStream(cameras[0])
          await ensureLocalPreview()
        }

        const callAgent = await callClient.createCallAgent(credential, {
          displayName: session?.displayName || role,
        })

        const joinOptions = localVideoStreamRef.current && cameraEnabled
          ? { videoOptions: { localVideoStreams: [localVideoStreamRef.current] } }
          : undefined

        const call = callAgent.join({ groupId: sessionId }, joinOptions)

        callClientRef.current = callClient
        deviceManagerRef.current = deviceManager
        callAgentRef.current = callAgent
        callRef.current = call
        setCallReady(true)

        call.on('stateChanged', () => {
          if (!active) {
            return
          }
          const state = call.state === 'Connected'
            ? 'connected'
            : call.state === 'Disconnected'
              ? 'disconnected'
              : 'connecting'
          setCallState(state)
        })

        const attachRemoteVideo = async (participant: RemoteParticipant, stream: RemoteVideoStream) => {
          if (!remoteVideosContainerRef.current) {
            return
          }
          if (!stream.isAvailable) {
            return
          }

          const key = `${participant.identifier.kind}-${stream.id}`
          if (remoteViewsRef.current.has(key)) {
            return
          }

          const renderer = new VideoStreamRenderer(stream)
          const view = await renderer.createView()
          if (!active) {
            view.dispose?.()
            renderer.dispose()
            return
          }

          const tile = document.createElement('div')
          tile.dataset.remoteVideo = key
          tile.style.border = '1px solid #d6dce5'
          tile.style.borderRadius = '12px'
          tile.style.overflow = 'hidden'
          tile.style.minHeight = '180px'
          tile.appendChild(view.target)
          remoteVideosContainerRef.current.appendChild(tile)
          remoteViewsRef.current.set(key, { renderer, view })
        }

        const detachRemoteVideo = (participant: RemoteParticipant, stream: RemoteVideoStream) => {
          const key = `${participant.identifier.kind}-${stream.id}`
          const rendered = remoteViewsRef.current.get(key)
          if (!rendered) {
            return
          }

          rendered.view.dispose?.()
          rendered.renderer.dispose()
          remoteViewsRef.current.delete(key)

          const tile = remoteVideosContainerRef.current?.querySelector(`[data-remote-video='${key}']`)
          if (tile && tile.parentElement) {
            tile.parentElement.removeChild(tile)
          }
        }

        const subscribeParticipant = (participant: RemoteParticipant) => {
          participant.videoStreams.forEach((stream) => {
            void attachRemoteVideo(participant, stream)
            stream.on('isAvailableChanged', () => {
              if (stream.isAvailable) {
                void attachRemoteVideo(participant, stream)
              } else {
                detachRemoteVideo(participant, stream)
              }
            })
          })

          participant.on('videoStreamsUpdated', (event) => {
            event.added.forEach((stream) => {
              void attachRemoteVideo(participant, stream)
              stream.on('isAvailableChanged', () => {
                if (stream.isAvailable) {
                  void attachRemoteVideo(participant, stream)
                } else {
                  detachRemoteVideo(participant, stream)
                }
              })
            })
            event.removed.forEach((stream) => {
              detachRemoteVideo(participant, stream)
            })
          })
        }

        call.remoteParticipants.forEach((participant) => {
          subscribeParticipant(participant)
        })

        call.on('remoteParticipantsUpdated', (event) => {
          event.added.forEach((participant) => subscribeParticipant(participant))
          event.removed.forEach((participant) => {
            participant.videoStreams.forEach((stream) => detachRemoteVideo(participant, stream))
          })
        })

        setTokenMessage('ACS call client connected.')
      } catch (cause) {
        if (!active) {
          return
        }
        const message = cause instanceof Error
          ? cause.message
          : 'Unable to start ACS call client. Use direct join link.'
        setTokenError(true)
        setTokenMessage(message)
        setCallState('error')
      }
    }

    void startCallClient()

    return () => {
      active = false

      const stopCall = async () => {
        try {
          if (callRef.current) {
            await callRef.current.hangUp()
          }
        } catch {
          // ignore shutdown failures
        }

        remoteViewsRef.current.forEach((rendered) => {
          rendered.view.dispose?.()
          rendered.renderer.dispose()
        })
        remoteViewsRef.current.clear()

        if (localViewRef.current) {
          localViewRef.current.view.dispose?.()
          localViewRef.current.renderer.dispose()
          localViewRef.current = null
        }

        callRef.current = null
        callAgentRef.current?.dispose()
        callAgentRef.current = null
        callClientRef.current = null
        localVideoStreamRef.current = null
      }

      void stopCall()
    }
  }, [cameraEnabled, canUseAcsCalling, role, session?.displayName, sessionId, tokenBootstrap?.accessToken])

  useEffect(() => {
    async function syncMicState() {
      const call = callRef.current
      if (!call) {
        return
      }

      if (micEnabled && call.isMuted) {
        await call.unmute()
      }

      if (!micEnabled && !call.isMuted) {
        await call.mute()
      }
    }

    void syncMicState()
  }, [micEnabled, callReady])

  useEffect(() => {
    async function syncCameraState() {
      const call = callRef.current
      const localStream = localVideoStreamRef.current
      if (!call || !localStream) {
        return
      }

      try {
        if (cameraEnabled) {
          await call.startVideo(localStream)
        } else {
          await call.stopVideo(localStream)
        }
      } catch {
        // leave current state if device start/stop fails
      }
    }

    void syncCameraState()
  }, [cameraEnabled, callReady])

  function openInNewTab() {
    if (!validJoinUrl) {
      return
    }

    if (isPlaceholderJoinHost) {
      const callClientUrl = buildTeleconsultCallClientUrl(joinUrl, role)
      window.open(callClientUrl, '_blank', 'noopener,noreferrer')
      return
    }

    window.open(joinUrl, '_blank', 'noopener,noreferrer')
  }

  if (!validJoinUrl) {
    return (
      <main className="app-shell" style={{ padding: '2rem', maxWidth: 960, margin: '0 auto' }}>
        <h1>Teleconsultation call</h1>
        <p className="form-status form-status--error" role="alert">
          Teleconsultation join URL is missing or invalid. Restart the session from doctor dashboard.
        </p>
      </main>
    )
  }

  return (
    <main className="app-shell" style={{ padding: '1rem', maxWidth: 1200, margin: '0 auto' }}>
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
        <div>
          <h1 style={{ marginBottom: '0.25rem' }}>Teleconsultation</h1>
          <p style={{ marginTop: 0 }}>Role: {role || 'UNKNOWN'}</p>
        </div>
        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          <button type="button" className="secondary-button" onClick={() => setMicEnabled((value) => !value)}>
            {micEnabled ? 'Mute mic' : 'Unmute mic'}
          </button>
          <button type="button" className="secondary-button" onClick={() => setCameraEnabled((value) => !value)}>
            {cameraEnabled ? 'Turn camera off' : 'Turn camera on'}
          </button>
          <button type="button" className="secondary-button" onClick={() => setShowEmbedded((value) => !value)}>
            {showEmbedded ? 'Hide embedded view' : 'Show embedded view'}
          </button>
          <button type="button" className="primary-button" onClick={openInNewTab}>
            Open direct ACS call
          </button>
        </div>
      </header>

      {tokenMessage ? (
        <p className={tokenError ? 'form-status form-status--error' : 'form-status form-status--success'} role={tokenError ? 'alert' : 'status'}>
          {tokenMessage}
        </p>
      ) : null}

      {tokenBootstrap?.expiresAt ? (
        <p>
          Token expiry: {new Date(tokenBootstrap.expiresAt).toLocaleString()}
        </p>
      ) : null}

      <section style={{ marginTop: '1rem', display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1rem' }}>
        <article style={{ border: '1px solid #d6dce5', borderRadius: 12, padding: '0.75rem' }}>
          <h2 style={{ fontSize: '1rem', marginTop: 0 }}>Local video</h2>
          <p style={{ marginTop: 0 }}>Call state: {callState}</p>
          <p style={{ marginTop: 0 }}>Client ready: {callReady ? 'Yes' : 'No'}</p>
          <div ref={localVideoContainerRef} style={{ minHeight: 180, background: '#f7f9fc', borderRadius: 8 }} />
        </article>

        <article style={{ border: '1px solid #d6dce5', borderRadius: 12, padding: '0.75rem' }}>
          <h2 style={{ fontSize: '1rem', marginTop: 0 }}>Remote participants</h2>
          <div
            ref={remoteVideosContainerRef}
            style={{
              minHeight: 180,
              background: '#f7f9fc',
              borderRadius: 8,
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
              gap: '0.5rem',
              padding: '0.5rem',
            }}
          />
        </article>
      </section>

      {showEmbedded && !isPlaceholderJoinHost ? (
        <section style={{ marginTop: '1rem' }}>
          <iframe
            src={joinUrl}
            title="Teleconsultation call"
            style={{ width: '100%', height: '78vh', border: '1px solid #d6dce5', borderRadius: 12, background: '#fff' }}
            allow="camera; microphone; fullscreen; display-capture"
            referrerPolicy="strict-origin-when-cross-origin"
          />
        </section>
      ) : (
        <p style={{ marginTop: '1rem' }}>
          {isPlaceholderJoinHost
            ? 'Embedded provider URL is not publicly reachable in this environment. Continue with the built-in ACS client controls on this page.'
            : 'Embedded view hidden. Use Open direct ACS call to continue in a new tab.'}
        </p>
      )}
    </main>
  )
}
