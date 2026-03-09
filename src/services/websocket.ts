import { Client, type StompSubscription } from "@stomp/stompjs";

const WS_URL = (() => {
  if (typeof window === "undefined") return "";
  const base = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
  return base.replace(/^http/, "ws") + "/ws";
})();

type SubscribeCallback = (body: unknown) => void;

interface PendingEntry {
  topic: string;
  callback: SubscribeCallback;
  resolve: (sub: StompSubscription) => void;
  reject: (reason?: unknown) => void;
  cleanup?: () => void;
}

class WsManager {
  private client: Client;
  private pending: PendingEntry[] = [];

  constructor() {
    this.client = new Client({
      brokerURL: WS_URL,
      reconnectDelay: 5000,
      onConnect: () => this.flush(),
      onDisconnect: () => {},
    });
    this.client.activate();
  }

  private flush() {
    const queued = [...this.pending];
    this.pending = [];
    queued.forEach(({ topic, callback, resolve, cleanup }) => {
      cleanup?.();
      const sub = this.client.subscribe(topic, (msg) => {
        try { callback(JSON.parse(msg.body)); } catch { /* ignore */ }
      });
      resolve(sub);
    });
  }

  subscribe(topic: string, callback: SubscribeCallback, signal?: AbortSignal): Promise<StompSubscription> {
    return new Promise((resolve, reject) => {
      if (signal?.aborted) {
        reject(new DOMException("Subscription aborted", "AbortError"));
        return;
      }
      if (this.client.connected) {
        const sub = this.client.subscribe(topic, (msg) => {
          try { callback(JSON.parse(msg.body)); } catch { /* ignore */ }
        });
        resolve(sub);
      } else {
        const entry: PendingEntry = { topic, callback, resolve, reject };
        if (signal) {
          const onAbort = () => {
            this.pending = this.pending.filter((pending) => pending !== entry);
            reject(new DOMException("Subscription aborted", "AbortError"));
          };
          signal.addEventListener("abort", onAbort, { once: true });
          entry.cleanup = () => signal.removeEventListener("abort", onAbort);
        }
        this.pending.push(entry);
      }
    });
  }
}

let instance: WsManager | null = null;

export function getWsManager(): WsManager | null {
  if (typeof window === "undefined") return null;
  if (!instance) instance = new WsManager();
  return instance;
}
