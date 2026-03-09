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
    queued.forEach(({ topic, callback, resolve }) => {
      const sub = this.client.subscribe(topic, (msg) => {
        try { callback(JSON.parse(msg.body)); } catch { /* ignore */ }
      });
      resolve(sub);
    });
  }

  subscribe(topic: string, callback: SubscribeCallback): Promise<StompSubscription> {
    return new Promise((resolve) => {
      if (this.client.connected) {
        const sub = this.client.subscribe(topic, (msg) => {
          try { callback(JSON.parse(msg.body)); } catch { /* ignore */ }
        });
        resolve(sub);
      } else {
        this.pending.push({ topic, callback, resolve });
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
