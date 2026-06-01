import React, { useState } from 'react';
import { Pencil, ChevronLeft, ChevronRight, Dices, HelpCircle, BookOpen, PenTool, Sparkles, Zap, Users, MessageSquare, Trophy, MousePointer2 } from 'lucide-react';
import { Button } from './components/Button';
import { AvatarCarousel } from './components/Landing/AvatarCarousel';
import HeroSection from './components/Landing/HeroSection';
import CreateRoomPage from './components/CreateRoomPage';
import GamePage from './components/GamePage';
import InviteJoinPage from './components/InviteJoinPage';
import { Route, Routes } from 'react-router-dom';


export default function App() {
  return (
    <Routes>
      {/* <CreateRoomPage /> */}
      {/* <GamePage roomCode={8947534985} /> */}

      <Route path="/" element={<HeroSection />} />
      <Route
        path="/create"
        element={<CreateRoomPage playerName={name} />}
      />
      <Route path="/invite/:roomCode" element={<InviteJoinPage />} />
      <Route path="/game/:roomCode" element={<GamePage />} />
    </Routes>
  );
}