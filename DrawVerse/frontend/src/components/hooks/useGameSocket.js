// hooks/useGameSocket.js
// Manages the WebSocket connection for the active game screen.
// Handles: player list, chat/guesses, drawing events, round/game state.

import { useCallback, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { roomApi } from '../api/roomApi';

const WS_URL = import.meta.env.VITE_WS_URL ?? 'http://localhost:8080/ws';

export function useGameSocket({ roomCode, playerId, onRemotePath, onCanvasClear, onDrawUndo }) {
    const [players, setPlayers] = useState([]);
    const [messages, setMessages] = useState([]);  // chat + guesses
    const [gameState, setGameState] = useState({
        phase: 'WAITING',   // WAITING | WORD_SELECTION | DRAWING | ROUND_END | GAME_OVER
        round: 1,
        totalRounds: 3,
        drawerId: null,
        word: null,         // full word (only for drawer) or null
        hint: null,         // e.g. "_e__e_"
        timeLeft: 0,
        scores: {},
    });
    const [isConnected, setIsConnected] = useState(false);
    const stompClientRef = useRef(null);

    // ── Load initial room state ───────────────────────────────────────────────
    useEffect(() => {
        if (!roomCode) return;
        roomApi.getRoom(roomCode)
            .then((room) => {
                setPlayers(room.players.filter((p) => p.status === 'CONNECTED'));
                setGameState((g) => ({ ...g, totalRounds: room.settings.rounds }));
            })
            .catch(console.error);
    }, [roomCode]);

    // ── Handle inbound events ─────────────────────────────────────────────────
    const handleEvent = useCallback((event) => {
        switch (event.event) {
            // ── Lobby events ──
            case 'PLAYER_JOINED':
            case 'PLAYER_LEFT':
                setPlayers(event.players);
                break;

            // ── Game lifecycle ──
            case 'GAME_STARTED':
                setGameState((g) => ({
                    ...g,
                    phase: 'ROUND_START',
                    round: event.round,
                    totalRounds: event.totalRounds,
                    drawerId: event.drawerId,
                    timeLeft: event.drawTimeSeconds,
                }));
                break;

            case 'ROUND_START':
                setPlayers(event.players || []);
                setGameState((g) => ({
                    ...g,
                    phase: 'DRAWING',
                    round: event.round ?? g.round,
                    drawerId: event.drawerId,
                    timeLeft: event.drawTimeSeconds,
                    word: event.word ?? null,     // Full word for drawer, null for others
                    hint: event.hint ?? null,     // Blank hint for guessers
                    totalTime: event.drawTimeSeconds,
                }));
                // Clear previous round's messages
                setMessages([]);
                break;

            case 'TIMER_TICK':
                setGameState((g) => ({ ...g, timeLeft: event.timeLeft }));
                break;

            case 'ROUND_END':
                setGameState((g) => ({
                    ...g,
                    phase: 'ROUND_END',
                    word: event.word,
                }));
                // Update player list with new scores
                setPlayers(event.players || []);
                break;

            case 'GAME_OVER':
                setGameState((g) => ({
                    ...g,
                    phase: 'GAME_OVER',
                }));
                // Final scores
                setPlayers(event.finalScores || []);
                break;

            // ── Drawing events ──
            case 'DRAW_DATA':
                if (onRemotePath) onRemotePath(event.path);
                break;

            case 'CANVAS_CLEAR':
                if (onCanvasClear) onCanvasClear();
                break;

            case 'DRAW_UNDO':
                if (onDrawUndo) onDrawUndo();
                break;

            // ── Chat & guesses ──
            case 'CHAT_MESSAGE':
                setMessages((m) => [...m, {
                    id: Date.now(),
                    type: 'chat',
                    playerName: event.playerName,
                    text: event.text,
                }]);
                break;

            case 'GUESS_CORRECT':
                setMessages((m) => [...m, {
                    id: Date.now(),
                    type: 'correct',
                    playerName: event.playerName,
                    text: `guessed correctly! (+${event.pointsAwarded} pts)`,
                }]);
                // Update player scores
                setPlayers((prev) =>
                    prev.map((p) =>
                        p.playerId === event.playerId
                            ? { ...p, score: event.newScore, guessedCorrectly: true }
                            : p
                    )
                );
                break;

            case 'GUESS_INCORRECT':
                setMessages((m) => [...m, {
                    id: Date.now(),
                    type: 'guess',
                    playerName: event.playerName,
                    text: event.feedback || 'made a guess...',
                }]);
                break;

            case 'GUESS_INVALID':
                console.warn('Invalid guess:', event.reason);
                break;

            case 'GUESS_RESULT':
                if (event.correct) {
                    setMessages((m) => [...m, {
                        id: Date.now(),
                        type: 'correct',
                        playerName: event.playerName,
                        text: `guessed the word! (+${event.points} pts)`,
                    }]);
                    // Update that player's score in list
                    setPlayers((prev) =>
                        prev.map((p) =>
                            p.playerId === event.playerId ? { ...p, score: p.score + event.points } : p
                        )
                    );
                }
                break;

            default:
                break;
        }
    }, [onRemotePath, onCanvasClear, onDrawUndo]);

    // ── Connect WebSocket ─────────────────────────────────────────────────────
    useEffect(() => {
        if (!roomCode || !playerId) return;

        const client = new Client({
            webSocketFactory: () => new SockJS(WS_URL),
            reconnectDelay: 3000,
            onConnect: () => {
                setIsConnected(true);

                // Subscribe to main room topic
                client.subscribe(`/topic/room.${roomCode}`, (message) => {
                    try { handleEvent(JSON.parse(message.body)); }
                    catch (e) { console.error('WS parse error', e); }
                });

                // Subscribe to guess results
                client.subscribe(`/topic/room.${roomCode}.guess_result`, (message) => {
                    try { handleEvent(JSON.parse(message.body)); }
                    catch (e) { console.error('WS guess result parse error', e); }
                });

                // Subscribe to errors
                client.subscribe(`/topic/room.${roomCode}.error`, (message) => {
                    try { console.error('Room error:', JSON.parse(message.body)); }
                    catch (e) { console.error('WS error parse error', e); }
                });

                // Register session
                client.publish({
                    destination: '/app/player.register',
                    body: JSON.stringify({ playerId, roomCode }),
                });
            },
            onDisconnect: () => setIsConnected(false),
        });

        client.activate();
        stompClientRef.current = client;

        return () => { client.deactivate(); };
    }, [roomCode, playerId, handleEvent]);

    // ── Send chat / guess ─────────────────────────────────────────────────────
    const sendGuess = useCallback((text) => {
        if (!text || !text.trim()) return;
        if (!stompClientRef.current?.connected) {
            console.warn('[Guess] STOMP not connected');
            return;
        }

        // Send to backend guess handler
        stompClientRef.current.publish({
            destination: `/app/room.${roomCode}.guess`,
            body: JSON.stringify({
                playerId,
                roomCode,
                guess: text.trim()
            }),
        });
    }, [roomCode, playerId]);

    return {
        players,
        messages,
        gameState,
        isConnected,
        stompClient: stompClientRef.current,
        sendGuess,
    };
}