// api/roomApi.js

const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1';

export class ApiError extends Error {
    constructor(status, message, fieldErrors) {
        super(message);
        this.status = status;
        this.fieldErrors = fieldErrors;
    }
}

async function handleResponse(res) {
    if (res.ok) return res.json();
    const body = await res.json().catch(() => ({}));
    throw new ApiError(res.status, body.message ?? 'Request failed', body.fieldErrors);
}

export const roomApi = {
    createRoom: (req) =>
        fetch(`${BASE_URL}/rooms`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req),
        }).then(handleResponse),

    joinRoom: (req) =>
        fetch(`${BASE_URL}/rooms/join`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req),
        }).then(handleResponse),

    getRoom: (roomCode) =>
        fetch(`${BASE_URL}/rooms/${roomCode}`).then(handleResponse),

    getPublicRooms: () =>
        fetch(`${BASE_URL}/rooms/public`).then(handleResponse),

    leaveRoom: (roomCode, playerId) =>
        fetch(`${BASE_URL}/rooms/${roomCode}/leave?playerId=${playerId}`, {
            method: 'DELETE',
        }).then((res) => {
            if (!res.ok) throw new ApiError(res.status, 'Leave failed');
        }),

    startGame: (roomCode, playerId) =>
        fetch(`${BASE_URL}/rooms/${roomCode}/start?playerId=${playerId}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
        }).then(handleResponse),
};