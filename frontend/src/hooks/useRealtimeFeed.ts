import { useEffect, useState } from 'react'

export function useRealtimeFeed(seed: string[]) {
  const [items, setItems] = useState(seed)

  useEffect(() => {
    const interval = window.setInterval(() => {
      setItems((current) => [
        `Realtime update @ ${new Date().toLocaleTimeString()}`,
        ...current,
      ].slice(0, 5))
    }, 5000)
    return () => window.clearInterval(interval)
  }, [])

  return items
}
