// hooks/useLobby.js
import { useCallback, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { roomApi } from '../api/roomApi';

const WS_URL = import.meta.env.VITE_WS_URL ?? 'http://localhost:8080/ws';

/**
 * useLobby — manages the live lobby state for a room.
 *
 * 1. Loads initial room state via REST on mount.
 * 2. Opens a STOMP/SockJS WebSocket connection.
 * 3. Subscribes to /topic/room.{roomCode} for real-time player events.
 * 4. Sends /app/player.register to link the WS session to the player record.
 *
 * @param {string} roomCode  - the 6-char room code
 * @param {string} playerId  - the client-generated UUID from localStorage
 * @returns {{ players, settings, hostPlayerId, isHost, isConnected, isGameStarting, error }}
 */
export function useLobby(roomCode, playerId) {
    const [players, setPlayers] = useState([]);
    const [settings, setSettings] = useState(null);
    const [hostPlayerId, setHostPlayerId] = useState(null);
    const [isConnected, setIsConnected] = useState(false);
    const [isGameStarting, setIsGameStarting] = useState(false);
    const [error, setError] = useState(null);

    const stompClientRef = useRef(null);

    // --- Step 1: Load initial room state via REST ---
    useEffect(() => {
        if (!roomCode) return;
        roomApi.getRoom(roomCode)
            .then((room) => {
                setPlayers(room.players.filter((p) => p.status === 'CONNECTED'));
                setSettings(room.settings);
                setHostPlayerId(room.hostPlayerId);
            })
            .catch((err) => setError(err.message));
    }, [roomCode]);

    // --- Handle inbound WS events ---
    const handleLobbyEvent = useCallback((event) => {
        switch (event.event) {
            case 'PLAYER_JOINED':
                setPlayers(event.players);
                break;
            case 'PLAYER_LEFT':
                setPlayers(event.players);
                if (event.newHostPlayerId) setHostPlayerId(event.newHostPlayerId);
                break;
            case 'GAME_STARTING':
                setIsGameStarting(true);
                break;
            default:
                break;
        }
    }, []);

    // --- Step 2: Connect WebSocket ---
    useEffect(() => {
        if (!roomCode || !playerId) return;

        const client = new Client({
            webSocketFactory: () => new SockJS(WS_URL),
            reconnectDelay: 3000,

            onConnect: () => {
                setIsConnected(true);
                setError(null);

                // Subscribe to this room's topic
                client.subscribe(`/topic/room.${roomCode}`, (message) => {
                    try {
                        const event = JSON.parse(message.body);
                        handleLobbyEvent(event);
                    } catch (e) {
                        console.error('Failed to parse WS event', e);
                    }
                });

                // Register this WS session with the player record on the backend
                client.publish({
                    destination: '/app/player.register',
                    body: JSON.stringify({ playerId, roomCode }),
                });
            },

            onDisconnect: () => setIsConnected(false),

            onStompError: (frame) => {
                setError(`WebSocket error: ${frame.headers['message']}`);
            },
        });

        client.activate();
        stompClientRef.current = client;

        return () => {
            client.deactivate();
        };
    }, [roomCode, playerId, handleLobbyEvent]);

    return {
        players,
        settings,
        hostPlayerId,
        isHost: hostPlayerId === playerId,
        isConnected,
        isGameStarting,
        error,
    };
}