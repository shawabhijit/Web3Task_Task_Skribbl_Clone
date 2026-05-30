import React, { useState } from 'react';
import { HelpCircle, BookOpen, PenTool, Users, Pencil, MessageSquare, Trophy, MousePointer2 } from 'lucide-react';
import { AvatarCarousel } from './AvatarCarousel';
import { Button } from '../Button';
import { roomApi, ApiError } from '../api/roomApi';
import { getOrCreatePlayerId } from '../utils/PlayerIdetity';

const HeroSection = () => {
    const [nickname, setNickname] = useState('');
    const [avatar, setAvatar] = useState('🎨');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const playerId = getOrCreatePlayerId();

    // ---- Validation ----
    const validateNickname = () => {
        const trimmed = nickname.trim();
        if (!trimmed) { setError('Please enter a nickname before playing.'); return null; }
        if (trimmed.length < 2) { setError('Nickname must be at least 2 characters.'); return null; }
        if (trimmed.length > 30) { setError('Nickname cannot exceed 30 characters.'); return null; }
        return trimmed;
    };

    // ---- Join a random public room ----
    const handlePlayPublic = async () => {
        const trimmedName = validateNickname();
        if (!trimmedName) return;

        setLoading('public');
        setError(null);

        try {
            // 1. Fetch open public rooms
            const publicRooms = await roomApi.getPublicRooms();

            let room;

            if (publicRooms.length > 0) {
                // 2a. Join the first available public room
                const targetRoom = publicRooms[0];
                room = await roomApi.joinRoom({
                    playerName: trimmedName,
                    playerId,
                    roomCode: targetRoom.roomCode,
                });
            } else {
                // 2b. No open rooms — auto-create a public one
                room = await roomApi.createRoom({
                    playerName: trimmedName,
                    playerId,
                    roomType: 'PUBLIC',
                    // Default settings — server fills in the rest
                });
            }

            // 3. Navigate to lobby
            window.location.href = `/lobby/${room.roomCode}`;
        } catch (err) {
            if (err instanceof ApiError) {
                setError(err.message);
            } else {
                setError('Could not connect to the server. Please try again.');
            }
        } finally {
            setLoading(false);
        }
    };

    // ---- Create a private room ----
    const handleCreatePrivate = async () => {
        const trimmedName = validateNickname();
        if (!trimmedName) return;

        setLoading('private');
        setError(null);

        try {
            const room = await roomApi.createRoom({
                playerName: trimmedName,
                playerId,
                roomType: 'PRIVATE',
            });

            // Navigate to lobby — host will see the invite link there
            window.location.href = `/lobby/${room.roomCode}`;
        } catch (err) {
            if (err instanceof ApiError) {
                setError(err.message);
            } else {
                setError('Could not create room. Please try again.');
            }
        } finally {
            setLoading(false);
        }
    };

    
    return (
        <div className="min-h-screen bg-slate-950 text-white font-sans selection:bg-blue-500/30 overflow-x-hidden">

            <main className="relative z-10 flex flex-col items-center justify-center min-h-screen">
                <div className="absolute top-6 right-6 flex items-center gap-2 px-4 py-2 bg-green-500/10 border border-green-500/20 rounded-full text-green-400 text-sm font-bold">
                    <span className="relative flex h-2 w-2">
                        <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span>
                        <span className="relative inline-flex rounded-full h-2 w-2 bg-green-500"></span>
                    </span>
                    4,231 Online
                </div>

                <div className="text-center mb-8 space-y-1">
                    <h1 className="text-6xl md:text-8xl font-black tracking-tighter filter drop-shadow-[0_0_15px_rgba(59,130,246,0.5)]">
                        <span className="text-transparent bg-clip-text bg-gradient-to-br from-blue-400 to-purple-600">Draw</span>
                        <span className="text-transparent bg-clip-text bg-gradient-to-br from-orange-400 to-red-500">Verse</span>
                    </h1>
                    <p className="text-blue-200/60 text-base font-medium tracking-wide italic">The World's Best Multiplayer Drawing Arena</p>
                </div>

                <div className="w-full max-w-lg bg-slate-900/60 backdrop-blur-2xl border border-white/10 p-6 md:p-8 rounded-[24px] shadow-[0_0_50px_rgba(0,0,0,0.3)]">
                    <div className="flex gap-2 mb-6">
                        <input
                            type="text"
                            placeholder="Enter your nickname..."
                            value={nickname}
                            onChange={(e) => { setNickname(e.target.value); setError(null); }}
                            maxLength={30}
                            className="w-full bg-slate-950/50 border border-white/10 rounded-xl px-5 py-3 focus:outline-none focus:border-blue-500 transition-all placeholder:text-gray-600"
                        />
                        <select className="bg-slate-950/50 border border-white/10 rounded-xl px-4 py-3 focus:outline-none cursor-pointer">
                            <option>English</option>
                            <option>Deutsch</option>
                            <option>Español</option>
                        </select>
                    </div>

                    <AvatarCarousel onAvatarChange={setAvatar} />

                    {/* Error message */}
                    {error && (
                        <p className="mt-3 text-sm text-red-400 text-center">{error}</p>
                    )}

                    <div className="mt-4 flex gap-3">
                        <Button
                            className="text-base"
                            onClick={handlePlayPublic}
                            disabled={!!loading}
                        >
                            {loading === 'public' ? 'Finding room...' : 'Play Public Room'}
                        </Button>
                        <Button
                            variant="secondary"
                            onClick={handleCreatePrivate}
                            disabled={!!loading}
                        >
                            {loading === 'private' ? 'Creating...' : 'Create Private Room'}
                        </Button>
                    </div>
                </div>
            </main>

            <section className="relative z-10 py-24 px-6">
                <div className="max-w-6xl mx-auto grid grid-cols-1 md:grid-cols-3 gap-8">
                    {/* About */}
                    <div className="p-8 bg-slate-900/40 backdrop-blur-xl border border-white/5 rounded-3xl">
                        <div className="flex items-center gap-3 mb-6">
                            <HelpCircle className="text-blue-400" size={28} />
                            <h3 className="text-2xl font-bold">About</h3>
                        </div>
                        <p className="text-gray-400 leading-relaxed mb-4">DrawVerse is a real-time multiplayer drawing and guessing game. Players take turns drawing hidden words while others race to guess them and earn points.</p>
                        <p className="text-gray-500 text-sm">Join thousands of players worldwide in this exciting social drawing experience!</p>
                    </div>

                    {/* News */}
                    <div className="p-8 bg-slate-900/40 backdrop-blur-xl border border-white/5 rounded-3xl">
                        <div className="flex items-center gap-3 mb-6">
                            <BookOpen className="text-blue-400" size={28} />
                            <h3 className="text-2xl font-bold">News</h3>
                        </div>
                        <div className="space-y-6">
                            {[
                                { v: 'v2.5.0', title: 'New Drawing Tools', desc: 'Added spray paint, gradient brush, and texture fills!' },
                                { v: 'v2.4.3', title: 'Multiplayer Improvements', desc: 'Reduced latency and improved real-time synchronization.' },
                            ].map((item, i) => (
                                <div key={i}>
                                    <div className="flex justify-between text-xs text-gray-500 mb-1"><span>{item.v}</span><span>2025-08-15</span></div>
                                    <h4 className="font-bold text-sm text-blue-300">{item.title}</h4>
                                    <p className="text-xs text-gray-400">{item.desc}</p>
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* How To Play */}
                    <div className="p-8 bg-slate-900/40 backdrop-blur-xl border border-white/5 rounded-3xl">
                        <div className="flex items-center gap-3 mb-6">
                            <PenTool className="text-blue-400" size={28} />
                            <h3 className="text-2xl font-bold">How To Play</h3>
                        </div>
                        <div className="space-y-5">
                            {[
                                { icon: Users, title: 'Join a Room', text: 'Enter your nickname and join a public or private room' },
                                { icon: Pencil, title: 'Draw Your Word', text: "When it's your turn, draw the secret word given to you" },
                                { icon: MessageSquare, title: 'Guess & Chat', text: 'Type your guesses in the chat while others draw' },
                                { icon: Trophy, title: 'Score Points', text: 'Earn points for correct guesses and fast answers' },
                                { icon: MousePointer2, title: 'Win the Game', text: 'Player with the most points at the end wins!' },
                            ].map((step, i) => {
                                const IconComponent = step.icon;
                                return (
                                    <div key={i} className="flex gap-4 items-start">
                                        <div className="mt-1 bg-blue-500/20 p-1.5 rounded-full text-blue-400"><IconComponent size={16} /></div>
                                        <div>
                                            <h4 className="font-bold text-sm">{step.title}</h4>
                                            <p className="text-xs text-gray-500">{step.text}</p>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                </div>
            </section>

            <footer className="relative z-10 py-12 text-center border-t border-white/5 bg-slate-950">
                <div className="flex justify-center gap-8 text-gray-500 text-sm font-medium mb-6">
                    <a href="#" className="hover:text-white">Contact</a>
                    <a href="#" className="hover:text-white">Terms of Service</a>
                    <a href="#" className="hover:text-white">Privacy Policy</a>
                </div>
                <p className="text-gray-700 text-sm">Made with ❤️ © 2026 DrawVerse.io - All rights reserved.</p>
            </footer>
        </div>
    );
};

export default HeroSection;