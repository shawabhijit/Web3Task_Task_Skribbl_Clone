// hooks/useDrawingCanvas.js
// Fabric.js canvas with full real-time drawing sync over WebSocket.

import { useCallback, useEffect, useRef, useState } from 'react';

export function useDrawingCanvas({ stompClient, roomCode, isDrawer }) {
    const canvasRef = useRef(null);
    const fabricRef = useRef(null);
    const historyRef = useRef([]);
    const stompRef = useRef(stompClient);
    const isDrawerRef = useRef(isDrawer);

    const [isReady, setIsReady] = useState(false);
    const [color, setColorState] = useState('#000000');
    const [brushSize, setBrushSizeState] = useState(5);
    const [isEraser, setIsEraser] = useState(false);

    useEffect(() => {
        stompRef.current = stompClient;
    }, [stompClient]);

    useEffect(() => {
        isDrawerRef.current = isDrawer;
        if (!fabricRef.current) return;
        fabricRef.current.isDrawingMode = isDrawer;
        // Ensure upper canvas is interactive when drawer
        if (fabricRef.current.upperCanvasEl) {
            fabricRef.current.upperCanvasEl.style.pointerEvents = isDrawer ? 'auto' : 'none';
        }
        fabricRef.current.renderAll();
    }, [isDrawer]);

    const initCanvas = useCallback(() => {
        if (!canvasRef.current || fabricRef.current) return;

        const attemptInit = () => {
            import('fabric').then((fabricModule) => {
                let fabric = null;
                if (fabricModule.fabric) {
                    fabric = fabricModule.fabric;
                } else if (fabricModule.default) {
                    fabric = fabricModule.default;
                } else {
                    fabric = fabricModule;
                }

                if (!fabric?.Canvas) {
                    console.error('Fabric Canvas not found');
                    return;
                }

                const container = canvasRef.current?.parentElement;
                let w = container?.clientWidth || 800;
                let h = container?.clientHeight || 600;

                // Wait if container is truly not sized (but allow narrow canvas for toolbar areas)
                if ((w === 0 || h === 0) || (w < 50 || h < 50)) {
                    console.log('[Canvas] Container not ready, waiting...', { w, h });
                    setTimeout(attemptInit, 100);
                    return;
                }

                console.log('[Canvas] Initializing with container size', { w, h });

                // Set canvas ELEMENT attributes (these are critical for Fabric.js)
                canvasRef.current.setAttribute('width', w);
                canvasRef.current.setAttribute('height', h);
                canvasRef.current.width = w;
                canvasRef.current.height = h;

                // Set canvas element CSS for proper rendering
                canvasRef.current.style.display = 'block';
                canvasRef.current.style.width = '100%';
                canvasRef.current.style.height = '100%';
                canvasRef.current.style.position = 'absolute';
                canvasRef.current.style.top = '0';
                canvasRef.current.style.left = '0';
                canvasRef.current.style.zIndex = '1';
                canvasRef.current.style.pointerEvents = 'auto'; // CRITICAL: Must be auto to receive events
                canvasRef.current.style.touchAction = 'none';
                canvasRef.current.className = 'touch-none';

                const canvas = new fabric.Canvas(canvasRef.current, {
                    backgroundColor: '#ffffff',
                    selection: false,
                    preserveObjectStacking: true,
                    width: w,
                    height: h,
                    renderOnAddRemove: true,
                    enablePointerEvents: true,
                });

                // Initialize drawing mode
                canvas.isDrawingMode = isDrawerRef.current;

                // Create PencilBrush for Fabric v7
                if (fabric.PencilBrush) {
                    canvas.freeDrawingBrush = new fabric.PencilBrush(canvas);
                    canvas.freeDrawingBrush.color = '#000000';
                    canvas.freeDrawingBrush.width = 5;
                    console.log('[Canvas] PencilBrush created successfully');
                }

                // Ensure upper canvas (the interactive one) has proper events
                if (canvas.upperCanvasEl) {
                    canvas.upperCanvasEl.setAttribute('width', w);
                    canvas.upperCanvasEl.setAttribute('height', h);
                    canvas.upperCanvasEl.style.position = 'absolute';
                    canvas.upperCanvasEl.style.top = '0';
                    canvas.upperCanvasEl.style.left = '0';
                    canvas.upperCanvasEl.style.zIndex = '10';
                    canvas.upperCanvasEl.style.width = '100%';
                    canvas.upperCanvasEl.style.height = '100%';
                    canvas.upperCanvasEl.style.pointerEvents = 'auto'; // CRITICAL
                    canvas.upperCanvasEl.style.touchAction = 'none';
                    canvas.upperCanvasEl.style.display = 'block';
                    canvas.upperCanvasEl.style.cursor = 'crosshair';
                    console.log('[Canvas] Upper canvas configured', {
                        width: canvas.upperCanvasEl.width,
                        height: canvas.upperCanvasEl.height
                    });
                }

                // Ensure lower canvas also properly sized
                if (canvas.lowerCanvasEl) {
                    canvas.lowerCanvasEl.setAttribute('width', w);
                    canvas.lowerCanvasEl.setAttribute('height', h);
                    canvas.lowerCanvasEl.style.position = 'absolute';
                    canvas.lowerCanvasEl.style.top = '0';
                    canvas.lowerCanvasEl.style.left = '0';
                    canvas.lowerCanvasEl.style.zIndex = '0';
                    canvas.lowerCanvasEl.style.width = '100%';
                    canvas.lowerCanvasEl.style.height = '100%';
                    canvas.lowerCanvasEl.style.display = 'block';
                }

                // Render loop to keep canvas responsive
                let animationId = null;
                const renderLoop = () => {
                    canvas.renderAll();
                    animationId = requestAnimationFrame(renderLoop);
                };
                renderLoop();

                canvas.on('path:created', (opt) => {
                    historyRef.current.push(JSON.stringify(canvas.toJSON()));

                    if (!isDrawerRef.current) return;

                    const client = stompRef.current;
                    if (!client?.connected) {
                        console.warn('[Canvas] STOMP not connected');
                        return;
                    }

                    const pathData = opt.path.toJSON();
                    pathData._canvasW = canvas.getWidth();
                    pathData._canvasH = canvas.getHeight();

                    client.publish({
                        destination: `/app/room.${roomCode}.draw`,
                        body: JSON.stringify({ type: 'PATH', path: pathData }),
                    });
                });

                fabricRef.current = canvas;
                console.log('[Canvas] Ready', { isDrawer: isDrawerRef.current, w, h });
                setIsReady(true);
            }).catch(err => console.error('[Canvas] Init failed', err));
        };

        // Use setTimeout to allow layout to complete
        setTimeout(attemptInit, 50);
    }, [roomCode]);


    const renderRemotePath = useCallback((pathData) => {
        const canvas = fabricRef.current;
        if (!canvas) return;

        import('fabric').then((fabricModule) => {
            const fabric = fabricModule.fabric || fabricModule.default || fabricModule;
            const scaleX = pathData._canvasW ? canvas.getWidth() / pathData._canvasW : 1;
            const scaleY = pathData._canvasH ? canvas.getHeight() / pathData._canvasH : 1;

            const applyPath = (path) => {
                path.scaleX = scaleX;
                path.scaleY = scaleY;
                path.selectable = false;
                path.evented = false;
                canvas.add(path);
                canvas.renderAll();
            };

            if (fabric.Path.fromObject.length >= 2) {
                fabric.Path.fromObject(pathData, applyPath);
            } else {
                fabric.Path.fromObject(pathData).then(applyPath);
            }
        });
    }, []);

    const clearCanvas = useCallback((fromServer = false) => {
        const canvas = fabricRef.current;
        if (!canvas) return;

        canvas.clear();
        canvas.setBackgroundColor('#ffffff', canvas.renderAll.bind(canvas));
        historyRef.current = [];

        if (!fromServer && isDrawerRef.current && stompRef.current?.connected) {
            stompRef.current.publish({
                destination: `/app/room.${roomCode}.canvas_clear`,
                body: JSON.stringify({}),
            });
        }
    }, [roomCode]);

    const undo = useCallback(() => {
        const canvas = fabricRef.current;
        if (!canvas || historyRef.current.length === 0) return;

        historyRef.current.pop();
        const snapshot = historyRef.current[historyRef.current.length - 1];

        import('fabric').then(() => {
            if (snapshot) {
                canvas.loadFromJSON(snapshot, () => canvas.renderAll());
            } else {
                canvas.clear();
                canvas.setBackgroundColor('#ffffff', canvas.renderAll.bind(canvas));
            }
        });

        if (isDrawerRef.current && stompRef.current?.connected) {
            stompRef.current.publish({
                destination: `/app/room.${roomCode}.draw_undo`,
                body: JSON.stringify({}),
            });
        }
    }, [roomCode]);

    const setColor = useCallback((newColor) => {
        console.log('[Canvas] Setting color to', newColor);
        setColorState(newColor);
        setIsEraser(false);
        if (fabricRef.current?.freeDrawingBrush) {
            fabricRef.current.freeDrawingBrush.color = newColor;
        }
    }, []);

    const setBrushSize = useCallback((size) => {
        console.log('[Canvas] Setting brush size to', size);
        setBrushSizeState(size);
        if (fabricRef.current?.freeDrawingBrush) {
            fabricRef.current.freeDrawingBrush.width = size;
        }
    }, []);

    const toggleEraser = useCallback(() => {
        console.log('[Canvas] Toggling eraser');
        setIsEraser((prev) => {
            const next = !prev;
            if (fabricRef.current?.freeDrawingBrush) {
                fabricRef.current.freeDrawingBrush.color = next ? '#ffffff' : color;
                fabricRef.current.freeDrawingBrush.width = next ? brushSize * 3 : brushSize;
            }
            return next;
        });
    }, [color, brushSize]);

    useEffect(() => {
        const handleResize = () => {
            const canvas = fabricRef.current;
            if (!canvas || !canvasRef.current) return;
            const container = canvasRef.current.parentElement;
            if (container) {
                const w = container.clientWidth;
                const h = container.clientHeight;
                canvasRef.current.width = w;
                canvasRef.current.height = h;
                canvas.setWidth(w);
                canvas.setHeight(h);
                canvas.renderAll();
                console.log('[Canvas] Resized to', { w, h });
            }
        };

        // Handle keyboard events globally
        const handleKeyDown = (e) => {
            const canvas = fabricRef.current;
            if (!canvas) return;

            // Ctrl+Z for undo
            if ((e.ctrlKey || e.metaKey) && e.key === 'z') {
                e.preventDefault();
                console.log('[Canvas] Undo triggered via keyboard');
            }
            // Backspace for clear
            if (e.key === 'Backspace' && isDrawerRef.current) {
                e.preventDefault();
                console.log('[Canvas] Clear triggered via keyboard');
            }
        };

        // Ensure canvas can receive focus
        if (canvasRef.current) {
            canvasRef.current.addEventListener('click', () => {
                if (canvasRef.current) canvasRef.current.focus();
            });
        }

        window.addEventListener('resize', handleResize);
        window.addEventListener('keydown', handleKeyDown);

        return () => {
            window.removeEventListener('resize', handleResize);
            window.removeEventListener('keydown', handleKeyDown);
        };
    }, []);

    useEffect(() => {
        return () => {
            fabricRef.current?.dispose();
            fabricRef.current = null;
        };
    }, []);

    return {
        canvasRef,
        initCanvas,
        setColor,
        setBrushSize,
        undo,
        clearCanvas,
        toggleEraser,
        renderRemotePath,
        isReady,
        isEraser,
        color,
        brushSize,
    };
}