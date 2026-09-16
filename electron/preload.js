'use strict';

const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('vidfetch', {
  call: (method, params) => ipcRenderer.invoke('engine:call', method, params),
  restartEngine: () => ipcRenderer.invoke('engine:restart'),

  onEvent: (cb) => {
    const handler = (_e, msg) => cb(msg);
    ipcRenderer.on('engine:event', handler);
    return () => ipcRenderer.removeListener('engine:event', handler);
  },
  onStatus: (cb) => {
    const handler = (_e, status) => cb(status);
    ipcRenderer.on('engine:status', handler);
    return () => ipcRenderer.removeListener('engine:status', handler);
  },
  onMaximized: (cb) => {
    const handler = (_e, val) => cb(val);
    ipcRenderer.on('maximized', handler);
    return () => ipcRenderer.removeListener('maximized', handler);
  },

  minimize: () => ipcRenderer.send('win:minimize'),
  maximize: () => ipcRenderer.send('win:maximize'),
  close: () => ipcRenderer.send('win:close'),
  isMaximized: () => ipcRenderer.invoke('win:isMaximized'),

  openPath: (p) => ipcRenderer.invoke('fs:openPath', p),
  reveal: (p) => ipcRenderer.invoke('fs:reveal', p),
  pickFolder: (defaultPath) => ipcRenderer.invoke('dialog:pickFolder', defaultPath),

  getSettings: () => ipcRenderer.invoke('settings:get'),
  setSettings: (patch) => ipcRenderer.invoke('settings:set', patch),
  readClipboard: () => ipcRenderer.invoke('clipboard:read'),
});
