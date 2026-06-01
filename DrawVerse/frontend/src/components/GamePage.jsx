import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Undo2, Trash2, Settings, ThumbsUp, ThumbsDown,
    Send, Eraser, Minus, Plus, Link2, LogOut, Copy, Check,
} from 'lucide-react';
import { useDrawingCanvas } from './hooks/useDrawingCanvas';
import { useGameSocket } from './hooks/useGameSocket';
import { getOrCreatePlayerId } from './utils/PlayerIdetity';
import { roomApi } from './api/roomApi';

const COLORS = [
    '#000000', '#ffffff', '#c1c1c1', '#ef130b', '#ff7100',
    '#ffe400', '#00cc00', '#00b2ff', '#231fd3', '#a300ba',
    '#d37caa', '#a0522d', '#ff7eb0', '#ffd5d5', '#ffecc9',
    '#ffffc9', '#d5f5c5', '#cce5ff', '#e5d5ff', '#f5c5f5',
];

// ─── PlayerCard ───────────────────────────────────────────────────────────────
const PlayerCard = ({ player, rank, isDrawing }) => (
    <div className={`flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all ${isDrawing
        ? 'bg-blue-500/15 border border-blue-500/30'
        : 'bg-white/[0.03] border border-white/5'
        }`}>
        <span className="text-xs font-black text-gray-600 w-5 text-center">#{rank}</span>
        <div className="w-8 h-8 rounded-full bg-gradient-to-br from-slate-700 to-slate-800 border border-white/10 flex items-center justify-center text-sm">
            {player.avatar || '😎'}
        </div>
        <div className="flex-1 min-w-0">
            <p className={`text-xs font-bold truncate ${isDrawing ? 'text-blue-300' : 'text-gray-200'}`}>
                {player.playerName}
                {isDrawing && <span className="ml-1 text-[10px] text-blue-400 font-normal">✏️</span>}
                {player.isHost && <span className="ml-1">👑</span>}
            </p>
            <p className="text-[10px] text-gray-600">{player.score.toLocaleString()} pts</p>
        </div>
        {player.guessedCorrectly && <span className="text-green-400 text-xs">✓</span>}
    </div>
);

// ─── ChatMessage ──────────────────────────────────────────────────────────────
const ChatMessage = ({ msg }) => {
    if (msg.type === 'correct') {
        return (
            <div className="px-3 py-1.5 bg-green-500/10 border border-green-500/20 rounded-xl">
                <span className="text-green-400 text-xs font-bold">{msg.playerName} </span>
                <span className="text-green-300 text-xs">{msg.text}</span>
            </div>
        );
    }
    return (
        <div className="px-1 py-0.5">
            <span className={`text-xs font-bold ${msg.type === 'own' ? 'text-blue-400' : 'text-blue-300'}`}>
                {msg.playerName}:{' '}
            </span>
            <span className="text-xs text-gray-400">{msg.text}</span>
        </div>
    );
};

