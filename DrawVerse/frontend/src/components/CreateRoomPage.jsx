import React, { useState } from 'react';
import {
    Users, Clock, RotateCcw, Gamepad2, BookOpen,
    Lightbulb, Link2, Play, ChevronDown, Globe, Type
} from 'lucide-react';
import { roomApi } from './api/roomApi';
import { getOrCreatePlayerId } from './utils/PlayerIdetity';


const SettingRow = ({ icon: Icon, label, value, onChange, options }) => (
    <div className="flex items-center gap-4 py-4 border-b border-white/5 last:border-0">
        <div className="flex items-center gap-3 w-40 shrink-0">
            <div className="p-2 rounded-xl bg-blue-500/10 text-blue-400">
                <Icon size={16} />
            </div>
            <span className="text-sm font-semibold text-gray-300">{label}</span>
        </div>
        <div className="relative flex-1">
            <select
                value={value}
                onChange={(e) => onChange(e.target.value)}
                className="w-full appearance-none bg-slate-950/80 border border-white/10 rounded-xl px-4 py-2.5 text-white text-sm font-medium focus:outline-none focus:border-blue-500 transition-colors cursor-pointer pr-10"
            >
                {options.map((opt) => (
                    <option key={opt.value} value={opt.value} className="bg-slate-900">
                        {opt.label}
                    </option>
                ))}
            </select>
            <ChevronDown size={14} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 pointer-events-none" />
        </div>
    </div>
);

