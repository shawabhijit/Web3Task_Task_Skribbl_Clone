import React from 'react'

export const Button = ({ children, variant = 'primary', className = '', ...props }) => {
    const base = "px-8 py-4 rounded-2xl font-bold transition-all duration-300 transform hover:scale-[1.02] active:scale-[0.98] shadow-lg flex items-center justify-center gap-2";
    const variants = {
        primary: "bg-gradient-to-r from-green-500 to-emerald-600 shadow-green-500/20 text-white border border-green-400/20 hover:shadow-green-500/40",
        secondary: "bg-white/10 backdrop-blur-md border border-white/20 text-white hover:bg-white/20 hover:border-white/40"
    };
    return <button className={`${base} ${variants[variant]} ${className}`} {...props}>{children}</button>;
};
