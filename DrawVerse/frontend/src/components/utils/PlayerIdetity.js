// Generates and persists a UUID for this browser so the player can
// reconnect to the same lobby slot after a tab refresh.

function generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
        const r = (Math.random() * 16) | 0;
        const v = c === 'x' ? r : (r & 0x3) | 0x8;
        return v.toString(16);
    });
}

export function getOrCreatePlayerId() {
    let id = localStorage.getItem('skribbl_player_id');
    if (!id) {
        id = generateUUID();
        localStorage.setItem('skribbl_player_id', id);
    }
    return id;
}