// ─── CreateRoomPage ────────────────────────────────────────────────────────────
const CreateRoomPage = ({ playerName: initialName, navigate }) => {
    const [playerName, setPlayerName] = useState(localStorage.getItem('skribbl_name') || '');
    const [nameError, setNameError] = useState('');
    const [settings, setSettings] = useState({
        maxPlayers: '2',
        language: 'English',
        drawTimeSeconds: '80',
        rounds: '3',
        wordMode: 'NORMAL',
        wordCount: '3',
        hints: '2',
        customWords: '',
        useCustomOnly: false,
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const playerId = getOrCreatePlayerId();

    const set = (key) => (val) => setSettings((s) => ({ ...s, [key]: val }));

    const handleStart = async () => {
        // Validate name
        if (!playerName.trim()) {
            setNameError('Please enter your name to create a room');
            return;
        }

        if (playerName.trim().length < 2) {
            setNameError('Name must be at least 2 characters');
            return;
        }

        setLoading(true);
        setError(null);
        setNameError('');
        try {
            const room = await roomApi.createRoom({
                playerName: playerName.trim(),
                playerId,
                roomType: 'PRIVATE',
                maxPlayers: Number(settings.maxPlayers),
                rounds: Number(settings.rounds),
                drawTimeSeconds: Number(settings.drawTimeSeconds),
                wordCount: Number(settings.wordCount),
                hints: Number(settings.hints),
                wordMode: settings.wordMode,
            });
            // Save name to localStorage
            localStorage.setItem('skribbl_name', playerName.trim());
            // Navigate to game
            window.location.href = `/game/${room.roomCode}`;
        } catch (err) {
            setError(err.message || 'Failed to create room.');
        } finally {
            setLoading(false);
        }
    };

    const handleCopyInvite = () => {
        const url = `${window.location.origin}/join`;
        navigator.clipboard.writeText(url).catch(() => { });
    };

    return (
        <div className="min-h-screen bg-slate-950 text-white font-sans overflow-x-hidden">

            {/* Ambient background blobs */}
            <div className="fixed inset-0 pointer-events-none overflow-hidden">
                <div className="absolute -top-40 -left-40 w-96 h-96 bg-blue-600/10 rounded-full blur-3xl" />
                <div className="absolute -bottom-40 -right-40 w-96 h-96 bg-purple-600/10 rounded-full blur-3xl" />
            </div>

            <div className="relative z-10 flex flex-col items-center justify-center min-h-screen px-4 py-12">

                {/* Header */}
                <div className="text-center mb-8">
                    <h1 className="text-5xl md:text-6xl font-black tracking-tighter mb-1">
                        <span className="text-transparent bg-clip-text bg-gradient-to-br from-blue-400 to-purple-600">Draw</span>
                        <span className="text-transparent bg-clip-text bg-gradient-to-br from-orange-400 to-red-500">Verse</span>
                    </h1>
                    <p className="text-gray-500 text-sm font-medium tracking-wide">Configure your private room</p>
                </div>

                <div className="w-full max-w-2xl">
                    {/* Round + player info bar */}
                    <div className="flex items-center gap-3 mb-3 px-1">
                        <div className="flex items-center gap-2 px-3 py-1.5 bg-white/5 rounded-full border border-white/10">
                            <div className="w-2 h-2 rounded-full bg-blue-400 animate-pulse" />
                            <span className="text-xs font-bold text-gray-400">Round 1 of {settings.rounds}</span>
                        </div>
                        <div className="flex items-center gap-2 px-3 py-1.5 bg-white/5 rounded-full border border-white/10">
                            <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">Waiting</span>
                        </div>
                    </div>

                    {/* Main card */}
                    <div className="bg-slate-900/60 backdrop-blur-2xl border border-white/10 rounded-[24px] shadow-[0_0_50px_rgba(0,0,0,0.4)] overflow-hidden">

                        {/* Name Input Section */}
                        <div className="px-6 py-5 border-b border-white/5 bg-white/[0.02]">
                            <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-2">Your Name</label>
                            <input
                                type="text"
                                value={playerName}
                                onChange={(e) => {
                                    setPlayerName(e.target.value);
                                    setNameError('');
                                }}
                                placeholder="Enter your name"
                                maxLength="20"
                                className="w-full bg-slate-950/80 border border-white/10 rounded-xl px-4 py-2.5 text-white placeholder-gray-600 focus:outline-none focus:border-blue-500 transition-colors text-sm"
                            />
                            {nameError && (
                                <p className="mt-2 text-xs text-red-400 flex items-center gap-1">
                                    <span>⚠️</span> {nameError}
                                </p>
                            )}
                        </div>

                        {/* Player panel — top strip */}
                        <div className="flex items-center gap-4 px-6 py-4 border-b border-white/5 bg-white/[0.02]">
                            <div className="flex items-center gap-3">
                                <div className="relative">
                                    <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-lg">
                                        👑
                                    </div>
                                    <div className="absolute -top-1 -right-1 w-3.5 h-3.5 bg-yellow-400 rounded-full border-2 border-slate-900 flex items-center justify-center">
                                        <span className="text-[6px]">★</span>
                                    </div>
                                </div>
                                <div>
                                    <p className="text-sm font-bold text-blue-300">{playerName || 'Your Name'} <span className="text-gray-500 font-normal">(You)</span></p>
                                    <p className="text-xs text-gray-600">Room Owner</p>
                                </div>
                            </div>
                            <div className="ml-auto text-xs text-green-400 font-semibold flex items-center gap-1.5 px-3 py-1.5 bg-green-500/10 rounded-full border border-green-500/20">
                                <span className="w-1.5 h-1.5 bg-green-400 rounded-full" />
                                1 / {settings.maxPlayers} players
                            </div>
                        </div>

                        {/* Settings */}
                        <div className="px-6 py-2">
                            <SettingRow icon={Users} label="Players" value={settings.maxPlayers} onChange={set('maxPlayers')} options={[2, 3, 4, 5, 6, 7, 8, 10, 12, 15, 20].map(n => ({ value: String(n), label: String(n) }))} />
                            <SettingRow icon={Globe} label="Language" value={settings.language} onChange={set('language')} options={['English', 'Deutsch', 'Español', 'Français', 'Polski'].map(l => ({ value: l, label: l }))} />
                            <SettingRow icon={Clock} label="Draw Time" value={settings.drawTimeSeconds} onChange={set('drawTimeSeconds')} options={[15, 30, 45, 60, 80, 100, 120, 150, 180, 240].map(n => ({ value: String(n), label: `${n}s` }))} />
                            <SettingRow icon={RotateCcw} label="Rounds" value={settings.rounds} onChange={set('rounds')} options={[2, 3, 4, 5, 6, 7, 8, 9, 10].map(n => ({ value: String(n), label: String(n) }))} />
                            <SettingRow icon={Gamepad2} label="Game Mode" value={settings.wordMode} onChange={set('wordMode')} options={[{ value: 'NORMAL', label: 'Normal' }, { value: 'HIDDEN', label: 'Hidden' }, { value: 'COMBINATION', label: 'Combination' }]} />
                            <SettingRow icon={BookOpen} label="Word Count" value={settings.wordCount} onChange={set('wordCount')} options={[1, 2, 3, 4, 5].map(n => ({ value: String(n), label: String(n) }))} />
                            <SettingRow icon={Lightbulb} label="Hints" value={settings.hints} onChange={set('hints')} options={[0, 1, 2, 3, 4, 5].map(n => ({ value: String(n), label: n === 0 ? 'None' : String(n) }))} />
                        </div>

                        {/* Error */}
                        {error && (
                            <p className="mx-6 mb-3 text-xs text-red-400 text-center bg-red-500/10 border border-red-500/20 rounded-xl px-4 py-2">{error}</p>
                        )}

                        {/* Action buttons */}
                        <div className="flex gap-3 p-4 border-t border-white/5 bg-white/[0.01]">
                            <button
                                onClick={handleStart}
                                disabled={loading}
                                className="flex-1 flex items-center justify-center gap-2 py-3.5 bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-400 hover:to-emerald-500 disabled:opacity-50 disabled:cursor-not-allowed rounded-xl font-black text-base tracking-wide transition-all active:scale-[0.98] shadow-[0_4px_20px_rgba(34,197,94,0.3)]"
                            >
                                <Play size={18} fill="white" />
                                {loading ? 'Creating...' : 'Start!'}
                            </button>
                            <button
                                onClick={handleCopyInvite}
                                className="flex items-center justify-center gap-2 px-6 py-3.5 bg-blue-600 hover:bg-blue-500 rounded-xl font-bold text-sm transition-all active:scale-[0.98] shadow-[0_4px_20px_rgba(59,130,246,0.2)]"
                            >
                                <Link2 size={16} />
                                Invite
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default CreateRoomPage;