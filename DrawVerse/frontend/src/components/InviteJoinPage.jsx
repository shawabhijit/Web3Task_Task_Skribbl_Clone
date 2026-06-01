import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Gamepad2, User } from 'lucide-react';
import { roomApi } from './api/roomApi';
import { getOrCreatePlayerId } from './utils/PlayerIdetity';

const InviteJoinPage = () => {
    const { roomCode } = useParams();
    const navigate = useNavigate();
    const [playerName, setPlayerName] = useState(localStorage.getItem('skribbl_name') || '');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const handleJoin = async (e) => {
        e.preventDefault();

        if (!playerName.trim()) {
            setError('Please enter your name');
            return;
        }

        setLoading(true);
        setError(null);

        try {
            const playerId = getOrCreatePlayerId();

            // Store the name
            localStorage.setItem('skribbl_name', playerName);

            // Join the room
            await roomApi.joinRoom({
                playerId,
                playerName: playerName.trim(),
                roomCode: roomCode.toUpperCase(),
            });

            // Redirect to game
            navigate(`/game/${roomCode.toUpperCase()}`);
        } catch (err) {
            setError(err.message || 'Failed to join room. The room may be full or closed.');
            console.error('Join error:', err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 flex items-center justify-center p-4">
            <div className="w-full max-w-md">
                {/* Header */}
                <div className="text-center mb-8">
                    <div className="flex justify-center mb-4">
                        <div className="p-4 bg-blue-500/20 rounded-2xl">
                            <Gamepad2 className="w-8 h-8 text-blue-400" />
                        </div>
                    </div>
                    <h1 className="text-3xl font-black text-white mb-2">DrawVerse</h1>
                    <p className="text-gray-400">You've been invited to play!</p>
                </div>

                {/* Join Form */}
                <form onSubmit={handleJoin} className="space-y-4">
                    {/* Name Input */}
                    <div>
                        <label className="block text-sm font-semibold text-gray-300 mb-2">
                            <div className="flex items-center gap-2">
                                <User size={16} />
                                Your Name
                            </div>
                        </label>
                        <input
                            type="text"
                            value={playerName}
                            onChange={(e) => setPlayerName(e.target.value)}
                            placeholder="Enter your name"
                            maxLength="20"
                            className="w-full bg-slate-900/80 border border-white/10 rounded-xl px-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:border-blue-500 transition-colors"
                            disabled={loading}
                        />
                    </div>

                    {/* Error Message */}
                    {error && (
                        <div className="p-3 bg-red-500/10 border border-red-500/30 rounded-xl">
                            <p className="text-sm text-red-400">{error}</p>
                        </div>
                    )}

                    {/* Room Code Display */}
                    <div className="p-4 bg-white/5 border border-white/10 rounded-xl">
                        <p className="text-xs text-gray-500 uppercase tracking-wider mb-1">Room Code</p>
                        <p className="text-2xl font-black text-blue-400 font-mono">{roomCode.toUpperCase()}</p>
                    </div>

                    {/* Play Button */}
                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full py-3 px-4 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-600/50 disabled:cursor-not-allowed text-white font-bold rounded-xl transition-colors flex items-center justify-center gap-2 mt-6"
                    >
                        {loading ? (
                            <>
                                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                                Joining...
                            </>
                        ) : (
                            <>
                                <Gamepad2 size={18} />
                                Play
                            </>
                        )}
                    </button>
                </form>

                {/* Footer */}
                <p className="text-center text-xs text-gray-600 mt-6">
                    Your name will be saved for future games
                </p>
            </div>
        </div>
    );
};

export default InviteJoinPage;
