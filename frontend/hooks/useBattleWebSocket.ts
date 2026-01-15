import { useCallback, useEffect, useRef, useState } from 'react';

export interface BattleStateMessage {
  t: 'STATE';
  matchId: number;
  endsAtEpochMs: number;
  lanes: Record<string, number>;
  scores: {
    teamA: number;
    teamB: number;
  };
  inputs: {
    teamA: number;
    teamB: number;
  };
}

export interface BattleFinishedMessage {
  t: 'FINISHED';
  matchId: number;
  winner: 'A' | 'B' | 'DRAW';
  reason: 'TIMEUP' | 'FORFEIT' | 'EARLY_WIN' | 'CANCELED';
  lane0: number;
  lane1: number;
  lane2: number;
  inputsA: number;
  inputsB: number;
  extra?: {
    sumA: number;
    sumB: number;
    forfeitUserId?: number;
  };
}

export type BattleMessage = BattleStateMessage | BattleFinishedMessage | { t: 'PING' } | { status: number; message: string };

export const useBattleWebSocket = (matchId: number | null) => {
  const [lastState, setLastState] = useState<BattleStateMessage | null>(null);
  const [finishedData, setFinishedData] = useState<BattleFinishedMessage | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);

  const connect = useCallback(() => {
    if (!matchId) return;

    const baseUrl = process.env.NEXT_PUBLIC_WS_BASE_URL || 'ws://localhost:8080';
    const socket = new WebSocket(`${baseUrl}/ws/battles/${matchId}`);
    wsRef.current = socket;

    socket.onopen = () => {
      console.log(`[Battle WS] Connected to match ${matchId}`);
      setIsConnected(true);
    };

    socket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        const type = data.t;
        
        if (type === 'STATE') {
          setLastState(data as BattleStateMessage);
        } else if (type === 'FINISHED') {
          setFinishedData(data as BattleFinishedMessage);
          socket.close(); // Close on finish
        } else if (data.status && data.status >= 400) {
          console.error('[Battle WS] Server Error:', data.message);
        }
      } catch (err) {
        console.error('[Battle WS] Parse Error:', err);
      }
    };

    socket.onclose = () => {
      console.log(`[Battle WS] Disconnected from match ${matchId}`);
      setIsConnected(false);
    };

    socket.onerror = (err) => {
      console.error('[Battle WS] Error:', err);
    };
  }, [matchId]);

  const disconnect = useCallback(() => {
    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }
  }, []);

  const sendInput = useCallback((lane: number) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      const msg = {
        t: 'INPUT',
        lane,
        power: 1
      };
      wsRef.current.send(JSON.stringify(msg));
    }
  }, []);

  useEffect(() => {
    if (matchId) {
      connect();
    }
    return () => {
      disconnect();
    };
  }, [matchId, connect, disconnect]);

  const clearFinished = useCallback(() => {
    setFinishedData(null);
    setLastState(null);
  }, []);

  return {
    lastState,
    finishedData,
    isConnected,
    disconnect,
    sendInput,
    clearFinished
  };
};
