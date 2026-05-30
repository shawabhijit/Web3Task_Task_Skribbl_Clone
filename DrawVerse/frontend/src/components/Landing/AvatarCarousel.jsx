import React from 'react'
import { useState } from 'react';
import { ChevronLeft, ChevronRight, Dices } from 'lucide-react';

const AVATARS = ['🎨', '🚀', '🐱', '🤖', '👻', '😎', '🦄', '🦁'];

export const AvatarCarousel = () => {
    const [index, setIndex] = useState(0);
    const next = () => setIndex((i) => (i + 1) % AVATARS.length);
    const prev = () => setIndex((i) => (i - 1 + AVATARS.length) % AVATARS.length);

    return (
        <div className="relative group p-6 bg-gradient-to-b from-white/10 to-transparent rounded-3xl border border-white/10 flex flex-col items-center">
            <div className="text-6xl mb-6 p-6 bg-slate-900 rounded-full shadow-inner border border-blue-500/30 group-hover:border-blue-400 transition-colors">
                {AVATARS[index]}
            </div>
            <div className="flex gap-4">
                <button onClick={prev} className="p-2 rounded-full bg-white/5 hover:bg-white/20"><ChevronLeft size={20} /></button>
                <button onClick={next} className="p-2 rounded-full bg-blue-600 hover:bg-blue-500"><Dices size={20} /></button>
                <button onClick={next} className="p-2 rounded-full bg-white/5 hover:bg-white/20"><ChevronRight size={20} /></button>
            </div>
        </div>
    );
};