// ─── DrawingToolbar ───────────────────────────────────────────────────────────
const DrawingToolbar = ({ color, brushSize, isEraser, setColor, setBrushSize, toggleEraser, undo, clearCanvas, isDrawer }) => {
    if (!isDrawer) return null;
    return (
        <div className="flex flex-col gap-3 p-3 bg-slate-900/80 backdrop-blur border-r border-white/5 w-16 items-center overflow-y-auto">
            <div className="grid grid-cols-2 gap-1">
                {COLORS.map((c) => (
                    <button
                        key={c}
                        onClick={() => setColor(c)}
                        style={{ backgroundColor: c }}
                        className={`w-5 h-5 rounded-sm border-2 transition-transform hover:scale-110 active:scale-95 ${color === c && !isEraser ? 'border-blue-400 scale-110' : 'border-white/20'
                            }`}
                    />
                ))}
            </div>
            <div className="w-full h-px bg-white/10" />
            <div className="flex flex-col items-center gap-1.5">
                <button onClick={() => setBrushSize(Math.max(1, brushSize - 2))} className="p-1 rounded-lg hover:bg-white/10 text-gray-400">
                    <Minus size={12} />
                </button>
                <div className="w-8 h-8 rounded-full bg-slate-950 border border-white/10 flex items-center justify-center">
                    <div className="rounded-full bg-gray-800" style={{ width: Math.min(brushSize, 22), height: Math.min(brushSize, 22), backgroundColor: color }} />
                </div>
                <button onClick={() => setBrushSize(Math.min(50, brushSize + 2))} className="p-1 rounded-lg hover:bg-white/10 text-gray-400">
                    <Plus size={12} />
                </button>
            </div>
            <div className="w-full h-px bg-white/10" />
            <button
                onClick={toggleEraser}
                title="Eraser"
                className={`p-2 rounded-xl transition-all ${isEraser ? 'bg-blue-600 text-white' : 'bg-white/5 text-gray-400 hover:bg-white/10'}`}
            >
                <Eraser size={16} />
            </button>
            <button onClick={undo} title="Undo" className="p-2 rounded-xl bg-white/5 text-gray-400 hover:bg-white/10 transition-all">
                <Undo2 size={16} />
            </button>
            <button onClick={() => clearCanvas(false)} title="Clear" className="p-2 rounded-xl bg-red-500/10 text-red-400 hover:bg-red-500/20 transition-all">
                <Trash2 size={16} />
            </button>
        </div>
    );
};

// ─── InviteButton ─────────────────────────────────────────────────────────
const InviteButton = ({ roomCode }) => {
    const [copied, setCopied] = useState(false);

    const handleCopyLink = async () => {
        const inviteLink = `http://localhost:5173/invite/${roomCode}`;
        try {
            await navigator.clipboard.writeText(inviteLink);
            setCopied(true);
            setTimeout(() => setCopied(false), 2000);
        } catch (err) {
            console.error('Failed to copy:', err);
        }
    };

    return (
        <button
            onClick={handleCopyLink}
            title="Copy invite link"
            className="p-2 rounded-xl bg-white/5 hover:bg-white/10 text-gray-400 hover:text-green-400 transition-all flex items-center gap-2"
        >
            {copied ? (
                <>
                    <Check size={16} />
                    <span className="text-xs">Copied!</span>
                </>
            ) : (
                <>
                    <Link2 size={16} />
                    <span className="text-xs">Invite</span>
                </>
            )}
        </button>
    );
};

