'use client';

import { useState, useEffect, useCallback, useRef } from 'react';

interface SSEOptions {
  url: string;
  onMessage?: (data: any) => void;
  onError?: (error: Event) => void;
  enabled?: boolean;
  retryInterval?: number;
  fallbackPollingUrl?: string;
  fallbackPollingInterval?: number;
  fallbackPollingFn?: () => Promise<any>;
}

/**
 * useSSE — Server-Sent Events hook with automatic fallback to polling.
 * Attempts SSE connection first; if SSE fails, falls back to periodic polling.
 */
export function useSSE({
  url,
  onMessage,
  onError,
  enabled = true,
  retryInterval = 5000,
  fallbackPollingInterval = 10000,
  fallbackPollingFn,
}: SSEOptions) {
  const [connected, setConnected] = useState(false);
  const [usingPolling, setUsingPolling] = useState(false);
  const eventSourceRef = useRef<EventSource | null>(null);
  const retryCountRef = useRef(0);
  const maxRetries = 3;

  const connect = useCallback(() => {
    if (!enabled || typeof window === 'undefined') return;

    try {
      const es = new EventSource(url);
      eventSourceRef.current = es;

      es.onopen = () => {
        setConnected(true);
        setUsingPolling(false);
        retryCountRef.current = 0;
      };

      es.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          onMessage?.(data);
        } catch {
          onMessage?.(event.data);
        }
      };

      es.onerror = (event) => {
        setConnected(false);
        es.close();
        onError?.(event);

        retryCountRef.current += 1;
        if (retryCountRef.current < maxRetries) {
          setTimeout(connect, retryInterval);
        } else {
          // Fallback to polling
          setUsingPolling(true);
        }
      };
    } catch {
      // SSE not supported — go straight to polling
      setUsingPolling(true);
    }
  }, [url, enabled, onMessage, onError, retryInterval]);

  // SSE connection
  useEffect(() => {
    if (!enabled) return;
    connect();
    return () => {
      eventSourceRef.current?.close();
    };
  }, [connect, enabled]);

  // Fallback polling
  useEffect(() => {
    if (!usingPolling || !enabled || !fallbackPollingFn) return;
    const poll = () => {
      fallbackPollingFn().then(data => {
        onMessage?.(data);
      }).catch(() => {});
    };
    poll();
    const interval = setInterval(poll, fallbackPollingInterval);
    return () => clearInterval(interval);
  }, [usingPolling, enabled, fallbackPollingFn, fallbackPollingInterval, onMessage]);

  const disconnect = useCallback(() => {
    eventSourceRef.current?.close();
    setConnected(false);
  }, []);

  return { connected, usingPolling, disconnect };
}