// ─── LeaveButton ──────────────────────────────────────────────────────────
const LeaveButton = ({ roomCode, playerId, onLeave }) => {
    const [loading, setLoading] = useState(false);

    const handleLeave = async () => {
        if (!window.confirm('Are you sure you want to leave the game?')) return;

        setLoading(true);
        try {
            await roomApi.leaveRoom(roomCode, playerId);
            onLeave?.();
        } catch (error) {
            console.error('Failed to leave:', error);
            alert('Failed to leave: ' + error.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <button
            onClick={handleLeave}
            disabled={loading}
            title="Leave game"
            className="p-2 rounded-xl bg-red-500/10 hover:bg-red-500/20 text-red-400 transition-all flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
            {loading ? (
                <>
                    <div className="w-4 h-4 border-2 border-red-400/30 border-t-red-400 rounded-full animate-spin" />
                </>
            ) : (
                <>
                    <LogOut size={16} />
                    <span className="text-xs">Leave</span>
                </>
            )}
        </button>
    );
};

// ─── StartGameButton ──────────────────────────────────────────────────────
const StartGameButton = ({ isHost, playerCount, minPlayers, roomCode, playerId, isLoading, onStartClick }) => {
    if (!isHost) return null;

    const isDisabled = playerCount < minPlayers || isLoading;

    return (
        <button
            onClick={onStartClick}
            disabled={isDisabled}
            className={`
                px-6 py-3 rounded-xl font-bold text-sm transition-all transform
                ${isDisabled
                    ? 'bg-gray-600 text-gray-400 cursor-not-allowed opacity-50'
                    : 'bg-blue-600 hover:bg-blue-500 text-white hover:scale-105 active:scale-95'
                }
            `}
        >
            {isLoading ? 'Starting...' : 'Start Game!'}
        </button>
    );
};

// ─── GamePage ─────────────────────────────────────────────────────────────────
const GamePage = ({ roomCode: propRoomCode }) => {
    const navigate = useNavigate();
    const roomCode = (propRoomCode || window.location.pathname.split('/').pop()).toUpperCase();
    const playerId = getOrCreatePlayerId();

    const chatEndRef = useRef(null);
    const [guessInput, setGuessInput] = useState('');
    const [isStartingGame, setIsStartingGame] = useState(false);

    const handleLeaveGame = () => {
        navigate('/');
    };

    // ── 1. Game socket — must come FIRST so we have stompClient ──────────────
    // We pass placeholder callbacks; the real ones come from useDrawingCanvas.
    // We use a ref to break the circular dependency.
    const renderRemotePathRef = useRef(null);
    const clearCanvasRef = useRef(null);
    const undoRef = useRef(null);

    const { players, messages, gameState, stompClient, sendGuess } = useGameSocket({
        roomCode,
        playerId,
        onRemotePath: (path) => renderRemotePathRef.current?.(path),
        onCanvasClear: () => clearCanvasRef.current?.(true),   // true = from server, don't re-broadcast
        onDrawUndo: () => undoRef.current?.(),
    });

    const isDrawer = gameState.drawerId === playerId;
    // DEBUG: Force drawer mode for local testing
    const isDrawerMode = isDrawer || process.env.NODE_ENV !== 'production';
    const sortedPlayers = [...players].sort((a, b) => b.score - a.score);

    // Find current player to check if host
    const currentPlayer = players.find(p => p.playerId === playerId);
    const isHost = currentPlayer?.isHost || false;

    // Handle start game click
    const handleStartGame = async () => {
        setIsStartingGame(true);
        try {
            const response = await roomApi.startGame(roomCode, playerId);
            console.log('Game started:', response);
        } catch (error) {
            console.error('Failed to start game:', error);
            alert('Failed to start game: ' + error.message);
        } finally {
            setIsStartingGame(false);
        }
    };

    // ── 2. Drawing canvas — receives LIVE stompClient + isDrawer ─────────────
    //    Both are passed reactively; useDrawingCanvas tracks them via refs
    //    internally so path:created always broadcasts with the correct values.
    const {
        canvasRef, initCanvas, setColor, setBrushSize,
        undo, clearCanvas, toggleEraser, renderRemotePath,
        isEraser, color, brushSize,
    } = useDrawingCanvas({ stompClient, roomCode, isDrawer: isDrawerMode });

    // Wire the refs so useGameSocket callbacks reach the canvas functions
    useEffect(() => { renderRemotePathRef.current = renderRemotePath; }, [renderRemotePath]);
    useEffect(() => { clearCanvasRef.current = clearCanvas; }, [clearCanvas]);
    useEffect(() => { undoRef.current = undo; }, [undo]);

    // ── Init canvas once on mount ─────────────────────────────────────────────
    useEffect(() => { initCanvas(); }, [initCanvas]);

    // ── Auto-scroll chat ──────────────────────────────────────────────────────
    useEffect(() => {
        chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    const handleSendGuess = () => {
        if (!guessInput.trim()) return;
        sendGuess(guessInput);
        setGuessInput('');
    };

    // ── Word display ──────────────────────────────────────────────────────────
    const wordDisplay = isDrawer
        ? (gameState.word || '...')
        : gameState.hint
            ? gameState.hint.split('').map((c, i) => (
                <span key={i} className={c !== '_' ? 'text-white' : 'text-gray-500'}>
                    {c}
                </span>
            ))
            : '...';

    return (
        <div className="h-screen bg-slate-950 text-white font-sans flex flex-col overflow-hidden">

            {/* ── Top bar ──────────────────────────────────────────────────────── */}
            <header className="flex items-center justify-between px-4 py-2.5 bg-slate-900/80 backdrop-blur border-b border-white/5 shrink-0">
                {/* Timer */}
                <div className="flex items-center gap-2 min-w-[80px]">
                    <div className="relative w-10 h-10 flex items-center justify-center">
                        <svg className="absolute inset-0 -rotate-90" viewBox="0 0 36 36">
                            <circle cx="18" cy="18" r="15" fill="none" stroke="#1e293b" strokeWidth="3" />
                            <circle
                                cx="18" cy="18" r="15" fill="none"
                                stroke={gameState.timeLeft > 10 ? '#3b82f6' : '#ef4444'}
                                strokeWidth="3"
                                strokeDasharray={`${(gameState.timeLeft / (gameState.totalTime || 80)) * 94} 94`}
                                className="transition-all duration-1000"
                            />
                        </svg>
                        <span className="text-xs font-black z-10">{gameState.timeLeft}</span>
                    </div>
                    <div>
                        <p className="text-[10px] text-gray-600 uppercase tracking-wider">Round</p>
                        <p className="text-xs font-black">{gameState.round} of {gameState.totalRounds}</p>
                    </div>
                </div>

                {/* Word / hint */}
                <div className="text-center">
                    <p className="text-[10px] text-gray-600 uppercase tracking-widest mb-0.5">
                        {isDrawer ? 'Your word' : 'Guess this'}
                    </p>
                    <p className="text-xl font-black tracking-[0.25em] text-blue-200 flex gap-1 justify-center">
                        {wordDisplay}
                    </p>
                </div>

                {/* Right slot */}
                <div className="min-w-[80px] flex justify-end gap-2 items-center">
                    {/* Connection indicator */}
                    <div className={`w-2 h-2 rounded-full ${stompClient?.connected ? 'bg-green-400' : 'bg-red-400'}`} title={stompClient?.connected ? 'Connected' : 'Disconnected'} />

                    {/* Invite Button */}
                    <InviteButton roomCode={roomCode} />

                    {/* Leave Button */}
                    <LeaveButton roomCode={roomCode} playerId={playerId} onLeave={handleLeaveGame} />

                    <button className="p-2 rounded-xl bg-white/5 hover:bg-white/10 text-gray-500 transition-all">
                        <Settings size={18} />
                    </button>
                </div>
            </header>

            {/* ── Main area ─────────────────────────────────────────────────────── */}
            <div className="flex flex-1 overflow-hidden">

                {/* Left: Players */}
                <aside className="w-52 shrink-0 flex flex-col bg-slate-900/40 border-r border-white/5 overflow-y-auto">
                    <div className="px-3 pt-3 pb-1">
                        <p className="text-[10px] text-gray-600 uppercase tracking-widest font-bold mb-2">Players</p>
                    </div>
                    <div className="px-2 pb-3 flex flex-col gap-1.5">
                        {sortedPlayers.map((p, i) => (
                            <PlayerCard
                                key={p.playerId}
                                player={p}
                                rank={i + 1}
                                isDrawing={p.playerId === gameState.drawerId}
                            />
                        ))}
                        {players.length === 0 && (
                            <p className="text-xs text-gray-700 text-center py-4">Waiting for players...</p>
                        )}
                    </div>
                </aside>

                {/* Center: Toolbar + Canvas */}
                <div className="flex flex-1 overflow-hidden">
                    <DrawingToolbar
                        color={color}
                        brushSize={brushSize}
                        isEraser={isEraser}
                        setColor={setColor}
                        setBrushSize={setBrushSize}
                        toggleEraser={toggleEraser}
                        undo={undo}
                        clearCanvas={clearCanvas}
                        isDrawer={isDrawerMode}
                    />

                    <div className="flex-1 flex flex-col overflow-hidden">
                        {/* Like/dislike strip */}
                        <div className="flex justify-end gap-2 px-3 py-1.5 bg-slate-900/30 shrink-0">
                            <button className="p-1.5 rounded-lg bg-green-500/10 hover:bg-green-500/20 text-green-400 transition-all">
                                <ThumbsUp size={13} />
                            </button>
                            <button className="p-1.5 rounded-lg bg-red-500/10 hover:bg-red-500/20 text-red-400 transition-all">
                                <ThumbsDown size={13} />
                            </button>
                        </div>

                        {/* Canvas — Fabric.js mounts here */}
                        <div
                            className="flex-1 relative overflow-hidden"
                            style={{
                                background: '#ffffff',
                                cursor: isDrawer ? 'crosshair' : 'default',
                                position: 'relative',
                                width: '100%',
                                height: '100%',
                                zIndex: 1
                            }}
                        >
                            <canvas
                                ref={canvasRef}
                                style={{
                                    display: 'block',
                                    width: '100%',
                                    height: '100%',
                                    position: 'absolute',
                                    top: 0,
                                    left: 0,
                                    zIndex: 1,
                                    pointerEvents: 'auto'
                                }}
                                className="touch-none"
                            />

                            {gameState.phase === 'ROUND_END' && (
                                <div className="absolute inset-0 flex items-center justify-center bg-slate-950/80 backdrop-blur-sm z-10">
                                    <div className="text-center bg-slate-900 border border-white/10 rounded-3xl p-8 shadow-2xl">
                                        <p className="text-gray-500 text-sm uppercase tracking-widest mb-2">The word was</p>
                                        <p className="text-4xl font-black text-blue-300 mb-4">{gameState.word}</p>
                                        <p className="text-gray-500 text-sm">Next round starting soon…</p>
                                    </div>
                                </div>
                            )}

                            {gameState.phase === 'GAME_OVER' && (
                                <div className="absolute inset-0 flex items-center justify-center bg-slate-950/90 backdrop-blur-sm z-10">
                                    <div className="text-center bg-slate-900 border border-white/10 rounded-3xl p-10 shadow-2xl">
                                        <p className="text-yellow-400 text-4xl mb-3">🏆</p>
                                        <p className="text-3xl font-black text-white mb-2">Game Over!</p>
                                        <p className="text-gray-400 text-sm">
                                            Winner: <span className="text-blue-300 font-bold">{sortedPlayers[0]?.playerName}</span>
                                        </p>
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                </div>

                {/* Right: Chat */}
                <aside className="w-56 shrink-0 flex flex-col bg-slate-900/40 border-l border-white/5">
                    <div className="flex-1 overflow-y-auto px-3 py-3 flex flex-col gap-1.5">
                        {messages.length === 0 && (
                            <p className="text-xs text-gray-700 text-center py-4">Chat will appear here</p>
                        )}
                        {messages.map((msg) => (
                            <ChatMessage key={msg.id} msg={msg} />
                        ))}
                        <div ref={chatEndRef} />
                    </div>

                    <div className="p-3 border-t border-white/5 shrink-0">
                        <div className="flex gap-2">
                            <input
                                type="text"
                                value={guessInput}
                                onChange={(e) => setGuessInput(e.target.value)}
                                onKeyDown={(e) => e.key === 'Enter' && handleSendGuess()}
                                disabled={isDrawer}
                                placeholder={isDrawer ? "You're drawing…" : 'Type your guess…'}
                                className="flex-1 min-w-0 bg-slate-950/80 border border-white/10 rounded-xl px-3 py-2 text-xs placeholder:text-gray-700 focus:outline-none focus:border-blue-500 transition-colors disabled:opacity-40"
                            />
                            <button
                                onClick={handleSendGuess}
                                disabled={isDrawer || !guessInput.trim()}
                                className="p-2 rounded-xl bg-blue-600 hover:bg-blue-500 disabled:opacity-30 disabled:cursor-not-allowed transition-all"
                            >
                                <Send size={14} />
                            </button>
                        </div>
                    </div>
                </aside>
            </div>
        </div>
    );
};

export default GamePage;